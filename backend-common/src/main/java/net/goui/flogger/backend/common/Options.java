package net.goui.flogger.backend.common;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
public final class Options {
  public static Options of(UnaryOperator<String> lookupFn) {
    return new Options(lookupFn, "");
  }

  /**
   * Returns whether an options string value is a case-insensitive match for any of the given
   * options.
   *
   * <p>This method can be useful when handling arbitrary values with some set of known special
   * cases, but in general you should prefer using enums where possible.
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

  private Options(UnaryOperator<String> getFn, String prefix) {
    this.prefix = requireNonNull(prefix);
    this.getFn = requireNonNull(getFn);
  }

  /**
   * Returns the child options of this instance given a (possibly multipart) name.
   *
   * <p>Child options are a convenient way to scope available options when passing options into
   * other code, but they are fundamentally no different to using the fully qualified name. In
   * particular, child options will still inherit any aliases present in parent options during
   * lookup.
   *
   * <p>For example, with the properties:
   *
   * <pre>{@code
   * foo = @alias
   * foo.bar.xxx = hello
   * alias.bar.yyy = world
   * }</pre>
   *
   * we see:
   *
   * <pre>{@code
   * root.get("foo.bar.xxx", "") ==> "hello"
   * root.get("foo.bar.yyy", "") ==> "world"
   * root.getChildOptions("foo").get("bar.yyy") ==> "world"
   * root.getChildOptions("foo.bar").get("yyy") ==> "world"
   * }</pre>
   *
   * <p>Note that since options are only looked up on demand, it is not possible to detect spelling
   * errors in the name, and if an incorrect name is given then a valid, but empty, options instance
   * will be returned.
   *
   * @param name the name of the child options, without a trailing '.' (e.g. "foo" or "foo.bar").
   */
  public Options getOptions(String name) {
    return new Options(getFn, prefix + checkName(name) + ".");
  }

  public List<Options> getOptionsArray(String name) {
    return collectArrayElements(name, (fqn, i) -> new Options(getFn, fqn + "." + i + "."));
  }

  /** Returns the raw string value of the given option (including handling aliases). */
  public Optional<String> get(String name) {
    return resolve(name, (fqn, v) -> v);
  }

  /** Returns the string value of the given option or the (non-null) default. */
  public String getString(String name, String defaultValue) {
    return get(name).orElse(requireNonNull(defaultValue));
  }

  /**
   * Returns the list of string values in the array of the given option name.
   *
   * <p>See {@link Options top level JavaDoc} for details on how arrays are defined.
   */
  public List<String> getStringArray(String name) {
    return getArray(name, (fqn, s) -> s);
  }

  /**
   * Returns the long value of the given option or the default.
   *
   * <p>Long values are parsed via {@link Long#parseLong(String)}.
   */
  public long getLong(String name, long defaultValue) {
    return resolve(name, TO_LONG).orElse(defaultValue);
  }

  /**
   * Returns the list of long values in the array of the given option name.
   *
   * <p>See {@link Options top level JavaDoc} for details on how arrays are defined.
   */
  public List<Long> getLongArray(String name) {
    return getArray(name, TO_LONG);
  }

  /**
   * Returns the double value of the given option or the default.
   *
   * <p>Double values are parsed via {@link Double#parseDouble(String)}.
   */
  public double getDouble(String name, double defaultValue) {
    return resolve(name, TO_DOUBLE).orElse(defaultValue);
  }

  /**
   * Returns the list of double values in the array of the given option name.
   *
   * <p>See {@link Options top level JavaDoc} for details on how arrays are defined.
   */
  public List<Double> getDoubleArray(String name) {
    return getArray(name, TO_DOUBLE);
  }

  /**
   * Returns the boolean value of the given option or the default.
   *
   * <p>Long values are parsed case-insensitively such that {@code "true" ==> true} and {@code
   * "false" ==> false}. This method does not use {@link Boolean#parseBoolean(String)} due to its
   * often confusing behaviour.
   */
  public boolean getBoolean(String name, boolean defaultValue) {
    return resolve(name, TO_BOOLEAN).orElse(defaultValue);
  }

  /**
   * Returns the list of boolean values in the array of the given option name.
   *
   * <p>See {@link Options top level JavaDoc} for details on how arrays are defined.
   */
  public List<Boolean> getBooleanArray(String name) {
    return getArray(name, TO_BOOLEAN);
  }

  /**
   * Returns the enum value of the given option or the (non-null) default.
   *
   * <p>Enum values are matched case-insensitively such that {@code "foo" ==> MyEnum#FOO}, but there
   * is no modification of the name's format (i.e. {@code "fooBar"} does not resolve to {@code
   * MyEnum#FOO_BAR}).
   */
  public <T extends Enum<T>> T getEnum(String name, T defaultValue) {
    @SuppressWarnings("unchecked")
    Class<T> enumClass = (Class<T>) defaultValue.getClass();
    return resolve(name, toEnum(enumClass)).orElse(defaultValue);
  }

  /**
   * Returns the list of enum values in the array of the given option name for the specified enum
   * type.
   *
   * <p>See {@link Options top level JavaDoc} for details on how arrays are defined.
   */
  public <T extends Enum<T>> List<T> getEnumArray(String name, Class<T> enumClass) {
    return getArray(name, toEnum(enumClass));
  }

  public <T> Optional<T> getValue(String name, Function<String, T> fn) {
    return resolve(name, wrap("value", fn));
  }

  /**
   * Returns the list of transformed values in the array of the given option name for the specified
   * function.
   *
   * <p>See {@link Options top level JavaDoc} for details on how arrays are defined.
   */
  public <T> List<T> getValueArray(String name, Function<String, T> fn) {
    return getArray(name, wrap("value", fn));
  }

  // ------------------------------------------------------------------------------------

  // '.' is not banned here, since this relates to names like "foo.bar".
  private static final String BANNED_CHARS = "@$,/|\\{}()[]";

  /** */
  public static String checkName(String name) {
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Option names must not be empty");
    }
    if (name.startsWith(".") || name.endsWith(".")) {
      throw new IllegalArgumentException("Option names must not start or end with '.'");
    }
    if (name.contains("..")) {
      throw new IllegalArgumentException("Option names must not contain empty segments");
    }
    if (name.chars().anyMatch(c -> BANNED_CHARS.indexOf(c) != -1)) {
      String badChars =
          BANNED_CHARS
              .codePoints()
              .mapToObj(Character::toString)
              .collect(Collectors.joining("', '", "'", "'"));
      throw new IllegalArgumentException("Option names must not contain any of: " + badChars);
    }
    return name;
  }

  // Returns the fully qualified name of an options value.
  private String fqn(String name) {
    return prefix.isEmpty() ? name : prefix + name;
  }

  // Returns a (possibly null) raw value given a fully qualified name.
  private String getRaw(String fqn) {
    return getFn.apply(fqn);
  }

  // Resolves the location of the first non-null value (including aliasing) for the given name, and
  // invokes the given function passing the fully qualified name and value. This function detects
  // infinite recursion in due to aliases.
  private <T> Optional<T> resolve(String name, BiFunction<String, String, T> op) {
    Set<String> seen = new LinkedHashSet<>();
    return Optional.ofNullable(resolveRecursively(fqn(checkName(name)), op, seen));
  }

  private <T> T resolveRecursively(
      String fqn, BiFunction<String, String, T> op, Set<String> aliases) {
    checkNoRecursion(fqn, aliases);
    String v = getRaw(fqn);
    if (v != null) {
      if (!v.startsWith("@")) {
        // Normal value with no leading '@' (e.g. "foo").
        return op.apply(fqn, v);
      }
      // Remove leading '@': Either get alias, or real value starting with '@'.
      String valueOrAlias = v.substring(1);
      if (valueOrAlias.startsWith("@")) {
        // Value with escaped leading '@' (e.g. "@@foo" -> "@foo").
        return op.apply(fqn, valueOrAlias);
      }
      // Resolve an aliased value directly (only one alias is currently allowed here).
      return resolveRecursively(checkName(valueOrAlias), op, aliases);
    }
    // Handle aliases at each level of the fully qualified name, starting with the deepest.
    int lastDot = fqn.lastIndexOf('.');
    while (lastDot > 0) {
      String aliasStr = getRaw(fqn.substring(0, lastDot));
      if (aliasStr != null) {
        // Handle an alias list of the form "@foo.bar, @baz"
        String childSuffix = fqn.substring(lastDot);
        for (String alias : aliasStr.split(",")) {
          // Handle a single alias of the form "@foo.bar".
          alias = alias.trim();
          if (!alias.startsWith("@")) {
            throw new OptionParseException(
                "Option aliases must start with '@' (found alias '%s' for group '%s')", alias, fqn);
          }
          // Resolve an aliased group recursively (adding any trailing ".bar" suffix related to the
          // current position in the current fully qualified name).
          T result = resolveRecursively(alias.substring(1) + childSuffix, op, aliases);
          if (result != null) {
            return result;
          }
        }
      }
      lastDot = fqn.lastIndexOf('.', lastDot - 1);
    }
    return null;
  }

  private static void checkNoRecursion(String fqn, Set<String> aliases) {
    if (!aliases.add(fqn)) {
      throw new OptionParseException(
          "Recursive aliases in options hierarchy:\n%s\n--> %s",
          String.join("\n--> ", aliases), fqn);
    }
  }

  private <T> List<T> getArray(String name, BiFunction<String, String, T> fn) {
    return collectArrayElements(name, (fqn, i) -> getElement(fqn, i, fn));
  }

  private <T> T getElement(String fqn, int i, BiFunction<String, String, T> fn) {
    String elementName = fqn + "." + i;
    String v = getRaw(elementName);
    if (v != null) {
      return fn.apply(elementName, v);
    }
    throw new OptionParseException("No such array element: %s[%d]", fqn, i);
  }

  private <T> List<T> collectArrayElements(String name, BiFunction<String, Integer, T> elementFn) {
    return resolve(
            checkName(name) + ".size",
            (fqn, s) -> {
              int size = Integer.parseUnsignedInt(s);
              List<T> list = new ArrayList<>(size);
              fqn = fqn.substring(0, fqn.lastIndexOf('.'));
              for (int i = 0; i < size; i++) {
                list.add(elementFn.apply(fqn, i));
              }
              return list;
            })
        .orElse(List.of());
  }

  private static <T> BiFunction<String, String, T> wrap(String type, Function<String, T> fn) {
    requireNonNull(fn);
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
