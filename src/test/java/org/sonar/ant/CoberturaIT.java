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
import org.sonar.wsclient.services.ResourceQuery;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CoberturaIT extends AbstractIT {

  @Override
  protected String getProjectKey() {
    return "org.sonar.ant.tests:cobertura";
  }

  @Test
  public void projectIsAnalyzed() {
    assertThat(sonar.find(new ResourceQuery(getProjectKey())).getVersion(), is("0.1-SNAPSHOT"));
  }

  @Test
  public void projectMetrics() {
    assertThat(getProjectMeasure("line_coverage").getValue(), is(50.0));
    assertThat(getProjectMeasure("lines_to_cover").getValue(), is(4.0));
    assertThat(getProjectMeasure("uncovered_lines").getValue(), is(2.0));

    assertThat(getProjectMeasure("branch_coverage").getValue(), is(50.0));
    assertThat(getProjectMeasure("conditions_to_cover").getValue(), is(2.0));
    assertThat(getProjectMeasure("uncovered_conditions").getValue(), is(1.0));

    assertThat(getProjectMeasure("coverage").getValue(), is(50.0));

    assertThat(getProjectMeasure("tests").getValue(), is(2.0));
    assertThat(getProjectMeasure("test_success_density").getValue(), is(50.0));
  }

}
