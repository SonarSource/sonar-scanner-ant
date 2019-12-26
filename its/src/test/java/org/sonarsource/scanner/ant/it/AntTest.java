/*
 * SonarSource :: IT :: Ant task
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonarsource.scanner.ant.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.AntBuild;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.BuildRunner;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsMeasures.Measure;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.component.ShowWsRequest;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.measure.ComponentWsRequest;

import static java.lang.Double.parseDouble;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class AntTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[7.9]"))
    .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "LATEST_RELEASE"))
    .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/ant/it/profile-java-empty.xml"))
    .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/ant/it/profile-java-classpath.xml"))
    .build();

  private void buildJava(String project, String target) {
    AntBuild build = AntBuild.create()
      .setBuildLocation(FileLocation.of("projects/" + project + "/build.xml"))
      .setTargets(target, "clean");
    orchestrator.executeBuild(build);
  }

  private void checkProjectAnalysed(String projectKey, @Nullable String profile) {
    Map<String, Measure> measures = getMeasuresByMetricKey(projectKey, "quality_profiles");
    Component project = getComponent(projectKey);
    assertThat(project.getVersion()).isEqualTo("0.1-SNAPSHOT");
    if (profile != null) {
      assertThat(measures.get("quality_profiles").getValue()).as("Profile").contains(profile);
    }
  }

  @Test
  public void testProjectMetadata() {
    buildJava("project-metadata", "all");
    checkProjectAnalysed("org.sonar.ant.tests:project-metadata", null);
    Component project = getComponent("org.sonar.ant.tests:project-metadata");
    assertThat(project.getName()).isEqualTo("Ant Project Metadata");
    assertThat(project.getDescription()).isEqualTo("Ant Project with complete metadata");
    assertThat(project.getVersion()).isEqualTo("0.1-SNAPSHOT");
  }

  @Test
  public void testProjectKeyWithoutGroupId() {
    buildJava("project-key-without-groupId", "all");
    checkProjectAnalysed("project-key-without-groupId", null);
  }

  @Test
  public void testClasspath() {
    String projectKey = "org.sonar.ant.tests:classpath";

    orchestrator.getServer().provisionProject(projectKey, "Classpath");
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", "classpath");
    buildJava("classpath", "all");
    checkProjectAnalysed(projectKey, "classpath");

    List<Issue> issues = newWsClient().issues().search(new SearchWsRequest()
      .setComponentKeys(Collections.singletonList(projectKey)))
      .getIssuesList();

    assertThat(issues.size()).isEqualTo(2);
    assertThat(containsRule("squid:CallToDeprecatedMethod", issues)).isTrue();
    assertThat(containsRule("squid:S1147", issues)).isTrue();
  }

  /**
   * This is a test against SONARPLUGINS-1322, which actually was fixed in Sonar 2.11 - see SONAR-2823
   */
  @Test
  public void testSquid() {
    String projectKey = "org.sonar.ant.tests:squid";

    orchestrator.getServer().provisionProject(projectKey, "Squid");
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", "classpath");

    buildJava("squid", "all");
    checkProjectAnalysed(projectKey, "classpath");

    List<Issue> issues = newWsClient().issues().search(new SearchWsRequest()
      .setComponentKeys(Collections.singletonList(projectKey)))
      .getIssuesList();
    assertThat(issues.size()).isEqualTo(1);
    assertThat(issues.get(0).getRule()).isEqualTo("squid:CallToDeprecatedMethod");
  }

  @Test
  public void testCustomLayout() {
    String projectKey = "org.sonar.ant.tests:custom-layout";

    orchestrator.getServer().provisionProject(projectKey, "Custom layout");
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", "empty");

    buildJava("custom-layout", "all");
    checkProjectAnalysed(projectKey, "empty");
    Map<String, Measure> measures = getMeasuresByMetricKey("org.sonar.ant.tests:custom-layout", "files", "classes", "functions");
    assertThat(parseDouble(measures.get("files").getValue())).isEqualTo(2.0);
    assertThat(parseDouble(measures.get("classes").getValue())).isEqualTo(2.0);
    assertThat(parseDouble(measures.get("functions").getValue())).isEqualTo(2.0);
  }

  @Test
  public void testJavaWithoutBytecode() {
    String projectKey = "org.sonar.ant.tests:java-without-bytecode";

    orchestrator.getServer().provisionProject(projectKey, "No bytecode");
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", "empty");

    buildJava("java-without-bytecode", "all");
    checkProjectAnalysed(projectKey, "empty");

    Map<String, Measure> measures = getMeasuresByMetricKey("org.sonar.ant.tests:java-without-bytecode",
      "lines", "violations");
    assertThat(parseDouble(measures.get("lines").getValue())).isGreaterThan(1);
    assertThat(parseDouble(measures.get("violations").getValue())).isGreaterThanOrEqualTo(0);
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

  @Test
  public void testEnvironmentProperties() {
    BuildRunner runner = new BuildRunner(orchestrator.getConfiguration());
    AntBuild build = AntBuild.create()
      .setEnvironmentVariable("SONAR_HOST_URL", "http://from-env.org")
      .setBuildLocation(FileLocation.of("projects/shared/build.xml"))
      .setTargets("all clean -v");
    BuildResult analysisResults = runner.runQuietly(null, build);

    assertThat(analysisResults.isSuccess()).isFalse();
    String logs = analysisResults.getLogs();
    assertThat(logs).contains("java.net.UnknownHostException: from-env.org");
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
    assertThat(analysisResults.getLastStatus()).isNotEqualTo(0);

    String logs = analysisResults.getLogs();
    assertThat(logs).contains("You must define the following mandatory properties", "sonar.projectKey");
  }

  private static boolean containsRule(final String ruleKey, List<Issue> issues) {
    return issues.stream()
      .filter(Objects::nonNull)
      .anyMatch(input -> ruleKey.equals(input.getRule()));
  }

  private static Map<String, Measure> getMeasuresByMetricKey(String componentKey, String... metricKeys) {
    return getStreamMeasures(componentKey, metricKeys)
      .filter(Measure::hasValue)
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  private static Stream<Measure> getStreamMeasures(String componentKey, String... metricKeys) {
    return newWsClient().measures().component(new ComponentWsRequest()
      .setComponent(componentKey)
      .setMetricKeys(asList(metricKeys)))
      .getComponent().getMeasuresList()
      .stream();
  }

  private static Component getComponent(String componentKey) {
    return newWsClient().components().show(new ShowWsRequest().setKey((componentKey))).getComponent();
  }

  private static WsClient newWsClient() {
    Server server = orchestrator.getServer();
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(server.getUrl())
      .build());
  }
}
