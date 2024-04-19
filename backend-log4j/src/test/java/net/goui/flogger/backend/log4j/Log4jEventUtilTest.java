package net.goui.flogger.backend.log4j;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.flogger.LogContext;
import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.context.Tags;
import com.google.common.flogger.testing.FakeLogSite;
import com.google.common.flogger.testing.FakeMetadata;
import java.util.logging.Level;
import org.apache.logging.log4j.util.StringMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Log4jEventUtilTest {
  public static final class Key {
    public static final MetadataKey<String> FOO = MetadataKey.single("foo", String.class);
    public static final MetadataKey<Integer> BAR = MetadataKey.repeated("bar", Integer.class);
  }

  @Test
  public void getLog4jSource() {
    StackTraceElement source =
        Log4jEventUtil.getLog4jSource(FakeLogSite.create("<class>", "<method>", 123, "<source>"));
    assertThat(source.getClassName()).isEqualTo("<class>");
    assertThat(source.getMethodName()).isEqualTo("<method>");
    assertThat(source.getLineNumber()).isEqualTo(123);
    assertThat(source.getFileName()).isEqualTo("<source>");
    // Note available via LogSite and not used by Flogger.
    assertThat(source.getClassLoaderName()).isNull();
    assertThat(source.getModuleName()).isNull();
  }

  @Test
  public void getLog4jLevel() {
    assertThat(Log4jEventUtil.getLog4jLevel(Level.INFO))
        .isEqualTo(org.apache.logging.log4j.Level.INFO);
    assertThat(Log4jEventUtil.getLog4jLevel(Level.FINE))
        .isEqualTo(org.apache.logging.log4j.Level.DEBUG);

    Level aboveInfo = new Level("custom", Level.INFO.intValue() + 1) {};
    Level belowInfo = new Level("custom", Level.INFO.intValue() - 1) {};
    assertThat(Log4jEventUtil.getLog4jLevel(aboveInfo))
        .isEqualTo(org.apache.logging.log4j.Level.INFO);
    assertThat(Log4jEventUtil.getLog4jLevel(belowInfo))
        .isEqualTo(org.apache.logging.log4j.Level.DEBUG);
  }

  @Test
  public void createContextMap() {
    FakeMetadata metadata =
        new FakeMetadata().add(Key.FOO, "Hello").add(Key.BAR, 1).add(Key.BAR, 2);
    StringMap contextMap =
        Log4jEventUtil.createContextMap(
            MetadataProcessor.forScopeAndLogSite(Metadata.empty(), metadata));

    assertThat(contextMap.containsKey("foo")).isTrue();
    assertThat((Object) contextMap.getValue("foo")).isEqualTo("Hello");

    assertThat(contextMap.containsKey("bar")).isTrue();
    assertThat((Object) contextMap.getValue("bar")).isInstanceOf(ValueQueue.class);

    assertThat(contextMap.toMap()).containsExactly("foo", "Hello", "bar", "[1, 2]");
  }

  @Test
  public void createContextMap_tags() {
    Tags tags =
        Tags.builder()
            .addTag("name_only")
            .addTag("long", 123)
            .addTag("string", "Hello")
            .addTag("foo", true)
            .addTag("foo", 123)
            .addTag("foo", "World")
            .build();
    FakeMetadata metadata = new FakeMetadata().add(Key.FOO, "Hello").add(LogContext.Key.TAGS, tags);
    StringMap contextMap =
        Log4jEventUtil.createContextMap(
            MetadataProcessor.forScopeAndLogSite(Metadata.empty(), metadata));

    // Tags are NOT promoted to the
    // Order of tags in list is alphabetical by label and value (and thus stable).
    assertThat(contextMap.toMap())
        .containsExactly(
            "name_only", "", "long", "123", "string", "Hello", "foo", "[Hello, true, World, 123]");
  }
}
