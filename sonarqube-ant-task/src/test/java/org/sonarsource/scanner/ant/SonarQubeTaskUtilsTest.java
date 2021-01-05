/*
 * SonarQube Scanner for Ant
 * Copyright (C) 2011-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.ant;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarQubeTaskUtilsTest {

  @Test
  public void shouldGetJarPath() {
    assertThat(SonarQubeTaskUtils.getJarPath()).isNotNull();
  }

  @Test
  public void shouldExtractURI() {
    assertThat(SonarQubeTaskUtils.extractURI(null, "jar:file:/temp/foo.jar!bar")).isEqualTo("file:/temp/foo.jar");
    assertThat(SonarQubeTaskUtils.extractURI("/mypackage/myClass.class", "file:/temp/foo.jar/mypackage/myClass.class")).isEqualTo("file:/temp/foo.jar");
    assertThat(SonarQubeTaskUtils.extractURI(null, "/temp/foo.jar")).isNull();
  }

  @Test
  public void shouldGetAntLoggerLever() {
    Project project = new Project();
    DefaultLogger logger = new DefaultLogger();
    logger.setMessageOutputLevel(2);
    project.addBuildListener(logger);

    assertThat(SonarQubeTaskUtils.getAntLoggerLever(project)).isEqualTo(2);
  }

  @Test
  public void shouldGetVersion() {
    String version = SonarQubeTaskUtils.getTaskVersion();
    assertThat(version).contains(".");
    assertThat(version).doesNotContain("$");
  }

}
