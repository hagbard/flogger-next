package net.goui.flogger.backend.common.formatter;


import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.MetadataKey.KeyValueHandler;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataHandler;
import com.google.common.flogger.backend.MetadataProcessor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.goui.flogger.backend.common.MetadataKeyLoader;
import net.goui.flogger.backend.common.Options;

/**
 * A configurable metadata handler to extract metadata for formatting according to user supplied
 * options.
 */
final class MetadataExtractor {
  private static final MetadataHandler.RepeatedValueHandler<Object, CustomMetadataCollector>
      REPEATED_VALUE_HANDLER = (k, v, c) -> k.safeEmitRepeated(v, c.getHandler(k));
  private static final MetadataHandler.ValueHandler<Object, CustomMetadataCollector>
      SINGLE_VALUE_HANDLER = (k, v, c) -> k.safeEmit(v, c.getHandler(k));

  private final MetadataHandler<CustomMetadataCollector> customMetadataCollector;
  private final LogMessageFormatter metadataFormatter;
  private final Map<MetadataKey<?>, Map<String, String>> customMetadataLabels;

  /**
   * Returns a new extractor based on a format template.
   *
   * @param options options scoped to the 'metadata' namespace. These contain mappings for custom
   *     formatted keys and ignored keys.
   * @param keyNames a set of custom key names, extracted from the format pattern, to be custom
   *     formatted.
   * @param valueAppender a callback for appending metadata values (this may perform operations such
   *     as JSON escaping which are not the responsibility of this extractor class).
   */
  MetadataExtractor(
      Options options, Set<String> keyNames, BiConsumer<StringBuilder, Object> valueAppender) {
    this.customMetadataLabels = new IdentityHashMap<>();
    for (String keyName : keyNames) {
      KeySpec key =
          options
              .getValue("key." + keyName, KeySpec::parse)
              .orElseThrow(() -> new IllegalStateException("no such key: " + keyName));
      customMetadataLabels
          .computeIfAbsent(key.getMetadataKey(), k -> new HashMap<>())
          .put(key.getLabel(), keyName);
    }

    MetadataHandler.Builder<CustomMetadataCollector> extractor =
        MetadataHandler.builder((k, v, m) -> {});
    for (MetadataKey<?> key : customMetadataLabels.keySet()) {
      if (key.canRepeat()) {
        extractor.addRepeatedHandler(key, REPEATED_VALUE_HANDLER);
      } else {
        extractor.addHandler(key, SINGLE_VALUE_HANDLER);
      }
    }
    this.customMetadataCollector = extractor.build();

    List<MetadataKey<?>> explicitlyIgnoredKeys =
        options.getValueArray("ignore", MetadataKeyLoader::loadMetadataKey);
    Set<MetadataKey<?>> allIgnoredKeys = new HashSet<>(explicitlyIgnoredKeys);
    allIgnoredKeys.addAll(customMetadataLabels.keySet());

    MetadataHandler<KeyValueHandler> handler =
        MetadataHandler.<KeyValueHandler>builder(MetadataKey::safeEmit)
            .setDefaultRepeatedHandler(MetadataKey::safeEmitRepeated)
            .ignoring(allIgnoredKeys)
            .build();
    this.metadataFormatter = new MetadataFormatter(handler, valueAppender);
  }

  /** Extracts a map of label-to-value from the given metadata for custom formatting. */
  Map<String, Object> extractCustomMetadata(MetadataProcessor metadata) {
    CustomMetadataCollector collector = new CustomMetadataCollector();
    metadata.process(customMetadataCollector, collector);
    return collector.getKeyMap();
  }

  /** Returns the formatter for non-custom metadata. */
  LogMessageFormatter getMetadataFormatter() {
    return metadataFormatter;
  }

  /**
   * Formatter for non-custom and non-ignored metadata. This currently has no options and just
   * formats metadata in "encounter order" as {@code key=value} pairs separated by space. The caller
   * provides the handler which filters which metadata should be emitted.
   */
  private static class MetadataFormatter extends LogMessageFormatter {
    private final MetadataHandler<KeyValueHandler> handler;
    private final BiConsumer<StringBuilder, Object> valueAppender;

    MetadataFormatter(
        MetadataHandler<KeyValueHandler> handler, BiConsumer<StringBuilder, Object> valueAppender) {
      this.handler = handler;
      this.valueAppender = valueAppender;
    }

    @Override
    public StringBuilder append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
      int start = buffer.length();
      metadata.process(
          handler,
          (k, v) -> {
            buffer.append(k).append('=');
            valueAppender.accept(buffer, v);
            buffer.append(' ');
          });
      // Remove final trailing space if one or more values were appended.
      if (buffer.length() > start) {
        buffer.setLength(buffer.length() - 1);
      }
      return buffer;
    }
  }

  /** Mutable collector to extract the custom formatted metadata values. This is not thread safe. */
  private class CustomMetadataCollector implements KeyValueHandler {
    // The mapping from metadata formatting label (as defined by a %{key.<label>} directive)
    // to the associated metadata value. This is just a map, rather than a multimap, because format
    // labels must be unique.
    private final Map<String, Object> collectedKeys = new HashMap<>();
    // A disambiguation mapping from emitted metadata keys (which can be duplicated across different
    // metadata) to metadata formatting labels (which must be unique).
    //
    // This field is reset each time the handler emits values to the collector, which avoids
    // allocating many unused key value handlers during extraction.
    private Map<String, String> labelMap = null;

    /** Resets the disambiguation map for the given key, and return ourselves as the handler. */
    KeyValueHandler getHandler(MetadataKey<?> key) {
      this.labelMap = customMetadataLabels.getOrDefault(key, Map.of());
      return this;
    }

    @Override
    public void handle(String emittedLabel, Object value) {
      String mappedLabel = labelMap.get(emittedLabel);
      if (mappedLabel != null) {
        collectedKeys.put(mappedLabel, value);
      }
    }

    /** Finishes extraction by returning the collected values. */
    public Map<String, Object> getKeyMap() {
      this.labelMap = null;
      return collectedKeys;
    }
  }

  /**
   * Specifies a metadata value via its associated {@link MetadataKey} field in an arbitrary class,
   * which can be loaded and used for custom metadata formatting.
   *
   * <p>Note that while most metadata keys emit a single value with the label of that key, they can
   * also emit multiple values with different labels. For this case, a trailing label name can be
   * used to match the actual emitted value when it differs from the key's default label.
   */
  private static class KeySpec {
    static KeySpec parse(String spec) {
      // TODO: Support Tags too via special 'tag:<label>'
      // '<metadataKey#field>', '<metadataKey#field>:<label>'
      int labelIdx = spec.indexOf(':');
      String label = "";
      if (labelIdx >= 0) {
        label = spec.substring(labelIdx + 1);
        spec = spec.substring(0, labelIdx);
      }
      return new KeySpec(MetadataKeyLoader.loadMetadataKey(spec), label);
    }

    private final MetadataKey<?> metadataKey;
    private final String label;

    KeySpec(MetadataKey<?> metadataKey, String label) {
      this.metadataKey = metadataKey;
      this.label = label;
    }

    String getLabel() {
      return label.isEmpty() ? metadataKey.getLabel() : label;
    }

    MetadataKey<?> getMetadataKey() {
      return metadataKey;
    }
  }
}
