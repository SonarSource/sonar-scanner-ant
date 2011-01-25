/*
 * Sonar Ant Task
 * Copyright (C) 2009 SonarSource
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

import org.apache.tools.ant.*;
import org.sonar.batch.bootstrapper.BatchDownloader;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class SonarTask extends Task {

  private String serverUrl = "http://localhost:9000";

  private File workDir;

  private ProjectElement projectElement = new ProjectElement();

  private BatchDownloader bootstrapper;

  public String getServerUrl() {
    return serverUrl;
  }

  /**
   * @param url Sonar host URL
   */
  public void setServer(String url) {
    this.serverUrl = url;
  }

  public File getWorkDir() {
    return workDir;
  }

  /**
   * @param workDir directory to which bootstrapper will download files
   */
  public void setWorkDir(File workDir) {
    this.workDir = workDir;
  }

  /**
   * @return project for analysis
   */
  public ProjectElement createProject() {
    return projectElement;
  }

  @Override
  public void execute() throws BuildException {
    log(Main.getAntVersion());
    log("Sonar Ant Task version: " + getTaskVersion());
    log("Loaded from: " + getJarPath());
    log("Sonar server: " + serverUrl);
    bootstrapper = new BatchDownloader(serverUrl);
    checkSonarVersion();
    delegateExecution(createClassLoader());
  }

  static String getTaskVersion() {
    try {
      InputStream in = SonarTask.class.getResourceAsStream("/org/sonar/ant/version.txt");
      Properties props = new Properties();
      props.load(in);
      in.close();
      return props.getProperty("version");
    } catch (Exception e) {
      throw new BuildException("Could not load the version information: " + e.getMessage());
    }
  }

  private void delegateExecution(SonarClassLoader sonarClassLoader) {
    ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(sonarClassLoader);
      Class<?> launcherClass = sonarClassLoader.findClass("org.sonar.ant.Launcher");
      Method method = launcherClass.getMethod("execute", SonarTask.class);
      Object launcher = launcherClass.newInstance();
      method.invoke(launcher, this);
    } catch (Exception e) {
      throw new BuildException("Failed to execute Sonar", e);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContextClassLoader);
    }
  }

  private SonarClassLoader createClassLoader() {
    log("Sonar boot directory: " + workDir.getAbsolutePath());
    List<File> files = bootstrapper.downloadBatchFiles(workDir);
    SonarClassLoader cl = new SonarClassLoader(getClass().getClassLoader());
    // Add Sonar files
    for (File file : files) {
      cl.addFile(file);
    }
    // Add JAR with Sonar Ant task - it's a Jar which contains this class
    cl.addURL(getJarPath());
    return cl;
  }

  private void checkSonarVersion() {
    String serverVersion = bootstrapper.getServerVersion();
    log("Sonar version: " + serverVersion);
    if (isVersionPriorTo2Dot6(serverVersion)) {
      throw new BuildException("Sonar " + serverVersion + " does not support Ant");
    }
  }

  static boolean isVersionPriorTo2Dot6(String version) {
    return isVersion(version, "1")
        || isVersion(version, "2.0")
        || isVersion(version, "2.1")
        || isVersion(version, "2.2")
        || isVersion(version, "2.3")
        || isVersion(version, "2.4")
        || isVersion(version, "2.5");
  }

  private static boolean isVersion(String version, String prefix) {
    return version.startsWith(prefix + ".") || version.equals(prefix);
  }

  public String getLoggerLevel() {
    int antLoggerLevel = getAntLoggerLever();
    switch (antLoggerLevel) {
      case 3:
        return "DEBUG";
      case 4:
        return "TRACE";
      default:
        return "INFO";
    }
  }

  /**
   * For unknown reasons <code>getClass().getProtectionDomain().getCodeSource().getLocation()</code> doesn't work under Ant 1.7.0.
   * So this is a workaround.
   * 
   * @return Jar which contains this class
   */
  static URL getJarPath() {
    String pathToClass = "/" + SonarTask.class.getName().replace('.', '/') + ".class";
    URL url = SonarTask.class.getResource(pathToClass);
    if (url != null) {
      String path = url.toString();
      String uri = null;
      if (path.startsWith("jar:file:")) {
        int bang = path.indexOf("!");
        uri = path.substring(4, bang);
      } else if (path.startsWith("file:")) {
        int tail = path.indexOf(pathToClass);
        uri = path.substring(0, tail);
      }
      if (uri != null) {
        try {
          return new URL(uri);
        } catch (MalformedURLException e) {
          // ignore
        }
      }
    }
    return null;
  }

  /**
   * Workaround to get Ant logger level. Possible values (see {@link org.apache.tools.ant.Main}):
   * <ul>
   * <li>1 - quiet</li>
   * <li>2 - default</li>
   * <li>3 - verbose</li>
   * <li>4 - debug</li>
   * </ul>
   */
  private int getAntLoggerLever() {
    try {
      Vector<BuildListener> listeners = getProject().getBuildListeners();
      for (BuildListener listener : listeners) {
        if (listener instanceof DefaultLogger) {
          DefaultLogger logger = (DefaultLogger) listener;
          Field field = DefaultLogger.class.getDeclaredField("msgOutputLevel");
          field.setAccessible(true);
          return (Integer) field.get(logger);
        }
      }
    } catch (Exception e) {
      // ignore
    }
    return 2;
  }
}
