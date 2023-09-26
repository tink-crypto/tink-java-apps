// Copyright 2023 Google Inc.
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

package com.google.crypto.tink.apps.paymentmethodtoken;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * An example on how to encrypt and decrypt messages using the paymentmethodtoken library.
 *
 * <p>https://developers.google.com/pay/api/web/guides/resources/payment-data-cryptography
 */
@RunWith(JUnit4.class)
public final class ExampleTest {

  /**
   * Sample Google private signing key for the ECv2 protocolVersion.
   *
   * <p>Corresponds to one of the public keys in {@link #GOOGLE_VERIFYING_PUBLIC_KEYS_JSON}.
   */
  private static final String GOOGLE_SIGNING_PRIVATE_KEY_PKCS8_BASE64 =
      "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgKvEdSS8f0mjTCNKev"
          + "aKXIzfNC5b4A104gJWI9TsLIMqhRANCAAT/X7ccFVJt2/6Ps1oCt2AzKhIAz"
          + "jfJHJ3Op2DVPGh1LMD3oOPgxzUSIqujHG6dq9Ui93Eacl4VWJPMW/MVHHIL";

  /** Sample Google provided JSON with its public signing keys. */
  private static final String GOOGLE_VERIFYING_PUBLIC_KEYS_JSON =
      "{\n"
          + "  \"keys\": [\n"
          + "    {\n"
          + "      \"keyValue\": \"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPYnHwS8uegWAewQtlxizmLFynw"
          + "HcxRT1PK07cDA6/C4sXrVI1SzZCUx8U8S0LjMrT6ird/VW7be3Mz6t/srtRQ==\",\n"
          + "      \"protocolVersion\": \"ECv1\"\n"
          + "    },\n"
          + "    {\n"
          + "      \"keyValue\": \"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE/1+3HBVSbdv+j7NaArdgMyoSAM"
          + "43yRydzqdg1TxodSzA96Dj4Mc1EiKroxxunavVIvdxGnJeFViTzFvzFRxyCw==\",\n"
          + "      \"keyExpiration\": \""
          + (System.currentTimeMillis() + 24 * 60 * 60 * 1000)
          + "\",\n"
          + "      \"protocolVersion\": \"ECv2\"\n"
          + "    },\n"
          + "    {\n"
          + "      \"keyValue\": \"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAENXvYqxD5WayKYhuXQevdGdLA8i"
          + "fV4LsRS2uKvFo8wwyiwgQHB9DiKzG6T/P1Fu9Bl7zWy/se5Dy4wk1mJoPuxg==\",\n"
          + "      \"keyExpiration\": \""
          + (System.currentTimeMillis() + 24 * 60 * 60 * 1000)
          + "\",\n"
          + "      \"protocolVersion\": \"ECv2SigningOnly\"\n"
          + "    }\n"
          + "  ]\n"
          + "}";

  /**
   * Sample Google intermediate private signing key.
   *
   * <p>Corresponds to {@link #GOOGLE_SIGNING_INTERMEDIATE_PUBLIC_KEY_X509_BASE64}.
   */
  private static final String GOOGLE_SIGNING_INTERMEDIATE_PRIVATE_KEY_PKCS8_BASE64 =
      "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgKvEdSS8f0mjTCNKev"
          + "aKXIzfNC5b4A104gJWI9TsLIMqhRANCAAT/X7ccFVJt2/6Ps1oCt2AzKhIAz"
          + "jfJHJ3Op2DVPGh1LMD3oOPgxzUSIqujHG6dq9Ui93Eacl4VWJPMW/MVHHIL";

  /**
   * Sample Google intermediate public signing key.
   *
   * <p>Corresponds to {@link #GOOGLE_SIGNING_INTERMEDIATE_PRIVATE_KEY_PKCS8_BASE64}.
   */
  private static final String GOOGLE_SIGNING_INTERMEDIATE_PUBLIC_KEY_X509_BASE64 =
      "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE/1+3HBVSbdv+j7NaArdgMyoSAM43yR"
          + "ydzqdg1TxodSzA96Dj4Mc1EiKroxxunavVIvdxGnJeFViTzFvzFRxyCw==";

  /**
   * Sample merchant private key.
   *
   * <p>Corresponds to {@link #MERCHANT_PUBLIC_KEY_BASE64}
   *
   * <pre>
   * openssl pkcs8 -topk8 -inform PEM -outform PEM -in merchant-key.pem -nocrypt
   * </pre>
   */
  private static final String MERCHANT_PRIVATE_KEY_PKCS8_BASE64 =
      "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgCPSuFr4iSIaQprjj"
          + "chHPyDu2NXFe0vDBoTpPkYaK9dehRANCAATnaFz/vQKuO90pxsINyVNWojabHfbx"
          + "9qIJ6uD7Q7ZSxmtyo/Ez3/o2kDT8g0pIdyVIYktCsq65VoQIDWSh2Bdm";

  /**
   * Sample merchant public key.
   *
   * <p>Corresponds to {@link #MERCHANT_PRIVATE_KEY_PKCS8_BASE64}
   *
   * <p>Created with:
   *
   * <pre>
   * openssl ec -in merchant-key.pem -pubout -text -noout 2> /dev/null | grep "pub:" -A5 \
   *     | xxd -r -p | base64
   * </pre>
   */
  private static final String MERCHANT_PUBLIC_KEY_BASE64 =
      "BOdoXP+9Aq473SnGwg3JU1aiNpsd9vH2ognq4PtDtlLGa3Kj8TPf+jaQNPyDSkh3JUhiS0KyrrlWhAgNZKHYF2Y=";

  @Test
  public void testSealAndUnseal() throws Exception {
    // Create a PaymentMethodTokenSender to send sealed messages to a merchant.
    PaymentMethodTokenSender sender =
        new PaymentMethodTokenSender.Builder()
            .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V2)
            .senderIntermediateSigningKey(GOOGLE_SIGNING_INTERMEDIATE_PRIVATE_KEY_PKCS8_BASE64)
            .senderIntermediateCert(
                new SenderIntermediateCertFactory.Builder()
                    .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V2)
                    .addSenderSigningKey(GOOGLE_SIGNING_PRIVATE_KEY_PKCS8_BASE64)
                    .senderIntermediateSigningKey(
                        GOOGLE_SIGNING_INTERMEDIATE_PUBLIC_KEY_X509_BASE64)
                    .expiration(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
                    .build()
                    .create())
            .recipientId("[YOUR MERCHANT ID]")
            .rawUncompressedRecipientPublicKey(MERCHANT_PUBLIC_KEY_BASE64)
            .build();

    String encryptedMessage = sender.seal("plaintext");

    // Create a PaymentMethodTokenRecipient to receive sealed messages.
    PaymentMethodTokenRecipient recipient =
        new PaymentMethodTokenRecipient.Builder()
            .protocolVersion("ECv2")
            // IMPORTANT: Instead of using "senderVerifyingKeys" to set the verifying public keys
            // of the sender, prefer calling:
            // .fetchSenderVerifyingKeysWith(GooglePaymentsPublicKeysManager.INSTANCE_PRODUCTION)
            .senderVerifyingKeys(GOOGLE_VERIFYING_PUBLIC_KEYS_JSON)
            .recipientId("[YOUR MERCHANT ID]")
            .addRecipientPrivateKey(MERCHANT_PRIVATE_KEY_PKCS8_BASE64)
            .build();

    String decryptedMessage = recipient.unseal(encryptedMessage);

    assertThat(decryptedMessage).isEqualTo("plaintext");
  }
}
