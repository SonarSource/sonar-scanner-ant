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

package org.sonar.ant.deprecated;

import org.apache.tools.ant.Project;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ant.SonarTask;

import static org.fest.assertions.Assertions.assertThat;

public class OldSonarTaskExecutorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldCheckVersion() {
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("1.0")).isEqualTo(true);
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("2.0")).isEqualTo(true);
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("2.1")).isEqualTo(true);
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("2.2")).isEqualTo(true);
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("2.3")).isEqualTo(true);
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("2.4")).isEqualTo(true);
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("2.4.1")).isEqualTo(true);
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("2.5")).isEqualTo(true);
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("2.6")).isEqualTo(true);
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("2.7")).isEqualTo(true);
    assertThat(OldSonarTaskExecutor.isVersionPriorTo2Dot8("2.8")).isEqualTo(false);
  }

  @Test
  public void shouldFailIfMandatoryPropertiesMissing() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("The following mandatory information is missing:");
    thrown.expectMessage("- task attribute 'key'");
    thrown.expectMessage("- task attribute 'version'");
    thrown.expectMessage("- task attribute 'sources' or property 'sonar.sources'");

    SonarTask task = new SonarTask();
    task.setProject(new Project());
    OldSonarTaskExecutor executor = new OldSonarTaskExecutor(task);

    executor.checkMandatoryProperties();
  }

  @Test
  public void shouldNotFailIfMandatoryPropertiesPresentWithSystemProp() {
    SonarTask task = new SonarTask();
    task.setProject(new Project());
    task.setKey("foo");
    task.setVersion("2");
    System.setProperty("sonar.sources", "src");

    OldSonarTaskExecutor executor = new OldSonarTaskExecutor(task);

    executor.checkMandatoryProperties();

    System.clearProperty("sonar.sources");
  }

  @Test
  public void shouldNotFailIfMandatoryPropertiesPresentWithProjectProp() {
    SonarTask task = new SonarTask();
    Project project = new Project();
    project.setProperty("sonar.sources", "src");
    task.setProject(project);
    task.setKey("foo");
    task.setVersion("2");

    OldSonarTaskExecutor executor = new OldSonarTaskExecutor(task);

    executor.checkMandatoryProperties();
  }

  @Test
  public void shouldNotFailIfMandatoryPropertiesPresentWithTaskProp() {
    SonarTask task = new SonarTask();
    task.getProperties().put("sonar.sources", "src");
    task.setProject(new Project());
    task.setKey("foo");
    task.setVersion("2");

    OldSonarTaskExecutor executor = new OldSonarTaskExecutor(task);

    executor.checkMandatoryProperties();
  }

  @Test
  public void shouldNotFailIfMandatoryPropertiesNotPresentButMultiModules() {
    SonarTask task = new SonarTask();
    task.getProperties().put("sonar.modules", "foo/build.xml,bar/build.xml");
    task.setProject(new Project());
    task.setKey("foo");
    task.setVersion("2");

    OldSonarTaskExecutor executor = new OldSonarTaskExecutor(task);

    executor.checkMandatoryProperties();
  }

}
