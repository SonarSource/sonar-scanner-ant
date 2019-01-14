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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

public final class SonarQubeTaskUtils {

  private SonarQubeTaskUtils() {
    // only static methods
  }

  /**
   * For unknown reasons <code>getClass().getProtectionDomain().getCodeSource().getLocation()</code> doesn't work under Ant 1.7.0.
   * So this is a workaround.
   *
   * @return Jar which contains this class
   */
  public static URL getJarPath() {
    String pathToClass = "/" + SonarQubeTask.class.getName().replace('.', '/') + ".class";
    URL url = SonarQubeTask.class.getResource(pathToClass);
    if (url != null) {
      String uri = extractURI(pathToClass, url.toString());
      if (uri != null) {
        try {
          return new URL(uri);
        } catch (MalformedURLException e) {
          return null;
        }
      }
    }
    return null;
  }

  // VisibleForTesting
  static String extractURI(String pathToClass, String path) {
    String uri = null;
    if (path.startsWith("jar:file:")) {
      int bang = path.indexOf('!');
      uri = path.substring(4, bang);
    } else if (path.startsWith("file:")) {
      int tail = path.indexOf(pathToClass);
      uri = path.substring(0, tail);
    }
    return uri;
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
      return 2;
    } catch (Exception e) {
      // if unable to determine level - just return default value
      return 2;
    }
  }

  /**
   * Returns the version of the Ant Task
   *
   * @return the version
   */
  public static String getTaskVersion() {
    try (InputStream in = SonarQubeTask.class.getResourceAsStream("version.txt")) {
      Properties props = new Properties();
      props.load(in);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new BuildException("Could not load the version information for SonarQube Ant Task", e);
    }
  }

}
