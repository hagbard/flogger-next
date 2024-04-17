package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.backend.MessageUtils;
import java.util.function.BiConsumer;

/** Simple helper to append metadata values during formatting is a JSON compatible way. */
final class JsonValueAppender {
  /** Returns an appender used by default formatting for metadata. */
  static BiConsumer<StringBuilder, Object> jsonAppender() {
    return JsonValueAppender::append;
  }

  private static void append(StringBuilder out, Object value) {
    if (value != null) {
      if (value instanceof Number || value instanceof Boolean) {
        out.append(value);
      } else {
        // safeToString() formats things like arrays better than String.valueOf().
        appendJsonEscape(out.append('"'), MessageUtils.safeToString(value)).append('"');
      }
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

  private JsonValueAppender() {}
}
