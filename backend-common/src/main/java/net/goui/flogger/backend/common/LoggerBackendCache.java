package net.goui.flogger.backend.common;

import static java.util.Objects.requireNonNull;

import com.google.common.flogger.backend.LoggerBackend;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Weak referenced cache for logger backends, best used when the naming strategy maps many logging
 * class names to a single backend. This class is thread safe.
 */
final class LoggerBackendCache<T extends LoggerBackend> {
  private final Function<String, T> newBackendFn;
  private final ConcurrentMap<String, WeakReference<T>> cache = new ConcurrentHashMap<>();

  LoggerBackendCache(Function<String, T> newBackendFn) {
    this.newBackendFn = requireNonNull(newBackendFn);
  }

  /** Returns a cached backend instance for the given name. */
  T getBackend(String backendName) {
    WeakReference<T> ref = cache.get(backendName);
    if (ref != null) {
      T cachedBackend = ref.get();
      if (cachedBackend != null) {
        return cachedBackend;
      }
      // Hanging, empty reference that needs replacing. Since the set of backends is likely small
      // (or at least bounded) and class unloading is rare, it's not necessary to worry about
      // proactively finding cleared references and tidying them up with a reference queue.
    }
    T newBackend = newBackendFn.apply(backendName);
    // Minor race condition means we might end up with non-unique backend classes, but this is both
    // very unlikely and harmless. We must avoid creating the new backing inside computeIfAbsent()
    // since there's still the race condition of it being cleared before returning. If we wanted
    // to avoid any race conditions here, we'd need a loop of some kind to ensure the added entry
    // wasn't cleared before being returned.
    cache.putIfAbsent(backendName, new WeakReference<>(newBackend));
    return newBackend;
  }
}
