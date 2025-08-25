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

# Builds and tests tink-java-apps using Bazel.
#
# The behavior of this script can be modified using the following optional env
# variables:
#
# - CONTAINER_IMAGE (unset by default): By default when run locally this script
#   executes tests directly on the host. The CONTAINER_IMAGE variable can be set
#   to execute tests in a custom container image for local testing. E.g.:
#
#   CONTAINER_IMAGE="us-docker.pkg.dev/tink-test-infrastructure/tink-ci-images/linux-tink-java-base:latest" \
#     sh ./kokoro/gcp_ubuntu/bazel/run_tests.sh

# Generated with openssl rand -hex 10
echo "==========================================================================="
echo "Tink Script ID: d07f521e94e09d66b551 (to quickly find the script from logs)"
echo "==========================================================================="

set -eEuox pipefail

if [[ ! -v TINK_BASE_DIR ]] ; then
  TINK_BASE_DIR="$(echo "${KOKORO_ARTIFACTS_DIR}"/git*)"
fi
readonly TINK_BASE_DIR
echo "TINK_BASE_DIR is ${TINK_BASE_DIR}"
if [[ ! -v CONTAINER_IMAGE ]] ; then
  source \
    "${TINK_BASE_DIR}/tink_java_apps/kokoro/testutils/java_test_container_images.sh"
  CONTAINER_IMAGE="${TINK_JAVA_BASE_IMAGE}"
fi
readonly CONTAINER_IMAGE
echo "CONTAINER_IMAGE is ${CONTAINER_IMAGE}"

cd "${TINK_BASE_DIR}/tink_java_apps"

if [[ -v TINK_REMOTE_BAZEL_CACHE_GCS_BUCKET ]]; then
  cp "${TINK_REMOTE_BAZEL_CACHE_SERVICE_KEY}" ./cache_key
  cat <<EOF > /tmp/env_variables.txt
BAZEL_REMOTE_CACHE_NAME=${TINK_REMOTE_BAZEL_CACHE_GCS_BUCKET}/bazel/${TINK_JAVA_BASE_IMAGE}
EOF
else
  touch /tmp/env_variables.txt
fi


### =========================================== EMBEDDED SCRIPT: _do_run_test.sh
### The _do_run_test.sh script will run as "tinkuser"
cat <<'EOF' > _do_run_test.sh
#!/bin/bash
set -euo pipefail

cd /home/tinkuser/tink_java_apps/

CACHE_FLAGS=()
if [[ -n "${BAZEL_REMOTE_CACHE_NAME:-}" ]]; then
  CACHE_FLAGS+=("--remote_cache=https://storage.googleapis.com/${BAZEL_REMOTE_CACHE_NAME}")
  CACHE_FLAGS+=("--google_credentials=$(realpath ./cache_key)")
fi
readonly CACHE_FLAGS

cat .bazelrc

echo "---------- BUILDING MAIN"
time bazelisk build "${CACHE_FLAGS[@]}" -- ...
echo "---------- TESTING MAIN"
time bazelisk test "${CACHE_FLAGS[@]}" -- ...

EOF
### ======================================================= END: _do_run_test.sh

chmod +x _do_run_test.sh

### =========================== EMBEDDED SCRIPT: _cp_switch_user_and_run_test.sh
### The _cp_switch_user_and_run_test.sh script will cp the build tree to a
### directory readable for tinkuser then execute _do_run_test.sh.
cat <<'EOF' > _cp_switch_user_and_run_test.sh
set -euo pipefail

echo "=== COPYING /tink_orig_dir to /home/tinkuser"

cp -r /tink_orig_dir/tink_java_apps /home/tinkuser
chown --recursive tinkuser:tinkgroup /home/tinkuser/tink_java_apps

su tinkuser /usr/bin/bash -c "/home/tinkuser/tink_java_apps/_do_run_test.sh"
exit
EOF
### ======================================= END: _cp_switch_user_and_run_test.sh

chmod +x _cp_switch_user_and_run_test.sh

if [[ ! -z "${TINK_GCR_SERVICE_KEY:-}" ]]; then
  gcloud auth activate-service-account --key-file="${TINK_GCR_SERVICE_KEY}"
  gcloud config set project tink-test-infrastructure
  gcloud auth configure-docker us-docker.pkg.dev --quiet
fi

echo "-- PULLING DOCKER IMAGE"
time docker pull "${CONTAINER_IMAGE}"

echo "-- RUNNING DOCKER"
time docker run \
  --network="host" \
  --mount type=bind,src="${TINK_BASE_DIR}",dst=/tink_orig_dir \
  --env-file /tmp/env_variables.txt \
  --rm \
  "${CONTAINER_IMAGE}" \
  bash -c "/tink_orig_dir/tink_java_apps/_cp_switch_user_and_run_test.sh"
