package net.goui.flogger.backend.system;

import com.google.common.flogger.FluentLogger;
import java.util.logging.Level;

class UnknownLoggerClass {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static void log(Level level, String message) {
    logger.at(level).log("%s", message);
  }

  static boolean isEnabled(Level level) {
    return logger.at(level).isEnabled();
  }
}
