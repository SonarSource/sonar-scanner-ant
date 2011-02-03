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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Environment;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.batch.bootstrapper.ProjectDefinition;

import java.io.File;
import java.util.Properties;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LauncherTest {
  private Project antProject;
  private SonarTask task;
  private Launcher launcher;

  @Before
  public void setUp() {
    antProject = new Project();
    antProject.setBaseDir(new File("."));
    task = new SonarTask();
    task.setProject(antProject);
    launcher = new Launcher(task);
  }

  @Test
  public void defaultValues() {
    antProject.setName("My project");
    antProject.setDescription("My description");
    task.setKey("org.example:example");
    task.setVersion("0.1-SNAPSHOT");

    ProjectDefinition sonarProject = launcher.defineProject();

    assertThat(sonarProject.getBaseDir(), is(antProject.getBaseDir()));
    assertThat(sonarProject.getWorkDir(), is(task.getWorkDir()));
    Properties sonarProperties = sonarProject.getProperties();
    assertThat(sonarProperties.getProperty(CoreProperties.PROJECT_KEY_PROPERTY), is("org.example:example"));
    assertThat(sonarProperties.getProperty(CoreProperties.PROJECT_VERSION_PROPERTY), is("0.1-SNAPSHOT"));
    assertThat(sonarProperties.getProperty(CoreProperties.PROJECT_NAME_PROPERTY), is("My project"));
    assertThat(sonarProperties.getProperty(CoreProperties.PROJECT_DESCRIPTION_PROPERTY), is("My description"));
  }

  @Test
  public void overrideDefaultValues() {
    antProject.setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "branch");
    task.setKey("org.example:example");
    task.setVersion("0.1-SNAPSHOT");

    setProperty(task, CoreProperties.PROJECT_NAME_PROPERTY, "My project");
    setProperty(task, CoreProperties.PROJECT_DESCRIPTION_PROPERTY, "My description");
    setProperty(task, CoreProperties.PROJECT_BRANCH_PROPERTY, "Not used");

    ProjectDefinition sonarProject = launcher.defineProject();

    Properties sonarProperties = sonarProject.getProperties();
    assertThat(sonarProperties.getProperty(CoreProperties.PROJECT_KEY_PROPERTY), is("org.example:example"));
    assertThat(sonarProperties.getProperty(CoreProperties.PROJECT_VERSION_PROPERTY), is("0.1-SNAPSHOT"));
    assertThat(sonarProperties.getProperty(CoreProperties.PROJECT_NAME_PROPERTY), is("My project"));
    assertThat(sonarProperties.getProperty(CoreProperties.PROJECT_DESCRIPTION_PROPERTY), is("My description"));
    assertThat(sonarProperties.getProperty(CoreProperties.PROJECT_BRANCH_PROPERTY), is("branch"));
  }

  private void setProperty(SonarTask task, String key, String value) {
    Environment.Variable var = new Environment.Variable();
    var.setKey(key);
    var.setValue(value);
    task.addConfiguredProperty(var);
  }
}
