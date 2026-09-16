// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.crypto.tink.apps.webpush;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.subtle.Base64;
import com.google.crypto.tink.subtle.EllipticCurves;
import com.google.crypto.tink.subtle.Random;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@code WebPushHybridDecrypt}. */
@RunWith(JUnit4.class)
public class WebPushHybridDecryptTest {
  // Copied from https://tools.ietf.org/html/rfc8291#section-5.
  private static final String PLAINTEXT = "V2hlbiBJIGdyb3cgdXAsIEkgd2FudCB0byBiZSBhIHdhdGVybWVsb24";
  private static final String RECEIVER_PRIVATE_KEY = "q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94";
  private static final String RECEIVER_PUBLIC_KEY =
      "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
  private static final String AUTH_SECRET = "BTBZMqHH6r4Tts7J_aSIgg";
  private static final String CIPHERTEXT =
      "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27ml"
          + "mlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPT"
          + "pK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN";
  private static final int RECORD_SIZE = 4096;

  @Test
  public void testWithRfc8291TestVector() throws Exception {
    byte[] plaintext = Base64.urlSafeDecode(PLAINTEXT);
    byte[] recipientPrivateKey = Base64.urlSafeDecode(RECEIVER_PRIVATE_KEY);
    byte[] recipientPublicKey = Base64.urlSafeDecode(RECEIVER_PUBLIC_KEY);
    byte[] authSecret = Base64.urlSafeDecode(AUTH_SECRET);
    byte[] ciphertext = Base64.urlSafeDecode(CIPHERTEXT);

    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withRecordSize(RECORD_SIZE)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(recipientPublicKey)
            .withRecipientPrivateKey(recipientPrivateKey)
            .build();
    assertArrayEquals(plaintext, hybridDecrypt.decrypt(ciphertext, /* contextInfo= */ null));
  }

  @Test
  public void testEncryptDecryptWithInvalidRecordSizes() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    // Test with out of range record sizes.
    {
      WebPushHybridDecrypt.Builder builder =
          new WebPushHybridDecrypt.Builder()
              .withRecordSize(WebPushConstants.MAX_CIPHERTEXT_SIZE + 1)
              .withAuthSecret(authSecret)
              .withRecipientPublicKey(uaPublicKey)
              .withRecipientPrivateKey(uaPrivateKey);
      assertThrows(IllegalArgumentException.class, () -> builder.build());

      WebPushHybridDecrypt.Builder builder2 =
          new WebPushHybridDecrypt.Builder()
              .withRecordSize(WebPushConstants.CIPHERTEXT_OVERHEAD - 1)
              .withAuthSecret(authSecret)
              .withRecipientPublicKey(uaPublicKey)
              .withRecipientPrivateKey(uaPrivateKey);
      assertThrows(IllegalArgumentException.class, () -> builder2.build());
    }

    // Test with random mismatched record size.
    {
      for (int i = 0; i < 50; i++) {
        int recordSize =
            WebPushConstants.CIPHERTEXT_OVERHEAD
                + Random.randInt(
                    WebPushConstants.MAX_CIPHERTEXT_SIZE
                        - WebPushConstants.CIPHERTEXT_OVERHEAD
                        - 1);
        HybridEncrypt hybridEncrypt =
            new WebPushHybridEncrypt.Builder()
                .withRecordSize(recordSize)
                .withAuthSecret(authSecret)
                .withRecipientPublicKey(uaPublicKey)
                .build();
        HybridDecrypt hybridDecrypt =
            new WebPushHybridDecrypt.Builder()
                .withRecordSize(recordSize + 1)
                .withAuthSecret(authSecret)
                .withRecipientPublicKey(uaPublicKey)
                .withRecipientPrivateKey(uaPrivateKey)
                .build();
        byte[] plaintext = Random.randBytes(recordSize - WebPushConstants.CIPHERTEXT_OVERHEAD);
        byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);

        assertThrows(
            GeneralSecurityException.class,
            () -> hybridDecrypt.decrypt(ciphertext, /* contextInfo= */ null));
      }
    }
  }

  @Test
  public void testNonNullContextInfo() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    HybridEncrypt hybridEncrypt =
        new WebPushHybridEncrypt.Builder()
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .build();
    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey)
            .build();
    byte[] plaintext = Random.randBytes(20);
    byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);
    byte[] contextInfo = new byte[0];
    assertThrows(
        GeneralSecurityException.class, () -> hybridDecrypt.decrypt(ciphertext, contextInfo));
  }

  @Test
  public void testModifyCiphertext() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    HybridEncrypt hybridEncrypt =
        new WebPushHybridEncrypt.Builder()
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .build();
    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey)
            .build();
    byte[] plaintext = Random.randBytes(20);
    byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);

    // Flipping bits.
    for (int b = 0; b < ciphertext.length; b++) {
      for (int bit = 0; bit < 8; bit++) {
        byte[] modified = Arrays.copyOf(ciphertext, ciphertext.length);
        modified[b] ^= (byte) (1 << bit);
        assertThrows(
            GeneralSecurityException.class,
            () -> hybridDecrypt.decrypt(modified, /* contextInfo= */ null));
      }
    }

    // Truncate the message.
    for (int length = 0; length < ciphertext.length; length++) {
      byte[] modified = Arrays.copyOf(ciphertext, length);
      assertThrows(
          GeneralSecurityException.class,
          () -> hybridDecrypt.decrypt(modified, /* contextInfo= */ null));
    }
  }

  @Test
  public void testEncryptDecrypt_withPadding_shouldWork() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    int paddingSize = 20;
    int plaintextSize = 20;
    HybridEncrypt hybridEncrypt =
        new WebPushHybridEncrypt.Builder()
            .withAuthSecret(authSecret)
            .withPaddingSize(paddingSize)
            .withRecipientPublicKey(uaPublicKey)
            .build();
    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey)
            .build();
    byte[] plaintext = Random.randBytes(plaintextSize);
    byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);

    assertEquals(
        ciphertext.length, plaintext.length + paddingSize + WebPushConstants.CIPHERTEXT_OVERHEAD);
    assertArrayEquals(plaintext, hybridDecrypt.decrypt(ciphertext, /* contextInfo= */ null));
  }

  @Test
  public void testDecrypt_largePayloadWithDefaultDecryptor_fails() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    byte[] plaintext = Random.randBytes(10 * 1024);
    HybridEncrypt hybridEncrypt =
        new WebPushHybridEncrypt.Builder()
            .withMaxCiphertextSize(20 * 1024)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .build();
    byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);

    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey)
            .build();

    assertThrows(
        GeneralSecurityException.class,
        () -> hybridDecrypt.decrypt(ciphertext, /* contextInfo= */ null));
  }

  @Test
  public void testDecrypt_largePayloadWithMaxCiphertextSize_succeeds() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    // 100 KB payload simulating large screenshot data.
    byte[] plaintext = Random.randBytes(100 * 1024);
    HybridEncrypt hybridEncrypt =
        new WebPushHybridEncrypt.Builder()
            .withMaxCiphertextSize(200 * 1024)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .build();
    byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);

    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withMaxCiphertextSize(200 * 1024)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey)
            .build();

    byte[] decrypted = hybridDecrypt.decrypt(ciphertext, /* contextInfo= */ null);
    assertArrayEquals(plaintext, decrypted);
  }

  @Test
  public void testDecrypt_largePayloadWithExplicitRecordSize_succeeds() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    byte[] plaintext = Random.randBytes(50 * 1024);
    int explicitRecordSize = 60 * 1024;
    HybridEncrypt hybridEncrypt =
        new WebPushHybridEncrypt.Builder()
            .withMaxCiphertextSize(100 * 1024)
            .withRecordSize(explicitRecordSize)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .build();
    byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);

    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withMaxCiphertextSize(100 * 1024)
            .withRecordSize(explicitRecordSize)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey)
            .build();

    byte[] decrypted = hybridDecrypt.decrypt(ciphertext, /* contextInfo= */ null);
    assertArrayEquals(plaintext, decrypted);
  }

  @Test
  public void testDecrypt_largePayloadWithMismatchedExplicitRecordSize_fails() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    byte[] plaintext = Random.randBytes(50 * 1024);
    int explicitRecordSize = 60 * 1024;
    HybridEncrypt hybridEncrypt =
        new WebPushHybridEncrypt.Builder()
            .withMaxCiphertextSize(100 * 1024)
            .withRecordSize(explicitRecordSize)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .build();
    byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);

    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withMaxCiphertextSize(100 * 1024)
            .withRecordSize(explicitRecordSize + 1)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey)
            .build();

    assertThrows(
        GeneralSecurityException.class,
        () -> hybridDecrypt.decrypt(ciphertext, /* contextInfo= */ null));
  }

  @Test
  public void testDecrypt_payloadExceedingConfiguredMaxCiphertextSize_fails() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    byte[] plaintext = Random.randBytes(10 * 1024);
    HybridEncrypt hybridEncrypt =
        new WebPushHybridEncrypt.Builder()
            .withMaxCiphertextSize(20 * 1024)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .build();
    byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);

    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withMaxCiphertextSize(8 * 1024)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey)
            .build();

    assertThrows(
        GeneralSecurityException.class,
        () -> hybridDecrypt.decrypt(ciphertext, /* contextInfo= */ null));
  }

  @Test
  public void testDecrypt_recordSizeExceedingMaxCiphertextSize_fails() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    byte[] plaintext = Random.randBytes(5 * 1024);
    // Header record size declares 20KB, but decryptor allows only 10KB.
    HybridEncrypt hybridEncrypt =
        new WebPushHybridEncrypt.Builder()
            .withMaxCiphertextSize(20 * 1024)
            .withRecordSize(20 * 1024)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .build();
    byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);

    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withMaxCiphertextSize(10 * 1024)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey)
            .build();

    assertThrows(
        GeneralSecurityException.class,
        () -> hybridDecrypt.decrypt(ciphertext, /* contextInfo= */ null));
  }

  @Test
  public void testDecrypt_recordSizeSmallerThanPayload_fails() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    byte[] plaintext = Random.randBytes(50 * 1024);
    HybridEncrypt hybridEncrypt =
        new WebPushHybridEncrypt.Builder()
            .withMaxCiphertextSize(100 * 1024)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .build();
    byte[] ciphertext = hybridEncrypt.encrypt(plaintext, /* contextInfo= */ null);
    // Tamper with the 4-byte record size field in the header to be smaller than the actual payload.
    ByteBuffer.wrap(ciphertext).putInt(WebPushConstants.SALT_SIZE, 1024);

    HybridDecrypt hybridDecrypt =
        new WebPushHybridDecrypt.Builder()
            .withMaxCiphertextSize(100 * 1024)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey)
            .build();

    assertThrows(
        GeneralSecurityException.class,
        () -> hybridDecrypt.decrypt(ciphertext, /* contextInfo= */ null));
  }

  @Test
  public void testBuilder_invalidMaxCiphertextSize_throws() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    WebPushHybridDecrypt.Builder builder =
        new WebPushHybridDecrypt.Builder()
            .withMaxCiphertextSize(WebPushConstants.CIPHERTEXT_OVERHEAD - 1)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey);
    assertThrows(IllegalArgumentException.class, () -> builder.build());
  }

  @Test
  public void testBuilder_explicitRecordSizeExceedsMaxCiphertextSize_throws() throws Exception {
    KeyPair uaKeyPair = EllipticCurves.generateKeyPair(WebPushConstants.NIST_P256_CURVE_TYPE);
    ECPrivateKey uaPrivateKey = (ECPrivateKey) uaKeyPair.getPrivate();
    ECPublicKey uaPublicKey = (ECPublicKey) uaKeyPair.getPublic();
    byte[] authSecret = Random.randBytes(16);

    WebPushHybridDecrypt.Builder builder =
        new WebPushHybridDecrypt.Builder()
            .withMaxCiphertextSize(5000)
            .withRecordSize(5001)
            .withAuthSecret(authSecret)
            .withRecipientPublicKey(uaPublicKey)
            .withRecipientPrivateKey(uaPrivateKey);
    assertThrows(IllegalArgumentException.class, () -> builder.build());
  }
}
