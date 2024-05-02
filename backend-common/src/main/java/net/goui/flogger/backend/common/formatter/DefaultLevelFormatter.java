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

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataProcessor;
import java.util.logging.Level;
import net.goui.flogger.backend.common.Options;

/**
 * Flogger message formatter plugin for the {@code %{level}} directive to format the log level.
 *
 * <h3>Options</h3>
 *
 * <ul>
 *   <li>{@code flogger.message_formatter.level.use_localized_name}: Boolean<br>
 *       Formats the log level using its localized name according to the current locale.
 * </ul>
 */
final class DefaultLevelFormatter extends LogMessageFormatter {
  private final boolean useLocalizedName;

  DefaultLevelFormatter(Options options) {
    this.useLocalizedName = options.getBoolean("use_localized_name", false);
  }

  @Override
  public StringBuilder append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
    Level level = logData.getLevel();
    buffer.append(useLocalizedName ? level.getLocalizedName() : level.getName());
    return buffer;
  }
}
