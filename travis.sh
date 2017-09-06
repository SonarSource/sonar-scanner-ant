#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v34 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

#build_snapshot "SonarSource/sonar-scanner-api"

export DEPLOY_PULL_REQUEST=true

regular_mvn_build_deploy_analyze
