package edu.umass.cs.dispersible.example;

import edu.umass.cs.dispersible.models.DispersibleRequest;
import edu.umass.cs.gigapaxos.interfaces.ClientRequest;
import edu.umass.cs.gigapaxos.paxospackets.RequestPacket;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.examples.linwrites.SimpleAppRequest;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author arun
 *
 *         A class like this is needed only if the app wants to use request
 *         types other than {@link RequestPacket}, which is generally useful
 *         only if the app wants to coordinate only some request types using
 *         consensus but process other requests locally at replicas or
 *         coordinate them using custom replica coordination protocols. For
 *         using just gigapaxos to implement linearizability, i.e.,
 *         coordinating all requests via consensus-based RSM, this class is
 *         unnecessary as applications can simply encapsulate requests as
 *         {@link RequestPacket}.
 */
public class LinWritesLocReadsRequest extends DispersibleRequest {

  /**
   * Packet type class for example application requests.
   */
  public enum PacketType implements IntegerPacketType {
    /**
     * Default coordinated app request.
     */
    COORDINATED_WRITE(401),
    /**
     * Uncoordinated app request.
     */
    LOCAL_READ(402), ;

    /******************************** BEGIN static ***********************/
    private static HashMap<Integer, PacketType> numbers = new HashMap<>();

    static {
      for (PacketType type : PacketType.values()) {
        if (!PacketType.numbers.containsKey(type.number)) {
          PacketType.numbers.put(type.number, type);
        } else {
          String error = "Duplicate or inconsistent enum type";
          throw new RuntimeException(error);
        }
      }
    }

    /**
     * @param type
     * @return PacketType from int type.
     */
    public static PacketType
    getPacketType(int type) {
      return PacketType.numbers.get(type);
    }

    /********************************** END static ***********************/

    private final int number;

    PacketType(int t) {
      this.number = t;
    }

    @Override
    public int getInt() {
      return this.number;
    }
  }

  /**
   *
   */
  public enum Keys {
    EPOCH, REQUEST_VALUE, STOP, ACK, RESPONSE_VALUE
  }

  // epoch number (nonzero if reconfigured)
  private final int epoch;

  // request value
  private final String value;

  // Whether this request should stop the RSM, which can
  // be used to delete the RSM entirely.
  private final boolean stop;

  // used when getResponse is invoked
  private String response = null;


  /**
   * @param epoch Number of time RSM has been reconfigured
   * @param value Request value
   * @param type Request type
   * @param stop Whether the RSM should be stopped entirely
   */
  public LinWritesLocReadsRequest(int epoch, String value, IntegerPacketType type, boolean stop) {
    super(type);
    this.epoch = epoch;
    this.stop = stop;
    this.value = value;
  }

  /**
   * @param value
   * @param type
   */
  public LinWritesLocReadsRequest(String value, IntegerPacketType type) {
    this(value, type,false);
  }

  /**
   * @param value
   * @param type
   * @param stop
   */
  public LinWritesLocReadsRequest(String value, IntegerPacketType type, boolean stop) {
    this(0, value, type, stop);
  }


  /**
   * @param json
   * @throws JSONException
   */
  public LinWritesLocReadsRequest(JSONObject json) throws JSONException {
    super(json);
    this.epoch = json.getInt(Keys.EPOCH.toString());
    this.stop = json.getBoolean(Keys.STOP.toString());
    this.value = json.getString(Keys.REQUEST_VALUE.toString());
  }

  @Override
  public IntegerPacketType getRequestType() {
    return PacketType.getPacketType(this.type);
  }

  /**
   * @return Request value.
   */
  public String getValue() {
    return this.value;
  }

  public JSONObject toJSONObjectImpl() throws JSONException {
    JSONObject json = new JSONObject();
    json.put(Keys.EPOCH.toString(), this.epoch);
    json.put(Keys.STOP.toString(), this.stop);
    json.put(Keys.REQUEST_VALUE.toString(), this.value);
    return json;
  }

  @Override
  public boolean needsCoordination() {
    return !this.getRequestType().equals(PacketType.LOCAL_READ);
  }

  @Override
  public ClientRequest getResponse() {
    return new SimpleAppRequest(getServiceName(), this.epoch, getRequestID(),
                                this.response == null ? Keys.ACK.toString() : this.response,
                                PacketType.getPacketType(type), this.stop);
  }

  public LinWritesLocReadsRequest setResponse(String response) {
    this.response = response;
    return this;
  }
}

