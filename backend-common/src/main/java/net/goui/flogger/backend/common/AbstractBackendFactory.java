package net.goui.flogger.backend.common;

import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.SimpleMessageFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.goui.flogger.backend.common.formatter.PatternFormatter;

public abstract class AbstractBackendFactory<T extends LoggerBackend> {
  private static final String PLUGIN_BACKEND_NAMING = "backend_naming";
  private static final String OPTION_NAMING_USE_SYSTEM_ROOTS = "use_system_roots";
  private static final String OPTION_USE_BACKEND_CACHE = "use_backend_cache";
  private static final String OPTION_MESSAGE_FORMATTER = "message_formatter";

  private final NamingStrategy namingStrategy;
  private final Function<String, T> backendFn;
  private final LogMessageFormatter backendFormatter;

  protected AbstractBackendFactory(Options options, Function<String, T> backendFn) {
    // Must not call any code which might risk triggering reentrant Flogger logging.
    this.namingStrategy = NamingStrategy.from(getNamingOptions(options));
    boolean shouldCacheBackends =
        options.getBoolean(OPTION_USE_BACKEND_CACHE, namingStrategy.shouldCacheBackends());
    this.backendFn =
        shouldCacheBackends ? new LoggerBackendCache<>(backendFn)::getBackend : backendFn;
    this.backendFormatter =
        FloggerPlugin.instantiate(
            LogMessageFormatter.class,
            options.getOptions(OPTION_MESSAGE_FORMATTER),
            Map.of(
                "default",
                AbstractBackendFactory::getDefaultFormatter,
                "pattern",
                PatternFormatter::new));
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

  public final T create(String loggingClassName) {
    return backendFn.apply(namingStrategy.getBackendName(loggingClassName));
  }

  public final LogMessageFormatter getBackendFormatter() {
    return backendFormatter;
  }
}
