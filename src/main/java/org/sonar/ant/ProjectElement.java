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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;

public class ProjectElement {

  private Project antProject;

  private Environment properties = new Environment();

  private Path sources;
  private Path binaries;

  public ProjectElement(Project antProject) {
    this.antProject = antProject;
  }

  public void addProperty(Environment.Variable property) {
    this.properties.addVariable(property);
  }

  public Environment getProperties() {
    return properties;
  }

  public Path createSources() {
    if (sources == null) {
      sources = new Path(antProject);
    }
    return sources;
  }

  public Path createBinaries() {
    if (binaries == null) {
      binaries = new Path(antProject);
    }
    return binaries;
  }

}
