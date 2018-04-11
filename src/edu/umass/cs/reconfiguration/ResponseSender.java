package edu.umass.cs.reconfiguration;

import edu.umass.cs.gigapaxos.interfaces.ClientRequest;

/**
 * This interface can be implemented to use a different mechanism to send response to a client
 * from an {@link ActiveReplica}. An instance of the implementation of this interface must be
 * set in {@link edu.umass.cs.reconfiguration.ActiveReplica.SenderAndRequest} to do the same.
 *
 * @author Sarthak Nandi on 10/4/18.
 */
public interface ResponseSender {

  /**
   *
   * @param response Response that needs to be sent
   * @return True - response sent, false otherwise
   */
  boolean sendResponse(ClientRequest response);
}
