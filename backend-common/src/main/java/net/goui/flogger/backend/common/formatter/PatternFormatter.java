package net.goui.flogger.backend.common.formatter;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.flogger.backend.BaseMessageFormatter;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.goui.flogger.backend.common.FloggerPlugin;
import net.goui.flogger.backend.common.Options;

// %{timestamp} (%{level} - %{location}): %{key.foo}%{key.bar/-} %{message}%{metadata/ [/]}
public final class PatternFormatter extends LogMessageFormatter {
  private final MetadataExtractor metadataExtractor;
  private final LogMessageFormatter metadataFormatter;
  private final List<String> parts;
  private final List<BiConsumer<FormatContext, StringBuilder>> formatters;

  private static final Pattern ESCAPED_CHAR = Pattern.compile("\\\\(.)");
  private static final Pattern PATTERN_REGEX =
      Pattern.compile(
          "((?:[^%\\\\]|\\\\.)*)" // literal part
              + "%\\{"
              + "([^/}\\\\]+)" // <label> (no '\' and no escaping)
              + "(?:/((?:[^/\\\\}]|\\\\.)*)" // /<prefix>
              + "(?:/((?:[^/\\\\}]|\\\\.)*))?)?" // /<suffix>
              + "}");

  public PatternFormatter(Options options) {
    String formatPattern = options.getString("pattern", "%{message}%{metadata/ [CONTEXT / ]}");

    // Options could be: raw, quote-if-string, escape-and-quote (JSON/HTML?)
    // * Single or double quotes.
    // * Escaping inner quotes.
    BiConsumer<StringBuilder, Object> valueAppender = ValueAppender.defaultAppender();

    List<MatchResult> patternParts = parsePatternParts(formatPattern);
    this.metadataExtractor =
        new MetadataExtractor(
            options.getOptions("metadata"), extractKeyNames(patternParts), valueAppender);
    this.metadataFormatter = metadataExtractor.getMetadataFormatter();

    this.parts = extractLiteralParts(patternParts, formatPattern);
    this.formatters =
        patternParts.stream()
            .map(m -> createFormatter(m, options, metadataFormatter, valueAppender))
            .collect(toList());
  }

  private static List<MatchResult> parsePatternParts(String formatPattern) {
    Matcher matcher = PATTERN_REGEX.matcher(formatPattern);
    List<MatchResult> patternParts = new ArrayList<>();
    int start = 0;
    // TODO: Maybe handle seeing same formatter more than once in a pattern?
    while (matcher.find(start)) {
      patternParts.add(matcher.toMatchResult());
      start = matcher.end();
    }
    return patternParts;
  }

  private static Set<String> extractKeyNames(List<MatchResult> patternParts) {
    return patternParts.stream()
        .map(m -> m.group(2))
        .filter(l -> l.startsWith("key."))
        .map(l -> l.substring(4))
        .collect(toSet());
  }

  private static List<String> extractLiteralParts(
      List<MatchResult> patternParts, String formatPattern) {
    int endIndex = patternParts.isEmpty() ? 0 : patternParts.get(patternParts.size() - 1).end();
    return Stream.concat(
            patternParts.stream().map(m -> unescape(m.group(1))),
            Stream.of(formatPattern.substring(endIndex)))
        .collect(toList());
  }

  private static BiConsumer<FormatContext, StringBuilder> createFormatter(
      MatchResult m,
      Options options,
      LogMessageFormatter metadataFormatter,
      BiConsumer<StringBuilder, Object> valueAppender) {
    // The directive name is a limited set of characters, so doesn't need unescaping.
    String label = m.group(2);
    boolean isOptionalDirective = false;
    BiConsumer<FormatContext, StringBuilder> formatter;
    switch (label) {
      case "message":
        formatter = (c, b) -> BaseMessageFormatter.appendFormattedMessage(c.getLogData(), b);
        break;
      case "timestamp":
        formatter = newFormatter(options, "timestamp", DefaultTimestampFormatter::new);
        break;
      case "location":
        formatter = newFormatter(options, "location", DefaultLocationFormatter::new);
        break;
      case "level":
        formatter = newFormatter(options, "level", DefaultLevelFormatter::new);
        break;
      case "metadata":
        isOptionalDirective = true;
        formatter = (c, b) -> metadataFormatter.append(c.getLogData(), c.getMetadata(), b);
        break;
      default:
        if (!label.startsWith("key.")) {
          throw new IllegalArgumentException(
              "unknown formatting directive %{" + label + "} in message format string");
        }
        isOptionalDirective = true;
        formatter = (c, b) -> valueAppender.accept(b, c.getValue(label.substring(4)));
        break;
    }
    // Only some directives have a prefix/suffix, but where it's not present these are empty.
    String prefix = unescape(m.group(3));
    String suffix = unescape(m.group(4));
    if (isOptionalDirective) {
      return FormatContext.wrap(prefix, suffix, formatter);
    }
    if (!prefix.isEmpty() || !suffix.isEmpty()) {
      throw new IllegalArgumentException(
          "format directive %{"
              + label
              + "} with non-optional value must not contain prefix or suffix");
    }
    return formatter;
  }

  private static String unescape(String s) {
    return s != null ? ESCAPED_CHAR.matcher(s).replaceAll("$1") : "";
  }

  private static BiConsumer<FormatContext, StringBuilder> newFormatter(
      Options options, String optionName, Function<Options, LogMessageFormatter> newFn) {
    return FormatContext.wrap(
        FloggerPlugin.instantiate(
            LogMessageFormatter.class, options.getOptions(optionName), Map.of("default", newFn)));
  }

  @Override
  public String format(LogData logData, MetadataProcessor metadata) {
    return super.format(logData, metadata);
  }

  @Override
  public StringBuilder append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
    Map<String, Object> keyMap = metadataExtractor.extractKeys(metadata);
    FormatContext context = new FormatContext(logData, metadata, keyMap);
    int partCount = formatters.size();
    for (int n = 0; n < partCount; n++) {
      buffer.append(parts.get(n));
      formatters.get(n).accept(context, buffer);
    }
    buffer.append(parts.get(partCount));
    return buffer;
  }

  private static class FormatContext {
    static BiConsumer<FormatContext, StringBuilder> wrap(LogMessageFormatter formatter) {
      return (c, b) -> formatter.append(c.getLogData(), c.getMetadata(), b);
    }

    static BiConsumer<FormatContext, StringBuilder> wrap(
        String prefix, String suffix, BiConsumer<FormatContext, StringBuilder> delegate) {
      return (c, b) -> {
        int start = b.length();
        b.append(prefix);
        delegate.accept(c, b);
        if (b.length() == start + prefix.length()) {
          b.setLength(start);
        } else {
          b.append(suffix);
        }
      };
    }

    private final LogData logData;
    private final MetadataProcessor metadata;
    private final Map<String, Object> keyMap;

    public FormatContext(LogData logData, MetadataProcessor metadata, Map<String, Object> keyMap) {
      this.logData = requireNonNull(logData);
      this.metadata = requireNonNull(metadata);
      this.keyMap = requireNonNull(keyMap);
    }

    LogData getLogData() {
      return logData;
    }

    MetadataProcessor getMetadata() {
      return metadata;
    }

    Object getValue(String key) {
      return keyMap.getOrDefault(key, null);
    }
  }
}
