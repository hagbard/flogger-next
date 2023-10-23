package net.goui.flogger.backend.common;

public interface NamingStrategy {
  static NamingStrategy from(Options options) {
    String strategy = options.getString("strategy", "default");
    if (Options.isAnyOf(strategy, "default", "per_class")) {
      return NamingStrategy::removeNestedOrInnerClassName;
    } else if (Options.isAnyOf(strategy, "per_package")) {
      return c -> getPackage(c, options.getLong("max_depth", 0));
    }
    return FloggerPlugin.instantiate(NamingStrategy.class, strategy, options);
  }

  String getLoggerName(String loggingClassName);

  private static String getPackage(String className, long maxDepth) {
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
    while (--maxDepth > 0 && idx < lastIdx) {
      idx = className.indexOf('.', idx + 1);
    }
    return className.substring(0, idx);
  }

  private static String removeNestedOrInnerClassName(String className) {
    int idx = className.indexOf("$");
    return idx >= 0 ? className.substring(0, idx) : className;
  }
}
