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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.bootstrapper.ProjectDefinition;
import org.sonar.batch.bootstrapper.Reactor;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class Launcher {

  private static final String INFO = "INFO";
  private static final String WARN = "WARN";
  private static final String DEBUG = "DEBUG";
  private static final String TRACE = "TRACE";
  private SonarTask task;

  public Launcher(SonarTask task) {
    this.task = task;
  }

  /**
   * This method invoked from {@link SonarTask}.
   */
  public void execute() {
    ProjectDefinition project = defineProject();
    Reactor reactor = new Reactor(project);
    Configuration config = getInitialConfiguration(project);
    initLogging(config);
    Batch batch = new Batch(config, new EnvironmentInformation("Ant", Main.getAntVersion()), reactor);
    batch.execute();
  }

  ProjectDefinition defineProject() {
    Project antProject = task.getProject();
    Properties properties = new Properties();
    ProjectDefinition definition = new ProjectDefinition(task.getBaseDir(), task.getWorkDir(), properties);

    definition.addContainerExtension(antProject);

    // Properties from task attributes
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, task.getKey());
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, task.getVersion());
    // Properties from project attributes
    if (antProject.getName() != null) {
      properties.setProperty(CoreProperties.PROJECT_NAME_PROPERTY, antProject.getName());
    }
    if (antProject.getDescription() != null) {
      properties.setProperty(CoreProperties.PROJECT_DESCRIPTION_PROPERTY, antProject.getDescription());
    }
    // Properties from task
    properties.putAll(task.getProperties());
    // Properties from Ant
    properties.putAll(antProject.getProperties());
    setPathProperties(properties, antProject);

    // Source directories
    for (String dir : getPathAsList(task.createSources())) {
      definition.addSourceDir(dir);
    }
    // Test directories
    for (String dir : getPathAsList(task.createTests())) {
      definition.addTestDir(dir);
    }
    // Binary directories
    for (String dir : getPathAsList(task.createBinaries())) {
      definition.addBinaryDir(dir);
    }
    // Files with libraries
    for (String file : getPathAsList(task.createLibraries())) {
      definition.addLibrary(file);
    }

    defineModules(antProject, definition);

    return definition;
  }

  private ProjectDefinition defineProject(Project antProject) {
    checkAntProjectForMandatoryProperties(antProject);

    File baseDir = antProject.getBaseDir();
    File workDir = new File(baseDir, ".sonar");
    Properties properties = new Properties();
    ProjectDefinition definition = new ProjectDefinition(baseDir, workDir, properties);

    definition.addContainerExtension(antProject);

    // Properties from project attributes
    if (antProject.getName() != null) {
      properties.setProperty(CoreProperties.PROJECT_NAME_PROPERTY, antProject.getName());
    }
    if (antProject.getDescription() != null) {
      properties.setProperty(CoreProperties.PROJECT_DESCRIPTION_PROPERTY, antProject.getDescription());
    }
    // Properties from project
    properties.putAll(antProject.getProperties());
    setPathProperties(properties, antProject);

    defineModules(antProject, definition);

    return definition;
  }

  protected static void checkAntProjectForMandatoryProperties(Project antProject) {
    // for submodule, only the "sonar.projectKey" and "sonar.sources" property are mandatory
    Collection<String> missingProps = new ArrayList<String>();
    if (isEmpty(antProject.getProperty("sonar.projectKey"))) {
      missingProps.add("\n  - property 'sonar.projectKey'");
    }
    if (isEmpty(antProject.getProperty("sonar.sources"))) {
      missingProps.add("\n  - property 'sonar.sources'");
    }
    if (!missingProps.isEmpty()) {
      StringBuilder message = new StringBuilder("\nThe following mandatory information is missing:");
      for (String prop : missingProps) {
        message.append(prop);
      }
      throw new IllegalArgumentException(message.toString());
    }
  }

  private static boolean isEmpty(String string) {
    return string == null || "".equals(string);
  }

  /**
   * @since 1.2
   */
  private void setPathProperties(Properties properties, Project antProject) {
    setPathProperty(properties, antProject, "sonar.libraries");
  }

  /**
   * @since 1.2
   */
  private void setPathProperty(Properties properties, Project antProject, String refid) {
    if (antProject.getReference(refid) == null) {
      return;
    }
    Object reference = antProject.getReference(refid);
    properties.setProperty(refid, Utils.convertResourceCollectionToString((ResourceCollection) reference));
  }

  private void defineModules(Project antProject, ProjectDefinition definition) {
    String[] modules = StringUtils.split(definition.getProperties().getProperty("sonar.modules", ""), ',');
    for (String module : modules) {
      File buildFile = findSubModuleBuildFile(antProject, module);
      Project antSubProject = prepareSubProject(antProject, buildFile);
      definition.addModule(defineProject(antSubProject));
    }
  }

  protected static File findSubModuleBuildFile(Project antProject, String module) {
    File buildFile = new File(module);
    if (!buildFile.isAbsolute()) {
      buildFile = new File(antProject.getBaseDir(), module);
    }
    return buildFile;
  }

  private Project prepareSubProject(Project antProject, File buildFile) {
    ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(task.getClass().getClassLoader());

      Project antSubProject = antProject.createSubProject();
      ProjectHelper.configureProject(antSubProject, buildFile);
      antSubProject.init();

      if (StringUtils.isNotBlank(task.getInitTarget())) {
        // Attaches the build listeners of the current project to the new project
        Iterator iter = antProject.getBuildListeners().iterator();
        while (iter.hasNext()) {
          antSubProject.addBuildListener((BuildListener) iter.next());
        }
        // Executes initialization target
        antSubProject.executeTarget(task.getInitTarget());
      }

      return antSubProject;

    } finally {
      Thread.currentThread().setContextClassLoader(oldContextClassLoader);
    }
  }

  private List<String> getPathAsList(Path path) {
    List<String> result = new ArrayList<String>();
    for (Iterator<?> i = path.iterator(); i.hasNext();) {
      Resource resource = (Resource) i.next();
      if (resource instanceof FileResource) {
        File fileResource = ((FileResource) resource).getFile();
        result.add(fileResource.getAbsolutePath());
      }
    }
    return result;
  }

  /**
   * TODO This method should use the component org.sonar.batch.bootstrapper.LoggingConfiguration
   * created in sonar 2.14. It requires that the minimum supported version of sonar is 2.14, but
   * it's currently 2.8.
   */
  private void initLogging(Configuration config) {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    InputStream input = Batch.class.getResourceAsStream("/org/sonar/batch/logback.xml");
    try {
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(lc);
      lc.reset();
      lc.putProperty("ROOT_LOGGER_LEVEL", getLoggerLevel(config));
      lc.putProperty("SQL_LOGGER_LEVEL", getSqlLevel(config));// since 2.14. Ignored on previous versions.
      lc.putProperty("SQL_RESULTS_LOGGER_LEVEL", getSqlResultsLevel(config));// since 2.14. Ignored on previous versions.
      configurator.doConfigure(input);
    } catch (JoranException e) {
      throw new SonarException("Can not initialize logging", e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  String getLoggerLevel(Configuration config) {
    if (config.getBoolean("sonar.verbose", false)) {
      return DEBUG;
    }

    int antLoggerLevel = Utils.getAntLoggerLever(task.getProject());
    switch (antLoggerLevel) {
      case 3:
        return DEBUG;
      case 4:
        return TRACE;
      default:
        return INFO;
    }
  }

  private Configuration getInitialConfiguration(ProjectDefinition project) {
    CompositeConfiguration configuration = new CompositeConfiguration();
    configuration.addConfiguration(new SystemConfiguration());
    configuration.addConfiguration(new EnvironmentConfiguration());
    configuration.addConfiguration(new MapConfiguration(project.getProperties()));
    return configuration;
  }

  protected static String getSqlLevel(Configuration config) {
    boolean showSql = config.getBoolean("sonar.showSql", false);
    return showSql ? DEBUG : WARN;
  }

  protected static String getSqlResultsLevel(Configuration config) {
    boolean showSql = config.getBoolean("sonar.showSqlResults", false);
    return showSql ? DEBUG : WARN;
  }

}
