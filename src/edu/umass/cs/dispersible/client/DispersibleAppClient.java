package edu.umass.cs.dispersible.client;

import edu.umass.cs.dispersible.models.DispersibleAppRequest;
import edu.umass.cs.dispersible.models.DispersibleAppResponse;
import edu.umass.cs.dispersible.models.DispersiblePacketType;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestFuture;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.ReconfigurableAppClientAsync;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ClientReconfigurationPacket;
import edu.umass.cs.reconfiguration.reconfigurationpackets.CreateServiceName;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Sarthak Nandi on 7/5/18.
 */
public class DispersibleAppClient extends ReconfigurableAppClientAsync<DispersibleAppRequest> {

  public DispersibleAppClient() throws IOException {
    super();
  }

  @Override
  public Request getRequest(String stringified) throws RequestParseException {
    try {
      return new DispersibleAppResponse(new JSONObject(stringified));
    } catch (JSONException e) {
      throw new RequestParseException(e);
    }
  }

  @Override
  public Set<IntegerPacketType> getRequestTypes() {
    return DispersiblePacketType.allDispersiblePacketTypes();
  }

  public static void main(String[] args) {
    String serviceName = args[0];
    String initialState = args[1];

    try {
      DispersibleAppClient client = new DispersibleAppClient();
      System.out.println("Sending create service request");
      RequestFuture<ClientReconfigurationPacket> createServiceFuture =
          client.sendRequest(new CreateServiceName(serviceName, initialState));

      ClientReconfigurationPacket response = createServiceFuture.get(20, TimeUnit.SECONDS);

      if (response.isFailed() || !(response instanceof CreateServiceName)) {
        throw new IllegalStateException("Unable to create service, response : " + response.toString());
      }
      System.out.println("Created service, response : " + response.toString());
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    System.exit(0);
  }
}
