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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.sonar.batch.bootstrapper.BatchDownloader;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

public class SonarTask extends Task {

  private String serverUrl = "http://localhost:9000";

  private File workDir;

  private ProjectElement projectElement = new ProjectElement();

  private BatchDownloader bootstrapper;

  /**
   * @param url Sonar host URL
   */
  public void setServer(String url) {
    this.serverUrl = url;
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
    log("Sonar server: " + serverUrl);
    bootstrapper = new BatchDownloader(serverUrl);
    checkSonarVersion();
    delegateExecution(createClassLoader());
  }

  private void delegateExecution(SonarClassLoader sonarClassLoader) {
    ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(sonarClassLoader);

    try {
      Class<?> launcherClass = sonarClassLoader.findClass("org.sonar.ant.Launcher");
      Method method = launcherClass.getMethod("execute", SonarTask.class);
      Object launcher = launcherClass.newInstance();
      method.invoke(launcher, this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Thread.currentThread().setContextClassLoader(oldContextClassLoader);
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
    cl.addURL(getClass().getProtectionDomain().getCodeSource().getLocation());
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

}
