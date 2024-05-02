/*******************************************************************************
 * Copyright (c) 2024, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

package net.goui.flogger.backend.system;

import com.google.common.flogger.FluentLogger;
import java.util.logging.Level;

class FloggerRootLogger {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static void log(Level level, String message) {
    logger.at(level).log("%s", message);
  }

  static boolean isEnabled(Level level) {
    return logger.at(level).isEnabled();
  }
}
