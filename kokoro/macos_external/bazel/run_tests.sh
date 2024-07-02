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

if [[ -n "${KOKORO_ROOT:-}" ]] ; then
  readonly TINK_BASE_DIR="$(echo "${KOKORO_ARTIFACTS_DIR}"/git*)"
  cd "${TINK_BASE_DIR}/tink_java_apps"
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11-latest/Contents/Home
  export XCODE_VERSION="14.1"
  export DEVELOPER_DIR="/Applications/Xcode.app/Contents/Developer"
fi

source ./kokoro/testutils/update_android_sdk.sh
# Skip Maven packages generation.
# TODO(b/342560474): Test all targets once Javadoc generation is fixed on Kokoro
# macOS.
./kokoro/testutils/run_bazel_tests.sh -m . //paymentmethodtoken/src/... \
  //rewardedads/src/... //webpush/src/...
