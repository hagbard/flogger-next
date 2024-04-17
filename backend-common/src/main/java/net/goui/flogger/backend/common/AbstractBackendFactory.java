package net.goui.flogger.backend.common;

import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.SimpleMessageFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.goui.flogger.backend.common.formatter.DefaultPatternFormatter;

/**
 * Helper class for implementing Flogger {@link
 * com.google.common.flogger.backend.system.BackendFactory backend factories} in the Flogger Next
 * project. This class enables system specific logger backends to utilize common Flogger plugins and
 * custom configuration without repeating a lot of code. System specific logger backends need only
 * define how to generate backend instances and not worry about logger name mappings or formatting
 * etc.
 *
 * <h3>Options</h3>
 *
 * <ul>
 *   <li>flogger.message_formatter.impl: Plugin<br>
 *       The default implementation is {@link DefaultPatternFormatter}.
 *   <li>flogger.backend_naming.impl: Plugin<br>
 *       The default implementation is {@link DefaultNamingStrategy}.
 *   <li>flogger.backend_naming.use_backend_cache: Boolean<br>
 *       If set, backend instances will be cached by name for sharing between Fluent loggers.
 * </ul>
 *
 * <h3>Implementation Details</h3>
 *
 * <p>Note that this class DOES NOT extend/implement {@link
 * com.google.common.flogger.backend.system.BackendFactory BackendFactory} because of the need to
 * delay configuration of this instance until first use, due to options parsing. As such, each
 * system specific Flogger backend implementation should use the following pattern:
 *
 * <pre>{@code
 * public class MyBackendFactory extends BackendFactory {
 *   // For use as a Java service API.
 *   public SystemBackendFactory() {}
 *
 *   @Override
 *   public LoggerBackend create(String loggingClassName) {
 *     return LazyFactory.INSTANCE.create(loggingClassName);
 *   }
 *
 *   // Delays backend initialization to avoid reading options "too early".
 *   private static final class LazyFactory extends AbstractBackendFactory<MyBackend> {
 *     static final LazyFactory INSTANCE = new LazyFactory();
 *
 *     LazyFactory() { super(getOptions(), MyBackend::new); }
 *
 *     private static Options getOptions() {
 *       // Load system specific options to pass to parent constructor.
 *     }
 *
 *     @Override protected List<String> getSystemRoots() {
 *       // Load any system specific backend root logger names.
 *     }
 *   }
 *
 *   public static class MyBackend extends AbstractBackend {
 *     Backend(String loggerName) { super(loggerName); }
 *
 *     ...
 *   }
 * }
 * }</pre>
 *
 * @param <T> the specific backend implementation returned by this factory.
 */
public abstract class AbstractBackendFactory<T extends LoggerBackend> {
  private static final String PLUGIN_MESSAGE_FORMATTER = "message_formatter";
  private static final String PLUGIN_BACKEND_NAMING = "backend_naming";
  private static final String OPTION_USE_BACKEND_CACHE = "use_backend_cache";
  private static final String OPTION_NAMING_USE_SYSTEM_ROOTS = "use_system_roots";

  private final NamingStrategy namingStrategy;
  private final Function<String, T> backendFn;
  private final LogMessageFormatter backendFormatter;

  /**
   * Initializes this factory with the given options and backend generating function. This class
   * then handles generating a custom message formatter and doing any backend name mapping specified
   * by the options.
   *
   * @param options Flogger options loaded in a system specific way.
   * @param backendFn creates a new backend instance for a given logger backend name.
   */
  protected AbstractBackendFactory(Options options, Function<String, T> backendFn) {
    // Must not call any code which might risk triggering reentrant Flogger logging.
    this.namingStrategy = NamingStrategy.from(getNamingOptions(options));
    boolean shouldCacheBackends =
        options.getBoolean(OPTION_USE_BACKEND_CACHE, namingStrategy.shouldCacheBackends());
    this.backendFn =
        shouldCacheBackends ? new LoggerBackendCache<>(backendFn)::getBackend : backendFn;
    this.backendFormatter =
        PluginLoader.instantiate(
            LogMessageFormatter.class,
            options.getOptions(PLUGIN_MESSAGE_FORMATTER),
            Map.of(
                "default",
                DefaultPatternFormatter::new,
                "pattern",
                DefaultPatternFormatter::new,
                "simple",
                AbstractBackendFactory::getDefaultFormatter));
  }

  /**
   * Returns the names of any system loggers statically configured for the underlying logging
   * system. This is used if the option "use_system_roots" was set to "import" existing, platform
   * specific logger names and avoid unwanted duplication in the configuration file.
   */
  protected abstract List<String> getSystemRoots();

  /**
   * Returns options suitable for configuring a naming strategy, possibly including additional
   * entries for system roots if the option "use_system_roots" was set.
   */
  private Options getNamingOptions(Options options) {
    Options namingOptions = options.getOptions(PLUGIN_BACKEND_NAMING);
    if (namingOptions.getBoolean(OPTION_NAMING_USE_SYSTEM_ROOTS, false)) {
      List<String> systemRoots = getSystemRoots();
      if (!systemRoots.isEmpty()) {
        Map<String, String> optionsMap = new HashMap<>();
        optionsMap.put("system_roots.size", Integer.toString(systemRoots.size()));
        for (int i = 0; i < systemRoots.size(); i++) {
          optionsMap.put("system_roots." + i, systemRoots.get(i));
        }
        return Options.of(name -> namingOptions.get(name).orElseGet(() -> optionsMap.get(name)));
      }
    }
    return namingOptions;
  }

  private static LogMessageFormatter getDefaultFormatter(Options options) {
    List<MetadataKey<?>> keysToIgnore =
        options.getValueArray("metadata.ignore", MetadataKeyLoader::loadMetadataKey);
    if (keysToIgnore.isEmpty()) {
      return SimpleMessageFormatter.getDefaultFormatter();
    } else {
      MetadataKey<?>[] keys = keysToIgnore.toArray(MetadataKey<?>[]::new);
      return SimpleMessageFormatter.getSimpleFormatterIgnoring(keys);
    }
  }

  /**
   * Returns a backend instance whose name is derived from the given logging class name via the name
   * mapping rules. Depending on the options used, this may be a cached value for sharing between
   * Fluent loggers.
   */
  public final T create(String loggingClassName) {
    return backendFn.apply(namingStrategy.getBackendName(loggingClassName));
  }

  /** Returns the configured message formatter for use by system specific backends. */
  public final LogMessageFormatter getMessageFormatter() {
    return backendFormatter;
  }
}
