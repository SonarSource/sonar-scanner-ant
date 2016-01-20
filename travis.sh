#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v21 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

function strongEcho {
  echo ""
  echo "================ $1 ================="
}

installTravisTools

case "$TARGET" in

CI)
  if [ "${TRAVIS_BRANCH}" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    strongEcho 'Build and analyze commit in master'
    export MAVEN_OPTS="-Xmx1G -Xms128m"
    mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
      -Pcoverage-per-test -Dmaven.test.redirectTestOutputToFile=false -B -e -V \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN

  elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ "$TRAVIS_SECURE_ENV_VARS" == "true" ]; then
    strongEcho 'Build and analyze pull request'
    mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
      -Pcoverage-per-test -Dmaven.test.redirectTestOutputToFile=false -B -e -V \
      -Dsonar.analysis.mode=issues \
      -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
      -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
      -Dsonar.github.oauth=$GITHUB_TOKEN \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN

  else
    strongEcho 'Build, no analysis'
    # Build branch, without any analysis

    # No need for Maven goal "install" as the generated JAR file does not need to be installed
    # in Maven local repository
    mvn verify -Dmaven.test.redirectTestOutputToFile=false -B -e -V
  fi
  ;;

IT)
  start_xvfb
  mvn install -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  if [ "${SQ_VERSION}" == "DEV" ]
  then
    build_snapshot "SonarSource/sonarqube"
  fi

  cd its
  mvn -Dsonar.runtimeVersion="${SQ_VERSION}" -Dmaven.test.redirectTestOutputToFile=false verify
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac
