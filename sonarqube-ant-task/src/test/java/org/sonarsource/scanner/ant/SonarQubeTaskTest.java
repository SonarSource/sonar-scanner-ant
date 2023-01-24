/*
 * SonarQube Scanner for Ant
 * Copyright (C) 2011-2023 SonarSource SA
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonarsource.scanner.api.LogOutput.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarQubeTaskTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private SonarQubeTask task;
  private Project project;

  @Test
  public void testAntProjectPropertiesPassedToSonarRunner() throws IOException {
    project = mock(Project.class);

    Hashtable<String, Object> props = new Hashtable<>();
    props.put("sonar.foo", "bar");
    when(project.getProperties()).thenReturn(props);

    execute();

    ArgumentCaptor<Map<String, String>> argument = ArgumentCaptor.forClass(Map.class);
    verify(task).launchAnalysis(argument.capture());
    assertThat(argument.getValue().get("sonar.foo")).isEqualTo("bar");
  }

  @Test
  public void testSkip() throws IOException {
    project = mock(Project.class);

    Hashtable<String, Object> props = new Hashtable<>();
    props.put("sonar.scanner.skip", "true");
    when(project.getProperties()).thenReturn(props);

    execute();

    verify(task, never()).launchAnalysis(any());
  }

  private void execute() throws IOException {
    execute(Collections.emptyMap());
  }

  private void execute(Map<String, String> env) throws IOException {
    task = new SonarQubeTask();

    when(project.getBaseDir()).thenReturn(folder.newFolder());
    task.setProject(project);
    task = spy(task);
    when(task.getEnv()).thenReturn(env);
    doNothing().when(task).launchAnalysis(any(Map.class));

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

  @Test
  public void testLogLevel() {
    task = new SonarQubeTask();
    SonarQubeTask.LogOutputImplementation logOutput = spy(task.new LogOutputImplementation());
    logOutput.log("Message", Level.TRACE);
    verify(logOutput).logWithTaskLogger("Message", Project.MSG_DEBUG);
    logOutput.log("Message", Level.DEBUG);
    verify(logOutput).logWithTaskLogger("Message", Project.MSG_VERBOSE);
    logOutput.log("Message", Level.INFO);
    verify(logOutput).logWithTaskLogger("Message", Project.MSG_INFO);
    logOutput.log("Message", Level.WARN);
    verify(logOutput).logWithTaskLogger("Message", Project.MSG_WARN);
    logOutput.log("Message", Level.ERROR);
    verify(logOutput).logWithTaskLogger("Message", Project.MSG_ERR);
  }

  @Test
  public void readPropsFromEnvVariable() throws IOException {
    project = mock(Project.class);
    when(project.getProperties()).thenReturn(new Hashtable<>());

    HashMap<String, String> env = new HashMap<>();
    env.put("SONARQUBE_SCANNER_PARAMS", "{\"sonar.foo\": \"bar\"}");

    execute(env);

    ArgumentCaptor<Map<String, String>> argument = ArgumentCaptor.forClass(Map.class);
    verify(task).launchAnalysis(argument.capture());
    assertThat(argument.getValue().get("sonar.foo")).isEqualTo("bar");
  }

  @Test
  public void simulationMode() throws IOException {
    project = mock(Project.class);
    Hashtable<String, Object> props = new Hashtable<>();
    File out = folder.newFile();
    props.put("sonar.scanner.dumpToFile", out.getAbsolutePath());
    when(project.getProperties()).thenReturn(props);

    task = new SonarQubeTask();

    File baseDir = folder.newFolder();
    when(project.getBaseDir()).thenReturn(baseDir);
    task.setProject(project);

    task.execute();

    Properties outProps = new Properties();
    try (BufferedReader reader = Files.newBufferedReader(out.toPath(), StandardCharsets.UTF_8)) {
      outProps.load(reader);
    }

    assertThat(outProps.entrySet()).extracting(Map.Entry::getKey, Map.Entry::getValue)
      .contains(
        tuple("sonar.projectBaseDir", baseDir.getAbsolutePath()),
        tuple("sonar.scanner.app", "Ant"));
  }

  private void testSonarVerboseForAntLevel(int antLevel, String sonarVerboseValue) throws IOException {
    project = mock(Project.class);

    Hashtable<String, Object> props = new Hashtable<>();
    when(project.getProperties()).thenReturn(props);

    when(project.getBuildListeners()).thenReturn(new Vector(Arrays.asList(new MyCustomAntLogger(antLevel))));

    execute();

    ArgumentCaptor<Map<String, String>> argument = ArgumentCaptor.forClass(Map.class);
    verify(task).launchAnalysis(argument.capture());
    assertThat(argument.getValue().get("sonar.verbose")).isEqualTo(sonarVerboseValue);
  }

  private static class MyCustomAntLogger extends DefaultLogger {

    public MyCustomAntLogger(int level) {
      this.msgOutputLevel = level;
    }

  }

}
