package edu.umass.cs.reconfiguration;

import static edu.umass.cs.reconfiguration.http.HTTPUtil.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import edu.umass.cs.gigapaxos.interfaces.ClientRequest;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.reconfiguration.ActiveReplica.SenderAndRequest;
import edu.umass.cs.reconfiguration.reconfigurationpackets.EchoRequest;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ReconfigurationPacket.PacketType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Sarthak Nandi on 31/3/18.
 */
public class HTTPActiveReplica implements Closeable {
  private static final Logger log = Logger.getLogger(HTTPActiveReplica.class.getSimpleName());

  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;
  private final Channel channel;
  private final String name;

  private boolean closed;

  HTTPActiveReplica(ActiveReplica activeReplica, InetSocketAddress sockAddr, boolean ssl)
      throws CertificateException, SSLException, InterruptedException {

    SslContext sslCtx = null;
    if (ssl) {
      SelfSignedCertificate ssc = new SelfSignedCertificate();
      sslCtx = SslContextBuilder.forServer(ssc.certificate(),
                                           ssc.privateKey()).build();
    }

    name = activeReplica.toString();

    // Configure the server.
    bossGroup = new NioEventLoopGroup(1); // TODO: number of threads must be configurable
    workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(new HttpActiveReplicaInitializer(sslCtx, activeReplica));

      channel = b.bind(sockAddr).sync().channel();
      log.log(Level.INFO, "{0} ready", new Object[]{this});

      channel.closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  public String toString() {
    return name + ":HTTP:" + this.channel.localAddress().toString();
  }

  private boolean isClosed() {
    return closed;
  }

  /**
   * Shuts down the HTTP channel. It handles multiple calls to close but not at the same time
   * from multiple threads.
   */
  @Override
  public void close() {
    if (isClosed()) {
      return;
    }

    channel.close();
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();

    closed = true;
  }

  private class HttpActiveReplicaInitializer extends
                                             ChannelInitializer<SocketChannel> {
    private final SslContext sslCtx;
    private ActiveReplica activeReplica;

    private HttpActiveReplicaInitializer(SslContext sslCtx,
                                        ActiveReplica activeReplica) {
      this.sslCtx = sslCtx;
      this.activeReplica = activeReplica;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
      ChannelPipeline p = ch.pipeline();
      if (sslCtx != null) {
        p.addLast(sslCtx.newHandler(ch.alloc()));
      }

      p.addLast(new HttpRequestDecoder());

      // Uncomment if you don't want to handle HttpChunks.
      p.addLast(new HttpObjectAggregator(1048576));

      p.addLast(new HttpResponseEncoder());

      p.addLast(new HttpActiveReplicaHandler(activeReplica));

    }
  }

  private class HttpActiveReplicaHandler extends SimpleChannelInboundHandler<Object> {
    private ActiveReplica activeReplica;

    HttpActiveReplicaHandler(ActiveReplica activeReplica) {
      this.activeReplica = activeReplica;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
      long recvTime = System.nanoTime();

      if (!(msg instanceof HttpContent) || !(msg instanceof HttpRequest)) {
        throw new IOException("Unable to read message : " + msg);
      }

      HttpRequest httpRequest = (HttpRequest) msg;
      boolean keepAlive = HttpUtil.isKeepAlive(httpRequest);
      HttpMethod method = httpRequest.method();

      // Message for CORS headers, just return the CORS headers in this message
      if (isCORSPreflightRequest(method)) {
        handleCORSPreflightRequest(ctx, keepAlive);
        return;
      }

      if (!HttpMethod.POST.equals(method)) {
        throw new IOException("Unrecognized method : " + method + " for message : " + msg);
      }

      HttpContent httpContent = (HttpContent) msg;
      Request request = getRequest(httpContent);

      processRequestInApp(ctx, recvTime, keepAlive, httpContent, request);

    }

    private Request getRequest(HttpContent httpContent)
        throws JSONException, UnknownHostException, edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException {
      String data = httpContent.content().toString(StandardCharsets.UTF_8);
      JSONObject json = new JSONObject(data);

      Request request;
      int typeInt;

      if (json.has("TYPE")) {
        typeInt = json.getInt("TYPE");
      } else {
        typeInt = json.getInt("type");
      }

      if (typeInt == PacketType.ECHO_REQUEST.getInt()) {
        request = new EchoRequest(json); // TODO: handle echo request here only ?
      } else {
        // App request
        request = activeReplica.getRequest(json);
      }
      return request;
    }

    private void processRequestInApp(ChannelHandlerContext ctx, long recvTime, boolean keepAlive,
                                     HttpContent httpContent, Request request) {
      Channel channel = ctx.channel();
      // These addresses must be InetSocketAddresses only as its HTTP Request
      InetSocketAddress senderAddress = (InetSocketAddress) channel.remoteAddress();
      InetSocketAddress receiverAddress = (InetSocketAddress) channel.localAddress();

      HttpResponseStatus status = httpContent.decoderResult().isSuccess() ? OK : BAD_REQUEST;

      HttpResponseSender responseSender = new HttpResponseSender(ctx, keepAlive, status);
      SenderAndRequest callback = activeReplica.new SenderAndRequest(request, senderAddress,
                                                                     receiverAddress, recvTime,
                                                                     responseSender);
      activeReplica.handRequestToApp(request, callback);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
      ctx.writeAndFlush(httpResponse);
      log.log(Level.INFO, "Unable to process message", cause);
    }

  }

  private class HttpResponseSender implements ResponseSender {

    private final ChannelHandlerContext ctx;
    private final boolean keepAlive;
    private final HttpResponseStatus status;

    private HttpResponseSender(ChannelHandlerContext ctx, boolean keepAlive,
                               HttpResponseStatus status) {
      this.ctx = ctx;
      this.keepAlive = keepAlive;
      this.status = status;
    }

    /**
     * Sends the response via {@link #ctx}.
     *
     * @param clientResponse Response to be sent
     * @return True - response sent without any exception, false otherwise
     */
    @Override
    public boolean sendResponse(ClientRequest clientResponse) {
      String response = clientResponse.toString();
      writeHTTPResponse(ctx, keepAlive, response, status);

      return true;
    }
  }
}
