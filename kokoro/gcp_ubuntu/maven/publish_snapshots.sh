#!/bin/bash
# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

# By default when run locally this script runs the command below directly on the
# host. The CONTAINER_IMAGE variable can be set to run on a custom container
# image for local testing. E.g.:
#
# CONTAINER_IMAGE="us-docker.pkg.dev/tink-test-infrastructure/tink-ci-images/linux-tink-java-base:latest" \
#  sh ./kokoro/gcp_ubuntu/bazel/run_tests.sh
#
# The user may specify TINK_BASE_DIR as the folder where to look for
# tink-java-awskms and its depndencies. That is:
#   ${TINK_BASE_DIR}/tink_java
#   ${TINK_BASE_DIR}/tink_java_apps
set -eEuo pipefail

IS_KOKORO="false"
if [[ -n "${KOKORO_ARTIFACTS_DIR:-}" ]] ; then
  IS_KOKORO="true"
fi
readonly IS_KOKORO

RUN_COMMAND_ARGS=()
if [[ "${IS_KOKORO}" == "true" ]] ; then
  TINK_BASE_DIR="$(echo "${KOKORO_ARTIFACTS_DIR}"/git*)"
  cd "${TINK_BASE_DIR}/tink_java_apps"
  source "./kokoro/testutils/tink_test_container_images.sh"
  CONTAINER_IMAGE="${TINK_JAVA_BASE_IMAGE}"
  RUN_COMMAND_ARGS+=( -k "${TINK_GCR_SERVICE_KEY}" )
fi
: "${TINK_BASE_DIR:=$(cd .. && pwd)}"
readonly TINK_BASE_DIR
readonly CONTAINER_IMAGE

cp WORKSPACE WORKSPACE.bak

./kokoro/testutils/replace_http_archive_with_local_repository.py \
  -f "WORKSPACE" -t ..

if [[ -n "${CONTAINER_IMAGE:-}" ]]; then
  RUN_COMMAND_ARGS+=( -c "${CONTAINER_IMAGE}" )
fi

# Run cleanup on EXIT.
trap cleanup EXIT

cleanup() {
  mv WORKSPACE.bak WORKSPACE
}

# TODO(b/263465812): Add test example app for testing the Maven snapshots.

readonly GITHUB_JOB_NAME="tink/github/java_apps/gcp_ubuntu/maven/continuous"

if [[ "${IS_KOKORO}" == "true" \
      && "${KOKORO_JOB_NAME}" == "${GITHUB_JOB_NAME}" ]] ; then
  # GITHUB_ACCESS_TOKEN is populated by Kokoro.
  readonly GIT_CREDENTIALS="ise-crypto:${GITHUB_ACCESS_TOKEN}"
  readonly GITHUB_URL="https://${GIT_CREDENTIALS}@github.com/tink-crypto/tink-java-apps.git"

  # Share the required env variables with the container to allow publishing the
  # snapshot on Sonatype.
  cat <<EOF > env_variables.txt
SONATYPE_USERNAME
SONATYPE_PASSWORD
EOF
  RUN_COMMAND_ARGS+=( -e env_variables.txt )

  ./kokoro/testutils/run_command.sh "${RUN_COMMAND_ARGS[@]}" \
    ./maven/maven_deploy_library.sh -u "${GITHUB_URL}" \
      -n paymentmethodtoken/maven snapshot apps-paymentmethodtoken \
      maven/tink-java-apps-paymentmethodtoken.pom.xml HEAD

  ./kokoro/testutils/run_command.sh "${RUN_COMMAND_ARGS[@]}" \
    ./maven/maven_deploy_library.sh -u "${GITHUB_URL}" \
      -n rewardedads/maven snapshot apps-rewardedads \
      maven/tink-java-apps-rewardedads.pom.xml HEAD

  ./kokoro/testutils/run_command.sh "${RUN_COMMAND_ARGS[@]}" \
    ./maven/maven_deploy_library.sh -u "${GITHUB_URL}" \
      -n webpush/maven snapshot apps-webpush \
      maven/tink-java-apps-webpush.pom.xml HEAD
fi
