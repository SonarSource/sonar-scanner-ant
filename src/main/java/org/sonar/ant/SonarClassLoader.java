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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Special {@link ClassLoader} to execute Sonar from Ant task.
 */
public class SonarClassLoader extends URLClassLoader {
  public SonarClassLoader(ClassLoader parent) {
    super(new URL[0], parent);
  }

  public void addFile(File file) {
    try {
      addURL(file.toURI().toURL());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void addURL(URL url) {
    super.addURL(url);
  }

  @Override
  public Class<?> findClass(String name) throws ClassNotFoundException {
    return super.findClass(name);
  }

  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // First, check if the class has already been loaded
    Class<?> c = findLoadedClass(name);
    // Load from parent
    if (c == null) {
      if (name.startsWith("org.apache.tools.ant.") || name.startsWith("org.sonar.ant.")) {
        c = getParent().loadClass(name);
      }
    }
    // Load from this
    if (c == null) {
      try {
        c = findClass(name);
      } catch (ClassNotFoundException e) {
        // ignore
      }
    }
    // Load from system
    if (c == null) {
      c = getSystemClassLoader().loadClass(name);
    }

    if (resolve) {
      resolveClass(c);
    }
    return c;
  }
}
