package net.goui.flogger.backend.log4j;

import static net.goui.flogger.backend.log4j.Log4jEventUtil.getLog4jLevel;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.MessageUtils;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.backend.Platform;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class Log4jBackend extends LoggerBackend {
  private final Logger logger;

  protected Log4jBackend(String backendName) {
    this.logger = (Logger) LogManager.getLogger(backendName);
  }

  @Override
  public void log(LogData data) {
    MetadataProcessor metadata =
        MetadataProcessor.forScopeAndLogSite(Platform.getInjectedMetadata(), data.getMetadata());
    logger.get().log(new Log4jEvent(data, metadata));
  }

  @Override
  public void handleError(RuntimeException error, LogData badData) {

    logger.warn(formatLogErrorMessage(badData, error), error);
  }

  @Override
  public String getLoggerName() {
    return logger.getName();
  }

  @Override
  public boolean isLoggable(Level level) {
    return logger.isEnabled(getLog4jLevel(level));
  }

  private static String formatLogErrorMessage(LogData logData, RuntimeException error) {
    StringBuilder errorMsg =
        new StringBuilder("LOGGING ERROR: ").append(error.getMessage()).append('\n');
    int length = errorMsg.length();
    try {
      appendLogData(logData, errorMsg);
    } catch (RuntimeException e) {
      // Reset partially written buffer when an error occurs.
      errorMsg.setLength(length);
      errorMsg.append("Cannot append LogData: ").append(e);
    }
    return errorMsg.toString();
  }

  /** Appends the given {@link LogData} to the given {@link StringBuilder}. */
  private static void appendLogData(LogData data, StringBuilder out) {
    out.append("  original message: ");
    if (data.getTemplateContext() == null) {
      out.append(data.getLiteralArgument());
    } else {
      // We know that there's at least one argument to display here.
      out.append(data.getTemplateContext().getMessage());
      out.append("\n  original arguments:");
      for (Object arg : data.getArguments()) {
        out.append("\n    ").append(MessageUtils.safeToString(arg));
      }
    }
    Metadata metadata = data.getMetadata();
    if (metadata.size() > 0) {
      out.append("\n  metadata:");
      for (int n = 0; n < metadata.size(); n++) {
        out.append("\n    ");
        out.append(metadata.getKey(n).getLabel()).append(": ").append(metadata.getValue(n));
      }
    }
    out.append("\n  level: ").append(data.getLevel());
    out.append("\n  timestamp (nanos): ").append(data.getTimestampNanos());
    out.append("\n  class: ").append(data.getLogSite().getClassName());
    out.append("\n  method: ").append(data.getLogSite().getMethodName());
    out.append("\n  line number: ").append(data.getLogSite().getLineNumber());
  }
}
