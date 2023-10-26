package net.goui.flogger.backend.common;

import java.util.Map;

public interface NamingStrategy {
  static NamingStrategy from(Options options) {
    return FloggerPlugin.instantiate(
        NamingStrategy.class,
        options,
        Map.of(
            "default",
            opt -> NamingStrategy::removeNestedOrInnerClassName,
            "per_package",
            PackageStrategy::new));
  }

  String getLoggerName(String loggingClassName);

  private static String removeNestedOrInnerClassName(String className) {
    int idx = className.indexOf("$");
    return idx >= 0 ? className.substring(0, idx) : className;
  }

  class PackageStrategy implements NamingStrategy {
    private final int maxDepth;

    PackageStrategy(Options options) {
      this.maxDepth = (int) Long.min(options.getLong("max_depth", 0), Integer.MAX_VALUE);
    }

    @Override
    public String getLoggerName(String className) {
      int lastIdx = className.lastIndexOf('.');
      if (lastIdx == -1) {
        // Edge class where no package exists. Return empty string to indicate "root logger".
        return "";
      }
      if (maxDepth <= 0) {
        return className.substring(0, lastIdx);
      }
      // 'idx' can never be -1 in this code since we know there's a sentinel at the end.
      int idx = className.indexOf('.');
      for (int depth = 1; depth < maxDepth && idx < lastIdx; depth++) {
        idx = className.indexOf('.', idx + 1);
      }
      return className.substring(0, idx);
    }
  }
}
