package net.goui.flogger.backend.system;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.Platform;
import com.google.common.flogger.backend.system.AbstractBackend;
import com.google.common.flogger.backend.system.BackendFactory;
import java.util.logging.LogManager;
import net.goui.flogger.backend.common.AbstractBackendFactory;
import net.goui.flogger.backend.common.Options;

/**
 * Service API providing Flogger Next integration with Log4j2.
 *
 * <p>This class is configured as a service API for {@link BackendFactory} via {@code
 * META-INF/services} and will be loaded automatically if it appears in the class path of an
 * application.
 *
 * <p>To force Flogger to use this class (e.g. if multiple service APIs for {@link BackendFactory}
 * exist), set the system property {@code flogger.backend_factory} to the fully qualified name of
 * this class.
 */
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
      super(loadOptions(), FloggerConfig.getSystemRoots());
    }

    @Override
    protected Backend newBackend(
        String backendName, LogMessageFormatter formatter, Options options) {
      return new Backend(backendName);
    }
    
    private static Options loadOptions() {
      // Must not call any code which might risk triggering reentrant Flogger logging.
      return Options.of(LogManager.getLogManager()::getProperty).getOptions("flogger");
    }
  }

  private static final class Backend extends AbstractBackend {
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
