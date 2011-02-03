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

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SonarClassLoaderTest {

  @Test
  public void shouldRestrictLoadingFromParent() throws Exception {
    assertThat(SonarClassLoader.canLoadFromParent("org.sonar.ant.Launcher"), is(true));
    assertThat(SonarClassLoader.canLoadFromParent("org.apache.tools.ant.Task"), is(true));
    assertThat(SonarClassLoader.canLoadFromParent("org.objectweb.asm.ClassVisitor"), is(false));
  }

}
