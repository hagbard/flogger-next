package net.goui.flogger.backend.log4j;

import static com.google.common.flogger.backend.MessageUtils.safeToString;
import static java.util.Objects.requireNonNull;

import java.util.Objects;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * A lightweight API for efficiently creating a list of values to use in an MDC.
 *
 * <p>This class is NOT thread safe during queue creation and ownership of the queue is assumed to
 * be taken by the Log4J log entry class as part of the MDC creation.
 */
final class ValueQueue {
  /**
   * Returns the concatenation for two values such that:
   *
   * <ul>
   *   <li>If {@code (existingValueOrQueue == null)}, {@code newValueOrQueue} is returned.
   *   <li>If {@code existingValueOrQueue} is not a {@code ValueQueue}, it forms the head of a new
   *       queue onto which the new values are concatenated.
   *   <li>Otherwise the new values are concatenated after the existing queue, which is returned.
   * </ul>
   *
   * <p>
   */
  public static Object concat(@NullableDecl Object existingValueOrQueue, Object newValueOrQueue) {
    requireNonNull(newValueOrQueue, "new value cannot be null");
    if (existingValueOrQueue == null) {
      return newValueOrQueue;
    }
    ValueQueue existingQueue = asQueue(existingValueOrQueue);
    existingQueue.tail = asQueue(newValueOrQueue);
    return existingQueue;
  }

  private final Object head;
  private ValueQueue tail;

  private ValueQueue(Object value) {
    this.head = value;
    this.tail = null;
  }

  private static ValueQueue asQueue(Object value) {
    return value instanceof ValueQueue ? (ValueQueue) value : new ValueQueue(value);
  }

  @Override
  public String toString() {
    if (tail == null) {
      return safeToString(head);
    }
    StringBuilder out = new StringBuilder("[");
    out.append(safeToString(head));
    ValueQueue nxt = tail;
    do {
      out.append(", ").append(safeToString(nxt.head));
      nxt = nxt.tail;
    } while (nxt != null);
    return out.append("]").toString();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ValueQueue) {
      ValueQueue lhs = this;
      ValueQueue rhs = (ValueQueue) other;
      while (Objects.equals(lhs.head, rhs.head)) {
        lhs = lhs.tail;
        rhs = rhs.tail;
        if (lhs == null || rhs == null) {
          return lhs == rhs;
        }
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(head, tail);
  }
}
