package net.goui.flogger.backend.system;

import java.util.logging.Level;
import net.goui.flogger.FluentLogger;

class UnknownLoggerClass {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static void log(Level level, String message) {
    logger.at(level).log("%s", message);
  }

  static boolean isEnabled(Level level) {
    return logger.at(level).isEnabled();
  }
}
