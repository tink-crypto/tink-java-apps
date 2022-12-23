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

set -euo pipefail

# Fail if RELEASE_VERSION is not set.
if [[ -z "${RELEASE_VERSION:-}" ]]; then
  echo "RELEASE_VERSION must be set" >2&
  exit 1
fi

IS_KOKORO="false"
if [[ -n "${KOKORO_ARTIFACTS_DIR:-}" ]]; then
  IS_KOKORO="true"
fi
readonly IS_KOKORO

# WARNING: Setting this environment varialble to "true" will cause this script
# to actually perform a release.
: "${DO_MAKE_RELEASE:="false"}"

if [[ ! "${DO_MAKE_RELEASE}" =~ ^(false|true)$ ]]; then
  echo "DO_MAKE_RELEASE must be either \"true\" or \"false\"" >2&
  exit 1
fi

########### Create a Maven release on Sonatype. ###########

GITUB_PROTOCOL_AND_AUTH="ssh://git"
if [[ "${IS_KOKORO}" == "true" ]] ; then
  readonly TINK_BASE_DIR="$(echo "${KOKORO_ARTIFACTS_DIR}"/git*)"
  cd "${TINK_BASE_DIR}/tink_java_apps"
  GITUB_PROTOCOL_AND_AUTH="https://ise-crypto:${GITHUB_ACCESS_TOKEN}"
fi
readonly GITUB_PROTOCOL_AND_AUTH
readonly TINK_JAVA_APPS_GITHUB_URL="github.com/tink-crypto/tink-java-apps"
readonly GITHUB_URL="${GITUB_PROTOCOL_AND_AUTH}@${TINK_JAVA_APPS_GITHUB_URL}"

MAVEN_DEPLOY_LIBRARY_OPTIONS=( -u "${GITHUB_URL}" )
if [[ "${DO_MAKE_RELEASE}" == "false" ]]; then
  MAVEN_DEPLOY_LIBRARY_OPTIONS+=( -d )
fi
readonly MAVEN_DEPLOY_LIBRARY_OPTIONS

./maven/maven_deploy_library.sh "${MAVEN_DEPLOY_LIBRARY_OPTIONS[@]}" \
  -n paymentmethodtoken/maven release apps-paymentmethodtoken \
  maven/tink-java-apps-paymentmethodtoken.pom.xml "${RELEASE_VERSION}"

./maven/maven_deploy_library.sh "${MAVEN_DEPLOY_LIBRARY_OPTIONS[@]}" \
  -n rewardedads/maven release apps-rewardedads \
  maven/tink-java-apps-rewardedads.pom.xml "${RELEASE_VERSION}"

./maven/maven_deploy_library.sh "${MAVEN_DEPLOY_LIBRARY_OPTIONS[@]}" \
  -n webpush/maven release apps-webpush \
  maven/tink-java-apps-webpush.pom.xml "${RELEASE_VERSION}"
