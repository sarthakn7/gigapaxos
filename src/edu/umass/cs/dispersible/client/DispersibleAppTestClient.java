package edu.umass.cs.dispersible.client;

import edu.umass.cs.dispersible.models.DispersibleAppRequest;
import edu.umass.cs.dispersible.models.DispersibleAppResponse;
import edu.umass.cs.dispersible.models.DispersiblePacketType;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestFuture;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.ReconfigurableAppClientAsync;
import edu.umass.cs.reconfiguration.examples.linwrites.SimpleAppRequest;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ClientReconfigurationPacket;
import edu.umass.cs.reconfiguration.reconfigurationpackets.CreateServiceName;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Sarthak Nandi on 24/4/18.
 */
public class DispersibleAppTestClient extends ReconfigurableAppClientAsync<DispersibleAppRequest> {

  public DispersibleAppTestClient() throws IOException {
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

  public static void main(String[] args) throws IOException {
    String serviceName = "LinWritesLocReads";
    String initialState = "20";

    DispersibleAppTestClient dispersibleAppTestClient = new DispersibleAppTestClient();

    try {
      // TODO: confirm usage of state
      System.out.println("Sending create service request");
      RequestFuture<ClientReconfigurationPacket> createServiceFuture =
          dispersibleAppTestClient.sendRequest(new CreateServiceName(serviceName, initialState));

      ClientReconfigurationPacket response = createServiceFuture.get(20, TimeUnit.SECONDS);

      if (!isValidResponse(response)) {
        throw new IllegalStateException("Unable to create service");
      }
      System.out.println("Created service");
    } catch (ReconfigurationException | InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }

    Request response = dispersibleAppTestClient.sendRequest(createNewAppRequest(serviceName));
    System.out.println("Response : " + response);

    response = dispersibleAppTestClient.sendRequest(createExecuteRequest(serviceName, 401));

    System.out.println("Response : " + response);

    response = dispersibleAppTestClient.sendRequest(createExecuteRequest(serviceName, 402));

    System.out.println("Response : " + response);
  }

  private static DispersibleAppRequest createExecuteRequest(String serviceName, int type) {
    JSONObject json = new JSONObject();

    try {
      json.put("type", type);
      json.put("SERVICE_NAME", serviceName);
      json.put("EPOCH", 0);
      json.put("REQUEST_ID", 10000);
      json.put("STOP", false);
      json.put("REQUEST_VALUE", 10);
      SimpleAppRequest replicableRequest = new SimpleAppRequest(json);
      return new DispersibleAppRequest(DispersiblePacketType.EXECUTE, serviceName, replicableRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private static DispersibleAppRequest createNewAppRequest(String serviceName) {
    return new DispersibleAppRequest(DispersiblePacketType.NEW_APP, serviceName);
  }

  private static boolean isValidResponse(ClientReconfigurationPacket response) {
    // TODO: check if response is valid
    return response != null;
  }


}
