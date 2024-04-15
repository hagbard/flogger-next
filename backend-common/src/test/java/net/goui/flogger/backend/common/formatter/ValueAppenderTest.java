package net.goui.flogger.backend.common.formatter;

import static com.google.common.truth.Truth.assertThat;
import static net.goui.flogger.backend.common.formatter.ValueAppender.defaultAppender;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ValueAppenderTest {
  @Test
  public void testDefaultAppender_simple() {
    assertThat(defaultAppend(true)).isEqualTo("true");
    assertThat(defaultAppend(123)).isEqualTo("123");
    assertThat(defaultAppend(123.456)).isEqualTo("123.456");
    assertThat(defaultAppend("xyz")).isEqualTo("\"xyz\"");
    assertThat(defaultAppend("new\nline")).isEqualTo("\"new\\nline\"");

    // Don't append anything for null input.
    assertThat(defaultAppend(null)).isEqualTo("");
  }

  @Test
  public void testDefaultAppender_badToString() {
    Object unexpected =
        new Object() {
          @Override
          public String toString() {
            throw new RuntimeException("Spanish Inquisition");
          }
        };
    assertThat(defaultAppend(unexpected)).isEqualTo("\"Error: <Spanish Inquisition>\"");
  }

  static String defaultAppend(Object value) {
    StringBuilder out = new StringBuilder();
    defaultAppender().accept(out, value);
    return out.toString();
  }
}
