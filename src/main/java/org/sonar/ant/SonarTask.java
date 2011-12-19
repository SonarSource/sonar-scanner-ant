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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.sonar.batch.bootstrapper.BootstrapClassLoader;
import org.sonar.batch.bootstrapper.Bootstrapper;
import org.sonar.batch.bootstrapper.BootstrapperIOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

public class SonarTask extends Task {

  /**
   * Array of prefixes of versions of Sonar without support of this Ant Task.
   */
  private static final String[] UNSUPPORTED_VERSIONS = { "1", "2.0", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7" };

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

  private Bootstrapper bootstrapper;

  private String initTarget;

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

  /**
   * Note about how Ant handles exceptions: according to {@link DefaultLogger#buildFinished(BuildEvent)} if we want to print stack trace,
   * then we shouldn't use {@link BuildException} with message.
   */
  @Override
  public void execute() {
    log(Main.getAntVersion());
    log("Sonar Ant Task version: " + getTaskVersion());
    log("Loaded from: " + Utils.getJarPath());
    log("Sonar work directory: " + getWorkDir().getAbsolutePath());
    log("Sonar server: " + getServerUrl());
    bootstrapper = new Bootstrapper("AntTask/" + getTaskVersion(), getServerUrl(), getWorkDir());
    checkSonarVersion();
    delegateExecution(createClassLoader());
  }

  /**
   * Loads {@link Launcher} from specified {@link BootstrapClassLoader} and passes control to it.
   * 
   * @see Launcher#execute()
   */
  private void delegateExecution(BootstrapClassLoader sonarClassLoader) {
    ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(sonarClassLoader);
      Class<?> launcherClass = sonarClassLoader.findClass("org.sonar.ant.Launcher");
      Constructor<?> constructor = launcherClass.getConstructor(SonarTask.class);
      Object launcher = constructor.newInstance(this);
      Method method = launcherClass.getMethod("execute");
      method.invoke(launcher);
    } catch (InvocationTargetException e) {
      // Unwrap original exception
      throw new BuildException(e.getTargetException());
    } catch (Exception e) {
      // Catch all other exceptions, which relates to reflection
      throw new BuildException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContextClassLoader);
    }
  }

  private BootstrapClassLoader createClassLoader() {
    return bootstrapper.createClassLoader(
        new URL[] { Utils.getJarPath() }, // Add JAR with Sonar Ant task - it's a Jar which contains this class
        getClass().getClassLoader(),
        "org.apache.tools.ant", "org.sonar.ant");
  }

  private void checkSonarVersion() {
    String serverVersion = bootstrapper.getServerVersion();
    log("Sonar version: " + serverVersion);
    if (isVersionPriorTo2Dot8(serverVersion)) {
      throw new BuildException("Sonar " + serverVersion + " does not support Sonar Ant Task " + getTaskVersion()
          + ". Please upgrade Sonar to version 2.8 or more.");
    }
  }

  static boolean isVersionPriorTo2Dot8(String version) {
    for (String unsupportedVersion : UNSUPPORTED_VERSIONS) {
      if (isVersion(version, unsupportedVersion)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isVersion(String version, String prefix) {
    return version.startsWith(prefix + ".") || version.equals(prefix);
  }

  public static String getTaskVersion() {
    InputStream in = null;
    try {
      in = SonarTask.class.getResourceAsStream("/org/sonar/ant/version.txt");
      Properties props = new Properties();
      props.load(in);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new BuildException("Could not load the version information for Sonar Ant Task", e);
    } finally {
      BootstrapperIOUtils.closeQuietly(in);
    }
  }

}
