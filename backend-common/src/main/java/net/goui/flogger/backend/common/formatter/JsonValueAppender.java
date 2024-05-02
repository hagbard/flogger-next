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
