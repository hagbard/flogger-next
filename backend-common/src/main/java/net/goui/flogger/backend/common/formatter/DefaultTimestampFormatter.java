package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.MetadataProcessor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import net.goui.flogger.backend.common.MessageFormatter;
import net.goui.flogger.backend.common.Options;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

class DefaultTimestampFormatter implements MessageFormatter {
  @NullableDecl private final DateTimeFormatter dateTimeFormatter;

  DefaultTimestampFormatter(Options options) {
    String pattern = options.getString("pattern", "");
    Optional<ZoneId> zoneId = options.getValue("zone_id", ZoneId::of);
    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
    if (!pattern.isEmpty() || zoneId.isPresent()) {
      ZoneId tz = zoneId.orElse(ZoneId.systemDefault());
      formatter = DateTimeFormatter.ofPattern(pattern).withZone(tz);
    }
    this.dateTimeFormatter = formatter;
  }

  @Override
  public void append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
    long epochNanos = logData.getTimestampNanos();
    long epochSeconds = epochNanos / 1_000_000_000;
    int nanosOfSecond = (int) (epochNanos - (1_000_000_000 * epochSeconds));
    Instant timestamp = Instant.ofEpochSecond(epochSeconds, nanosOfSecond);
    if (dateTimeFormatter != null) {
      dateTimeFormatter.formatTo(timestamp, buffer);
    } else {
      buffer.append(timestamp);
    }
  }
}
