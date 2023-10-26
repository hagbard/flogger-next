package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.MetadataKey.KeyValueHandler;
import com.google.common.flogger.backend.MetadataHandler;
import com.google.common.flogger.backend.MetadataProcessor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.goui.flogger.backend.common.MessageFormatter;
import net.goui.flogger.backend.common.Options;
import net.goui.flogger.backend.common.RenameThisClass;

final class MetadataExtractor {
  private static final MetadataHandler.RepeatedValueHandler<Object, KeyCollector>
      REPEATED_VALUE_HANDLER = (k, v, c) -> k.safeEmitRepeated(v, c.getHandler(k));
  private static final MetadataHandler.ValueHandler<Object, KeyCollector> SINGLE_VALUE_HANDLER =
      (k, v, c) -> k.safeEmit(v, c.getHandler(k));

  private final MetadataHandler<KeyCollector> keyExtractor;
  private final MessageFormatter metadataFormatter;
  private final Map<MetadataKey<?>, Map<String, String>> labelTable;

  MetadataExtractor(Options options, Set<String> keyNames) {
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
    labelTable
        .keySet()
        .forEach(
            key -> {
              if (key.canRepeat()) {
                extractor.addRepeatedHandler(key, REPEATED_VALUE_HANDLER);
              } else {
                extractor.addHandler(key, SINGLE_VALUE_HANDLER);
              }
            });
    this.keyExtractor = extractor.build();

    List<MetadataKey<?>> explicitlyIgnoredKeys =
        options.getValueArray("ignore", RenameThisClass::loadMetadataKey);
    Set<MetadataKey<?>> allIgnoredKeys = new HashSet<>(explicitlyIgnoredKeys);
    allIgnoredKeys.addAll(labelTable.keySet());

    MetadataHandler<KeyValueHandler> handler =
        MetadataHandler.<KeyValueHandler>builder(MetadataKey::safeEmit)
            .setDefaultRepeatedHandler(MetadataKey::safeEmitRepeated)
            .ignoring(allIgnoredKeys)
            .build();
    this.metadataFormatter =
        (d, m, b) -> m.process(handler, (k, v) -> b.append(k).append('=').append(v).append(' '));
  }

  Map<String, Object> extractKeys(MetadataProcessor metadata) {
    KeyCollector collector = new KeyCollector();
    metadata.process(keyExtractor, collector);
    return collector.getKeyMap();
  }

  MessageFormatter getMetadataFormatter() {
    return metadataFormatter;
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
      // '<metadataKey#field>:<label>' OR 'tag:<label>'
      int idx = spec.indexOf(':');
      if (idx == -1) {
        throw new IllegalArgumentException(
            "invalid key specification (expected '<class#field>:<label>'): " + spec);
      }
      String keyField = spec.substring(0, idx);
      String label = spec.substring(idx + 1);
      return new KeySpec(label, RenameThisClass.loadMetadataKey(keyField));
    }

    private final String label;
    private final MetadataKey<?> metadataKey;

    KeySpec(String label, MetadataKey<?> metadataKey) {
      this.label = label;
      this.metadataKey = metadataKey;
    }

    String getLabel() {
      return label;
    }

    MetadataKey<?> getMetadataKey() {
      return metadataKey;
    }
  }
}
