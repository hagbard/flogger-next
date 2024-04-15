package net.goui.flogger.backend.common;

import com.google.common.flogger.MetadataKey;

public final class MetadataKeyLoader {

  public static MetadataKey<?> loadMetadataKey(String keyName) {
    // Expected: "foo.bar.Class#Field"
    int idx = keyName.indexOf('#');
    if (idx == -1) {
      throw new IllegalArgumentException("Invalid metadata key name: " + keyName);
    }
    String className = keyName.substring(0, idx);
    String fieldName = keyName.substring(idx + 1);
    try {
      return (MetadataKey<?>) Class.forName(className).getDeclaredField(fieldName).get(null);
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot load metadata key: " + keyName, e);
    }
  }

  private MetadataKeyLoader() {}
}
