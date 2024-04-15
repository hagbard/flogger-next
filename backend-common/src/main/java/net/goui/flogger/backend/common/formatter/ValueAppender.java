package net.goui.flogger.backend.common.formatter;

import java.util.function.BiConsumer;

final class ValueAppender {

  static BiConsumer<StringBuilder, Object> defaultAppender() {
    return ValueAppender::appendDefault;
  }

  private static void appendDefault(StringBuilder out, Object value) {
    if (value != null) {
      if (value instanceof Number || value instanceof Boolean) {
        out.append(value);
      } else {
        appendJsonEscape(out.append('"'), toSafeString(value)).append('"');
      }
    }
  }

  private static String toSafeString(Object value) {
    try {
      return String.valueOf(value);
    } catch (RuntimeException e) {
      return "Error: <" + e.getLocalizedMessage() + ">";
    }
  }

  private static StringBuilder appendJsonEscape(StringBuilder out, String s) {
    int start = 0;
    int idx;
    for (idx = 0; idx < s.length(); idx++) {
      int escIdx = "\"\\\n\t\r".indexOf(s.charAt(idx));
      if (escIdx == -1) {
        continue;
      }
      out.append(s, start, idx).append('\\').append("\"\\ntr".charAt(escIdx));
      start = idx + 1;
    }
    out.append(s, start, idx);
    return out;
  }
}
