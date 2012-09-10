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

package org.sonar.ant.utils;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.resources.Union;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtilsTest {

  @Test
  public void shouldSetPathProperty() {
    Project project = mock(Project.class);
    Path path1 = new Path(project);
    File file1 = new File("foo.jar");
    path1.setLocation(file1);
    Path path2 = new Path(project);
    File file2 = new File("bar.jar");
    path1.setLocation(file2);
    Union union = new Union();
    union.add(path1);
    union.add(path2);

    when(project.getReference("foo")).thenReturn(union);

    Properties props = new Properties();
    SonarAntTaskUtils.setPathProperty(props, project, "foo");

    assertThat(props.getProperty("foo")).isEqualTo(file1.getAbsolutePath() + "," + file2.getAbsolutePath());
  }

  @Test
  public void shouldNotSetPathProperty() {
    Project project = mock(Project.class);

    Properties props = new Properties();
    SonarAntTaskUtils.setPathProperty(props, project, "foo");

    assertThat(props.getProperty("foo")).isNull();
  }

  @Test
  public void shouldGetJarPath() {
    assertThat(SonarAntTaskUtils.getJarPath(), not(nullValue()));
  }

  @Test
  public void shouldGetAntLoggerLever() {
    Project project = new Project();
    DefaultLogger logger = new DefaultLogger();
    logger.setMessageOutputLevel(2);
    project.addBuildListener(logger);

    assertThat(SonarAntTaskUtils.getAntLoggerLever(project), is(2));
  }

  @Test
  public void shouldGetVersion() {
    String version = SonarAntTaskUtils.getTaskVersion();
    assertThat(version, containsString("."));
    assertThat(version, not(containsString("$")));
  }

}
