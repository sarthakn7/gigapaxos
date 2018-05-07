package edu.umass.cs.dispersible.models;

import static edu.umass.cs.dispersible.models.DispersibleKeys.*;

import edu.umass.cs.gigapaxos.interfaces.ClientRequest;
import edu.umass.cs.nio.JSONPacket;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.interfaces.ReplicableRequest;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Sarthak Nandi on 22/4/18.
 */
public class DispersibleAppRequest extends JSONPacket implements
                                                      ReplicableRequest, ClientRequest {

  private final long requestId;
  private final String dispersedAppRequestString;
  private final DispersiblePacketType packetType;
  private final String serviceName;

  private ReplicableRequest dispersedAppRequest;
  private DispersibleResponseCode responseCode;

  public DispersibleAppRequest(JSONObject json) throws JSONException {
    super(json);

    packetType = DispersiblePacketType.getPacketType(type)
        .orElseThrow(() -> new IllegalStateException("Invalid packet type"));

    requestId = json.getLong(REQUEST_ID.name());
    serviceName = json.getString(SERVICE_NAME.name());

    if (packetType == DispersiblePacketType.NEW_APP || json.has("RESPONSE_CODE")) {
      dispersedAppRequestString = null;
    } else {
      dispersedAppRequestString = json.getString(APP_REQUEST.name());
      // TODO: make change in js client to put request in this
    }
  }

  public DispersibleAppRequest(DispersiblePacketType packetType, String serviceName) {
    super(packetType);
    this.requestId = (long) (Math.random() * Long.MAX_VALUE);
    this.packetType = packetType;
    this.serviceName = serviceName;
    this.dispersedAppRequestString = "";
  }

  public DispersibleAppRequest(DispersiblePacketType packetType, String serviceName, ReplicableRequest replicableRequest) {
    super(packetType);
    this.requestId = (long) (Math.random() * Long.MAX_VALUE);
    this.packetType = packetType;
    this.serviceName = serviceName;
    this.dispersedAppRequestString = replicableRequest.toString();
    this.dispersedAppRequest = replicableRequest;
  }

  @Override
  public ClientRequest getResponse() {
    return new DispersibleAppResponse(this, responseCode);
  }

  @Override
  public boolean needsCoordination() {
    if (packetType == DispersiblePacketType.NEW_APP) {
      return true;
    } else {
      return dispersedAppRequest.needsCoordination();
    }
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
  protected JSONObject toJSONObjectImpl() throws JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(SERVICE_NAME.name(), serviceName);
    jsonObject.put(REQUEST_ID.name(), requestId);
    jsonObject.put(JSONPacket.PACKET_TYPE, packetType.getInt());
    if (dispersedAppRequestString != null) {
      jsonObject.put(APP_REQUEST.name(), dispersedAppRequestString);
    }

    return jsonObject;
  }

  public String getDispersedAppRequestString() {
    return dispersedAppRequestString;
  }

  public ReplicableRequest getDispersedAppRequest() {
    return dispersedAppRequest;
  }

  public void setDispersedAppRequest(ReplicableRequest dispersedAppRequest) {
    this.dispersedAppRequest = dispersedAppRequest;
  }

  public void setResponseCode(DispersibleResponseCode responseCode) {
    this.responseCode = responseCode;
  }

  @Override
  public String toString() {
    try {
      return toJSONObjectImpl().toString();
    } catch (JSONException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }
}