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

package org.sonar.ant.utils;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Union;
import org.sonar.ant.SonarTask;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public final class SonarAntTaskUtils {

  private SonarAntTaskUtils() {
    // only static methods
  }

  /**
   * For unknown reasons <code>getClass().getProtectionDomain().getCodeSource().getLocation()</code> doesn't work under Ant 1.7.0.
   * So this is a workaround.
   * 
   * @return Jar which contains this class
   */
  public static URL getJarPath() {
    String pathToClass = "/" + SonarTask.class.getName().replace('.', '/') + ".class";
    URL url = SonarTask.class.getResource(pathToClass);
    if (url != null) {
      String path = url.toString();
      String uri = null;
      if (path.startsWith("jar:file:")) {
        int bang = path.indexOf('!');
        uri = path.substring(4, bang);
      } else if (path.startsWith("file:")) {
        int tail = path.indexOf(pathToClass);
        uri = path.substring(0, tail);
      }
      if (uri != null) {
        try {
          return new URL(uri);
        } catch (MalformedURLException e) { // NOSONAR
        }
      }
    }
    return null;
  }

  /**
   * Workaround to get Ant logger level. Possible values (see {@link org.apache.tools.ant.Main}):
   * <ul>
   * <li>1 - quiet</li>
   * <li>2 - default</li>
   * <li>3 - verbose</li>
   * <li>4 - debug</li>
   * </ul>
   */
  public static int getAntLoggerLever(Project project) {
    try {
      List<BuildListener> listeners = project.getBuildListeners();
      for (BuildListener listener : listeners) {
        if (listener instanceof DefaultLogger) {
          DefaultLogger logger = (DefaultLogger) listener;
          Field field = DefaultLogger.class.getDeclaredField("msgOutputLevel");
          field.setAccessible(true);
          return (Integer) field.get(logger);
        }
      }
    } catch (Exception e) { // NOSONAR if unable to determine level - just return default value
    }
    return 2;
  }

  /**
   * Returns the version of the Ant Task
   * 
   * @return the version
   */
  public static String getTaskVersion() {
    InputStream in = null;
    try {
      in = SonarTask.class.getResourceAsStream("/org/sonar/ant/version.txt");
      Properties props = new Properties();
      props.load(in);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new BuildException("Could not load the version information for Sonar Ant Task", e);
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException ioe) { // NOSONAR
      }
    }
  }

  /**
   * Looks up a "Path" property referenced by "refid" in the Ant project, and transforms it
   * into a "String" property (comma-separated list).
   */
  public static void setPathProperty(Properties properties, Project antProject, String refid) {
    if (antProject.getReference(refid) == null) {
      return;
    }
    Object reference = antProject.getReference(refid);
    properties.setProperty(refid, convertResourceCollectionToString((ResourceCollection) reference));
  }

  private static String convertResourceCollectionToString(ResourceCollection c) {
    String[] list = new Union(c).list();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < list.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(list[i]);
    }
    return sb.toString();
  }

}
