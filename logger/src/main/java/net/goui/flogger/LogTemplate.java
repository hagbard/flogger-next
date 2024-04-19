package net.goui.flogger;

import com.google.common.flogger.LazyArg;
import java.util.ArrayList;
import java.util.List;

/** Adapter to efficiently unwrap {@link LazyArg} arguments in a {@link StringTemplate}. */
final class LogTemplate implements StringTemplate {
  private final List<String> fragments;
  private final List<Object> values;

  LogTemplate(StringTemplate delegate) {
    this.fragments = delegate.fragments();
    this.values = resolveLazyArgs(delegate.values());
  }

  private static List<Object> resolveLazyArgs(List<Object> values) {
    boolean hasLazyArgs = false;
    for (Object value : values) {
      if (value instanceof LazyArg<?>) {
        hasLazyArgs = true;
        break;
      }
    }
    if (!hasLazyArgs) {
      return values;
    }
    ArrayList<Object> resolved = new ArrayList<>(values.size());
    for (Object value : values) {
      resolved.add(value instanceof LazyArg ? ((LazyArg<?>) value).evaluate() : value);
    }
    return resolved;
  }

  @Override
  public List<String> fragments() {
    return fragments;
  }

  @Override
  public List<Object> values() {
    return values;
  }
}
