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

import static com.google.common.truth.Truth.assertThat;
import static net.goui.flogger.backend.common.formatter.JsonValueAppender.jsonAppender;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JsonValueAppenderTest {
  @Test
  public void testJsonAppender_simple() {
    assertThat(defaultAppend(true)).isEqualTo("true");
    assertThat(defaultAppend(123)).isEqualTo("123");
    assertThat(defaultAppend(123.456)).isEqualTo("123.456");
    assertThat(defaultAppend("xyz")).isEqualTo("\"xyz\"");
    assertThat(defaultAppend("new\nline")).isEqualTo("\"new\\nline\"");

    // Don't append anything for null input.
    assertThat(defaultAppend(null)).isEqualTo("");
  }

  @Test
  public void testJsonAppender_badToString() {
    Object unexpected =
        new Object() {
          @Override
          public String toString() {
            throw new IllegalStateException("Spanish Inquisition");
          }
        };
    String actual = defaultAppend(unexpected);
    assertThat(actual).contains("java.lang.IllegalStateException");
    assertThat(actual).contains("Spanish Inquisition");
  }

  static String defaultAppend(Object value) {
    StringBuilder out = new StringBuilder();
    jsonAppender().accept(out, value);
    return out.toString();
  }
}
