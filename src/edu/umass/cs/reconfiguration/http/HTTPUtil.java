package edu.umass.cs.reconfiguration.http;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.StandardCharsets;

/**
 * @author Sarthak Nandi on 10/4/18.
 */
public class HTTPUtil {

  /**
   * Sets all required headers for a response, including those required for CORS.
   *
   * @param response Response to be sent to the client
   * @param keepAlive If the connection is keep alive (true) or not
   */
  public static void setHeaders(FullHttpResponse response, boolean keepAlive) {
    HttpHeaders headers = response.headers();

    // CORS headers, required to access resource in browser from different origin
    headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*"); // Allowing all origins
    headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST"); // Can also add PUT,DELETE,OPTIONS
    headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "X-Requested-With, Content-Type, Content-Length");

    headers.set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

    if (keepAlive) {
      // Add 'Content-Length' header only for a keep-alive connection.
      headers.setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
      // Add keep alive header as per:
      // -
      // http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
      headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }
  }

  /**
   * This method checks if a request is a CORS preflight request by checking if the request method
   * is {@link HttpMethod#OPTIONS}, although that is not the sole property of a CORS preflight request.
   *
   * @param httpMethod {@link HttpMethod} of the request
   * @return True - request is a CORS preflight request, false otherwise
   */
  public static boolean isCORSPreflightRequest(HttpMethod httpMethod) {
    return httpMethod == HttpMethod.OPTIONS;
  }

  /**
   * Sends an empty response containing C
   * @param ctx {@link ChannelHandlerContext} for the request
   * @param keepAlive If the connection is keep alive (true) or not
   */
  public static void handleCORSPreflightRequest(ChannelHandlerContext ctx, boolean keepAlive) {
    FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK);
    setHeaders(httpResponse, keepAlive);

    ChannelFuture channelFuture = ctx.writeAndFlush(httpResponse);
    if (!keepAlive) {
      channelFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  /**
   * Sends the provided response via the provided {@link ChannelHandlerContext}.
   *
   * @param response Response to send
   */
  public static void writeHTTPResponse(ChannelHandlerContext ctx, boolean keepAlive,
                                       String response, HttpResponseStatus status) {
    ByteBuf content = Unpooled.copiedBuffer(response, StandardCharsets.UTF_8);
    FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, status, content);

    setHeaders(httpResponse, keepAlive);

    ChannelFuture channelFuture = ctx.writeAndFlush(httpResponse);
    if (!keepAlive) {
      channelFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }
}
