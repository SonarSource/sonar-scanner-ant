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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

/**
 * This task is a kind of bootstrap for all Sonar Ant tasks.
 * For now it just creates isolated ClassLoader with one JAR - from where this task was loaded.
 * But we can imagine more complex logic here: eg. download dependencies from server.
 */
public class IsolateClassloader extends Task {
  private String name = "sonar";

  /**
   * @param name name of the loader, which would be created
   */
  public void setName(String name) {
    this.name = name;
  }

  private Path newPath(String path) {
    Path result = new Path(null);
    result.setPath(path);
    return result;
  }

  private Path setupClasspath() {
    Path classpath = new Path(null);
    // Add JAR with Sonar Ant tasks
    classpath.add(newPath(IsolateClassloader.class.getProtectionDomain().getCodeSource().getLocation().getPath()));
    return classpath;
  }

  @Override
  public void execute() {
    Path classpath = setupClasspath();

    AntClassLoader loader = new AntClassLoader(null, getProject(), classpath, false);
    loader.setIsolated(true);
    getProject().addReference(name, loader);

    log("Created isolated classloader '" + name + "': " + classpath);
  }
}
