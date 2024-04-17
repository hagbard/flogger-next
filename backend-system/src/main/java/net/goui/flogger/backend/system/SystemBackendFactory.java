package net.goui.flogger.backend.system;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.Platform;
import com.google.common.flogger.backend.system.AbstractBackend;
import com.google.common.flogger.backend.system.BackendFactory;
import java.util.List;
import java.util.logging.LogManager;
import net.goui.flogger.backend.common.AbstractBackendFactory;
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
    return LazyFactory.INSTANCE.getMessageFormatter();
  }

  private static final class LazyFactory extends AbstractBackendFactory<Backend> {
    static final LazyFactory INSTANCE = new LazyFactory();

    LazyFactory() {
      super(getOptions(), Backend::new);
    }

    private static Options getOptions() {
      // Must not call any code which might risk triggering reentrant Flogger logging.
      return Options.of(LogManager.getLogManager()::getProperty).getOptions("flogger");
    }

    /**
     * A heuristic to find the list of loggers configured in the log manager. Without a general way
     * to read the set of properties, we have to heuristically determine the properties based on the
     * known set of loggers and then confirm they were in the property file (rather than being
     * configured programmatically).
     *
     * <p>This is called back by the parent constructor and should be called only once, early in the
     * application's life.
     */
    @Override
    protected List<String> getSystemRoots() {
      // Must not call any code which might risk triggering reentrant Flogger logging.
      return FloggerConfig.getSystemRoots();
    }
  }

  public static class Backend extends AbstractBackend {
    Backend(String loggerName) {
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
