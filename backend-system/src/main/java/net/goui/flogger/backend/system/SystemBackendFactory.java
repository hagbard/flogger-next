package net.goui.flogger.backend.system;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.Platform;
import com.google.common.flogger.backend.system.AbstractBackend;
import com.google.common.flogger.backend.system.BackendFactory;
import java.util.logging.LogManager;
import net.goui.flogger.backend.common.MessageFormatter;
import net.goui.flogger.backend.common.NamingStrategy;
import net.goui.flogger.backend.common.Options;

public class SystemBackendFactory extends BackendFactory {
  // Explicit since this is a service API and called during Platform initialization.
  public SystemBackendFactory() {
    // !! DO NOT INVOKE UNKNOWN CODE HERE !!
  }

  @Override
  public LoggerBackend create(String loggingClassName) {
    return LazyFactory.INSTANCE.create(loggingClassName);
  }

  // Only called by SystemLogRecord.
  static LogMessageFormatter getFormatter() {
    return LazyFactory.INSTANCE.formatter;
  }

  static final class LazyFactory {
    static final LazyFactory INSTANCE = new LazyFactory();

    private final NamingStrategy namingStrategy;
    private final LogMessageFormatter formatter;

    LazyFactory() {
      // Must not call any code which might risk triggering reentrant Flogger logging.
      LogManager logManager = LogManager.getLogManager();
      Options options = Options.of(logManager::getProperty).getOptions("flogger");
      this.namingStrategy = NamingStrategy.from(options.getOptions("logger_naming"));
      this.formatter = MessageFormatter.from(options.getOptions("message_formatter"));
    }

    LoggerBackend create(String loggingClassName) {
      return new Backend(namingStrategy.getLoggerName(loggingClassName));
    }
  }

  public static class Backend extends AbstractBackend {
    protected Backend(String loggerName) {
      super(loggerName);
    }

    @Override
    public void log(LogData data) {
      Metadata context = Platform.getInjectedMetadata();
      log(SystemLogRecord.create(data, context), data.wasForced());
    }

    @Override
    public void handleError(RuntimeException error, LogData badData) {
      Metadata context = Platform.getInjectedMetadata();
      log(SystemLogRecord.error(error, badData, context), badData.wasForced());
    }
  }
}
