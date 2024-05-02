/*******************************************************************************
 * Copyright (c) 2024, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

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
 *   <li>{@code flogger.message_formatter.timestamp.pattern}: String<br>
 *       Formats the timestamp using the given {@link DateTimeFormatter} pattern.
 *   <li>{@code flogger.message_formatter.timestamp.zone_id}: String<br>
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
