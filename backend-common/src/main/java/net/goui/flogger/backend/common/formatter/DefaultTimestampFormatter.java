package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataProcessor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import net.goui.flogger.backend.common.Options;

/**
 * Flogger message formatter plugin for the {@code %{timestamp}} directive to format the log's
 * timestamp.
 *
 * <h3>Options</h3>
 *
 * <ul>
 *   <li>{@code message_formatter.timestamp.pattern}: String<br>
 *       Formats the timestamp using the given {@link DateTimeFormatter} pattern.
 *   <li>{@code message_formatter.timestamp.zone_id}: String<br>
 *       Uses the specified {@link ZoneId} to adjust the timestamp's timezone.
 * </ul>
 */
final class DefaultTimestampFormatter extends LogMessageFormatter {
  private final DateTimeFormatter dateTimeFormatter;

  DefaultTimestampFormatter(Options options) {
    String pattern = options.getString("pattern", "");
    Optional<ZoneId> zoneId = options.getValue("zone_id", ZoneId::of);
    DateTimeFormatter formatter =
        pattern.isEmpty() ? DateTimeFormatter.ISO_INSTANT : DateTimeFormatter.ofPattern(pattern);
    this.dateTimeFormatter = formatter.withZone(zoneId.orElse(ZoneId.systemDefault()));
  }

  @Override
  public StringBuilder append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
    long epochNanos = logData.getTimestampNanos();
    long epochSeconds = epochNanos / 1_000_000_000;
    int nanosOfSecond = (int) (epochNanos - (1_000_000_000 * epochSeconds));
    Instant timestamp = Instant.ofEpochSecond(epochSeconds, nanosOfSecond);
    dateTimeFormatter.formatTo(timestamp, buffer);
    return buffer;
  }
}
