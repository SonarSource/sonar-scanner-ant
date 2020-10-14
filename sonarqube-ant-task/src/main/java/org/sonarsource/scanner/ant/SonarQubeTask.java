/*
 * SonarQube Scanner for Ant
 * Copyright (C) 2011-2019 SonarSource SA
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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.sonarsource.scanner.api.EmbeddedScanner;
import org.sonarsource.scanner.api.LogOutput;
import org.sonarsource.scanner.api.ScanProperties;
import org.sonarsource.scanner.api.Utils;

import static java.util.stream.Collectors.toMap;

public class SonarQubeTask extends Task {

  class LogOutputImplementation implements LogOutput {
    @Override
    public void log(String formattedMessage, Level level) {
      logWithTaskLogger(formattedMessage, toAntLevel(level));
    }

    // Visible for mocking
    void logWithTaskLogger(String formattedMessage, int msgLevel) {
      SonarQubeTask.this.log(formattedMessage, msgLevel);
    }

    private int toAntLevel(LogOutput.Level level) {
      switch (level) {
        case TRACE:
          return Project.MSG_DEBUG;
        case DEBUG:
          return Project.MSG_VERBOSE;
        case INFO:
          return Project.MSG_INFO;
        case WARN:
          return Project.MSG_WARN;
        case ERROR:
          return Project.MSG_ERR;
        default:
          throw new IllegalArgumentException(level.name());
      }
    }
  }

  private static final String PROJECT_BASEDIR_PROPERTY = "sonar.projectBaseDir";
  private static final String VERBOSE_PROPERTY = "sonar.verbose";

  @SuppressWarnings("unchecked")
  @Override
  public void execute() {
    log(Main.getAntVersion());
    log("SonarQube Ant Task version: " + SonarQubeTaskUtils.getTaskVersion());
    log("Loaded from: " + SonarQubeTaskUtils.getJarPath());

    Map<String, String> allProps = new HashMap<>();
    allProps.put(PROJECT_BASEDIR_PROPERTY, getProject().getBaseDir().getAbsolutePath());
    if (SonarQubeTaskUtils.getAntLoggerLever(getProject()) >= 3) {
      allProps.put(VERBOSE_PROPERTY, "true");
    }

    putAll(Utils.loadEnvironmentProperties(getEnv()), allProps);
    allProps.putAll(
      getProject().getProperties()
        .entrySet()
        .stream()
        .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().toString())));

    if ("true".equalsIgnoreCase(allProps.get(ScanProperties.SKIP))) {
      log("SonarQube Scanner analysis skipped");
      return;
    }

    launchAnalysis(allProps);
  }

  // Visible for mocking
  Map<String, String> getEnv() {
    return System.getenv();
  }

  static void putAll(Properties src, Map<String, String> dest) {
    for (final String name : src.stringPropertyNames()) {
      dest.put(name, src.getProperty(name));
    }
  }

  // VisibleForTesting
  void launchAnalysis(Map<String, String> properties) {
    EmbeddedScanner runner = EmbeddedScanner.create("Ant", SonarQubeTaskUtils.getTaskVersion(), new LogOutputImplementation())
      .addGlobalProperties(properties);
    runner.start();
    runner.execute(properties);
  }

}
