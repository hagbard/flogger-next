package net.goui.flogger.backend.common;

import com.google.common.flogger.MetadataKey;

public final class RenameThisClass {


  public static MetadataKey<?> loadMetadataKey(String keyName) {
    // Expected: "foo.bar.Class#Field"
    int idx = keyName.indexOf('#');
    if (idx == -1) {
      throw new IllegalStateException("Invalid metadata key name: " + keyName);
    }
    String className = keyName.substring(0, idx);
    String fieldName = keyName.substring(idx + 1);
    try {
      return (MetadataKey<?>) Class.forName(className).getDeclaredField(fieldName).get(null);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load metadata key: " + keyName, e);
    }
  }

  private RenameThisClass() {}
}
