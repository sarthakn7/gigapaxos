package edu.umass.cs.dispersible.app;

import static edu.umass.cs.dispersible.models.DispersiblePacketType.*;
import static edu.umass.cs.dispersible.models.DispersibleResponseCode.*;

import edu.umass.cs.dispersible.models.DispersibleAppRequest;
import edu.umass.cs.dispersible.models.DispersiblePacketType;
import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.interfaces.Reconfigurable;
import edu.umass.cs.reconfiguration.interfaces.ReconfigurableRequest;
import edu.umass.cs.reconfiguration.interfaces.ReplicableRequest;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Sarthak Nandi on 22/4/18.
 */
public class DispersibleApp implements Replicable, Reconfigurable {

  private static final String DYNAMIC_CLASS_PATH = "file:DynamicClasses/";
  private static final String CHECKPOINT_FILE = "checkpoint_file";

  private Map<String, Replicable> apps = new HashMap<>();

  @Override
  public boolean execute(Request request, boolean doNotReplyToClient) {
    if (!(request instanceof DispersibleAppRequest)) {
      throw new IllegalArgumentException("Incompatible request received");
    }

    DispersibleAppRequest appRequest = (DispersibleAppRequest) request;

    IntegerPacketType requestType = appRequest.getRequestType();
    if (requestType == NEW_APP) {
      String serviceName = appRequest.getServiceName();
      Optional<Replicable> appForService = getAppForService(serviceName);
      if (!appForService.isPresent()) {
        System.out.println("Unable to find app for service : " + serviceName);
        appRequest.setResponseCode(UNABLE_TO_CREATE_APP);
      } else {
        appRequest.setResponseCode(CREATED_APP);
      }

      return true;
    } else if (requestType == EXECUTE) {
      return executeAppRequest(appRequest);
    }

    return false;
  }

  private boolean executeAppRequest(DispersibleAppRequest appRequest) {
    String serviceName = appRequest.getServiceName();

    if (!apps.containsKey(serviceName)) {
      appRequest.setResponseCode(UNABLE_TO_EXECUTE_REQUEST);
      return true;
    }

    Replicable app = getAppForService(serviceName).orElse(null);
    assert app != null; // App must be present for the service at this stage

    try {
      boolean result = app.execute(appRequest.getDispersedAppRequest());
      appRequest.setResponseCode(REQUEST_EXECUTED);
      return result;
    } catch (Exception e) {
      e.printStackTrace();
      appRequest.setResponseCode(UNABLE_TO_EXECUTE_REQUEST);
      return true;
    }
  }

  @Override
  public String checkpoint(String name) {
    if (!apps.containsKey(name)) {
      return null;
    }

    return apps.get(name).checkpoint(name);
  }

  @Override
  public boolean restore(String name, String state) {
    if (state == null) {
      apps.remove(name);
      return true;
    }

    return getAppForService(name)
        .map(app -> app.restore(name, state))
        .orElse(false);
  }

  @Override
  public ReconfigurableRequest getStopRequest(String name, int epoch) {
    return null;
  }

  @Override
  public String getFinalState(String name, int epoch) {
    throw new IllegalStateException("This method should not have been called");
  }

  @Override
  public void putInitialState(String name, int epoch, String state) {
    throw new IllegalStateException("This method should not have been called");
  }

  @Override
  public boolean deleteFinalState(String name, int epoch) {
    throw new IllegalStateException("This method should not have been called");
  }

  @Override
  public Integer getEpoch(String name) {
    throw new IllegalStateException("This method should not have been called");
  }

  @Override
  public boolean execute(Request request) {
    return execute(request, false);
  }

  @Override
  public Request getRequest(String stringified) throws RequestParseException {
    try {
      JSONObject jsonObject = new JSONObject(stringified);

      DispersibleAppRequest request = new DispersibleAppRequest(jsonObject);

      if (request.getRequestType() == EXECUTE) {
        Replicable app = getAppForService(request.getServiceName())
            .orElseThrow(() -> new RequestParseException(new Exception("No app found for request : " + request)));

        ReplicableRequest userRequest = (ReplicableRequest) app
            .getRequest(request.getDispersedAppRequestString());
        request.setDispersedAppRequest(userRequest);
      }

      return request;

    } catch (JSONException e) {
      throw new RequestParseException(e);
    }
  }

  private Optional<Replicable> getAppForService(String serviceName) {
    Replicable app = apps.get(serviceName);

    if (app != null) {
      return Optional.of(app);
    }

    String className = serviceName + ".class";
    if (!downloadClass(className)) {
      return Optional.empty();
    }

    String requestClassName = serviceName + "Request.class";
    if (!downloadClass(requestClassName)) {
      // If request class is not found, still return empty as it won't be possible to execute
      // the request
      return Optional.empty();
    }

    try {
      URLClassLoader classLoader = new URLClassLoader (
          new URL[] {new URL(DYNAMIC_CLASS_PATH)}, DispersibleApp.class.getClassLoader());
      Class<?> clazz = Class.forName(serviceName, true, classLoader);
      app = (Replicable) clazz.newInstance();
      apps.put(serviceName, app);
      return Optional.of(app);
    } catch (MalformedURLException | IllegalAccessException | InstantiationException
        | ClassNotFoundException e) {
      e.printStackTrace();
      throw new IllegalStateException("Unable to instantiate class : " + className);
    }
  }

  private boolean downloadClass(String className) {
    if (Files.exists(Paths.get(className))) {
      return true;
    }
    // todo: if found in dropbox/DB and able to download it return true

    return false;
  }

  @Override
  public Set<IntegerPacketType> getRequestTypes() {
    return DispersiblePacketType.allDispersiblePacketTypes();
  }
}
