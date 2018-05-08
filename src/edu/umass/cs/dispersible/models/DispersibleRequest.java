package edu.umass.cs.dispersible.models;

import edu.umass.cs.gigapaxos.interfaces.ClientRequest;
import edu.umass.cs.nio.JSONPacket;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.interfaces.ReplicableRequest;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Sarthak Nandi on 7/5/18.
 */
public abstract class DispersibleRequest extends JSONPacket implements
                                                   ReplicableRequest, ClientRequest {

  public DispersibleRequest(IntegerPacketType t) {
    super(t);
  }

  public DispersibleRequest(JSONObject t) throws JSONException {
    super(t);
  }

  // Below methods need not be implemented by Dispersible apps

  @Override
  public final String getServiceName() {
    return "";
  }

  @Override
  public final long getRequestID() {
    return 0;
  }
}
