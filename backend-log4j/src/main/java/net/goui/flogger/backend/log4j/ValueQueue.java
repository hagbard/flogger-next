package net.goui.flogger.backend.log4j;

import static java.util.Objects.requireNonNull;

import com.google.common.flogger.backend.MessageUtils;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 */
public final class ValueQueue {
  // Since the number of elements is almost never above 1 or 2, a LinkedList saves space.
  private final List<Object> values = new LinkedList<>();

  public ValueQueue() {}

  private ValueQueue(Object item) {
    values.add(item);
  }

  public static Object concat(@NullableDecl Object existingValue, Object value) {
    requireNonNull(value, "value");
    if (existingValue == null) {
      return value;
    } else {
      // This should only rarely happen, so a few small allocations seems acceptable.
      ValueQueue existingQueue =
          existingValue instanceof ValueQueue
              ? (ValueQueue) existingValue
              : new ValueQueue(existingValue);
      if (value instanceof ValueQueue) {
        existingQueue.values.addAll(((ValueQueue) value).values);
      } else {
        existingQueue.values.add(value);
      }
      return existingQueue;
    }
  }

  /**
   * Returns a string representation of the contents of the specified value queue.
   *
   * <ul>
   *   <li>If the value queue is empty, the method returns an empty string.
   *   <li>If the value queue contains a single element {@code a}, this method returns {@code
   *       a.toString()}.
   *   <li>Otherwise, the contents of the queue are formatted like a {@code List}.
   * </ul>
   */
  @Override
  public String toString() {
    if (values.isEmpty()) {
      return "";
    }
    if (values.size() == 1) {
      return MessageUtils.safeToString(values.get(0));
    }
    return MessageUtils.safeToString(values);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValueQueue that = (ValueQueue) o;
    return values.equals(that.values);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(values);
  }
}
