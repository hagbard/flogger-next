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

import static com.google.common.flogger.backend.Metadata.empty;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.testing.FakeLogData;
import java.util.List;
import java.util.logging.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AbstractBackendFactoryTest {
  public static final class Key {
    public static final MetadataKey<String> FOO = MetadataKey.single("foo", String.class);
    public static final MetadataKey<Integer> BAR = MetadataKey.single("bar", Integer.class);
  }

  @Test
  public void create_defaultNaming() {
    TestFactory factory = new TestFactory(Options.of(s -> null));

    // The default naming strategy is a no-op (no roots present).
    assertThat(factory.create("foo.bar.Baz").getLoggerName()).isEqualTo("foo.bar.Baz");
    assertThat(factory.create("<any string>").getLoggerName()).isEqualTo("<any string>");
  }

  @Test
  public void create_withoutSystemRoots() {
    // Naming strategy is tested separately, so only test the setup here.
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "backend_naming.roots.size",
            "2",
            "backend_naming.roots.0",
            "com.foo",
            "backend_naming.roots.1",
            "com.bar");
    TestFactory factory = new TestFactory(Options.of(opts::get));

    assertThat(factory.create("com.foo.Class").getLoggerName()).isEqualTo("com.foo");
    assertThat(factory.create("com.foo.bar.Class").getLoggerName()).isEqualTo("com.foo");
    assertThat(factory.create("com.bar.Class").getLoggerName()).isEqualTo("com.bar");
    // Anything that's not a root is left alone.
    assertThat(factory.create("com.system.Class").getLoggerName()).isEqualTo("com.system.Class");
    assertThat(factory.create("<not a root>").getLoggerName()).isEqualTo("<not a root>");
  }

  @Test
  public void create_withSystemRoots() {
    // System roots add additional entry for "com.system".
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "backend_naming.use_system_roots",
            "true",
            "backend_naming.roots.size",
            "2",
            "backend_naming.roots.0",
            "com.foo",
            "backend_naming.roots.1",
            "com.bar");
    TestFactory factory = new TestFactory(Options.of(opts::get), "com.system");

    assertThat(factory.create("com.foo.Class").getLoggerName()).isEqualTo("com.foo");
    assertThat(factory.create("com.bar.Class").getLoggerName()).isEqualTo("com.bar");
    assertThat(factory.create("com.system.Class").getLoggerName()).isEqualTo("com.system");
    // Anything that's not a root is left alone.
    assertThat(factory.create("com.foobar.Class").getLoggerName()).isEqualTo("com.foobar.Class");
    assertThat(factory.create("<not a root>").getLoggerName()).isEqualTo("<not a root>");
  }

  @Test
  public void create_noCachedBackends() {
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "use_backend_cache",
            "false",
            "backend_naming.roots.size",
            "2",
            "backend_naming.roots.0",
            "com.foo",
            "backend_naming.roots.1",
            "com.bar");
    TestFactory factory = new TestFactory(Options.of(opts::get));

    FakeBackend first = factory.create("com.foo.Class");
    FakeBackend second = factory.create("com.foo.Class");
    FakeBackend other = factory.create("com.bar.Class");

    assertThat(first).isNotSameInstanceAs(second);
    assertThat(first).isNotSameInstanceAs(other);
  }

  @Test
  public void create_cachedBackends() {
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "use_backend_cache",
            "true",
            "backend_naming.roots.size",
            "2",
            "backend_naming.roots.0",
            "com.foo",
            "backend_naming.roots.1",
            "com.bar");
    TestFactory factory = new TestFactory(Options.of(opts::get));

    FakeBackend first = factory.create("com.foo.Class");
    FakeBackend second = factory.create("com.foo.Class");
    FakeBackend other = factory.create("com.bar.Class");

    assertThat(first).isSameInstanceAs(second);
    assertThat(first).isNotSameInstanceAs(other);
  }

  @Test
  public void getDefaultFormatter() {
    // Pattern formatter is tested thoroughly elsewhere, so just test the setup here.
    ImmutableMap<String, String> opts =
        ImmutableMap.of("message_formatter.pattern", "%{message}%{metadata/ {/\\}}");
    TestFactory factory = new TestFactory(Options.of(opts::get));

    FakeLogData data =
        FakeLogData.of("<message>").addMetadata(Key.FOO, "xyz").addMetadata(Key.BAR, 42);
    MetadataProcessor metadata = MetadataProcessor.forScopeAndLogSite(data.getMetadata(), empty());
    assertThat(factory.getMessageFormatter().format(data, metadata))
        .isEqualTo("<message> {foo=\"xyz\" bar=42}");
    assertThat(factory.getMessageFormatter().getClass().getName())
        .startsWith("net.goui.flogger.backend");
  }

  @Test
  public void getDefaultFormatter_ignoredKeys() {
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "message_formatter.metadata.ignore.size",
            "1",
            "message_formatter.metadata.ignore.0",
            Key.class.getName() + "#FOO");
    TestFactory factory = new TestFactory(Options.of(opts::get));

    FakeLogData data =
        FakeLogData.of("<message>").addMetadata(Key.FOO, "xyz").addMetadata(Key.BAR, 42);
    MetadataProcessor metadata = MetadataProcessor.forScopeAndLogSite(data.getMetadata(), empty());
    assertThat(factory.getMessageFormatter().format(data, metadata))
        .isEqualTo("<message> [CONTEXT bar=42 ]");
  }

  @Test
  public void getPatternFormatter_ignoredKeys() {
    // Pattern formatter is tested thoroughly elsewhere, so just test the setup here.
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "message_formatter.pattern",
            "%{message}%{metadata/ {/\\}}",
            "message_formatter.metadata.ignore.size",
            "1",
            "message_formatter.metadata.ignore.0",
            Key.class.getName() + "#FOO");
    TestFactory factory = new TestFactory(Options.of(opts::get));

    FakeLogData data =
        FakeLogData.of("<message>").addMetadata(Key.FOO, "xyz").addMetadata(Key.BAR, 42);
    MetadataProcessor metadata = MetadataProcessor.forScopeAndLogSite(data.getMetadata(), empty());

    assertThat(factory.getMessageFormatter().format(data, metadata))
        .isEqualTo("<message> {bar=42}");
    assertThat(factory.getMessageFormatter().getClass().getName())
        .startsWith("net.goui.flogger.backend");
  }

  static class TestFactory extends AbstractBackendFactory<FakeBackend> {
    TestFactory(Options options, String... systemRoots) {
      super(options, List.of(systemRoots));
    }

    @Override
    protected FakeBackend newBackend(
        String backendName, LogMessageFormatter formatter, Options options) {
      return new FakeBackend(backendName);
    }
  }

  static class FakeBackend extends LoggerBackend {
    private final String name;

    FakeBackend(String name) {
      this.name = name;
    }

    @Override
    public String getLoggerName() {
      return name;
    }

    @Override
    public boolean isLoggable(Level level) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void log(LogData logData) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void handleError(RuntimeException e, LogData logData) {
      throw new UnsupportedOperationException();
    }
  }
}
