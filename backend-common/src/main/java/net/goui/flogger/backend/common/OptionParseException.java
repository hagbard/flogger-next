/*******************************************************************************
 * Copyright (c) 2024, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

package net.goui.flogger.backend.common;

public final class OptionParseException extends IllegalStateException {
  OptionParseException(String fqn, String type, String value, Throwable e) {
    this(String.format("cannot parse option %s as %s from '%s'", fqn, type, value), e);
  }

  OptionParseException(String message, Object... args) {
    super(String.format(message, args));
  }
}
