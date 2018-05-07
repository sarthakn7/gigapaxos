package edu.umass.cs.dispersible.example;

import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Sarthak Nandi on 24/4/18.
 */
public class LinWritesLocReads implements Replicable {

  private int total = 0;

  @Override
  public boolean execute(Request request, boolean doNotReplyToClient) {
    return this.execute(request);
  }

  @Override
  public String checkpoint(String name) {
    // should return entire state here
    return this.total+"";
  }

  @Override
  public boolean restore(String name, String state) {
    // null state is equivalent to reinitialization
    if(state == null) {
      return true;
    }

    try {
      int number = Integer.valueOf(state);
      this.total = number;
    } catch(NumberFormatException nfe) {
      nfe.printStackTrace();
    }
    return true;
  }

  @Override
  public boolean execute(Request request) {

    // coordinated
    IntegerPacketType requestType = request.getRequestType();
    if (request instanceof LinWritesLocReadsRequest && requestType
        .equals(LinWritesLocReadsRequest.PacketType.COORDINATED_WRITE)) {
      this.total += Integer.valueOf(((LinWritesLocReadsRequest) request)
                                        .getValue());
      ((LinWritesLocReadsRequest) request).setResponse("total=" + this.total);
    }
    // uncoordinated
    else if (request instanceof LinWritesLocReadsRequest && requestType
        .equals(LinWritesLocReadsRequest.PacketType.LOCAL_READ)) {
      ((LinWritesLocReadsRequest) request).setResponse("total="+this.total);
    }

    return true;
  }

  @Override
  public Request getRequest(String stringified) throws RequestParseException {
    try {
      return new LinWritesLocReadsRequest(new JSONObject(stringified));
    } catch(JSONException je) {
      throw new RequestParseException(je);
    }
  }

  @Override
  public Set<IntegerPacketType> getRequestTypes() {
    return new HashSet<>(Arrays.asList(LinWritesLocReadsRequest
                                           .PacketType.values()));
  }

  public static void main(String[] args) {
    System.out.println("Test");
  }
}
