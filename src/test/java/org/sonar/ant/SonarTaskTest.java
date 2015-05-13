/*
 * SonarQube Ant Task
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

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarTaskTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private SonarTask task;
  private Project project;

  @Test
  public void testAntProjectPropertiesPassedToSonarRunner() throws IOException {
    project = mock(Project.class);

    Properties props = new Properties();
    props.put("sonar.foo", "bar");
    when(project.getProperties()).thenReturn(props);

    execute();

    ArgumentCaptor<Properties> argument = ArgumentCaptor.forClass(Properties.class);
    verify(task).launchAnalysis(argument.capture());
    assertThat(argument.getValue().getProperty("sonar.foo")).isEqualTo("bar");

  }

  private void execute() throws IOException {
    task = new SonarTask();

    when(project.getBaseDir()).thenReturn(folder.newFolder());
    task.setProject(project);
    task = spy(task);
    doNothing().when(task).launchAnalysis(any(Properties.class));

    task.execute();
  }

  // SONARPLUGINS-2840
  @Test
  public void testSonarVerbose() throws IOException {
    testSonarVerboseForAntLevel(1, null);
    testSonarVerboseForAntLevel(2, null);
    testSonarVerboseForAntLevel(3, "true");
    testSonarVerboseForAntLevel(4, "true");

  }

  private void testSonarVerboseForAntLevel(int antLevel, String sonarVerboseValue) throws IOException {
    project = mock(Project.class);

    Properties props = new Properties();
    when(project.getProperties()).thenReturn(props);

    when(project.getBuildListeners()).thenReturn(new Vector(Arrays.asList(new MyCustomAntLogger(antLevel))));

    execute();

    ArgumentCaptor<Properties> argument = ArgumentCaptor.forClass(Properties.class);
    verify(task).launchAnalysis(argument.capture());
    assertThat(argument.getValue().getProperty("sonar.verbose")).isEqualTo(sonarVerboseValue);
  }

  private static class MyCustomAntLogger extends DefaultLogger {

    public MyCustomAntLogger(int level) {
      this.msgOutputLevel = level;
    }

  }

}
