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

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class SonarTaskUtilsTest {

  @Test
  public void shouldGetJarPath() {
    assertThat(SonarTaskUtils.getJarPath()).isNotNull();
  }

  @Test
  public void shouldExtractURI() {
    assertThat(SonarTaskUtils.extractURI(null, "jar:file:/temp/foo.jar!bar")).isEqualTo("file:/temp/foo.jar");
    assertThat(SonarTaskUtils.extractURI("/mypackage/myClass.class", "file:/temp/foo.jar/mypackage/myClass.class")).isEqualTo("file:/temp/foo.jar");
    assertThat(SonarTaskUtils.extractURI(null, "/temp/foo.jar")).isNull();
  }

  @Test
  public void shouldGetAntLoggerLever() {
    Project project = new Project();
    DefaultLogger logger = new DefaultLogger();
    logger.setMessageOutputLevel(2);
    project.addBuildListener(logger);

    assertThat(SonarTaskUtils.getAntLoggerLever(project)).isEqualTo(2);
  }

  @Test
  public void shouldGetVersion() {
    String version = SonarTaskUtils.getTaskVersion();
    assertThat(version).contains(".");
    assertThat(version).doesNotContain("$");
  }

}
