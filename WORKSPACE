workspace(name = "tink_java_apps")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "tink_java",
    urls = ["https://github.com/tink-crypto/tink-java/releases/download/v1.11.0/tink-java-1.11.0.zip"],
    strip_prefix = "tink-java-1.11.0",
    sha256 = "2bd264c2f0c474c77e2d1e04c627398e963b7a6d0164cfb743ab60a59ab998bd",
)

load("@tink_java//:tink_java_deps.bzl", "tink_java_deps", "TINK_MAVEN_ARTIFACTS")

tink_java_deps()

load("@tink_java//:tink_java_deps_init.bzl", "tink_java_deps_init")

tink_java_deps_init()

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = TINK_MAVEN_ARTIFACTS,
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)
