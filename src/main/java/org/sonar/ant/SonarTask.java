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

import com.google.common.annotations.VisibleForTesting;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Task;
import org.sonar.runner.api.EmbeddedRunner;

import java.util.Properties;

public class SonarTask extends Task {

  private static final String PROJECT_BASEDIR_PROPERTY = "sonar.projectBaseDir";
  private static final String VERBOSE_PROPERTY = "sonar.verbose";

  @SuppressWarnings("unchecked")
  @Override
  public void execute() {
    log(Main.getAntVersion());
    log("SonarQube Ant Task version: " + SonarTaskUtils.getTaskVersion());
    log("Loaded from: " + SonarTaskUtils.getJarPath());

    Properties allProps = new Properties();
    allProps.put(PROJECT_BASEDIR_PROPERTY, getProject().getBaseDir().getAbsolutePath());
    if (SonarTaskUtils.getAntLoggerLever(getProject()) >= 3) {
      allProps.put(VERBOSE_PROPERTY, "true");
    }
    allProps.putAll(getProject().getProperties());
    launchAnalysis(allProps);
  }

  @VisibleForTesting
  void launchAnalysis(Properties properties) {
    EmbeddedRunner.create()
      .addProperties(properties)
      .unmask("org.apache.tools.ant")
      .unmask("org.sonar.ant")
      .setApp("Ant", SonarTaskUtils.getTaskVersion())
      .addExtensions(getProject())
      .execute();
  }

}
