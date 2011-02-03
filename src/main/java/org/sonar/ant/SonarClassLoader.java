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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Special {@link URLClassLoader} to execute Sonar from Ant task.
 */
public class SonarClassLoader extends URLClassLoader {

  public SonarClassLoader(ClassLoader parent) {
    super(new URL[0], parent);
  }

  /**
   * Appends the specified File to the list of URLs to search for classes and resources.
   */
  public void addFile(File file) {
    try {
      addURL(file.toURI().toURL());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc} Visibility of a method has been relaxed to public.
   */
  @Override
  public void addURL(URL url) {
    super.addURL(url);
  }

  /**
   * {@inheritDoc} Visibility of a method has been relaxed to public.
   */
  @Override
  public Class<?> findClass(String name) throws ClassNotFoundException {
    return super.findClass(name);
  }

  /**
   * @return true, if class can be loaded from parent ClassLoader
   */
  static boolean canLoadFromParent(String name) {
    return name.startsWith("org.apache.tools.ant.")
        || name.startsWith("org.sonar.ant.");
  }

  /**
   * Same behavior as in {@link URLClassLoader#loadClass(String, boolean)}, except loading from parent.
   */
  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // First, check if the class has already been loaded
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      try {
        // Load from parent
        if ((getParent() != null) && canLoadFromParent(name)) {
          c = getParent().loadClass(name);
        } else {
          // Load from system
          c = getSystemClassLoader().loadClass(name);
        }
      } catch (ClassNotFoundException e) {
        // If still not found, then invoke findClass in order
        // to find the class.
        c = findClass(name);
      }
    }
    if (resolve) {
      resolveClass(c);
    }
    return c;
  }

}
