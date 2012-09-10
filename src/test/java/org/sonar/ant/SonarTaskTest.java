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
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class SonarTaskTest {

  private SonarTask task;

  @Before
  public void init() {
    task = new SonarTask();
    task.setProject(new Project());
  }

  @Test
  public void shouldReturnDefaultValues() {
    assertThat(task.getServerUrl()).isEqualTo("http://localhost:9000");
    assertThat(task.getBaseDir()).isEqualTo(task.getProject().getBaseDir());
    assertThat(task.getWorkDir()).isEqualTo(new File(task.getProject().getBaseDir(), ".sonar"));
  }

  @Test
  public void shouldSetBaseAndWorkingDirectories() {
    // set base dir
    task.setBaseDir(new File("foo"));
    assertThat(task.getBaseDir()).isEqualTo(new File("foo"));

    // set relative working dir through property
    task.getProject().setProperty("sonar.working.directory", ".bar");
    assertThat(task.getWorkDir()).isEqualTo(new File("foo/.bar"));

    // for code coverage ;-)
    assertThat(task.getWorkDir()).isEqualTo(new File("foo/.bar"));
  }

  @Test
  public void shouldNotUseEmptySonarWorkingDirectoryProperty() {
    task.getProject().setProperty("sonar.working.directory", "");
    assertThat(task.getWorkDir()).isEqualTo(new File(task.getProject().getBaseDir(), ".sonar"));
  }

  @Test
  public void shouldSetAbsoluteWorkingDirectory() {
    File file = new File("src");
    task.getProject().setProperty("sonar.working.directory", file.getAbsolutePath());
    assertThat(task.getWorkDir()).isEqualTo(file.getAbsoluteFile());
  }

  @Test
  public void testIsCompatibilityModeActivated() {
    Properties props = new Properties();

    // default value
    assertThat(task.isCompatibilityModeActivated(props)).isFalse();

    // CASE 1: value set by something else
    task.setCompatibilityMode(true);
    assertThat(task.isCompatibilityModeActivated(props)).isTrue();
    task.setCompatibilityMode(false); // reset to default value

    // CASE 2: value correctly set through the property, even with bogus case
    props.setProperty("sonar.anttask.compatibilitymode", "oN");
    assertThat(task.isCompatibilityModeActivated(props)).isTrue();
    props.remove("sonar.anttask.compatibilitymode"); // remove the prop

    // this does not happen if value wrongly set through the property
    props.setProperty("sonar.anttask.compatibilitymode", "blabla");
    assertThat(task.isCompatibilityModeActivated(props)).isFalse();

    // CASE 3: "sonar.modules" refers to XML files
    props.setProperty("sonar.modules", "module1/build.xml, module2/build.xml");
    assertThat(task.isCompatibilityModeActivated(props)).isTrue();

    // This does not happen if "sonar.modules" does not refer to XML files
    props.setProperty("sonar.modules", "module1, module2");
    assertThat(task.isCompatibilityModeActivated(props)).isFalse();

    // CASE 4: a deprecated property is used
    task.setInitTarget("blabla");
    assertThat(task.isCompatibilityModeActivated(props)).isTrue();
  }

  @Test
  public void testGetListFromProperty() {
    Properties props = new Properties();
    assertThat(task.getListFromProperty(props, "tutu").length).isEqualTo(0);

    props.put("prop", "  foo  ,  bar  , \n\ntoto,tutu");
    assertThat(task.getListFromProperty(props, "prop")).containsOnly("foo", "bar", "toto", "tutu");
  }

}
