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

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.sonar.bootstrapper.Bootstrapper;

import java.io.File;
import java.util.List;

/**
 * This task is a bootstrap for all other Sonar Ant tasks.
 * Creates isolated {@link AntClassLoader} with specified name and which contains:
 * <ul>
 * <li>JAR from where this task was loaded</li>
 * <li>all JARs to execute sonar-batch by downloading them from specified server</li>
 * </ul>
 */
public class BootstrapTask extends Task {

  private String serverUrl = "http://localhost:9000";

  private String name = "sonar";

  private File workDir;

  /**
   * @param url Sonar host URL
   */
  public void setServer(String url) {
    this.serverUrl = url;
  }

  /**
   * @param name name of the loader, which would be created
   */
  public void setName(String name) {
    this.name = name;
  }

  public void setWorkDir(File workDir) {
    this.workDir = workDir;
  }

  private Path newPath(String path) {
    Path result = new Path(null);
    result.setPath(path);
    return result;
  }

  private Path setupClasspath(Bootstrapper bootstrapper) {
    Path classpath = new Path(null);

    // Add JAR with Sonar Ant tasks - it's a Jar which contains this class
    classpath.add(newPath(BootstrapTask.class.getProtectionDomain().getCodeSource().getLocation().getPath()));

    List<File> files = bootstrapper.downloadFiles(workDir);
    for (File file : files) {
      classpath.add(newPath(file.getAbsolutePath()));
    }

    return classpath;
  }

  @Override
  public void execute() {
    Bootstrapper bootstrapper = new Bootstrapper(serverUrl);

    String serverVersion = bootstrapper.getServerVersion();
    System.out.println("Sonar version: " + serverVersion);

    if (isVersionPriorTo2Dot6(serverVersion)) {
      throw new BuildException("Sonar " + serverVersion + " does not support Ant");
    }

    Path classpath = setupClasspath(bootstrapper);

    AntClassLoader loader = new AntClassLoader(null, getProject(), classpath, false);
    loader.setIsolated(true);
    getProject().addReference(name, loader);

    log("Created isolated classloader '" + name + "': " + classpath);
  }

  private static boolean isVersionPriorTo2Dot6(String version) {
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
