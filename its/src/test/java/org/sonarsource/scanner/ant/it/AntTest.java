/*
 * SonarSource :: IT :: Ant task
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonarsource.scanner.ant.it;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.AntBuild;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsMeasures.Measure;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.component.ShowWsRequest;
import org.sonarqube.ws.client.measure.ComponentWsRequest;

import static java.lang.Double.parseDouble;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class AntTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static Orchestrator orchestrator = null;

  @BeforeClass
  public static void startServer() {
    OrchestratorBuilder builder = Orchestrator.builderEnv();

    builder
      .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
      .addPlugin("java")
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/ant/it/profile-groovy.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/ant/it/profile-java-empty.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/ant/it/profile-java-classpath.xml"));

    orchestrator = builder.build();
    orchestrator.start();
  }

  @AfterClass
  public static void stopServer() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @After
  public void resetData() {
    orchestrator.resetData();
  }

  private void buildJava(String project, String target, @Nullable String profile) {
    AntBuild build = AntBuild.create()
      .setBuildLocation(FileLocation.of("projects/" + project + "/build.xml"))
      .setTargets(target, "clean")
      .setProperty("sonar.profile", profile);
    orchestrator.executeBuild(build);
  }

  private void checkProjectAnalysed(String projectKey, @Nullable String profile) {
    Map<String, Measure> measures = getMeasuresByMetricKey(projectKey, "quality_profiles");
    assertThat(getProjectVersion(projectKey)).isEqualTo("0.1-SNAPSHOT");
    if (profile != null) {
      assertThat(measures.get("quality_profiles").getValue()).as("Profile").contains(profile);
    }
  }

  @Test
  public void testProjectMetadata() {
    buildJava("project-metadata", "all", null);
    checkProjectAnalysed("org.sonar.ant.tests:project-metadata:1.1.x", null);
    Component project = getComponent("org.sonar.ant.tests:project-metadata:1.1.x");
    assertThat(project.getName()).isEqualTo("Ant Project Metadata 1.1.x");
    assertThat(project.getDescription()).isEqualTo("Ant Project with complete metadata");
    assertThat(getProjectVersion("org.sonar.ant.tests:project-metadata:1.1.x")).isEqualTo("0.1-SNAPSHOT");
  }

  @Test
  public void testProjectKeyWithoutGroupId() {
    buildJava("project-key-without-groupId", "all", null);
    checkProjectAnalysed("project-key-without-groupId", null);
  }

  @Test
  public void testClasspath() {
    buildJava("classpath", "all", "classpath");
    checkProjectAnalysed("org.sonar.ant.tests:classpath", "classpath");

    List<Issue> issues = orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create().componentRoots("org.sonar.ant.tests:classpath")).list();
    assertThat(issues.size()).isEqualTo(2);
    assertThat(containsRule("squid:CallToDeprecatedMethod", issues)).isTrue();
    assertThat(containsRule("squid:S1147", issues)).isTrue();
  }

  /**
   * This is a test against SONARPLUGINS-1322, which actually was fixed in Sonar 2.11 - see SONAR-2823
   */
  @Test
  public void testSquid() {
    buildJava("squid", "all", "classpath");
    checkProjectAnalysed("org.sonar.ant.tests:squid", "classpath");

    List<Issue> issues = orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create().componentRoots("org.sonar.ant.tests:squid")).list();
    assertThat(issues.size()).isEqualTo(1);
    assertThat(issues.get(0).ruleKey()).isEqualTo("squid:CallToDeprecatedMethod");
  }

  @Test
  public void testCustomLayout() {
    buildJava("custom-layout", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests:custom-layout", "empty");
    Map<String, Measure> measures = getMeasuresByMetricKey("org.sonar.ant.tests:custom-layout", "files", "classes", "functions");
    assertThat(parseDouble(measures.get("files").getValue())).isEqualTo(2.0);
    assertThat(parseDouble(measures.get("classes").getValue())).isEqualTo(2.0);
    assertThat(parseDouble(measures.get("functions").getValue())).isEqualTo(2.0);
  }

  @Test
  public void testJavaWithoutBytecode() {
    buildJava("java-without-bytecode", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests:java-without-bytecode", "empty");

    Map<String, Measure> measures = getMeasuresByMetricKey("org.sonar.ant.tests:java-without-bytecode",
      "lines", "violations");
    assertThat(parseDouble(measures.get("lines").getValue())).isGreaterThan(1);
    assertThat(parseDouble(measures.get("violations").getValue())).isGreaterThanOrEqualTo(0);
  }

  /**
   * SONARPLUGINS-2224
   */
  @Test
  public void testModules() {
    buildJava("modules", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests.modules:root", "empty");

    // Module name
    assertThat(getComponent("org.sonar.ant.tests.modules:root").getName()).isEqualTo("Project with modules");
    assertThat(getComponent("org.sonar.ant.tests.modules:root:one").getName()).isEqualTo("Module One");

    // Metrics on project

    Map<String, Measure> projectMeasures = getMeasuresByMetricKey("org.sonar.ant.tests.modules:root", "lines", "files");
    assertThat(parseDouble(projectMeasures.get("lines").getValue())).isEqualTo(10.0);
    assertThat(parseDouble(projectMeasures.get("files").getValue())).isEqualTo(2.0);

    // Metrics on module
    Map<String, Measure> moduleMeasures = getMeasuresByMetricKey("org.sonar.ant.tests.modules:root:one", "lines", "files");
    assertThat(parseDouble(moduleMeasures.get("lines").getValue())).isEqualTo(5.0);
    assertThat(parseDouble(moduleMeasures.get("files").getValue())).isEqualTo(1.0);

    // Metrics on file
    Map<String, Measure> fileMeasures = getMeasuresByMetricKey("org.sonar.ant.tests.modules:root:one", "lines");
    assertThat(parseDouble(fileMeasures.get("lines").getValue())).isEqualTo(5.0);
  }

  /**
   * SONARPLUGINS-1853
   */
  @Test
  public void testModulesWithSpaces() {
    buildJava("modules-with-spaces", "all", "empty");

    checkProjectAnalysed("org.sonar.ant.tests.modules:root", "empty");

    assertThat(getComponent("org.sonar.ant.tests.modules:root").getName()).isEqualTo("Project with modules with spaces");
    assertThat(getComponent("org.sonar.ant.tests.modules:root:one").getName()).isEqualTo("Module One");
    assertThat(getComponent("org.sonar.ant.tests.modules:root:two").getName()).isEqualTo("Module Two");
  }

  @Test
  public void testJacoco() {
    buildJava("jacoco", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests:jacoco", "empty");

    Map<String, Measure> measures = getMeasuresByMetricKey("org.sonar.ant.tests:jacoco",
      "line_coverage", "lines_to_cover", "uncovered_lines",
      "branch_coverage", "conditions_to_cover", "uncovered_conditions",
      "coverage",
      "tests", "test_success_density");

    assertThat(parseDouble(measures.get("line_coverage").getValue())).isEqualTo(50.0);
    assertThat(parseDouble(measures.get("lines_to_cover").getValue())).isEqualTo(4.0);
    assertThat(parseDouble(measures.get("uncovered_lines").getValue())).isEqualTo(2.0);

    assertThat(parseDouble(measures.get("branch_coverage").getValue())).isEqualTo(50.0);
    assertThat(parseDouble(measures.get("conditions_to_cover").getValue())).isEqualTo(2.0);
    assertThat(parseDouble(measures.get("uncovered_conditions").getValue())).isEqualTo(1.0);

    assertThat(parseDouble(measures.get("coverage").getValue())).isEqualTo(50.0);

    assertThat(parseDouble(measures.get("tests").getValue())).isEqualTo(2.0);
    assertThat(parseDouble(measures.get("test_success_density").getValue())).isEqualTo(50.0);
  }

  @Test
  public void testJacocoModules() {
    buildJava("jacoco-modules", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests.jacoco-modules:root", "empty");

    // Metrics on project
    Map<String, Measure> projectMeasures = getMeasuresByMetricKey("org.sonar.ant.tests.jacoco-modules:root",
      "lines", "tests", "test_success_density", "coverage");
    assertThat(parseDouble(projectMeasures.get("lines").getValue())).isEqualTo(52.0);
    assertThat(parseDouble(projectMeasures.get("tests").getValue())).isEqualTo(4.0);
    assertThat(parseDouble(projectMeasures.get("test_success_density").getValue())).isEqualTo(50.0);
    assertThat(parseDouble(projectMeasures.get("coverage").getValue())).isEqualTo(54.5);

    // Metrics on module
    Map<String, Measure> moduleMeasures = getMeasuresByMetricKey("org.sonar.ant.tests.jacoco-modules:root:one",
      "lines", "tests", "test_success_density", "coverage");
    assertThat(parseDouble(moduleMeasures.get("lines").getValue())).isEqualTo(26.0);
    assertThat(parseDouble(moduleMeasures.get("tests").getValue())).isEqualTo(2.0);
    assertThat(parseDouble(moduleMeasures.get("test_success_density").getValue())).isEqualTo(50.0);
    assertThat(parseDouble(moduleMeasures.get("coverage").getValue())).isEqualTo(54.5);
  }

  @Test
  public void testJacocoTestng() {
    buildJava("testng", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests:testng", "empty");

    Map<String, Measure> projectMeasures = getMeasuresByMetricKey("org.sonar.ant.tests:testng",
      "line_coverage", "lines_to_cover", "uncovered_lines",
      "branch_coverage", "conditions_to_cover", "uncovered_conditions",
      "coverage",
      "tests", "test_success_density");

    assertThat(parseDouble(projectMeasures.get("line_coverage").getValue())).isEqualTo(55.6);
    assertThat(parseDouble(projectMeasures.get("lines_to_cover").getValue())).isEqualTo(9.0);
    assertThat(parseDouble(projectMeasures.get("uncovered_lines").getValue())).isEqualTo(4.0);

    assertThat(parseDouble(projectMeasures.get("branch_coverage").getValue())).isEqualTo(50.0);
    assertThat(parseDouble(projectMeasures.get("conditions_to_cover").getValue())).isEqualTo(2.0);
    assertThat(parseDouble(projectMeasures.get("uncovered_conditions").getValue())).isEqualTo(1.0);

    assertThat(parseDouble(projectMeasures.get("coverage").getValue())).isEqualTo(54.5);

    assertThat(parseDouble(projectMeasures.get("tests").getValue())).isEqualTo(2.0);
    assertThat(parseDouble(projectMeasures.get("test_success_density").getValue())).isEqualTo(50.0);
  }

  /**
   * SONARPLUGINS-2840
   */
  @Test
  public void testVerbose() {
    AntBuild build = AntBuild.create()
      .setBuildLocation(FileLocation.of("projects/shared/build.xml"))
      // Workaround for ORCH-174
      .setTargets("all clean -v");
    BuildResult analysisResults = orchestrator.executeBuild(build);

    String logs = analysisResults.getLogs();
    // The list of Sensors is only displayed in DEBUG mode
    assertThat(logs).contains("Sensors : ");
  }

  /**
   * SONARPLUGINS-1609 + SONARPLUGINS-1674
   */
  @Test
  public void shouldFailIfMissingProperty() {
    AntBuild build = AntBuild.create()
      .setBuildLocation(FileLocation.of("projects/missing-mandatory-properties/build.xml"))
      .setTargets("sonar");

    BuildResult analysisResults = orchestrator.executeBuildQuietly(build);
    assertThat(analysisResults.getStatus()).isNotEqualTo(0);

    String logs = analysisResults.getLogs();
    assertThat(logs).contains("You must define the following mandatory properties", "sonar.projectKey", "sonar.sources");
  }

  private static boolean containsRule(final String ruleKey, List<Issue> issues) {
    return Iterables.any(issues, new Predicate<Issue>() {
      @Override
      public boolean apply(@Nullable Issue input) {
        return input != null && ruleKey.equals(input.ruleKey());
      }
    });
  }

  private static Map<String, Measure> getMeasuresByMetricKey(String componentKey, String... metricKeys) {
    return getStreamMeasures(componentKey, metricKeys)
      .filter(Measure::hasValue)
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  private static Stream<Measure> getStreamMeasures(String componentKey, String... metricKeys) {
    return newWsClient().measures().component(new ComponentWsRequest()
      .setComponentKey(componentKey)
      .setMetricKeys(asList(metricKeys)))
      .getComponent().getMeasuresList()
      .stream();
  }

  private static Component getComponent(String componentKey) {
    return newWsClient().components().show(new ShowWsRequest().setKey((componentKey))).getComponent();
  }

  private static String getProjectVersion(String componentKey) {
    // Waiting for SONAR-7745 to have version in api/components/show, we use internal api/navigation/component WS to get the component
    // version
    String content = newWsClient().wsConnector().call(new GetRequest("api/navigation/component").setParam("componentKey", componentKey))
      .failIfNotSuccessful()
      .content();
    return ComponentNavigation.parse(content).getVersion();
  }

  private static WsClient newWsClient() {
    Server server = orchestrator.getServer();
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(server.getUrl())
      .build());
  }

  private static class ComponentNavigation {
    private String version;

    String getVersion() {
      return version;
    }

    static ComponentNavigation parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, ComponentNavigation.class);
    }
  }

}
