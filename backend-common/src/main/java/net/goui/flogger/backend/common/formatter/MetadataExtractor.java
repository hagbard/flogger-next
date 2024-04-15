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

final class MetadataExtractor {
  private static final MetadataHandler.RepeatedValueHandler<Object, KeyCollector>
      REPEATED_VALUE_HANDLER = (k, v, c) -> k.safeEmitRepeated(v, c.getHandler(k));
  private static final MetadataHandler.ValueHandler<Object, KeyCollector> SINGLE_VALUE_HANDLER =
      (k, v, c) -> k.safeEmit(v, c.getHandler(k));

  private final MetadataHandler<KeyCollector> keyExtractor;
  private final LogMessageFormatter metadataFormatter;
  private final Map<MetadataKey<?>, Map<String, String>> labelTable;

  MetadataExtractor(
      Options options, Set<String> keyNames, BiConsumer<StringBuilder, Object> valueAppender) {
    this.labelTable = new IdentityHashMap<>();
    for (String keyName : keyNames) {
      KeySpec key =
          options
              .getValue("key." + keyName, KeySpec::parse)
              .orElseThrow(() -> new IllegalStateException("no such key: " + keyName));
      labelTable
          .computeIfAbsent(key.getMetadataKey(), k -> new HashMap<>())
          .put(key.getLabel(), keyName);
    }

    MetadataHandler.Builder<KeyCollector> extractor = MetadataHandler.builder((k, v, m) -> {});
    for (MetadataKey<?> key : labelTable.keySet()) {
      if (key.canRepeat()) {
        extractor.addRepeatedHandler(key, REPEATED_VALUE_HANDLER);
      } else {
        extractor.addHandler(key, SINGLE_VALUE_HANDLER);
      }
    }
    this.keyExtractor = extractor.build();

    List<MetadataKey<?>> explicitlyIgnoredKeys =
        options.getValueArray("ignore", MetadataKeyLoader::loadMetadataKey);
    Set<MetadataKey<?>> allIgnoredKeys = new HashSet<>(explicitlyIgnoredKeys);
    allIgnoredKeys.addAll(labelTable.keySet());

    MetadataHandler<KeyValueHandler> handler =
        MetadataHandler.<KeyValueHandler>builder(MetadataKey::safeEmit)
            .setDefaultRepeatedHandler(MetadataKey::safeEmitRepeated)
            .ignoring(allIgnoredKeys)
            .build();
    this.metadataFormatter = new MetadataFormatter(handler, valueAppender);
  }

  Map<String, Object> extractKeys(MetadataProcessor metadata) {
    KeyCollector collector = new KeyCollector();
    metadata.process(keyExtractor, collector);
    return collector.getKeyMap();
  }

  LogMessageFormatter getMetadataFormatter() {
    return metadataFormatter;
  }

  private static class MetadataFormatter extends LogMessageFormatter {
    private final MetadataHandler<KeyValueHandler> handler;
    private final BiConsumer<StringBuilder, Object> valueAppender;

    public MetadataFormatter(
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
      if (buffer.length() > start) {
        buffer.setLength(buffer.length() - 1);
      }
      return buffer;
    }
  }

  private class KeyCollector implements KeyValueHandler {
    private final Map<String, Object> collectedKeys = new HashMap<>();
    private Map<String, String> labelMap = null;

    KeyValueHandler getHandler(MetadataKey<?> key) {
      this.labelMap = labelTable.getOrDefault(key, Map.of());
      return this;
    }

    @Override
    public void handle(String emittedLabel, Object value) {
      String mappedLabel = labelMap.get(emittedLabel);
      if (mappedLabel != null) {
        collectedKeys.put(mappedLabel, value);
      }
    }

    public Map<String, Object> getKeyMap() {
      this.labelMap = null;
      return collectedKeys;
    }
  }

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
