/*******************************************************************************
 * Copyright (c) 2024, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

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
