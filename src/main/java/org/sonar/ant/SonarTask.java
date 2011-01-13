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

public class SonarTask extends Task {
  private String serverUrl = "http://localhost:9000";
  private ProjectElement projectElement = new ProjectElement();

  public void setServerUrl(String url) {
    this.serverUrl = url;
  }

  public ProjectElement createProject() {
    return projectElement;
  }

  @Override
  public void execute() throws BuildException {
    try {
      ServerMetadata server = new ServerMetadata(serverUrl);

      System.out.println("Sonar version: " + server.getVersion());

      if (server.supportsAnt()) {
        new Bootstraper().start(projectElement);
      } else {
        throw new BuildException("Sonar " + server.getVersion() + " does not support Ant");
      }
    } catch (Exception e) {
      throw new BuildException("Failed to execute Sonar", e);
    }
  }
}
