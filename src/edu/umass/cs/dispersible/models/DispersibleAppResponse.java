package edu.umass.cs.dispersible.models;

import static edu.umass.cs.dispersible.models.DispersibleKeys.*;

import edu.umass.cs.gigapaxos.interfaces.ClientRequest;
import edu.umass.cs.nio.JSONPacket;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Sarthak Nandi on 22/4/18.
 */
public class DispersibleAppResponse implements ClientRequest {

  private long requestId;
  private String serviceName;
  private IntegerPacketType packetType;
  private DispersibleResponseCode responseCode;

  DispersibleAppResponse(DispersibleAppRequest request, DispersibleResponseCode responseCode) {
    this.requestId = request.getRequestID();
    this.serviceName = request.getServiceName();
    this.packetType = request.getRequestType();
    this.responseCode = responseCode;
  }

  @Override
  public ClientRequest getResponse() {
    return this;
  }

  @Override
  public IntegerPacketType getRequestType() {
    return packetType;
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }

  @Override
  public long getRequestID() {
    return requestId;
  }

  @Override
  public String toString() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put(RESPONSE_CODE.name(), responseCode.name());
      jsonObject.put(SERVICE_NAME.name(), serviceName);
      jsonObject.put(REQUEST_ID.name(), requestId);
      jsonObject.put(JSONPacket.PACKET_TYPE, packetType.getInt());
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
    return jsonObject.toString();
  }

}
