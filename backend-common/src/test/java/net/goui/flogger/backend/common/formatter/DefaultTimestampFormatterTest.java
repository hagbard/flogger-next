package net.goui.flogger.backend.common.formatter;

import static com.google.common.truth.Truth.assertThat;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.testing.FakeLogData;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import net.goui.flogger.backend.common.Options;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DefaultTimestampFormatterTest {

  @Test
  public void testDefaultIsoFormat() {
    DefaultTimestampFormatter fmt = new DefaultTimestampFormatter(Options.of(k -> null));
    Instant now = Instant.now();
    FakeLogData data = FakeLogData.of("<message>").setTimestampNanos(asTimestamp(now));

    assertThat(fmt.format(data, noMetadata())).isEqualTo(now.toString());
    assertThat(fmt.format(data, noMetadata())).isEqualTo(ISO_INSTANT.format(now));
  }

  @Test
  public void testLocalTime() {
    // Note: It's not clear why 7 second fractions are used, but it matches the default formatting.
    // Also, use ".SSS" and NOT ".nnn", since the 'n' directive will remove leading zeros.
    //
    // Also, sometimes, the predefined java.time formats truncate trailing zeros.
    // Expected :12:36:18.067904
    // Actual   :12:36:18.0679040
    ImmutableMap<String, String> opts =
        ImmutableMap.of("pattern", "HH:mm:ss.SSSSSSS", "zone_id", "UTC");
    DefaultTimestampFormatter fmt = new DefaultTimestampFormatter(Options.of(opts::get));
    Instant now = Instant.now();

    LocalTime time = LocalTime.ofInstant(now, ZoneId.of("UTC"));
    FakeLogData data = FakeLogData.of("<message>").setTimestampNanos(asTimestamp(now));
    assertThat(fmt.format(data, noMetadata())).startsWith(ISO_LOCAL_TIME.format(time));
  }

  @Test
  public void testLocalDateTime() {
    ImmutableMap<String, String> opts =
        ImmutableMap.of("pattern", "yyy-MM-dd'T'HH:mm:ss.SSSSSSS", "zone_id", "UTC");
    DefaultTimestampFormatter fmt = new DefaultTimestampFormatter(Options.of(opts::get));
    Instant now = Instant.now();

    LocalDateTime dateTime = LocalDateTime.ofInstant(now, ZoneId.of("UTC"));
    FakeLogData data = FakeLogData.of("<message>").setTimestampNanos(asTimestamp(now));
    assertThat(fmt.format(data, noMetadata())).startsWith(ISO_LOCAL_DATE_TIME.format(dateTime));
  }

  @Test
  public void testLocalDateTime_CET() {
    ImmutableMap<String, String> opts =
        ImmutableMap.of("pattern", "yyy-MM-dd'T'HH:mm:ss.SSSSSSS", "zone_id", "CET");
    DefaultTimestampFormatter fmt = new DefaultTimestampFormatter(Options.of(opts::get));
    Instant now = Instant.now();

    LocalDateTime dateTime = LocalDateTime.ofInstant(now, ZoneId.of("CET"));
    FakeLogData data = FakeLogData.of("<message>").setTimestampNanos(asTimestamp(now));
    assertThat(fmt.format(data, noMetadata())).startsWith(ISO_LOCAL_DATE_TIME.format(dateTime));

    LocalDateTime utcDateTime = LocalDateTime.ofInstant(now, ZoneId.of("UTC"));
    assertThat(fmt.format(data, noMetadata()))
        .doesNotContain(ISO_LOCAL_DATE_TIME.format(utcDateTime));
  }

  private static long asTimestamp(Instant t) {
    return (t.getEpochSecond() * 1_000_000_000) + t.getNano();
  }

  private static MetadataProcessor noMetadata() {
    return MetadataProcessor.forScopeAndLogSite(Metadata.empty(), Metadata.empty());
  }
}
