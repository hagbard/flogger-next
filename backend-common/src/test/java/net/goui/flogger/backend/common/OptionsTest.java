/*******************************************************************************
 * Copyright (c) 2024, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

package net.goui.flogger.backend.common;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OptionsTest {

  @Test
  public void isAnyOf() {
    assertThat(Options.isAnyOf("foo", "foo")).isTrue();
    assertThat(Options.isAnyOf("foo", "FOO")).isTrue();
    assertThat(Options.isAnyOf("foo", "bar", "foo")).isTrue();

    assertThat(Options.isAnyOf("foo", "bar")).isFalse();
    assertThat(Options.isAnyOf("foo", "fooo")).isFalse();
  }

  @Test
  public void badNames() {
    Options opts = options();
    assertThat(assertThrows(IllegalArgumentException.class, () -> opts.get("")))
        .hasMessageThat()
        .isEqualTo("Option names must not be empty");
    assertThat(assertThrows(IllegalArgumentException.class, () -> opts.get(".foo")))
        .hasMessageThat()
        .isEqualTo("Option names must not start or end with '.'");
    assertThat(assertThrows(IllegalArgumentException.class, () -> opts.get("foo.")))
        .hasMessageThat()
        .isEqualTo("Option names must not start or end with '.'");
    assertThat(assertThrows(IllegalArgumentException.class, () -> opts.get("foo..bar")))
        .hasMessageThat()
        .isEqualTo("Option names must not contain empty segments");
    assertThat(assertThrows(IllegalArgumentException.class, () -> opts.get("foo,bar")))
        .hasMessageThat()
        .contains("Option names must not contain any of:");
  }

  @Test
  public void get() {
    Options opts = options(kvp("foo", "123"), kvp("foo.bar", "answer"), kvp("foo.baz", "42"));
    assertThat(opts.get("foo")).hasValue("123");
    assertThat(opts.get("foo.bar")).hasValue("answer");
    assertThat(opts.get("foo.baz")).hasValue("42");
  }

  @Test
  public void get_withAlias() {
    Options opts =
        options(
            kvp("foo", "@alias"),
            kvp("alias.bar", "inherited alias"),
            kvp("foo.baz", "@direct.alias"),
            kvp("direct.alias", "direct alias"));
    assertThat(opts.get("foo.bar")).hasValue("inherited alias");
    assertThat(opts.get("foo.baz")).hasValue("direct alias");
    assertThat(opts.get("foo")).isEmpty();
  }

  @Test
  public void get_infiniteLoop() {
    Options opts =
        options(
            kvp("batman", "@adam.west"),
            kvp("adam.west", "@bruce_wayne"),
            kvp("bruce_wayne", "@batman"));
    var e = assertThrows(OptionParseException.class, () -> opts.get("batman"));
    assertThat(e).hasMessageThat().contains("--> adam.west");
    assertThat(e).hasMessageThat().contains("--> bruce_wayne");
    assertThat(e).hasMessageThat().contains("--> batman");

    // Also fails when resolving missing attribute.
    assertThrows(OptionParseException.class, () -> opts.get("batman.sidekick"));

    // And fails when getting via child options.
    Options adam = opts.getOptions("adam");
    assertThrows(OptionParseException.class, () -> adam.get("west"));
  }

  @Test
  public void getOptions() {
    Options opts =
        options(
            kvp("foo", "123"),
            kvp("foo.bar", "answer"),
            kvp("foo.baz.quux", "42"),
            kvp("bar", "unused"));
    Options foo = opts.getOptions("foo");
    assertThat(foo.get("bar")).hasValue("answer");
    assertThat(foo.get("baz.quux")).hasValue("42");
  }

  @Test
  public void getOptions_withAliases() {
    Options opts =
        options(
            kvp("foo", "@bar, @baz"),
            kvp("foo.xxx", "override in foo"),
            kvp("bar.xxx", "hidden by foo"),
            kvp("baz.yyy", "inherit from baz"));
    Options foo = opts.getOptions("foo");
    assertThat(foo.get("xxx")).hasValue("override in foo");
    assertThat(foo.get("yyy")).hasValue("inherit from baz");
    // We can get the alias value directly.
    assertThat(opts.get("bar.xxx")).hasValue("hidden by foo");
  }

  @Test
  public void getOptionsArray() {
    Options opts =
        options(
            kvp("foo.bar.size", "3"),
            kvp("foo.bar.0.xxx", "first option value"),
            kvp("foo.bar.0.yyy", "first option other value"),
            kvp("foo.bar.1.xxx", "second option value"),
            kvp("foo.bar.1.zzz", "second option other value"),
            kvp("foo.bar.2", "@alias"),
            kvp("foo.bar.2.xxx", "third option value"),
            kvp("alias.yyy", "third option aliased value"));
    List<Options> arr = opts.getOptionsArray("foo.bar");

    assertThat(arr.get(0).get("xxx")).hasValue("first option value");
    assertThat(arr.get(0).get("yyy")).hasValue("first option other value");
    assertThat(arr.get(1).get("xxx")).hasValue("second option value");
    assertThat(arr.get(1).get("zzz")).hasValue("second option other value");
    assertThat(arr.get(2).get("xxx")).hasValue("third option value");
    assertThat(arr.get(2).get("yyy")).hasValue("third option aliased value");
  }

  @Test
  public void getString() {
    Options opts = options(kvp("foo", "bar"), kvp("bar", "@foo"), kvp("baz", "@@foo"));
    assertThat(opts.getString("foo", "")).isEqualTo("bar");
    assertThat(opts.getString("bar", "")).isEqualTo("bar");
    assertThat(opts.getString("baz", "")).isEqualTo("@foo");
    // Disallow null default values (even if the value is present).
    assertThrows(NullPointerException.class, () -> opts.getString("foo", null));
  }

  @Test
  public void getStringArray() {
    Options opts =
        options(
            kvp("foo.size", "3"),
            kvp("foo.0", "first"),
            kvp("foo.1", "second"),
            kvp("foo.2", "last"));
    assertThat(opts.getStringArray("foo")).containsExactly("first", "second", "last").inOrder();
  }

  @Test
  public void getStringArray_withAlias() {
    Options opts =
        options(
            kvp("foo.bar", "@baz"),
            kvp("baz.size", "3"),
            kvp("baz.0", "first"),
            kvp("baz.1", "second"),
            kvp("baz.2", "last"));
    List<String> list = opts.getStringArray("foo.bar");
    assertThat(list).containsExactly("first", "second", "last").inOrder();
    assertThat(list).isEqualTo(opts.getStringArray("baz"));
  }

  @Test
  public void getStringArray_noMixing() {
    Options opts =
        options(
            kvp("foo", "$bar"),
            kvp("foo.size", "2"),
            kvp("foo.0", "first"),
            kvp("foo.1", "second"),
            // NOT overridden since 'size' is specified in foo already.
            kvp("bar.size", "1"),
            kvp("bar.0", "not overridden"));
    assertThat(opts.getStringArray("foo")).containsExactly("first", "second").inOrder();

    Options barArray =
        options(
            kvp("foo", "$bar"),
            kvp("foo.size", "2"),
            kvp("foo.0", "first"),
            // NOT included as the 2nd element (mixing array elements is prohibited).
            kvp("bar.1", "not included"));
    var e = assertThrows(OptionParseException.class, () -> barArray.getStringArray("foo"));
    assertThat(e).hasMessageThat().contains("No such array element: foo[1]");
    assertThat(e).hasMessageThat().contains("foo");
  }

  @Test
  public void getLong() {
    Options opts =
        options(
            kvp("foo.x", "1234"),
            kvp("foo.y", Long.toString(Long.MAX_VALUE)),
            kvp("foo.z", "-1234"),
            kvp("foo.nan", "<not a number>"));
    assertThat(opts.getLong("foo.x", 0)).isEqualTo(1234);
    assertThat(opts.getLong("foo.y", 0)).isEqualTo(Long.MAX_VALUE);
    assertThat(opts.getLong("foo.z", 0)).isEqualTo(-1234);
    assertThat(opts.getLong("foo.missing", 123)).isEqualTo(123);
    assertThrows(OptionParseException.class, () -> opts.getLong("foo.nan", 0));
  }

  @Test
  public void getLongArray() {
    Options opts =
        options(
            kvp("foo.size", "3"),
            kvp("foo.0", "1234"),
            kvp("foo.1", Long.toString(Long.MAX_VALUE)),
            kvp("foo.2", "-1234"));
    assertThat(opts.getLongArray("foo")).containsExactly(1234L, Long.MAX_VALUE, -1234L).inOrder();
  }

  @Test
  public void getDouble() {
    Options opts =
        options(
            kvp("foo.x", "123.4"),
            kvp("foo.y", "42"),
            kvp("foo.z", "-1.23"),
            kvp("foo.nan", "<not a number>"));
    assertThat(opts.getDouble("foo.x", 123.4)).isEqualTo(123.4);
    assertThat(opts.getDouble("foo.y", 42)).isEqualTo(42D);
    assertThat(opts.getDouble("foo.z", -1.23)).isEqualTo(-1.23D);
    assertThat(opts.getDouble("foo.missing", 3.141)).isEqualTo(3.141);
    assertThrows(OptionParseException.class, () -> opts.getDouble("foo.nan", 0));
  }

  @Test
  public void getBoolean() {
    Options opts =
        options(
            kvp("foo.x", "true"),
            kvp("foo.y", "FALSE"),
            kvp("foo.z", "TruE"),
            kvp("foo.bad_logic", "0"));
    assertThat(opts.getBoolean("foo.x", false)).isEqualTo(true);
    assertThat(opts.getBoolean("foo.y", true)).isEqualTo(false);
    assertThat(opts.getBoolean("foo.z", false)).isEqualTo(true);
    assertThat(opts.getBoolean("foo.missing", true)).isEqualTo(true);
    assertThrows(OptionParseException.class, () -> opts.getBoolean("foo.bad_logic", true));
  }

  private enum TestEnum {
    UNKNOWN,
    FOO,
    BAR
  }

  @Test
  public void getEnum() {
    Options opts = options(kvp("foo.x", "foo"), kvp("foo.y", "Bar"), kvp("foo.z", "BAZ"));
    assertThat(opts.getEnum("foo.x", TestEnum.UNKNOWN)).isEqualTo(TestEnum.FOO);
    assertThat(opts.getEnum("foo.y", TestEnum.UNKNOWN)).isEqualTo(TestEnum.BAR);
    assertThat(opts.getEnum("foo.missing", TestEnum.UNKNOWN)).isEqualTo(TestEnum.UNKNOWN);
    assertThrows(OptionParseException.class, () -> opts.getEnum("foo.z", TestEnum.UNKNOWN));
  }

  @Test
  public void getValue() {
    Options opts = options(kvp("foo.x", "  with space  "), kvp("foo.y", "1.234"));
    assertThat(opts.getValue("foo.x", String::trim)).hasValue("with space");
    assertThat(opts.getValue("foo.y", Double::parseDouble)).hasValue(1.234);

    assertThrows(OptionParseException.class, () -> opts.getValue("foo.y", Long::parseLong));
    assertThat(opts.getValue("foo.missing", String::trim)).isEmpty();
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
