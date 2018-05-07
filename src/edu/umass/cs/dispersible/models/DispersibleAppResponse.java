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
  private String appResponse;

  public DispersibleAppResponse(JSONObject jsonObject) throws JSONException {
    this(new DispersibleAppRequest(jsonObject));
    responseCode = DispersibleResponseCode.valueOf(jsonObject.getString(RESPONSE_CODE.name()));
    if (jsonObject.has(APP_RESPONSE.name())) {
      appResponse = jsonObject.getString(APP_RESPONSE.name());
    }
  }

  DispersibleAppResponse(DispersibleAppRequest request, DispersibleResponseCode responseCode) {
    this(request);
    this.responseCode = responseCode;

  }

  DispersibleAppResponse(DispersibleAppRequest request) {
    this.requestId = request.getRequestID();
    this.serviceName = request.getServiceName();
    this.packetType = request.getRequestType();
    if (request.getDispersedAppRequest() != null) {
      appResponse = ((ClientRequest) request.getDispersedAppRequest()).getResponse().toString();
    }
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
      if (appResponse != null) {
        jsonObject.put(APP_RESPONSE.name(), appResponse);
      }
      jsonObject.put(JSONPacket.PACKET_TYPE, packetType.getInt());
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
    return jsonObject.toString();
  }

}
