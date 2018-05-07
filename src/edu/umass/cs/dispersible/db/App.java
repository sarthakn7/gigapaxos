package edu.umass.cs.dispersible.db;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import java.nio.ByteBuffer;

/**
 * @author Sarthak Nandi on 6/5/18.
 */
@Table(name = "apps")
public class App {

  @PartitionKey
  private String service;

  @Column(name = "app_class_name")
  private String appClassName;

  @Column(name = "jar_file_name")
  private String jarFileName;

  @Column
  private ByteBuffer jar;

  public App(String service, String appClassName, String jarFileName, ByteBuffer jar) {
    this.service = service;
    this.appClassName = appClassName;
    this.jarFileName = jarFileName;
    this.jar = jar;
  }

  public String getService() {
    return this.service;
  }

  public String getAppClassName() {
    return this.appClassName;
  }

  public String getJarFileName() {
    return this.jarFileName;
  }

  public ByteBuffer getJar() {
    return this.jar;
  }

  public void setService(String service) {
    this.service = service;
  }

  public void setAppClassName(String appClassName) {
    this.appClassName = appClassName;
  }

  public void setJarFileName(String jarFileName) {
    this.jarFileName = jarFileName;
  }

  public void setJar(ByteBuffer jar) {
    this.jar = jar;
  }

  public String toString() {
    return "App(service=" + this.getService() + ", appClassName=" + this.getAppClassName() + ", jarFileName=" + this.getJarFileName() + ", jar=" + this.getJar() + ")";
  }
}
