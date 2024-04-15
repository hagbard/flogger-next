package net.goui.flogger.backend.common.formatter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.testing.FakeLogData;
import net.goui.flogger.backend.common.Options;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PatternFormatterTest {
  public static class Keys {
    public static final MetadataKey<String> FOO_KEY = MetadataKey.repeated("foo", String.class);
    public static final MetadataKey<Integer> BAR_KEY = MetadataKey.single("bar", Integer.class);
    public static final MetadataKey<Integer> INT_KEY =
        new MetadataKey<>("int", Integer.class, false) {
          @Override
          protected void emit(Integer value, KeyValueHandler kvh) {
            kvh.handle("dec", value);
            kvh.handle("hex", "0x" + Integer.toHexString(value));
          }
        };
  }

  @Test
  public void testFormatting_noPattern() {
    Options options = Options.of(s -> null);
    PatternFormatter fmt = new PatternFormatter(options);
    FakeLogData log = FakeLogData.of("<message>");

    assertThat(fmt.format(log, toMetadata(log))).isEqualTo("<message>");

    log.addMetadata(Keys.FOO_KEY, "Hello");
    assertThat(fmt.format(log, toMetadata(log))).isEqualTo("<message> [CONTEXT foo=\"Hello\" ]");

    log.addMetadata(Keys.BAR_KEY, 123);
    assertThat(fmt.format(log, toMetadata(log)))
        .isEqualTo("<message> [CONTEXT foo=\"Hello\" bar=123 ]");
  }

  @Test
  public void testFormatting_escapedMetadata() {
    Options options = Options.of(s -> null);
    PatternFormatter fmt = new PatternFormatter(options);

    FakeLogData log = FakeLogData.of("<message>").addMetadata(Keys.FOO_KEY, "New\nLine");
    assertThat(fmt.format(log, toMetadata(log)))
        .isEqualTo("<message> [CONTEXT foo=\"New\\nLine\" ]");
  }

  @Test
  public void testFormatting_patternWithDefaults() {
    ImmutableMap<String, String> opts =
        ImmutableMap.of("pattern", "%{timestamp} %{level}[%{location}] %{message}%{metadata/ [/]}");
    PatternFormatter fmt = new PatternFormatter(Options.of(opts::get));

    FakeLogData log = FakeLogData.of("<message>").addMetadata(Keys.BAR_KEY, 42);
    assertThat(fmt.format(log, toMetadata(log)))
        .isEqualTo("1970-01-01T00:00:00Z INFO[com.google.FakeClass#fakeMethod] <message> [bar=42]");
  }

  @Test
  public void testFormatting_patternWithCustomKeys() {
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "pattern",
            "%{message}%{key.foo/ }%{metadata/ [/]}",
            "metadata.key.foo",
            Keys.class.getName() + "#FOO_KEY");
    PatternFormatter fmt = new PatternFormatter(Options.of(opts::get));

    FakeLogData log =
        FakeLogData.of("<message>")
            .addMetadata(Keys.FOO_KEY, "Hello")
            .addMetadata(Keys.BAR_KEY, 42);
    assertThat(fmt.format(log, toMetadata(log))).isEqualTo("<message> \"Hello\" [bar=42]");
  }

  @Test
  public void testFormatting_withExtractedKeyFormatting() {
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "pattern",
            "%{message}%{key.bar/ BAR:/}%{metadata/ [/]}",
            "metadata.key.bar",
            Keys.class.getName() + "#BAR_KEY");
    PatternFormatter fmt = new PatternFormatter(Options.of(opts::get));

    FakeLogData log =
        FakeLogData.of("<message>")
            .addMetadata(Keys.FOO_KEY, "Hello")
            .addMetadata(Keys.BAR_KEY, 42);
    assertThat(fmt.format(log, toMetadata(log))).isEqualTo("<message> BAR:42 [foo=\"Hello\"]");
  }

  @Test
  public void testFormatting_withCustomKeyFormatting() {
    // For custom keys which emit more than one values, you can specify a trailing tag to indicate
    // which emitted value you are interested in.
    ImmutableMap<String, String> opts =
        ImmutableMap.of(
            "pattern",
            "%{message}%{key.dec/ }%{key.hex/=}%{metadata/ [/]}",
            "metadata.key.hex",
            Keys.class.getName() + "#INT_KEY:hex",
            "metadata.key.dec",
            Keys.class.getName() + "#INT_KEY:dec");
    PatternFormatter fmt = new PatternFormatter(Options.of(opts::get));

    FakeLogData log = FakeLogData.of("<message>").addMetadata(Keys.INT_KEY, 42);
    assertThat(fmt.format(log, toMetadata(log))).isEqualTo("<message> 42=\"0x2a\"");
  }

  @Test
  public void testFormatting_withRepeatedMetadata() {
    ImmutableMap<String, String> opts = ImmutableMap.of("pattern", "%{message}%{metadata/ [/]}");
    PatternFormatter fmt = new PatternFormatter(Options.of(opts::get));

    FakeLogData log =
        FakeLogData.of("<message>")
            .addMetadata(Keys.FOO_KEY, "Hello")
            .addMetadata(Keys.FOO_KEY, "Goodbye");
    assertThat(fmt.format(log, toMetadata(log)))
        .isEqualTo("<message> [foo=\"Hello\" foo=\"Goodbye\"]");
  }

  @Test
  public void testFormatting_withCustomTimestamp() {
    ImmutableMap<String, String> opts =
        ImmutableMap.of("pattern", "%{timestamp} %{message}", "timestamp.pattern", "HH:mm:ss");
    PatternFormatter fmt = new PatternFormatter(Options.of(opts::get));

    FakeLogData log = FakeLogData.of("<message>");
    assertThat(fmt.format(log, toMetadata(log))).isEqualTo("01:00:00 <message>");
  }

  @Test
  public void testFormatting_unknownDirective() {
    ImmutableMap<String, String> opts = ImmutableMap.of("pattern", "%{zoot/</>}");
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> new PatternFormatter(Options.of(opts::get)));
    assertThat(e).hasMessageThat().contains("%{zoot}");
    assertThat(e).hasMessageThat().contains("unknown formatting directive");
  }

  @Test
  public void testFormatting_escapePrefixAndSuffix() {
    // Use backslash to '%' or '\' in pattern body and '/', '}', or '\' in prefix/suffix.
    ImmutableMap<String, String> opts =
        ImmutableMap.of("pattern", "\\\\\\%%{message}\\%\\\\%{metadata/ \\/\\\\{/\\}\\/\\\\}");

    PatternFormatter fmt = new PatternFormatter(Options.of(opts::get));
    FakeLogData log = FakeLogData.of("<message>").addMetadata(Keys.BAR_KEY, 42);
    assertThat(fmt.format(log, toMetadata(log))).isEqualTo("\\%<message>%\\ /\\{bar=42}/\\");
  }

  @Test
  public void testFormatting_badEscaping() {
    // Opening '{' need not be escaped, but we deal with it.
    ImmutableMap<String, String> opts =
        ImmutableMap.of("pattern", "%{message} %{metadata/\\{/\\}}");

    PatternFormatter fmt = new PatternFormatter(Options.of(opts::get));
    FakeLogData log = FakeLogData.of("<message>").addMetadata(Keys.BAR_KEY, 42);
    assertThat(fmt.format(log, toMetadata(log))).isEqualTo("<message> {bar=42}");
  }

  @Test
  public void testFormatting_badPatterns() {
    // Since message is non-optional, it has no prefix/suffix.
    ImmutableMap<String, String> opts = ImmutableMap.of("pattern", "%{message/</>}");
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> new PatternFormatter(Options.of(opts::get)));
    assertThat(e).hasMessageThat().contains("%{message}");
    assertThat(e).hasMessageThat().contains("must not contain prefix or suffix");
  }

  MetadataProcessor toMetadata(LogData logData) {
    return MetadataProcessor.forScopeAndLogSite(logData.getMetadata(), Metadata.empty());
  }
}
