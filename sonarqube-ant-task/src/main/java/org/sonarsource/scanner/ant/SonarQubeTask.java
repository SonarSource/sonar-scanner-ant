/*
 * SonarQube Scanner for Ant
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Properties;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.sonarsource.scanner.api.EmbeddedScanner;
import org.sonarsource.scanner.api.LogOutput;
import org.sonarsource.scanner.api.ScanProperties;
import org.sonarsource.scanner.api.Utils;

public class SonarQubeTask extends Task {

  private final class LogOutputImplementation implements LogOutput {
    @Override
    public void log(String formattedMessage, Level level) {
      SonarQubeTask.this.log(formattedMessage, toAntLevel(level));
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

    Properties allProps = new Properties();
    allProps.put(PROJECT_BASEDIR_PROPERTY, getProject().getBaseDir().getAbsolutePath());
    if (SonarQubeTaskUtils.getAntLoggerLever(getProject()) >= 3) {
      allProps.put(VERBOSE_PROPERTY, "true");
    }

    allProps.putAll(Utils.loadEnvironmentProperties(System.getenv()));
    allProps.putAll(getProject().getProperties());

    if ("true".equalsIgnoreCase(allProps.getProperty(ScanProperties.SKIP))) {
      log("SonarQube Scanner analysis skipped");
      return;
    }

    launchAnalysis(allProps);
  }

  // VisibleForTesting
  void launchAnalysis(Properties properties) {
    EmbeddedScanner runner = EmbeddedScanner.create(new LogOutputImplementation())
      .addGlobalProperties(properties)
      .unmask("org.apache.tools.ant")
      .unmask("org.sonar.ant")
      .setApp("Ant", SonarQubeTaskUtils.getTaskVersion());

    try {
      runner.start();
      try {
        runner.addExtensions(getProject());
      } catch (Exception e) {
        // Not supported in recent SQ versions. Ignore
      }
      runner.runAnalysis(properties);
    } finally {
      runner.stop();
    }
  }

}
