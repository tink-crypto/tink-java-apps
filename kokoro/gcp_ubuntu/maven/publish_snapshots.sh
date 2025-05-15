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

# Builds and publishes tink-java-apps Maven snapshots.
#
# The behavior of this script can be modified using the following optional env
# variables:
#
# - CONTAINER_IMAGE (unset by default): By default when run locally this script
#   executes tests directly on the host. The CONTAINER_IMAGE variable can be set
#   to execute tests in a custom container image for local testing. E.g.:
#
#   CONTAINER_IMAGE="us-docker.pkg.dev/tink-test-infrastructure/tink-ci-images/linux-tink-java-base:latest" \
#     sh ./kokoro/gcp_ubuntu/maven/publish_snapshots.sh
set -eEuo pipefail

IS_KOKORO="false"
if [[ -n "${KOKORO_ARTIFACTS_DIR:-}" ]] ; then
  IS_KOKORO="true"
fi
readonly IS_KOKORO

RUN_COMMAND_ARGS=()
if [[ "${IS_KOKORO}" == "true" ]] ; then
  readonly TINK_BASE_DIR="$(echo "${KOKORO_ARTIFACTS_DIR}"/git*)"
  cd "${TINK_BASE_DIR}/tink_java_apps"
  source ./kokoro/testutils/java_test_container_images.sh
  CONTAINER_IMAGE="${TINK_JAVA_BASE_IMAGE}"
  RUN_COMMAND_ARGS+=( -k "${TINK_GCR_SERVICE_KEY}" )
fi
readonly CONTAINER_IMAGE

if [[ -n "${CONTAINER_IMAGE:-}" ]]; then
  RUN_COMMAND_ARGS+=( -c "${CONTAINER_IMAGE}" )
fi

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

  ./kokoro/testutils/docker_execute.sh "${RUN_COMMAND_ARGS[@]}" \
    ./maven/maven_deploy_library.sh -u "${GITHUB_URL}" \
      -n paymentmethodtoken/maven snapshot apps-paymentmethodtoken \
      maven/tink-java-apps-paymentmethodtoken.pom.xml HEAD

  ./kokoro/testutils/docker_execute.sh "${RUN_COMMAND_ARGS[@]}" \
    ./maven/maven_deploy_library.sh -u "${GITHUB_URL}" \
      -n rewardedads/maven snapshot apps-rewardedads \
      maven/tink-java-apps-rewardedads.pom.xml HEAD

  ./kokoro/testutils/docker_execute.sh "${RUN_COMMAND_ARGS[@]}" \
    ./maven/maven_deploy_library.sh -u "${GITHUB_URL}" \
      -n webpush/maven snapshot apps-webpush \
      maven/tink-java-apps-webpush.pom.xml HEAD
fi
