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
import org.apache.tools.ant.BuildException;
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

  public void execute(SonarTask task) {
    this.task = task;
    try {
      initLogging();
      executeBatch();
    } catch (Exception e) {
      throw new BuildException("Failed to execute Sonar", e);
    }
  }

  /**
   * Transforms {@link ProjectElement} into {@link ProjectDefinition}.
   */
  private ProjectDefinition defineProject() {
    ProjectElement project = task.createProject();

    Properties properties = new Properties();
    ProjectDefinition definition = new ProjectDefinition(task.getProject().getBaseDir(), properties);

    // Properties
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, project.getKey());
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, project.getVersion());

    Enumeration<Variable> e = project.getProperties().getVariablesVector().elements();
    while (e.hasMoreElements()) {
      Variable property = e.nextElement();
      String key = property.getKey();
      String value = property.getValue();
      properties.setProperty(key, value);
    }

    // Binaries (classes and libraries)
    StringBuilder sb = new StringBuilder();
    for (Iterator<?> i = project.createBinaries().iterator(); i.hasNext();) {
      Resource resource = (Resource) i.next();
      if (resource instanceof FileResource) {
        File fileResource = ((FileResource) resource).getFile();
        sb.append(fileResource.getAbsolutePath()).append(',');
      }
    }
    properties.setProperty("sonar.projectBinaries", sb.toString());

    // Source directories
    for (Iterator<?> i = project.createSources().iterator(); i.hasNext();) {
      Resource resource = (Resource) i.next();
      if (resource.isDirectory() && resource instanceof FileResource) {
        File dir = ((FileResource) resource).getFile();
        definition.addSourceDir(dir.getAbsolutePath());
      }
    }

    // TODO test directories

    return definition;
  }

  private void executeBatch() throws Exception {
    Reactor reactor = new Reactor(defineProject());
    Batch batch = new Batch(getInitialConfiguration(), Environment.ANT, reactor);
    batch.execute();
  }

  private void initLogging() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(context);
    context.reset();
    InputStream input = Batch.class.getResourceAsStream("/org/sonar/batch/logback.xml");

    System.setProperty("ROOT_LOGGER_LEVEL", task.getLoggerLevel());
    try {
      jc.doConfigure(input);

    } catch (JoranException e) {
      throw new SonarException("can not initialize logging", e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  private Configuration getInitialConfiguration() {
    CompositeConfiguration configuration = new CompositeConfiguration();
    configuration.addConfiguration(new SystemConfiguration());
    configuration.addConfiguration(new EnvironmentConfiguration());
    // TODO configuration.addConfiguration(new MapConfiguration(project.getProperties()));
    Configuration projectProperties = new BaseConfiguration();
    configuration.addConfiguration(projectProperties);
    return configuration;
  }

}
