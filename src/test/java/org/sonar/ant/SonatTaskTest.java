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

import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class SonatTaskTest {
  @Test
  public void shouldCheckVersion() {
    assertThat(SonarTask.isVersionPriorTo2Dot6("1.0"), is(true));
    assertThat(SonarTask.isVersionPriorTo2Dot6("2.0"), is(true));
    assertThat(SonarTask.isVersionPriorTo2Dot6("2.1"), is(true));
    assertThat(SonarTask.isVersionPriorTo2Dot6("2.2"), is(true));
    assertThat(SonarTask.isVersionPriorTo2Dot6("2.3"), is(true));
    assertThat(SonarTask.isVersionPriorTo2Dot6("2.4"), is(true));
    assertThat(SonarTask.isVersionPriorTo2Dot6("2.4.1"), is(true));
    assertThat(SonarTask.isVersionPriorTo2Dot6("2.5"), is(true));
  }

  @Test
  public void shouldGetVersion() {
    String version = SonarTask.getTaskVersion();
    assertThat(version, containsString("."));
    assertThat(version, not(containsString("$")));
  }

  @Test
  public void shouldGetJarPath() {
    assertThat(SonarTask.getJarPath(), not(nullValue()));
  }

  @Test
  public void defaultValues() {
    SonarTask task = new SonarTask();
    assertThat(task.getServerUrl(), is("http://localhost:9000"));
    assertThat(task.getLoggerLevel(), is("INFO"));
  }

}
