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
import net.goui.flogger.backend.common.Options;
import net.goui.flogger.backend.common.PluginLoader;

/**
 * Flogger plugin for customizable log message formatting.
 *
 * <h3>Options</h3>
 *
 * <ul>
 *   <li>{@code flogger.message_formatter.pattern}: String<br>
 *       A rich format template for log message formatting (see below for details).
 *   <li>{@code flogger.message_formatter.metadata.key.<label>}: String<br>
 *       Maps a label to a public static {@link com.google.common.flogger.MetadataKey MetadataKey}
 *       field to permit custom formatting in the log message (see the {@code %{key.<label>}}
 *       directive).
 *   <li>{@code flogger.message_formatter.metadata.ignore}: String[]<br>
 *       A list of {@link com.google.common.flogger.MetadataKey MetadataKey} fields to be ignored
 *       when formatting the {@code %{metadata}} directive.
 * </ul>
 *
 * <h3>Formatter Directives</h3>
 *
 * <p>Custom message formatting is controlled primarily by the {@code pattern} option, which defines
 * a template string containing formatter directives (e.g. {@code %{message}} or {@code
 * %{metadata}}).
 *
 * <p>Directives can be either "built in" or provided via plugins, which allows further
 * customization of message formatting.
 *
 * <ul>
 *   <li>{@code %{message}}: Built-in<br>
 *       Emits the basic formatted log message (i.e. what was passed to the {@code log()} method).
 *   <li>{@code %{metadata}}: Built-in<br>
 *       Emits log metadata (e.g. scope or log site metadata such as task IDs or rate limiting
 *       information). Note that specific metadata can be formatted separately using {@code
 *       %{key.<label>}} or ignored via the {@code flogger.message_formatter.metadata.ignore} option
 *       list.
 *   <li>{@code %{key.<label>}}: Built-in<br>
 *       Emits metadata value(s) associated with a known {@link
 *       com.google.common.flogger.MetadataKey MetadataKey}. The given label must match an entry in
 *       the {@code message_formatter.metadata.key.<label>} options.
 *   <li>{@code %{level}}: Plugin {@link DefaultLevelFormatter}<br>
 *       Override default implementation via {@code flogger.message_formatter.level.impl}.
 *   <li>{@code %{location}}: Plugin {@link DefaultLocationFormatter}<br>
 *       Override default implementation via {@code flogger.message_formatter.location.impl}.
 *   <li>{@code %{timestamp}}: Plugin {@link DefaultTimestampFormatter}<br>
 *       Override default implementation via {@code flogger.message_formatter.timestamp.impl}.
 * </ul>
 *
 * <h3>Prefix/Suffix Formatting</h3>
 *
 * <p>Formatter directives with optional values can also accept prefix/suffix strings which are only
 * emitted if a value exists. This improves the formatting of optional values, such as metadata.
 *
 * <p>The prefix/suffix format syntax is {@code %{label/prefix/suffix}} where the suffix can be
 * omitted if empty. For example:
 *
 * <ul>
 *   <li>{@code %{metadata/[/]}}: Formats metadata values surrounded by '[',']' if metadata exists,
 *       but emits nothing otherwise.
 *   <li>{@code %{key.foo/Foo=}}: Formats a specific metadata value as {@code Foo=<value>} if it
 *       exists, but emits nothing otherwise.
 * </ul>
 *
 * <p>Since prefix/suffix strings need to be arbitrary, they employ simple backslash escaping to
 * allow characters such as '/' or '}' to be included. Simply prefix a character with {@code '\'} to
 * make it a literal.
 *
 * <p>Note that this feature is very useful for controlling spaces between optional message parts
 * and very commonly, either a prefix will start with a space, or a suffix will end with one.
 *
 * <p>Currently only the {@code metadata} and {@code key.<label>} directives have optional values.
 * Directives such as {@code %{timestamp}} which always emit a value do not accept prefix/suffix
 * strings, since they would always be emitted. Any formatting around these directives should just
 * be given normally in the pattern template.
 *
 * <h3>Example Format Pattern Strings</h3>
 *
 * <ul>
 *   <li>{@code "%{message}%{metadata/ [CONTEXT / ]}"}<br>
 *       Mimics the original Flogger format (note the leading space in the prefix string to avoid
 *       always leaving a space at the end of logs). This pattern string is the default.
 *   <li>{@code "%{message}%{metadata/ [/]}"}<br>
 *       Similar to the original Flogger format, but formats metadata more concisely.
 *   <li>{@code "%{key.task_id/Task=/: }%{message}%{metadata/ [/]}"}<br>
 *       As above, but moving a specific piece of metadata to be formatted before the message. Any
 *       metadata values formatted explicitly this way will not also be emitted via the {@code
 *       %{metadata}} directive.
 *   <li>{@code "%{message}"}<br>
 *       Formats just the log message, ignoring all metadata. This is only recommended if you are
 *       using something like Log4J, where metadata can be passed to the underlying logging system.
 *   <li>{@code "%{timestamp} [%{location}]%{level} %{message}%{metadata/ [/]}"}<br>
 *       Formats the log message and metadata with additional timestamp, location and level
 *       information. This is only recommended if you have configured the underlying logging system
 *       to omit this additional information.
 * </ul>
 */
public final class DefaultPatternFormatter extends LogMessageFormatter {
  private final MetadataExtractor metadataExtractor;
  private final LogMessageFormatter metadataFormatter;
  private final List<String> parts;
  private final List<BiConsumer<FormatContext, StringBuilder>> formatters;

  private static final Pattern ESCAPED_CHAR = Pattern.compile("\\\\(.)");

  // Note: Option names cannot contain:
  // '@', '$', ',', '/', '|', '\', '{', '}', '(', ')', '[', ']'
  // and non-key directives are a fixed set, so the <label> string does not need unescaping.
  // This gives the ability to add options directly in the label part (e.g. for time formats).
  private static final Pattern PATTERN_REGEX =
      Pattern.compile(
          "((?:[^%\\\\]|\\\\.)*)" // literal part
              + "%\\{"
              + "([^/}\\\\]+)" // <label> (no unescaping needed)
              + "(?:/((?:[^/\\\\}]|\\\\.)*)" // /<prefix>
              + "(?:/((?:[^/\\\\}]|\\\\.)*))?)?" // /<suffix>
              + "}");

  /** Returns a configured Flogger plugin for message formatting based on the given options. */
  public DefaultPatternFormatter(Options options) {
    String formatPattern = options.getString("pattern", "%{message}%{metadata/ [CONTEXT / ]}");

    // Options could be: raw, quote-if-string, escape-and-quote (JSON/HTML?)
    // * Single or double quotes.
    // * Escaping inner quotes.
    BiConsumer<StringBuilder, Object> valueAppender = JsonValueAppender.jsonAppender();

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
        formatter = (c, b) -> valueAppender.accept(b, c.getCustomMetadataValue(label.substring(4)));
        break;
    }
    // Only some directives have a prefix/suffix, but where it's not present these are empty.
    String prefix = unescape(m.group(3));
    String suffix = unescape(m.group(4));
    if (isOptionalDirective) {
      return allowPrefixAndSuffix(prefix, suffix, formatter);
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
    LogMessageFormatter formatter =
        PluginLoader.instantiate(
            LogMessageFormatter.class, options.getOptions(optionName), Map.of("default", newFn));
    return (c, b) -> formatter.append(c.getLogData(), c.getMetadata(), b);
  }

  private static BiConsumer<FormatContext, StringBuilder> allowPrefixAndSuffix(
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

  @Override
  public String format(LogData logData, MetadataProcessor metadata) {
    return super.format(logData, metadata);
  }

  @Override
  public StringBuilder append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
    FormatContext context = new FormatContext(logData, metadata);
    int partCount = formatters.size();
    for (int n = 0; n < partCount; n++) {
      buffer.append(parts.get(n));
      formatters.get(n).accept(context, buffer);
    }
    return buffer.append(parts.get(partCount));
  }

  /**
   * Context passed to formatting callbacks providing access to all runtime log data.
   *
   * <p>This approach allows long-lived, repeated use format directive callbacks to be created in
   * the pattern formatter and minimizes work during actual log message formatting.
   */
  private class FormatContext {
    private final LogData logData;
    // All user supplied metadata (context and log-site), including custom formatted values.
    private final MetadataProcessor metadata;
    // Extracted values for custom formatted metadata. Note that the keys of the returned map DO NOT
    // necessarily match the default labels of associated MetadataKeys (since a custom MetadataKey
    // can emit multiple values with distinct labels). This means you cannot lookup values directly
    // from the metadata in getCustomMetadataValue().
    private final Map<String, Object> customMetadataMap;

    FormatContext(LogData logData, MetadataProcessor metadata) {
      this.logData = requireNonNull(logData);
      this.metadata = requireNonNull(metadata);
      this.customMetadataMap = metadataExtractor.extractCustomMetadata(metadata);
    }

    LogData getLogData() {
      return logData;
    }

    MetadataProcessor getMetadata() {
      return metadata;
    }

    /** Returns the value of custom formatted metadata via the {@code %{key.<label>}} directive. */
    Object getCustomMetadataValue(String label) {
      return customMetadataMap.getOrDefault(label, null);
    }
  }
}
