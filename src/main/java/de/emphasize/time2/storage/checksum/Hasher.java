package de.emphasize.time2.storage.checksum;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {
  private final MessageDigest digest;

  private Hasher() {
    try {
      this.digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("sha-256 hash not available", e);
    }
  }

  public static Hasher create() {
    return new Hasher();
  }

  public String hash(String value) {
    byte[] encodedhash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(encodedhash);
  }

  private static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (int i = 0; i < hash.length; i++) {
      String hex = Integer.toHexString(0xff & hash[i]);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
