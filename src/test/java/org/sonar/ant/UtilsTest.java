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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.resources.Union;
import org.junit.Test;

public class UtilsTest {

  @Test
  public void test() {
    Project project = mock(Project.class);
    Path path1 = new Path(project);
    path1.setPath("/tmp/foo.jar");
    Path path2 = new Path(project);
    path2.setPath("/tmp/bar.jar");
    Union union = new Union();
    union.add(path1);
    union.add(path2);

    assertThat(Utils.convertResourceCollectionToString(union), is("/tmp/foo.jar,/tmp/bar.jar"));
  }

  @Test
  public void shouldGetJarPath() {
    assertThat(Utils.getJarPath(), not(nullValue()));
  }

  @Test
  public void shouldGetAntLoggerLever() {
    Project project = new Project();
    DefaultLogger logger = new DefaultLogger();
    logger.setMessageOutputLevel(2);
    project.addBuildListener(logger);

    assertThat(Utils.getAntLoggerLever(project), is(2));
  }

}
