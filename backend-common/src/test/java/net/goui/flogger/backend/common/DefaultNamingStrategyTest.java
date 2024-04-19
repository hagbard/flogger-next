package net.goui.flogger.backend.common;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DefaultNamingStrategyTest {
  @Test
  public void getBackendName_simple() {
    Options opts =
        options(
            kvp("roots.size", "5"),
            kvp("roots.0", "com.first.second.third.forth"),
            kvp("roots.1", "com.first"),
            kvp("roots.2", "com.alpha.beta"),
            kvp("roots.3", "com.alphabet"),
            kvp("roots.4", "com"));

    DefaultNamingStrategy strategy = new DefaultNamingStrategy(opts);

    // Shortening package names until it flips to the next available parent.
    assertThat(strategy.getBackendName("com.first.second.third.forth.fifth.Class"))
        .isEqualTo("com.first.second.third.forth");
    assertThat(strategy.getBackendName("com.first.second.third.forth.Class"))
        .isEqualTo("com.first.second.third.forth");
    assertThat(strategy.getBackendName("com.first.second.third.Class")).isEqualTo("com.first");

    // Make sure we don't match package segments unless equal.
    assertThat(strategy.getBackendName("com.first.second.thrd.forth.Class")).isEqualTo("com.first");
    assertThat(strategy.getBackendName("com.first.second.third.forthy.Class"))
        .isEqualTo("com.first");

    // Handle "overlapping" package segments as expected.
    assertThat(strategy.getBackendName("com.alpha.beta.Class")).isEqualTo("com.alpha.beta");
    assertThat(strategy.getBackendName("com.alphabet.Class")).isEqualTo("com.alphabet");
    assertThat(strategy.getBackendName("com.alphabeta.Class")).isEqualTo("com");

    // When no root exists, just return the given class name.
    assertThat(strategy.getBackendName("org.first.second.Class"))
        .isEqualTo("org.first.second.Class");
  }

  @Test
  public void getBackendName_trimming() {
    Options opts = options(kvp("trim_at_least", "2"), kvp("retain_at_most", "4"));

    DefaultNamingStrategy strategy = new DefaultNamingStrategy(opts);

    // Trimming takes precedence.
    assertThat(strategy.getBackendName("com.first.second.third.Class"))
        .isEqualTo("com.first.second");
    assertThat(strategy.getBackendName("com.first.Class")).isEqualTo("com");
    assertThat(strategy.getBackendName("com.Class")).isEqualTo("");
    assertThat(strategy.getBackendName("Class")).isEqualTo("");

    // Then the retention rule is applied (which may trim further).
    assertThat(strategy.getBackendName("com.first.second.third.forth.fifth.Class"))
        .isEqualTo("com.first.second.third");
  }

  @Test
  public void getBackendName_systemRoots() {
    Options opts =
        options(
            kvp("roots.size", "2"),
            kvp("roots.0", "com.foo"),
            kvp("roots.1", "com.bar"),
            // Added synthetically depending on the specific logger implementation used.
            kvp("system_roots.size", "2"),
            kvp("system_roots.0", "net.system.foo"),
            kvp("system_roots.1", "net.system.bar"));

    DefaultNamingStrategy strategy = new DefaultNamingStrategy(opts);

    // Both sets of roots are merged.
    assertThat(strategy.getBackendName("com.foo.bar.Class")).isEqualTo("com.foo");
    assertThat(strategy.getBackendName("net.system.foo.bar.Class")).isEqualTo("net.system.foo");
  }

  @Test
  public void getBackendName_withWildcards() {
    Options opts =
        options(
            kvp("roots.size", "4"),
            // This acts the same as "retain_at_most = 1"
            kvp("roots.0", "*"),
            kvp("roots.1", "com.*.*"),
            kvp("roots.2", "com.foo.bar.baz.*"),
            kvp("roots.3", "com.foo.bar.Class"));

    DefaultNamingStrategy strategy = new DefaultNamingStrategy(opts);

    // No package name does nothing (yields single segment name).
    assertThat(strategy.getBackendName("Class")).isEqualTo("Class");
    // Longer unmatched names also yield single segment result (due to "*")
    assertThat(strategy.getBackendName("org.Class")).isEqualTo("org");
    assertThat(strategy.getBackendName("org.other.Class")).isEqualTo("org");

    // Things under "com" (but not matched more specifically) are kept to 3 segments.
    assertThat(strategy.getBackendName("com.other.Class")).isEqualTo("com.other.Class");
    assertThat(strategy.getBackendName("com.other.path.Class")).isEqualTo("com.other.path");
    assertThat(strategy.getBackendName("com.foo.bar.Class")).isEqualTo("com.foo.bar.Class");

    // Using wildcards can lead to a mix of class and package names being used.
    assertThat(strategy.getBackendName("com.foo.bar.baz.other.Class"))
        .isEqualTo("com.foo.bar.baz.other");
    assertThat(strategy.getBackendName("com.foo.bar.baz.Class")).isEqualTo("com.foo.bar.baz.Class");
  }

  @Test
  public void getBackendName_miscellaneous() {
    Options opts =
        options(
            kvp("roots.size", "4"),
            kvp("roots.0", "net.Capitalized"),
            kvp("roots.1", "net._underscore"),
            kvp("roots.2", "net.$technically$.$valid$"),
            kvp("roots.3", "net.utf16_\uD852\uDF62_chars")); // 𤭢

    DefaultNamingStrategy strategy = new DefaultNamingStrategy(opts);

    // Miscellaneous odd (but legal) cases. Each test has a matched and non-matched version.
    assertThat(strategy.getBackendName("net.Capitalized.unmatched.Class"))
        .isEqualTo("net.Capitalized");
    assertThat(strategy.getBackendName("net.CapiTaliZed.unmatched.Class"))
        .isEqualTo("net.CapiTaliZed.unmatched.Class");

    assertThat(strategy.getBackendName("net._underscore.unmatched.Class"))
        .isEqualTo("net._underscore");
    assertThat(strategy.getBackendName("net.__underscore.unmatched.Class"))
        .isEqualTo("net.__underscore.unmatched.Class");

    assertThat(strategy.getBackendName("net.$technically$.$valid$.unmatched.Class"))
        .isEqualTo("net.$technically$.$valid$");
    assertThat(strategy.getBackendName("net.$technically$valid$.unmatched.Class"))
        .isEqualTo("net.$technically$valid$.unmatched.Class");

    assertThat(strategy.getBackendName("net.utf16_\uD852\uDF62_chars.unmatched.Class")) // 𤭢
        .isEqualTo("net.utf16_\uD852\uDF62_chars");
    assertThat(strategy.getBackendName("net.utf16_\uD852\uDF63_chars.unmatched.Class")) // 𤭣
        .isEqualTo("net.utf16_\uD852\uDF63_chars.unmatched.Class");
  }

  @Test
  public void getBackendName_edgeCases() {
    Options opts =
        options(
            kvp("roots.size", "4"),
            kvp("roots.0", "com.first.second"),
            kvp("roots.1", "com.first"),
            // Triggers a unique code path.
            kvp("roots.2", "com.first.secondly"),
            kvp("roots.3", "com"));

    DefaultNamingStrategy strategy = new DefaultNamingStrategy(opts);

    assertThat(strategy.getBackendName("")).isEqualTo("");

    // Insertion point at index 0 (before "com").
    assertThat(strategy.getBackendName("aaa.bbb.Class")).isEqualTo("aaa.bbb.Class");

    // Differing in the first segment hits a different code path.
    assertThat(strategy.getBackendName("co.something.else.Class"))
        .isEqualTo("co.something.else.Class");
    assertThat(strategy.getBackendName("comm.something.else.Class"))
        .isEqualTo("comm.something.else.Class");

    // Trim inner/nested classes from class names.
    assertThat(strategy.getBackendName("aaa.bbb.Class$Inner")).isEqualTo("aaa.bbb.Class");
    // but allow '$' as first character in class name to avoid empty final segment.
    assertThat(strategy.getBackendName("aaa.bbb.$Class")).isEqualTo("aaa.bbb.$Class");
  }

  @Test
  public void getBackendName_badOptions() {
    IllegalArgumentException badRetain =
        assertThrows(
            IllegalArgumentException.class,
            () -> new DefaultNamingStrategy(options(kvp("retain_at_most", "-1"))));
    assertThat(badRetain).hasMessageThat().contains("retain_at_most");
    assertThat(badRetain).hasMessageThat().contains("cannot be negative");
    assertThat(badRetain).hasMessageThat().contains("-1");

    IllegalArgumentException badTrim =
        assertThrows(
            IllegalArgumentException.class,
            () -> new DefaultNamingStrategy(options(kvp("trim_at_least", "-23"))));
    assertThat(badTrim).hasMessageThat().contains("trim_at_least");
    assertThat(badTrim).hasMessageThat().contains("cannot be negative");
    assertThat(badTrim).hasMessageThat().contains("-23");
  }

  @Test
  public void getBackendName_emptyRoot() {
    IllegalArgumentException emptyRoot =
        assertThrows(
            IllegalArgumentException.class,
            () -> new DefaultNamingStrategy(options(kvp("roots.size", "1"), kvp("roots.0", ""))));
    assertThat(emptyRoot).hasMessageThat().contains("logger root cannot be empty");
  }

  @Test
  public void getBackendName_duplicateRoot() {
    IllegalArgumentException duplicateRoot =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new DefaultNamingStrategy(
                    options(
                        kvp("roots.size", "2"),
                        kvp("roots.0", "foo.bar.*"),
                        kvp("roots.1", "foo.bar.*.*"))));
    assertThat(duplicateRoot).hasMessageThat().contains("multiple logger roots");
    assertThat(duplicateRoot).hasMessageThat().contains("foo.bar");
  }

  @Test
  public void getBackendName_badPackage() {
    for (String pkg : List.of(".", "..", ".name", "name.", "foo bar", "foo.!bar")) {
      IllegalArgumentException badPackage =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new DefaultNamingStrategy(options(kvp("roots.size", "1"), kvp("roots.0", pkg))));
      assertThat(badPackage).hasMessageThat().contains("invalid root name");
      assertThat(badPackage).hasMessageThat().contains("'" + pkg + "'");
    }
  }

  @Test
  public void shouldCacheBackends() {
    assertThat(new DefaultNamingStrategy(options()).shouldCacheBackends()).isFalse();
    // Enable caching when any trimming is enabled, since it allows sharing many loggers.
    assertThat(new DefaultNamingStrategy(options(kvp("retain_at_most", "1"))).shouldCacheBackends())
        .isTrue();
    assertThat(new DefaultNamingStrategy(options(kvp("trim_at_least", "1"))).shouldCacheBackends())
        .isTrue();
    // Don't enable caching just because root entries exist as they probably won't cover most
    // loggers.
    Options withRoots = options(kvp("roots.size", "1"), kvp("roots.0", "com"));
    assertThat(new DefaultNamingStrategy(withRoots).shouldCacheBackends()).isFalse();
  }

  private static Map.Entry<String, String> kvp(String key, String value) {
    return Maps.immutableEntry(key, value);
  }

  @SafeVarargs
  private static Options options(Map.Entry<String, String>... e) {
    ImmutableMap<String, String> map =
        Arrays.stream(e).collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    return Options.of(map::get);
  }
}
