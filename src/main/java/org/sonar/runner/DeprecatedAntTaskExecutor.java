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

package org.sonar.runner;

import org.apache.tools.ant.BuildException;
import org.sonar.ant.SonarTask;
import org.sonar.ant.utils.SonarAntTaskUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This class is a big part of the code of the Ant Task < 1.5.
 * It is kept to offer backward compatibility for a while, but may be dropped at some point of time.
 * <p/>
 * It is deprecated since version 1.5, for which the use of the embedded Sonar Runner is preferred.
 */
@Deprecated
public class DeprecatedAntTaskExecutor {

  /**
   * Array of prefixes of versions of Sonar without support of this Deprecated Ant Task.
   */
  private static final String[] UNSUPPORTED_VERSIONS = {"1", "2.0", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7"};

  private static final String SONAR_SOURCES_PROPERTY = "sonar.sources";

  private SonarTask sonarTask;
  private Bootstrapper bootstrapper;

  public DeprecatedAntTaskExecutor(SonarTask sonarTask) {
    this.sonarTask = sonarTask;
  }

  /**
   * Note about how Ant handles exceptions: according to "DefaultLogger#buildFinished(BuildEvent)" if we want to print stack trace,
   * then we shouldn't use {@link BuildException} with message.
   */
  public void execute() {
    checkMandatoryProperties();
    bootstrapper = new Bootstrapper("AntTask/" + SonarAntTaskUtils.getTaskVersion(), sonarTask.getServerUrl(), sonarTask.getWorkDir());
    checkSonarVersion();
    delegateExecution(createClassLoader());
  }

  protected void checkMandatoryProperties() {
    Collection<String> missingProps = new ArrayList<String>();
    if (isEmpty(sonarTask.getKey())) {
      missingProps.add("\n  - task attribute 'key'");
    }
    if (isEmpty(sonarTask.getVersion())) {
      missingProps.add("\n  - task attribute 'version'");
    }
    if (isNotFound("sonar.modules") && isSourceInfoMissing()) {
      missingProps.add("\n  - task attribute 'sources' or property 'sonar.sources'");
    }
    if (!missingProps.isEmpty()) {
      StringBuilder message = new StringBuilder("\nThe following mandatory information is missing:");
      for (String prop : missingProps) {
        message.append(prop);
      }
      throw new IllegalArgumentException(message.toString());
    }
  }

  private void checkSonarVersion() {
    String serverVersion = bootstrapper.getServerVersion();
    sonarTask.log("Sonar version: " + serverVersion);
    if (isVersionPriorTo2Dot8(serverVersion)) {
      throw new BuildException("Sonar " + serverVersion + " does not support Sonar Ant Task " + SonarAntTaskUtils.getTaskVersion()
        + ". Please upgrade Sonar to version 2.8 or more.");
    }
  }

  /**
   * Loads {@link DeprecatedAntLauncher} from specified {@link BootstrapClassLoader} and passes control to it.
   *
   * @see DeprecatedAntLauncher#execute()
   */
  private void delegateExecution(BootstrapClassLoader sonarClassLoader) {
    ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(sonarClassLoader);
      Class<?> launcherClass = sonarClassLoader.findClass("org.sonar.runner.DeprecatedAntLauncher");
      Constructor<?> constructor = launcherClass.getConstructor(SonarTask.class);
      Object launcher = constructor.newInstance(sonarTask);
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
      new URL[]{SonarAntTaskUtils.getJarPath()}, // Add JAR with Sonar Ant task - it's a Jar which contains this class
      getClass().getClassLoader(),
      "org.apache.tools.ant", "org.sonar.ant");
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

  private boolean isNotFound(String string) {
    String systemProp = System.getProperty(string);
    String projectProp = sonarTask.getProject().getProperty(string);
    String taskProp = sonarTask.getProperties().getProperty(string);
    return isEmpty(systemProp) && isEmpty(projectProp) && isEmpty(taskProp);
  }

  private boolean isSourceInfoMissing() {
    return sonarTask.getSources() == null && isNotFound(SONAR_SOURCES_PROPERTY);
  }

  private boolean isEmpty(String string) {
    return string == null || "".equals(string);
  }

}
