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

# Builds and releases tink-java-apps on Maven.
#
# The behavior of this script can be modified using the following optional env
# variables:
#
# - CONTAINER_IMAGE (unset by default): By default when run locally this script
#   executes tests directly on the host. The CONTAINER_IMAGE variable can be set
#   to execute tests in a custom container image for local testing. E.g.:
#
#   CONTAINER_IMAGE="us-docker.pkg.dev/tink-test-infrastructure/tink-ci-images/linux-tink-java-gcloud:latest" \
#     RELEASE_VERSION=HEAD \
#     sh ./kokoro/gcp_ubuntu/release/run_tests.sh
set -euo pipefail

# Fail if RELEASE_VERSION is not set.
if [[ -z "${RELEASE_VERSION:-}" ]]; then
  echo "RELEASE_VERSION must be set" >&2
  exit 1
fi

IS_KOKORO="false"
if [[ -n "${KOKORO_ARTIFACTS_DIR:-}" ]]; then
  IS_KOKORO="true"
fi
readonly IS_KOKORO

GITUB_PROTOCOL_AND_AUTH="ssh://git"
if [[ "${IS_KOKORO}" == "true" ]]; then
  readonly TINK_BASE_DIR="$(echo "${KOKORO_ARTIFACTS_DIR}"/git*)"
  cd "${TINK_BASE_DIR}/tink_java_apps"
  GITUB_PROTOCOL_AND_AUTH="https://ise-crypto:${GITHUB_ACCESS_TOKEN}"
  source "./kokoro/testutils/java_test_container_images.sh"
  CONTAINER_IMAGE="${TINK_JAVA_GCLOUD_IMAGE}"
  RUN_COMMAND_ARGS+=( -k "${TINK_GCR_SERVICE_KEY}" )
fi
readonly GITUB_PROTOCOL_AND_AUTH
readonly CONTAINER_IMAGE

if [[ -n "${CONTAINER_IMAGE:-}" ]]; then
  RUN_COMMAND_ARGS+=( -c "${CONTAINER_IMAGE}" )
fi

# WARNING: Setting this environment varialble to "true" will cause this script
# to actually perform a release.
: "${DO_MAKE_RELEASE:="false"}"

if [[ ! "${DO_MAKE_RELEASE}" =~ ^(false|true)$ ]]; then
  echo "DO_MAKE_RELEASE must be either \"true\" or \"false\"" >&2
  exit 1
fi

readonly TINK_JAVA_APPS_GITHUB_URL="github.com/tink-crypto/tink-java-apps"
readonly GITHUB_URL="${GITUB_PROTOCOL_AND_AUTH}@${TINK_JAVA_APPS_GITHUB_URL}"

MAVEN_DEPLOY_LIBRARY_OPTIONS=( -u "${GITHUB_URL}" )
if [[ "${DO_MAKE_RELEASE}" == "false" ]]; then
  MAVEN_DEPLOY_LIBRARY_OPTIONS+=( -d )
fi
readonly MAVEN_DEPLOY_LIBRARY_OPTIONS

if [[ "${IS_KOKORO}" == "true" ]]; then
  # Import the PGP signing key and make the passphrase available as an env
  # variable.
  gpg --import --pinentry-mode loopback \
    --passphrase-file \
    "${KOKORO_KEYSTORE_DIR}/70968_tink_dev_maven_pgp_passphrase" \
    --batch "${KOKORO_KEYSTORE_DIR}/70968_tink_dev_maven_pgp_secret_key"
  export TINK_DEV_MAVEN_PGP_PASSPHRASE="$(cat \
    "${KOKORO_KEYSTORE_DIR}/70968_tink_dev_maven_pgp_passphrase")"
fi

# Ensure that secrets are not inadvertently logged.
set +x

./kokoro/testutils/run_command.sh "${RUN_COMMAND_ARGS[@]}" \
  ./maven/maven_deploy_library.sh "${MAVEN_DEPLOY_LIBRARY_OPTIONS[@]}" \
  -n paymentmethodtoken/maven release apps-paymentmethodtoken \
  maven/tink-java-apps-paymentmethodtoken.pom.xml "${RELEASE_VERSION}"

./kokoro/testutils/run_command.sh "${RUN_COMMAND_ARGS[@]}" \
  ./maven/maven_deploy_library.sh "${MAVEN_DEPLOY_LIBRARY_OPTIONS[@]}" \
  -n rewardedads/maven release apps-rewardedads \
  maven/tink-java-apps-rewardedads.pom.xml "${RELEASE_VERSION}"

./kokoro/testutils/run_command.sh "${RUN_COMMAND_ARGS[@]}" \
  ./maven/maven_deploy_library.sh "${MAVEN_DEPLOY_LIBRARY_OPTIONS[@]}" \
  -n webpush/maven release apps-webpush \
  maven/tink-java-apps-webpush.pom.xml "${RELEASE_VERSION}"
