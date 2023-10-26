package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.MetadataProcessor;
import java.util.Map;
import java.util.Set;
import net.goui.flogger.backend.common.FloggerPlugin;
import net.goui.flogger.backend.common.MessageFormatter;
import net.goui.flogger.backend.common.Options;

// %{timestamp} (%{level} - %{location}): %{key.foo}%{key.bar/-} %{message}%{metadata/ [/]}
public class PatternFormatter implements MessageFormatter {
  private final MessageFormatter timestampFormatter;
  private final MessageFormatter levelFormatter;
  private final MessageFormatter locationFormatter;
  private final MetadataExtractor metadataExtractor;
  private final MessageFormatter metadataFormatter;

  public PatternFormatter(Options options) {
    String formatPattern = options.getString("pattern", "");
    Set<String> keyNames = Set.of("foo_tag");

    // ${key.xxx/p/s} or ${key.yyy|p|s} etc.

    // NEVER empty, so skip prefix/suffix (disallow in parsing too).
    this.timestampFormatter =
        FloggerPlugin.instantiate(
            MessageFormatter.class,
            options.getOptions("timestamp"),
            Map.of("default", DefaultTimestampFormatter::new));
    // NEVER empty, so skip prefix/suffix (disallow in parsing too).
    this.levelFormatter =
        FloggerPlugin.instantiate(
            MessageFormatter.class,
            options.getOptions("level"),
            Map.of("default", DefaultLevelFormatter::new));
    // NEVER empty, so skip prefix/suffix (disallow in parsing too).
    this.locationFormatter =
        FloggerPlugin.instantiate(
            MessageFormatter.class,
            options.getOptions("location"),
            Map.of("default", DefaultLocationFormatter::new));
    this.metadataExtractor = new MetadataExtractor(options.getOptions("metadata"), keyNames);
    this.metadataFormatter = withPrefixAndSuffix(metadataExtractor.getMetadataFormatter(), " [ ", "]");
  }

  static MessageFormatter withPrefixAndSuffix(
      MessageFormatter formatter, String prefix, String suffix) {
    return prefix.isEmpty() && suffix.isEmpty()
        ? formatter
        : new PrefixSuffixFormatter(prefix, suffix, formatter);
  }

  @Override
  public void append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
    metadataExtractor.extractKeys(metadata);
    timestampFormatter.append(logData, metadata, buffer);
    buffer.append(":");
    levelFormatter.append(logData, metadata, buffer);
    buffer.append(":");
    locationFormatter.append(logData, metadata, buffer);
    buffer.append(":");
    buffer.append("<message>");
    metadataFormatter.append(logData, metadata, buffer);
  }
}
