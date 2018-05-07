package edu.umass.cs.dispersible.app;

import static edu.umass.cs.dispersible.models.DispersiblePacketType.*;
import static edu.umass.cs.dispersible.models.DispersibleResponseCode.*;

import edu.umass.cs.dispersible.db.App;
import edu.umass.cs.dispersible.db.AppDao;
import edu.umass.cs.dispersible.models.DispersibleAppRequest;
import edu.umass.cs.dispersible.models.DispersiblePacketType;
import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.interfaces.Reconfigurable;
import edu.umass.cs.reconfiguration.interfaces.ReconfigurableRequest;
import edu.umass.cs.reconfiguration.interfaces.ReplicableRequest;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
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

  private static final String JAR_DIRECTORY = "AppJars/"; // TODO: make configurable

  private AppDao appDao;

  private Map<String, Replicable> apps = new HashMap<>();

  public DispersibleApp() {
    // TODO: get below from config
    String host = "127.0.0.1";
    int port = 9042;
    String keyspace = "dispersibility";
    appDao = new AppDao(host, port, keyspace);
  }

  @Override
  public boolean execute(Request request, boolean doNotReplyToClient) {
    System.out.println("Request received : " + request);
    if (!(request instanceof DispersibleAppRequest)) {
      throw new IllegalArgumentException("Incompatible request received");
    }

    DispersibleAppRequest appRequest = (DispersibleAppRequest) request;

    IntegerPacketType requestType = appRequest.getRequestType();
    if (requestType == NEW_APP) {
      String serviceName = appRequest.getServiceName();
      Optional<Replicable> appForService = loadAppForService(serviceName);
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

    Replicable app = apps.get(serviceName);
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

    return loadAppForService(name)
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
        Replicable app = loadAppForService(request.getServiceName())
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

  private Optional<Replicable> loadAppForService(String serviceName) {
    Replicable replicableApp = apps.get(serviceName);

    if (replicableApp != null) {
      return Optional.of(replicableApp);
    }

    App app = appDao.getByServiceName(serviceName).orElse(null);

    if (app == null) {
      return Optional.empty();
    }

    Path filePath = Paths.get(JAR_DIRECTORY, app.getJarFileName());

    if (!Files.exists(filePath)) {
      createFile(filePath, app.getJar());
    }

    try {
      replicableApp = initializeReplicableClassFromJar(filePath, app.getAppClassName());
      apps.put(serviceName, replicableApp);
      return Optional.of(replicableApp);
    } catch (ClassNotFoundException | IllegalAccessException | MalformedURLException | InstantiationException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  private void createFile(Path filePath, ByteBuffer content) {
    try {
      BufferedOutputStream stream =
          new BufferedOutputStream(new FileOutputStream(new File(filePath.toString())));
      stream.write(content.array());
      stream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Replicable initializeReplicableClassFromJar(Path jarFilePath, String className)
      throws ClassNotFoundException, IllegalAccessException, InstantiationException,
             MalformedURLException {
    File myJar = new File(jarFilePath.toString());
    URLClassLoader child = new URLClassLoader(new URL[]{myJar.toURI().toURL()}, this.getClass().getClassLoader());
    Class classToLoad = Class.forName(className, true, child);
    return (Replicable) classToLoad.newInstance();
  }

  @Override
  public Set<IntegerPacketType> getRequestTypes() {
    return DispersiblePacketType.allDispersiblePacketTypes();
  }
}
