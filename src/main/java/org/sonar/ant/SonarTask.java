/*
 * Sonar Ant Task
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ant;

import org.apache.tools.ant.Main;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.sonar.ant.deprecated.OldSonarTaskExecutor;
import org.sonar.ant.utils.Utils;

import java.io.File;
import java.util.Properties;

public class SonarTask extends Task {

  private static final String HOST_PROPERTY = "sonar.host.url";

  private File workDir;
  private File baseDir;
  private Properties properties = new Properties();
  private String key;
  private String version;
  private Path sources;
  private Path tests;
  private Path binaries;
  private Path libraries;
  private String initTarget;

  @Override
  public void execute() {
    log(Main.getAntVersion());
    log("Sonar Ant Task version: " + Utils.getTaskVersion());
    log("Loaded from: " + Utils.getJarPath());
    log("Sonar work directory: " + getWorkDir().getAbsolutePath());
    log("Sonar server: " + getServerUrl());

    OldSonarTaskExecutor oldSonarTaskExecutor = new OldSonarTaskExecutor(this);
    oldSonarTaskExecutor.execute();
  }

  /**
   * @return value of property "sonar.host.url", default is "http://localhost:9000"
   */
  public String getServerUrl() {
    String serverUrl = getProperties().getProperty(HOST_PROPERTY); // from task
    if (serverUrl == null) {
      serverUrl = getProject().getProperty(HOST_PROPERTY); // from ant
    }
    if (serverUrl == null) {
      serverUrl = "http://localhost:9000"; // default
    }
    return serverUrl;
  }

  /**
   * @return work directory, default is ".sonar" in project directory
   */
  public File getWorkDir() {
    if (workDir == null) {
      workDir = new File(getBaseDir(), ".sonar");
    }
    return workDir;
  }

  /**
   * @since 1.1
   */
  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  /**
   * @return base directory, default is the current project base directory
   * @since 1.1
   */
  public File getBaseDir() {
    if (baseDir == null) {
      baseDir = getProject().getBaseDir();
    }
    return baseDir;
  }

  /*
   * =============================================================================
   * 
   * Methods and related properties beyond this point are all deprecated.
   * 
   * They should be removed at some point of time, when backward compatibility is
   * not necessary any longer (= when the "org.sonar.ant.deprecated" package is
   * is removed)
   * 
   * =============================================================================
   */

  /**
   * @since 1.1
   */
  public void setInitTarget(String initTarget) {
    this.initTarget = initTarget;
  }

  /**
   * @since 1.1
   */
  public String getInitTarget() {
    return initTarget;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public Path getSources() {
    return sources;
  }

  /**
   * Note that name of this method is important - see http://ant.apache.org/manual/develop.html#nested-elements
   */
  public void addConfiguredProperty(Environment.Variable property) {
    properties.setProperty(property.getKey(), property.getValue());
  }

  public Properties getProperties() {
    return properties;
  }

  public Path createSources() {
    if (sources == null) {
      sources = new Path(getProject());
    }
    return sources;
  }

  public Path createTests() {
    if (tests == null) {
      tests = new Path(getProject());
    }
    return tests;
  }

  public Path createBinaries() {
    if (binaries == null) {
      binaries = new Path(getProject());
    }
    return binaries;
  }

  public Path createLibraries() {
    if (libraries == null) {
      libraries = new Path(getProject());
    }
    return libraries;
  }

}
