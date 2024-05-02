/*******************************************************************************
 * Copyright (c) 2024, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

package net.goui.flogger.backend.log4j;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Log4jMessageTest {
  @Test
  public void testMessage_simple() {
    Throwable cause = new RuntimeException();
    Log4jMessage message = new Log4jMessage(b -> b.append("FORMAT"), cause);

    StringBuilder buf = new StringBuilder();
    message.formatTo(buf);
    assertThat(buf.toString()).isEqualTo("FORMAT");

    assertThat(message.getFormattedMessage()).isEqualTo("FORMAT");
    assertThat(message.getThrowable()).isSameInstanceAs(cause);
    assertThat(message.getFormat()).isEqualTo("");
    assertThat(message.getParameters()).isEmpty();
  }

  @Test
  public void testMessage_caching() {
    // Sneaky test format function that emits the current call count.
    Consumer<StringBuilder> formatFn =
        new Consumer<>() {
          private int count = 0;

          @Override
          public void accept(StringBuilder buf) {
            buf.append("FORMATED: ").append(++count);
          }
        };

    Log4jMessage message = new Log4jMessage(formatFn, null);

    // Calling formatTo() to append the buffer does work every time.
    assertThat(manualFormatTo(message)).isEqualTo("FORMATED: 1");
    assertThat(manualFormatTo(message)).isEqualTo("FORMATED: 2");
    assertThat(manualFormatTo(message)).isEqualTo("FORMATED: 3");

    // Once getFormattedMessage() is called, the message is cached.
    assertThat(message.getFormattedMessage()).isEqualTo("FORMATED: 4");
    assertThat(message.getFormattedMessage()).isEqualTo("FORMATED: 4");

    // Even if we call formatTo() again.
    assertThat(manualFormatTo(message)).isEqualTo("FORMATED: 4");
  }

  private static String manualFormatTo(Log4jMessage message) {
    StringBuilder buf = new StringBuilder();
    message.formatTo(buf);
    return buf.toString();
  }

  @Test
  public void testSerialization() throws IOException, ClassNotFoundException {
    Throwable cause = new RuntimeException();
    Log4jMessage message = new Log4jMessage(b -> b.append("FORMAT"), cause);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (var os = new ObjectOutputStream(out)) {
      os.writeObject(message);
    }
    byte[] bytes = out.toByteArray();

    Log4jMessage copy;
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    try (var is = new ObjectInputStream(in)) {
      copy = (Log4jMessage) is.readObject();
    }

    assertThat(copy.getFormattedMessage()).isEqualTo("FORMAT");
    assertThat(copy.getThrowable()).isNull();
    assertThat(copy.getFormat()).isEqualTo("");
    assertThat(copy.getParameters()).isEmpty();
  }
}
