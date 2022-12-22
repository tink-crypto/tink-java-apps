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

IS_KOKORO="false"
if [[ -n "${KOKORO_ARTIFACTS_DIR:-}" ]] ; then
  IS_KOKORO="true"
fi
readonly IS_KOKORO

if [[ "${IS_KOKORO}" == "true" ]] ; then
  TINK_BASE_DIR="$(echo "${KOKORO_ARTIFACTS_DIR}"/git*)"
  cd "${TINK_BASE_DIR}/tink_java_apps"
fi

./kokoro/testutils/replace_http_archive_with_local_repository.py \
  -f "WORKSPACE" -t "${TINK_BASE_DIR}"

# TODO(b/263465812): Add test example app for testing the Maven snapshots.

if [[ "${IS_KOKORO}" == "true" \
      && "${KOKORO_JOB_NAME}" == "${GITHUB_JOB_NAME}" ]] ; then
  readonly GITHUB_JOB_NAME="tink/github/java_apps/gcp_ubuntu/maven/continuous"
  # GITHUB_ACCESS_TOKEN is populated by Kokoro.
  readonly GIT_CREDENTIALS="ise-crypto:${GITHUB_ACCESS_TOKEN}"
  readonly GITHUB_URL="https://${GIT_CREDENTIALS}@github.com/tink-crypto/tink-java-apps.git"

  ./maven/maven_deploy_library.sh -u "${GITHUB_URL}" \
    -n paymentmethodtoken/maven snapshot apps-paymentmethodtoken \
    maven/tink-java-apps-paymentmethodtoken.pom.xml HEAD

  ./maven/maven_deploy_library.sh -u "${GITHUB_URL}" \
    -n rewardedads/maven snapshot apps-rewardedads \
    maven/tink-java-apps-rewardedads.pom.xml HEAD

  ./maven/maven_deploy_library.sh -u "${GITHUB_URL}" \
    -n webpush/maven snapshot apps-webpush \
    maven/tink-java-apps-webpush.pom.xml HEAD
fi
