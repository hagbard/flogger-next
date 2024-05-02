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

import com.google.common.flogger.LogSite;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataProcessor;
import net.goui.flogger.backend.common.Options;

/**
 * Flogger message formatter plugin for the {@code %{location}} directive to format the caller's log
 * site information.
 *
 * <h3>Options</h3>
 *
 * <p>None.
 */
final class DefaultLocationFormatter extends LogMessageFormatter {
  DefaultLocationFormatter(Options unused) {}

  @Override
  public StringBuilder append(
      LogData logData, MetadataProcessor metadataProcessor, StringBuilder buffer) {
    LogSite logSite = logData.getLogSite();
    buffer.append(logSite.getClassName()).append("#").append(logSite.getMethodName());
    return buffer;
  }
}
