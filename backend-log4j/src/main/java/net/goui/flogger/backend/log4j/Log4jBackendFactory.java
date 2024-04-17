package net.goui.flogger.backend.log4j;

import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.system.BackendFactory;
import java.util.List;
import net.goui.flogger.backend.common.AbstractBackendFactory;
import net.goui.flogger.backend.common.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.lookup.StrLookup;

public class Log4jBackendFactory extends BackendFactory {
  // Explicit since this is a service API and called during Platform initialization.
  public Log4jBackendFactory() {
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

  static final class LazyFactory extends AbstractBackendFactory<Log4jBackend> {
    static final LazyFactory INSTANCE = new LazyFactory();

    LazyFactory() {
      super(getOptions(), Log4jBackend::new);
    }

    private static Options getOptions() {
      // Must not call any code which might risk triggering reentrant Flogger logging.
      StrLookup properties =
          ((LoggerContext) LogManager.getContext())
              .getConfiguration()
              .getStrSubstitutor()
              .getVariableResolver();
      return Options.of(properties::lookup).getOptions("flogger");
    }

    @Override
    protected List<String> getSystemRoots() {
      return List.copyOf(
          ((LoggerContext) LogManager.getContext()).getConfiguration().getLoggers().keySet());
    }
  }
}
