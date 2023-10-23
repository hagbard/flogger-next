package net.goui.flogger.backend.common;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Common options API for a Flogger backend module.
 *
 * <p>Different logging frameworks (e.g. JDL logging or Log4J) may handle options differently, but
 * this API will always present a simple, hierarchical, "properties-like" options interface for use
 * when configuring a Flogger backend module.
 *
 * <p>This class should be used only during module factory initialization and should not need to be
 * performant.
 */
public class Options {
  public static Options of(UnaryOperator<String> lookupFn) {
    return new Options(lookupFn, "");
  }

  /**
   * Returns whether an options string value is considered a match for any of the given possible
   * options. A value is a match for an option if
   */
  public static boolean isAnyOf(String value, String... options) {
    for (String option : options) {
      if (option.equalsIgnoreCase(value)) {
        return true;
      }
    }
    return false;
  }

  private final UnaryOperator<String> getFn;
  // By construction this is either empty or ends with '.' (e.g. "foo.bar.").
  private final String prefix;

  Options(UnaryOperator<String> getFn, String prefix) {
    this.prefix = prefix;
    this.getFn = getFn;
  }

  /**
   * Returns the child options of this instance given a (possibly multipart) name.
   *
   * <p>Note that since options are only looked up on demand, it is not possible to detect spelling
   * errors in the name, and if an incorrect name is given then a valid, but empty, options instance
   * will be returned.
   *
   * @param name the name of the child options, without a trailing '.' (e.g. "foo" or "foo.bar").
   */
  public Options getChildOptions(String name) {
    return new Options(getFn, prefix + checkName(name) + ".");
  }

  public Optional<String> get(String name) {
    return resolve(name, (fqn, v) -> v);
  }

  public String getString(String name, String defaultValue) {
    return get(name).orElse(defaultValue);
  }

  public List<String> getStringArray(String name) {
    return getValueArray(name, s -> s);
  }

  public long getLong(String name, long defaultValue) {
    return resolve(name, TO_LONG).orElse(defaultValue);
  }

  public List<Long> getLongArray(String name) {
    return getArray(name, TO_LONG);
  }

  public double getDouble(String name, double defaultValue) {
    return resolve(name, TO_DOUBLE).orElse(defaultValue);
  }

  public List<Double> getDoubleArray(String name) {
    return getArray(name, TO_DOUBLE);
  }

  public boolean getBoolean(String name, boolean defaultValue) {
    return resolve(name, TO_BOOLEAN).orElse(defaultValue);
  }

  public List<Boolean> getBooleanArray(String name) {
    return getArray(name, TO_BOOLEAN);
  }

  public <T extends Enum<T>> T getEnum(String name, T defaultValue) {
    @SuppressWarnings("unchecked")
    Class<T> enumClass = (Class<T>) defaultValue.getClass();
    return resolve(name, toEnum(enumClass)).orElse(defaultValue);
  }

  public <T extends Enum<T>> List<T> getEnumArray(String name, Class<T> enumClass) {
    return getArray(name, toEnum(enumClass));
  }

  public <T> Optional<T> getValue(String name, Function<String, T> fn) {
    return resolve(name, wrap("value", fn));
  }

  public <T> List<T> getValueArray(String name, Function<String, T> fn) {
    return getArray(name, wrap("value", fn));
  }

  // ------------------------------------------------------------------------------------

  private static final String BANNED_CHARS = ",$";

  private static String checkName(String name) {
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Option names must not be empty");
    }
    if (name.startsWith(".") || name.endsWith(".")) {
      throw new IllegalArgumentException("Option names must not start or end with '.'");
    }
    if (name.chars().anyMatch(c -> BANNED_CHARS.indexOf(c) != -1)) {
      throw new IllegalArgumentException("Option names must not contain any of: " + BANNED_CHARS);
    }
    return name;
  }

  private String fqn(String name) {
    return prefix.isEmpty() ? name : prefix + name;
  }

  private String getRaw(String fqn) {
    return getFn.apply(fqn);
  }

  private <T> Optional<T> resolve(String name, BiFunction<String, String, T> op) {
    Set<String> seen = new LinkedHashSet<>();
    return Optional.ofNullable(resolveRecursively(fqn(checkName(name)), op, seen));
  }

  private <T> T resolveRecursively(
      String fqn, BiFunction<String, String, T> op, Set<String> aliases) {
    String v = getRaw(fqn);
    if (v != null) {
      return op.apply(fqn, v);
    }
    int lastDot = fqn.lastIndexOf('.');
    while (lastDot > 0) {
      String aliasStr = getRaw(fqn.substring(0, lastDot));
      if (aliasStr != null) {
        String childSuffix = fqn.substring(lastDot);
        for (String alias : aliasStr.split(",")) {
          alias = alias.trim();
          if (!alias.startsWith("$")) {
            throw new OptionParseException(
                "Option aliases must start with '$' (found alias '%s' for group '%s')", alias, fqn);
          }
          String aliasFqn = alias.substring(1) + childSuffix;
          if (!aliases.add(aliasFqn)) {
            throw new OptionParseException(
                "Recursive aliases in options hierarchy for: %s\n%s",
                fqn, String.join("\n<- ", aliases));
          }
          T result = resolveRecursively(aliasFqn, op, aliases);
          if (result != null) {
            return result;
          }
        }
      }
      lastDot = fqn.lastIndexOf('.', lastDot - 1);
    }
    return null;
  }

  private <T> List<T> getArray(String name, BiFunction<String, String, T> fn) {
    return resolve(checkName(name) + ".size", (fqn, size) -> getArrayImpl(fqn, size, fn))
        .orElse(List.of());
  }

  private <T> List<T> getArrayImpl(
      String sizeFqn, String sizeValue, BiFunction<String, String, T> fn) {
    int size = Integer.parseUnsignedInt(sizeValue);
    List<T> list = new ArrayList<>(size);
    String fqn = sizeFqn.substring(0, sizeFqn.lastIndexOf('.'));
    for (int i = 0; i < size; i++) {
      String indexFqn = fqn + "." + i;
      String s = getRaw(indexFqn);
      if (s != null) {
        list.add(fn.apply(indexFqn, s));
      } else {
        throw new OptionParseException("Mismatched array size (not enough elements): %s", fqn);
      }
    }
    if (getRaw(fqn + "." + size) != null) {
      throw new OptionParseException("Mismatched array size (too many elements): %s", fqn);
    }
    return list;
  }

  private static <T> BiFunction<String, String, T> wrap(String type, Function<String, T> fn) {
    return (fqn, v) -> {
      try {
        return fn.apply(v);
      } catch (RuntimeException e) {
        throw new OptionParseException(fqn, type, v, e);
      }
    };
  }

  private static <T extends Enum<T>> BiFunction<String, String, T> toEnum(Class<T> enumClass) {
    T[] values = enumClass.getEnumConstants();
    return wrap(
        enumClass.getSimpleName(),
        v -> {
          for (T t : values) {
            if (t.name().equalsIgnoreCase(v)) {
              return t;
            }
          }
          throw new OptionParseException(
              "no matching enum value for '%s' from: %s", v, Arrays.asList(values));
        });
  }

  public static final BiFunction<String, String, Long> TO_LONG = wrap("long", Long::parseLong);
  public static final BiFunction<String, String, Double> TO_DOUBLE =
      wrap("double", Double::parseDouble);
  private static final BiFunction<String, String, Boolean> TO_BOOLEAN =
      wrap(
          "boolean",
          v -> {
            if ("true".equalsIgnoreCase(v)) return TRUE;
            if ("false".equalsIgnoreCase(v)) return FALSE;
            throw new IllegalArgumentException("invalid boolean: " + v);
          });
}
