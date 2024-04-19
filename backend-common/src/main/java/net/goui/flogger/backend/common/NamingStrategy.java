package net.goui.flogger.backend.common;

import static net.goui.flogger.backend.common.PluginLoader.DEFAULT_PLUGIN_NAME;

import java.util.Map;

/** A Flogger backend plugin for controlling how logging class names are mapped to backend names. */
public interface NamingStrategy {
  /**
   * Returns a {@link NamingStrategy} instance based on the given options.
   *
   * <p>To override default behaviour, set the Flogger option {@code backend_naming.impl}.
   *
   * <h3>Built in plugins</h3>
   *
   * <ul>
   *   <li><em>default</em>: See {@link DefaultNamingStrategy} for further options.
   * </ul>
   */
  static NamingStrategy from(Options options) {
    return PluginLoader.instantiate(
        NamingStrategy.class, options, Map.of(DEFAULT_PLUGIN_NAME, DefaultNamingStrategy::new));
  }

  /**
   * Returns the backend name for the given logging class.
   *
   * <p>Backend names are arbitrary and may, or may not, be derived from the given class name. The
   * set of backend names defines configuration space for fluent loggers, and the choice of naming
   * strategy is often a trade-off between the number of allocated backends and the degree of
   * configuration required.
   *
   * <p>Some common naming strategies are:
   *
   * <ul>
   *   <li><em>Use class names</em>:<br>
   *       This is the default behaviour and offers maximal configuration flexibility at the expense
   *       of allocating many logger backends in large applications.
   *   <li><em>Use package names</em>:<br>
   *       All logging classes in the same package will share the same logging backend. This
   *       prevents configuring loggers on per-class basis using the underlying logging system, but
   *       is likely to dramatically reduce the number of allocated backends.
   *   <li><em>Semantic naming</em>:<br>
   *       Maps logging classes to some arbitrary, unrelated, naming hierarchy often based on the
   *       perceived roll a class has. This allows direct control of the configuration name space
   *       and the exact number of allocated backends, but comes at the expense of additional
   *       maintenance.
   * </ul>
   *
   * <p>The default plugin for backend naming is flexible and offers all the above features, so it
   * is not expected that additional plugins will need to be written.
   */
  String getBackendName(String loggingClassName);

  /**
   * Returns whether a naming strategy is likely to work better if logger backends are cached.
   *
   * <p>For the default "use class names" behaviour, caching backends is likely detrimental, since
   * backends are not shared between loggers, but for other behaviours which map many logging
   * classes to a few backends, caching will reduce repeated allocations for the same backend name.
   */
  boolean shouldCacheBackends();
}
