package net.goui.flogger.backend.common;

import static com.google.common.flogger.backend.Metadata.empty;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.backend.LogData;
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
  public static final class Keys {
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
    TestFactory factory = new TestFactory(Options.of(opts::get));

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
    TestFactory factory = new TestFactory(Options.of(s -> null));

    FakeLogData data =
        FakeLogData.of("<message>").addMetadata(Keys.FOO, "xyz").addMetadata(Keys.BAR, 42);
    MetadataProcessor metadata = MetadataProcessor.forScopeAndLogSite(data.getMetadata(), empty());
    assertThat(factory.getBackendFormatter().format(data, metadata))
        .isEqualTo("<message> [CONTEXT foo=\"xyz\" bar=42 ]");
    assertThat(factory.getBackendFormatter().getClass().getName())
        .startsWith("com.google.common.flogger");
  }

  @Test
  public void getDefaultFormatter_ignoredKeys() {
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "message_formatter.metadata.ignore.size",
            "1",
            "message_formatter.metadata.ignore.0",
            Keys.class.getName() + "#FOO");
    TestFactory factory = new TestFactory(Options.of(opts::get));

    FakeLogData data =
        FakeLogData.of("<message>").addMetadata(Keys.FOO, "xyz").addMetadata(Keys.BAR, 42);
    MetadataProcessor metadata = MetadataProcessor.forScopeAndLogSite(data.getMetadata(), empty());
    assertThat(factory.getBackendFormatter().format(data, metadata)).isEqualTo("<message> [CONTEXT bar=42 ]");
  }

  @Test
  public void getPatternFormatter() {
    // Pattern formatter is tested thoroughly elsewhere, so just test the setup here.
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "message_formatter.impl",
            "pattern",
            "message_formatter.pattern",
            "%{message}%{metadata/ {/\\}}");
    TestFactory factory = new TestFactory(Options.of(opts::get));

    FakeLogData data =
        FakeLogData.of("<message>").addMetadata(Keys.FOO, "xyz").addMetadata(Keys.BAR, 42);
    MetadataProcessor metadata = MetadataProcessor.forScopeAndLogSite(data.getMetadata(), empty());
    assertThat(factory.getBackendFormatter().format(data, metadata))
        .isEqualTo("<message> {foo=\"xyz\" bar=42}");
    assertThat(factory.getBackendFormatter().getClass().getName())
        .startsWith("net.goui.flogger.backend");
  }

  @Test
  public void getPatternFormatter_ignoredKeys() {
    // Pattern formatter is tested thoroughly elsewhere, so just test the setup here.
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "message_formatter.impl",
            "pattern",
            "message_formatter.pattern",
            "%{message}%{metadata/ {/\\}}",
            "message_formatter.metadata.ignore.size",
            "1",
            "message_formatter.metadata.ignore.0",
            Keys.class.getName() + "#FOO");
    TestFactory factory = new TestFactory(Options.of(opts::get));

    FakeLogData data =
        FakeLogData.of("<message>").addMetadata(Keys.FOO, "xyz").addMetadata(Keys.BAR, 42);
    MetadataProcessor metadata = MetadataProcessor.forScopeAndLogSite(data.getMetadata(), empty());

    assertThat(factory.getBackendFormatter().format(data, metadata))
        .isEqualTo("<message> {bar=42}");
    assertThat(factory.getBackendFormatter().getClass().getName())
        .startsWith("net.goui.flogger.backend");
  }

  static class TestFactory extends AbstractBackendFactory<FakeBackend> {

    protected TestFactory(Options options) {
      super(options, FakeBackend::new);
    }

    @Override
    protected List<String> getSystemRoots() {
      // Called from super-class init, so cannot just return an instance field here.
      // In real implementations, this is derived from the underlying logging config.
      return List.of("com.system");
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
