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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.apache.commons.configuration.*;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.types.Environment.Variable;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.platform.Environment;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.Batch;
import org.sonar.batch.bootstrapper.ProjectDefinition;
import org.sonar.batch.bootstrapper.Reactor;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

public class Launcher {

  private SonarTask task;

  public Launcher(SonarTask task) {
    this.task = task;
  }

  /**
   * This method invoked from {@link SonarTask}.
   */
  public void execute() {
    initLogging();
    executeBatch();
  }

  /**
   * Transforms {@link ProjectElement} into {@link ProjectDefinition}.
   */
  private ProjectDefinition defineProject() {
    Properties properties = new Properties();
    File baseDir = task.getProject().getBaseDir();
    File workDir = task.getWorkDir();
    ProjectDefinition definition = new ProjectDefinition(baseDir, workDir, properties);

    // Properties
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, task.getKey());
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, task.getVersion());

    Enumeration<Variable> e = task.getProperties().getVariablesVector().elements();
    while (e.hasMoreElements()) {
      Variable property = e.nextElement();
      String key = property.getKey();
      String value = property.getValue();
      properties.setProperty(key, value);
    }

    // Binaries (classes and libraries)
    StringBuilder sb = new StringBuilder();
    for (Iterator<?> i = task.createBinaries().iterator(); i.hasNext();) {
      Resource resource = (Resource) i.next();
      if (resource instanceof FileResource) {
        File fileResource = ((FileResource) resource).getFile();
        sb.append(fileResource.getAbsolutePath()).append(',');
      }
    }
    properties.setProperty("sonar.projectBinaries", sb.toString());

    // Source directories
    for (Iterator<?> i = task.createSources().iterator(); i.hasNext();) {
      Resource resource = (Resource) i.next();
      if (resource.isDirectory() && resource instanceof FileResource) {
        File dir = ((FileResource) resource).getFile();
        definition.addSourceDir(dir.getAbsolutePath());
      }
    }

    // TODO test directories

    return definition;
  }

  private void executeBatch() {
    ProjectDefinition project = defineProject();
    Reactor reactor = new Reactor(project);
    Batch batch = new Batch(getInitialConfiguration(project), Environment.ANT, reactor);
    batch.execute();
  }

  private void initLogging() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(context);
    context.reset();
    InputStream input = Batch.class.getResourceAsStream("/org/sonar/batch/logback.xml");

    System.setProperty("ROOT_LOGGER_LEVEL", getLoggerLevel());
    try {
      jc.doConfigure(input);

    } catch (JoranException e) {
      throw new SonarException("Can not initialize logging", e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  private String getLoggerLevel() {
    int antLoggerLevel = Utils.getAntLoggerLever(task.getProject());
    switch (antLoggerLevel) {
      case 3:
        return "DEBUG";
      case 4:
        return "TRACE";
      default:
        return "INFO";
    }
  }

  private Configuration getInitialConfiguration(ProjectDefinition project) {
    CompositeConfiguration configuration = new CompositeConfiguration();
    configuration.addConfiguration(new SystemConfiguration());
    configuration.addConfiguration(new EnvironmentConfiguration());
    // Ant properties
    configuration.addConfiguration(new MapConfiguration(task.getProject().getProperties()));
    // Task properties
    configuration.addConfiguration(new MapConfiguration(project.getProperties()));
    return configuration;
  }

}
