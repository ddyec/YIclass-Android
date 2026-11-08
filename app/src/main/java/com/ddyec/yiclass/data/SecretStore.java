package com.ddyec.yiclass.data;

import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

public final class SecretStore {
  private final Context context;

  public SecretStore(Context c) {
    context = c.getApplicationContext();
  }

  private javax.crypto.SecretKey key() throws GeneralSecurityException, java.io.IOException {
    KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
    ks.load(null);
    if (!ks.containsAlias("yiclass-api")) {
      KeyGenerator g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
      g.init(
          new KeyGenParameterSpec.Builder(
                  "yiclass-api", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      g.generateKey();
    }
    return (javax.crypto.SecretKey) ks.getKey("yiclass-api", null);
  }

  public void save(String value) throws Exception {
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.ENCRYPT_MODE, key());
    context
        .getSharedPreferences("secrets", 0)
        .edit()
        .putString(
            "key",
            Base64.encodeToString(
                c.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)), Base64.NO_WRAP))
        .putString("iv", Base64.encodeToString(c.getIV(), Base64.NO_WRAP))
        .apply();
  }

  public String read() throws Exception {
    var p = context.getSharedPreferences("secrets", 0);
    String value = p.getString("key", "");
    if (value.isEmpty()) return "";
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(
        Cipher.DECRYPT_MODE,
        key(),
        new GCMParameterSpec(128, Base64.decode(p.getString("iv", ""), Base64.NO_WRAP)));
    return new String(
        c.doFinal(Base64.decode(value, Base64.NO_WRAP)), java.nio.charset.StandardCharsets.UTF_8);
  }
}
