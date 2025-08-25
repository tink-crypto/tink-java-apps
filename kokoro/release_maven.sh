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

# The behavior of this script can be modified using the following optional env
# variables:
#
# - CONTAINER_IMAGE (unset by default): By default when run locally this script
#   executes tests directly on the host. The CONTAINER_IMAGE variable can be set
#   to execute tests in a custom container image for local testing. E.g.:
#
#   CONTAINER_IMAGE="us-docker.pkg.dev/tink-test-infrastructure/tink-ci-images/linux-tink-java-base:latest" \
#     sh ./kokoro/release_maven.sh

set -euo pipefail

# Fail if RELEASE_VERSION is not set.
if [[ -z "${RELEASE_VERSION:-}" ]]; then
  echo "RELEASE_VERSION must be set" >&2
  exit 1
fi

readonly TINK_JAVA_APPS_GITHUB_URL="github.com/tink-crypto/tink-java-apps"
IS_KOKORO="false"
if [[ -n "${KOKORO_ARTIFACTS_DIR:-}" ]]; then
  IS_KOKORO="true"
fi
readonly IS_KOKORO

# WARNING: Setting this environment varialble to "true" will cause this script
# to actually perform a release.
: "${DO_MAKE_RELEASE:="false"}"

if [[ ! "${DO_MAKE_RELEASE}" =~ ^(false|true)$ ]]; then
  echo "DO_MAKE_RELEASE must be either \"true\" or \"false\"" >&2
  exit 1
fi

#######################################
# Create a Maven release on Sonatype.
#
# Globals:
#   GITHUB_ACCESS_TOKEN (optional from Kokoro)
#   IS_KOKORO
#   RELEASE_VERSION
#   TINK_JAVA_APPS_GITHUB_URL
#
#######################################
create_maven_release() {
  local gitub_protocol_and_auth="ssh://git"
  if [[ "${IS_KOKORO}" == "true" ]] ; then
    gitub_protocol_and_auth="https://ise-crypto:${GITHUB_ACCESS_TOKEN}"
    source "./kokoro/testutils/java_test_container_images.sh"
    CONTAINER_IMAGE="${TINK_JAVA_BASE_IMAGE}"
    RUN_COMMAND_ARGS+=( -k "${TINK_GCR_SERVICE_KEY}" )
  fi
  readonly gitub_protocol_and_auth
  local -r github_url="${gitub_protocol_and_auth}@${TINK_JAVA_APPS_GITHUB_URL}"
  readonly CONTAINER_IMAGE

  if [[ -n "${CONTAINER_IMAGE:-}" ]]; then
    RUN_COMMAND_ARGS+=( -c "${CONTAINER_IMAGE}" )
  fi

  local maven_deploy_library_options=( -u "${github_url}" )
  readonly maven_deploy_library_options

  local install_mvn_certificate_cmd=""

  if [[ "${IS_KOKORO}" == "true" && "${DO_MAKE_RELEASE}" == "true" ]]; then
    # Copy PGP key and passphrase files where the container can access them.
    cp "${KOKORO_KEYSTORE_DIR}/70968_tink_dev_maven_pgp_secret_key" \
      tink_dev_maven_pgp_secret_key
    cp "${KOKORO_KEYSTORE_DIR}/70968_tink_dev_maven_pgp_passphrase" \
      tink_dev_maven_pgp_passphrase
    cat <<EOF > install_maven_key.sh
gpg --import --pinentry-mode loopback --passphrase-file \
  ./tink_dev_maven_pgp_passphrase --batch ./tink_dev_maven_pgp_secret_key
EOF
     chmod +x install_maven_key.sh

     install_mvn_certificate_cmd="./install_maven_key.sh &&"
  fi

  readonly install_mvn_certificate_cmd

  # Share the required env variables with the container to allow publishing the
  # snapshot on Sonatype.
  cat <<EOF > env_variables.txt
SONATYPE_USERNAME
SONATYPE_PASSWORD
TINK_DEV_MAVEN_PGP_PASSPHRASE
EOF
  RUN_COMMAND_ARGS+=( -e env_variables.txt )

  ./kokoro/testutils/docker_execute.sh "${RUN_COMMAND_ARGS[@]}" \
    "${install_mvn_certificate_cmd}" \
    ./maven/maven_deploy_library.sh "${maven_deploy_library_options[@]}" \
      -n paymentmethodtoken/maven release apps-paymentmethodtoken \
      maven/tink-java-apps-paymentmethodtoken.pom.xml "${RELEASE_VERSION}"

  ./kokoro/testutils/docker_execute.sh "${RUN_COMMAND_ARGS[@]}" \
    "${install_mvn_certificate_cmd}" \
    ./maven/maven_deploy_library.sh "${maven_deploy_library_options[@]}" \
      -n rewardedads/maven release apps-rewardedads \
      maven/tink-java-apps-rewardedads.pom.xml "${RELEASE_VERSION}"

  ./kokoro/testutils/docker_execute.sh "${RUN_COMMAND_ARGS[@]}" \
    "${install_mvn_certificate_cmd}" \
    ./maven/maven_deploy_library.sh "${maven_deploy_library_options[@]}" \
      -n webpush/maven release apps-webpush \
      maven/tink-java-apps-webpush.pom.xml "${RELEASE_VERSION}"
}

main() {
  if [[ "${IS_KOKORO}" == "true" ]] ; then
    readonly TINK_BASE_DIR="$(echo "${KOKORO_ARTIFACTS_DIR}"/git*)"
    cd "${TINK_BASE_DIR}/tink_java_apps"
  fi
  create_maven_release
}

main "$@"
