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

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ManySourceDirsIT extends AbstractIT {

  @Override
  public String getProjectKey() {
    return "org.sonar.ant.tests:many-source-dirs";
  }

  @Test
  public void projectMetrics() {
    assertThat(getProjectMeasure("packages").getValue(), is(1.0));
    assertThat(getProjectMeasure("files").getValue(), is(2.0));
    assertThat(getProjectMeasure("classes").getValue(), is(2.0));
    assertThat(getProjectMeasure("functions").getValue(), is(2.0));
  }

}
