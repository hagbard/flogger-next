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

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.function.Consumer;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * A simplified serializable Log4J {@link Message} instance which avoids using Log4J classes that
 * expect specific formatting.
 */
final class Log4jMessage implements Message, StringBuilderFormattable, Serializable {
  private static final Object[] EMPTY_ARGS = new Object[0];

  private static final long serialVersionUID = 1094571572809411166L;

  // Only used for asynchronous message handling.
  private volatile String cachedMessage = null;
  // This WILL be null if the instance is re-serialized but this is handled.
  private final transient Consumer<StringBuilder> formatter;

  // Looking at the Log4J FormattedMessage class, it appears that no attempt is ever made to
  // serialize the cause of a message. This is pretty poor since it changes the semantics of
  // the log event and could be very confusing. And, sadly, while the ThrownProxy exists, it's
  // not sufficient since it also doesn't reconstitute a Throwable instance after deserialization.
  //
  // Rather than attempting to serialize a cause if it happens to be serializable (which feels
  // like a potentially serious security issue) we just ignore it for serialization.
  private final transient Throwable thrown;

  Log4jMessage(Consumer<StringBuilder> formatter, Throwable thrown) {
    this.formatter = requireNonNull(formatter);
    this.thrown = thrown;
  }

  @Override
  public void formatTo(StringBuilder buffer) {
    // Local read of volatile field.
    String message = cachedMessage;
    if (message == null) {
      // Expected since StringBuilderFormattable is only *meant* to be used synchronously.
      formatter.accept(buffer);
    } else {
      buffer.append(message);
    }
  }

  // From various Log4J docs:
  // * "When configured to log asynchronously, this method is called before the Message is
  // queued"
  // * "The intention is that the Message implementation caches this formatted message and
  // returns it on subsequent calls. (See LOG4J2-763)"
  // * "When logging synchronously, this method WILL NOT BE CALLED for Messages that implement
  // the StringBuilderFormattable interface."
  @Override
  public String getFormattedMessage() {
    // Local read of volatile field.
    String message = cachedMessage;
    if (cachedMessage == null) {
      StringBuilder buffer = new StringBuilder();
      formatter.accept(buffer);
      cachedMessage = message = buffer.toString();
    }
    return message;
  }

  /**
   * Returns the empty string to indicate this {@link Message} has no concept of accessing the
   * unformatted message data.
   *
   * <p>{@code Message} is a bit of an awkwardly designed API, since it appears to be trying to
   * allow callers to use the unformatted message information, while simultaneously being agnostic
   * about which format is used. This is something Flogger explicitly avoids (since it inevitably
   * creates ambiguity and confusion).
   */
  @Override
  public String getFormat() {
    return "";
  }

  /**
   * Returns the empty string to indicate this {@link Message} has no concept of accessing the
   * unformatted message parameters.
   */
  @Override
  public Object[] getParameters() {
    return EMPTY_ARGS;
  }

  /**
   * Returns the cause associated with this {@link Message}.
   *
   * <p>WARNING: This is always {@code null} is the message is serialized, which appears to match
   * the behaviour of {@link org.apache.logging.log4j.message.FormattedMessage FormattedMessage}.
   */
  @Override
  public Throwable getThrowable() {
    return thrown;
  }

  private void readObject(java.io.ObjectInputStream in)
      throws java.io.IOException, ClassNotFoundException {
    // Reads instance with cached formatted message and no formatter or thrown cause.
    in.defaultReadObject();
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    // Triggers message to be cached before serialization.
    getFormattedMessage();
    out.defaultWriteObject();
  }
}
