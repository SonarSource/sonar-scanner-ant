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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.sonar.ant.utils.SonarAntTaskUtils;
import org.sonar.runner.DeprecatedAntTaskExecutor;
import org.sonar.runner.Runner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class SonarTask extends Task {

  private static final String HOST_PROPERTY = "sonar.host.url";
  private static final String PROPERTY_WORK_DIRECTORY = "sonar.working.directory";
  private static final String DEF_VALUE_WORK_DIRECTORY = ".sonar";

  private File workDir;
  private File baseDir;

  // BEGIN of deprecated fields/properties list used only by the DeprecatedAntTaskExecutor and the associated compatibility mode
  private static final String ENV_PROPERTY_COMPATIBILITY_MODE = "SONAR_ANT_TASK_COMPATIBILITY_MODE";
  private static final String PROPERTY_COMPATIBILITY_MODE = "sonar.anttask.compatibilitymode";
  private static final String COMPATIBILITY_MODE_ON = "on";
  private boolean compatibilityMode = false;

  private Properties taskProperties = new Properties();
  private String key;
  private String version;
  private Path sources;
  private Path tests;
  private Path binaries;
  private Path libraries;
  private String initTarget;

  // END of deprecated properties

  @SuppressWarnings("unchecked")
  @Override
  public void execute() {
    log(Main.getAntVersion());
    log("Sonar Ant Task version: " + SonarAntTaskUtils.getTaskVersion());
    log("Loaded from: " + SonarAntTaskUtils.getJarPath());
    log("Sonar work directory: " + getWorkDir().getAbsolutePath());
    log("Sonar server: " + getServerUrl());

    Properties allProps = new Properties();
    allProps.putAll(getProject().getProperties());
    launchAnalysis(allProps);
  }

  private void launchAnalysis(Properties properties) {
    if (isCompatibilityModeActivated(properties)) {
      // Compatibility mode is activated to prevent issues with the standard way to execute analyses (= with the Sonar Runner)
      log("/!\\ Sonar Ant Task running in compatibility mode: please refer to the documentation to udpate your scripts to comply with the standards.",
        Project.MSG_WARN);
      DeprecatedAntTaskExecutor deprecatedAntTaskExecutor = new DeprecatedAntTaskExecutor(this);
      deprecatedAntTaskExecutor.execute();
    } else {
      // Standard mode
      Runner runner = Runner.create(properties, baseDir);
      runner.setUnmaskedPackages("org.apache.tools.ant", "org.sonar.ant");
      runner.setEnvironmentInformation("Ant", SonarAntTaskUtils.getTaskVersion());
      runner.addContainerExtension(getProject());
      runner.execute();
    }
  }

  /**
   * Compatibility mode is activated if:
   * - at least one of the old deprecated properties has been set
   * - "sonar.anttask.compatibilitymode=on" has been passed to the JVM
   * - SONAR_ANT_TASK_COMPATIBILITY_MODE env variable exists and is set to "on"
   * - "sonar.modules" exists and references XML files
   */
  @VisibleForTesting
  protected boolean isCompatibilityModeActivated(Properties properties) {
    return compatibilityMode
      || COMPATIBILITY_MODE_ON.equalsIgnoreCase(properties.getProperty(PROPERTY_COMPATIBILITY_MODE, ""))
      || COMPATIBILITY_MODE_ON.equalsIgnoreCase(System.getenv(ENV_PROPERTY_COMPATIBILITY_MODE))
      || modulesPropertyReferencesXmlFiles(properties);
  }

  private boolean modulesPropertyReferencesXmlFiles(Properties properties) {
    String[] modules = getListFromProperty(properties, "sonar.modules");
    for (String module : modules) {
      if (module.endsWith(".xml")) {
        return true;
      }
    }
    return false;
  }

  @VisibleForTesting
  protected String[] getListFromProperty(Properties properties, String key) {
    String list = properties.getProperty(key);
    if (list == null) {
      return new String[0];
    }

    List<String> values = new ArrayList<String>();
    StringTokenizer tokenizer = new StringTokenizer(list, ",");
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      values.add(token.trim());
    }
    return values.toArray(new String[values.size()]);
  }

  @VisibleForTesting
  protected void setCompatibilityMode(boolean compatibilityMode) {
    this.compatibilityMode = compatibilityMode;
  }

  /**
   * @return value of property "sonar.host.url", default is "http://localhost:9000"
   */
  public String getServerUrl() {
    String serverUrl = getProperties().getProperty(HOST_PROPERTY); // from task: will be dropped some day...
    if (serverUrl == null) {
      serverUrl = getProject().getProperty(HOST_PROPERTY); // from ant
    }
    if (serverUrl == null) {
      serverUrl = "http://localhost:9000"; // default
    }
    return serverUrl;
  }

  /**
   * @return work directory, default is ".sonar" in project directory
   */
  public File getWorkDir() {
    if (workDir == null) {
      String customPath = getProject().getProperty(PROPERTY_WORK_DIRECTORY);
      if (customPath == null || "".equals(customPath.trim())) {
        workDir = new File(getBaseDir(), DEF_VALUE_WORK_DIRECTORY);
      } else {
        workDir = new File(customPath);
        if (!workDir.isAbsolute()) {
          workDir = new File(getBaseDir(), customPath);
        }
      }
    }
    return workDir;
  }

  /**
   * @since 1.1
   */
  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  /**
   * @return base directory, default is the current project base directory
   * @since 1.1
   */
  public File getBaseDir() {
    if (baseDir == null) {
      baseDir = getProject().getBaseDir();
    }
    return baseDir;
  }

  /*
   * =============================================================================
   * 
   * Methods and related properties beyond this point are all deprecated.
   * 
   * They should be removed at some point of time, when backward compatibility is
   * not necessary any longer (= when the "org.sonar.ant.deprecated" package is
   * is removed)
   * 
   * =============================================================================
   */

  public void setKey(String key) {
    this.key = key;
    this.compatibilityMode = true;
  }

  public String getKey() {
    return key;
  }

  public void setVersion(String version) {
    this.version = version;
    this.compatibilityMode = true;
  }

  public String getVersion() {
    return version;
  }

  /**
   * @since 1.1
   */
  public void setInitTarget(String initTarget) {
    this.initTarget = initTarget;
    this.compatibilityMode = true;
  }

  /**
   * @since 1.1
   */
  public String getInitTarget() {
    return initTarget;
  }

  public Path getSources() {
    return sources;
  }

  /**
   * Note that name of this method is important - see http://ant.apache.org/manual/develop.html#nested-elements
   */
  public void addConfiguredProperty(Environment.Variable property) {
    taskProperties.setProperty(property.getKey(), property.getValue());
    this.compatibilityMode = true;
  }

  public Properties getProperties() {
    return taskProperties;
  }

  public Path createSources() {
    this.compatibilityMode = true;
    if (sources == null) {
      sources = new Path(getProject());
    }
    return sources;
  }

  public Path createTests() {
    this.compatibilityMode = true;
    if (tests == null) {
      tests = new Path(getProject());
    }
    return tests;
  }

  public Path createBinaries() {
    this.compatibilityMode = true;
    if (binaries == null) {
      binaries = new Path(getProject());
    }
    return binaries;
  }

  public Path createLibraries() {
    this.compatibilityMode = true;
    if (libraries == null) {
      libraries = new Path(getProject());
    }
    return libraries;
  }

}
