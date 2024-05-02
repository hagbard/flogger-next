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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.testing.FakeLogData;
import java.util.Locale;
import java.util.logging.Level;
import net.goui.flogger.backend.common.Options;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DefaultLevelFormatterTest {
  @Test
  public void testDefaultFormat() {
    DefaultLevelFormatter fmt = new DefaultLevelFormatter(Options.of(k -> null));
    FakeLogData data = FakeLogData.of("<message>").setLevel(Level.INFO);

    assertThat(fmt.format(data, noMetadata())).isEqualTo("INFO");
  }

  @Test
  public void testLocalizedName() {
    ImmutableMap<String, String> opts = ImmutableMap.of("use_localized_name", "true");
    DefaultLevelFormatter fmt = new DefaultLevelFormatter(Options.of(opts::get));
    FakeLogData data = FakeLogData.of("<message>").setLevel(Level.INFO);

    Locale oldDefault = Locale.getDefault();
    try {
      Locale.setDefault(Locale.GERMAN);
      assertThat(fmt.format(data, noMetadata())).isEqualTo("INFORMATION");
    } finally {
      Locale.setDefault(oldDefault);
    }
  }

  private static MetadataProcessor noMetadata() {
    return MetadataProcessor.forScopeAndLogSite(Metadata.empty(), Metadata.empty());
  }
}
