# Tink Java Apps

<!-- GCP Ubuntu --->

[tink_java_apps_bazel_badge_gcp_ubuntu]: https://storage.googleapis.com/tink-kokoro-build-badges/tink-java-apps-bazel-gcp-ubuntu.svg
[tink_java_apps_maven_badge_gcp_ubuntu]: https://storage.googleapis.com/tink-kokoro-build-badges/tink-java-apps-maven-gcp-ubuntu.svg

<!-- MacOS --->

[tink_java_apps_bazel_badge_macos]: https://storage.googleapis.com/tink-kokoro-build-badges/tink-java-apps-bazel-macos-external.svg

**Test**     | **GCP Ubuntu**                                                 | **MacOS**
------------ | -------------------------------------------------------------- | ---------
Tink (Bazel) | [![Bazel_GcpUbuntu][tink_java_apps_bazel_badge_gcp_ubuntu]](#) | [![Bazel_MacOs][tink_java_apps_bazel_badge_macos]](#)
Maven        | [![Maven_GcpUbuntu][tink_java_apps_maven_badge_gcp_ubuntu]](#) | N/A

This repository contains extensions and applications of the
[Tink Java](https://github.com/tink-crypto/tink-java) library:

*   `apps-paymentmethodtoken`: An implementation of
    [Google Payment Method Token](https://developers.google.com/pay/api/payment-data-cryptography)
*   `apps-rewardedads`: An implementation of the verifier side of
    [Server-Side Verification of Google AdMob Rewarded Ads](https://developers.google.com/admob/android/ssv)
*   `apps-webpush`: An implementation of
    [RFC 8291 - Message Encryption for Web Push](https://www.rfc-editor.org/rfc/rfc8291)

The latest version of these applications is 1.13.0.

The official Tink documentation is available at
https://developers.google.com/tink.

### apps-paymentmethodtoken

You can add this library as a Maven dependency:

```xml
<dependency>
  <groupId>com.google.crypto.tink</groupId>
  <artifactId>apps-paymentmethodtoken</artifactId>
  <version>1.13.0</version>
</dependency>
```

The Javadoc for the latest release can be found
[here](https://tink-crypto.github.io/tink-java-apps/javadoc/apps-paymentmethodtoken/1.13.0/).

### apps-rewardedads

You can add this library as a Maven dependency:

```xml
<dependency>
  <groupId>com.google.crypto.tink</groupId>
  <artifactId>apps-rewardedads</artifactId>
  <version>1.13.0</version>
</dependency>
```

The Javadoc for the latest release can be found
[here](https://tink-crypto.github.io/tink-java-apps/javadoc/apps-rewardedads/1.13.0/).

### apps-webpush

You can add this library as a Maven dependency:

```xml
<dependency>
  <groupId>com.google.crypto.tink</groupId>
  <artifactId>apps-webpush</artifactId>
  <version>1.13.0</version>
</dependency>
```

The Javadoc for the latest release can be found
[here](https://tink-crypto.github.io/tink-java-apps/javadoc/apps-webpush/1.13.0/).

You can encrypt a plaintext as follows:

```java
import com.google.crypto.tink.HybridEncrypt;
import java.security.interfaces.ECPublicKey;

ECPublicKey reicipientPublicKey = ...;
byte[] authSecret = ...;
HybridEncrypt hybridEncrypt = new WebPushHybridEncrypt.Builder()
     .withAuthSecret(authSecret)
     .withRecipientPublicKey(recipientPublicKey)
     .build();
byte[] plaintext = ...;
byte[] ciphertext = hybridEncrypt.encrypt(plaintext, null /* contextInfo, must be null */);
```

To decrypt:

```java
import com.google.crypto.tink.HybridDecrypt;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

ECPrivateKey recipientPrivateKey = ...;
ECPublicKey  recipientPublicKey = ...;
HybridDecrypt hybridDecrypt = new WebPushHybridDecrypt.Builder()
     .withAuthSecret(authSecret)
     .withRecipientPublicKey(recipientPublicKey)
     .withRecipientPrivateKey(recipientPrivateKey)
     .build();
byte[] ciphertext = ...;
byte[] plaintext = hybridDecrypt.decrypt(ciphertext, /* contextInfo, must be null */);
```

## Contact and mailing list

If you want to contribute, please read [CONTRIBUTING](docs/CONTRIBUTING.md) and
send us pull requests. You can also report bugs or file feature requests.

If you'd like to talk to the developers or get notified about major product
updates, you may want to subscribe to our
[mailing list](https://groups.google.com/forum/#!forum/tink-users).

## Maintainers

Tink is maintained by (A-Z):

-   Moreno Ambrosin
-   Taymon Beal
-   William Conner
-   Thomas Holenstein
-   Stefan Kölbl
-   Charles Lee
-   Cindy Lin
-   Fernando Lobato Meeser
-   Ioana Nedelcu
-   Sophie Schmieg
-   Elizaveta Tretiakova
-   Jürg Wullschleger

Alumni:

-   Haris Andrianakis
-   Daniel Bleichenbacher
-   Tanuj Dhir
-   Thai Duong
-   Atul Luykx
-   Rafael Misoczki
-   Quan Nguyen
-   Bartosz Przydatek
-   Enzo Puig
-   Laurent Simon
-   Veronika Slívová
-   Paula Vidas
