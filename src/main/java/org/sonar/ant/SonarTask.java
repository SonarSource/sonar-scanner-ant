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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.platform.Environment;
import org.sonar.api.project.ProjectDefinition;
import org.sonar.api.project.ProjectDirectory;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.*;

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;

public class SonarTask extends Task {

  private ProjectElement projectElement = new ProjectElement();

  public ProjectElement createProject() {
    return projectElement;
  }

  @Override
  public void execute() throws BuildException {
    try {
      System.out.println("Starting...");
      initLogging();
      executeBatch();
    } catch (Exception e) {
      throw new BuildException("Failed to execute Sonar", e);
    }
  }

  /**
   * Transforms {@link ProjectElement} into {@link ProjectDefinition}.
   */
  private DefaultProjectDefinition defineProject(ProjectElement project) {
    DefaultProjectDefinition definition = new DefaultProjectDefinition();

    definition.setKey(project.getKey());

    definition.setSonarWorkingDirectory(new File("/tmp/ant-test")); // TODO hard-coded value

    for (Iterator<?> i = project.createSources().iterator(); i.hasNext();) {
      Resource resource = (Resource) i.next();
      if (resource.isDirectory() && resource instanceof FileResource) {
        File dir = ((FileResource) resource).getFile();

        DefaultProjectDirectory directory = new DefaultProjectDirectory();
        directory.setKind(ProjectDirectory.Kind.SOURCES);
        directory.setLocation(dir);

        definition.addDir(directory);
      }
    }

    return definition;
  }

  private void executeBatch() throws Exception {
    Reactor reactor = new Reactor(defineProject(projectElement));
    Batch batch = new Batch(getInitialConfiguration(),
        Environment.ANT, new FakeMavenPluginExecutor(), reactor);
    batch.execute();
  }

  private void initLogging() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(context);
    context.reset();
    InputStream input = Batch.class.getResourceAsStream("/org/sonar/batch/logback.xml");
    // System.setProperty("ROOT_LOGGER_LEVEL", getLog().isDebugEnabled() ? "DEBUG" : "INFO");
    System.setProperty("ROOT_LOGGER_LEVEL", "DEBUG");
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
    projectProperties.setProperty("sonar.core.codeCoveragePlugin", "none");
    configuration.addConfiguration(projectProperties);
    return configuration;
  }

  public static class FakeMavenPluginExecutor implements MavenPluginExecutor {
    public void execute(Project project, String goal) {
    }

    public MavenPluginHandler execute(Project project, MavenPluginHandler handler) {
      return handler;
    }
  }

}
