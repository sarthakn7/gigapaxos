package edu.umass.cs.dispersible.db;

import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import java.nio.ByteBuffer;

/**
 * @author Sarthak Nandi on 30/4/18.
 */
@Accessor
public interface AppAccessor {

  @Query("INSERT INTO apps (service , app_class_name , jar_file_name, jar ) VALUES (?, ?, ?, ?)")
  void insertApp(String service, String appClassName, String jarFileName, ByteBuffer jar);

  @Query("SELECT * FROM apps WHERE service = ?")
  App getByServiceName(String service);
}

