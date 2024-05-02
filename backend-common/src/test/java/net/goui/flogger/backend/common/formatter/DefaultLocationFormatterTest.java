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
import static org.junit.Assert.*;

import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.testing.FakeLogData;
import com.google.common.flogger.testing.FakeLogSite;
import net.goui.flogger.backend.common.Options;
import org.junit.Test;

public class DefaultLocationFormatterTest {
  @Test
  public void testDefaultFormat() {
    DefaultLocationFormatter fmt = new DefaultLocationFormatter(Options.of(k -> null));
    FakeLogData data =
        FakeLogData.of("<message>")
            .setLogSite(FakeLogSite.create("com.foo.bar.Class", "someMethod", 123, "<unused>"));

    assertThat(fmt.format(data, noMetadata())).isEqualTo("com.foo.bar.Class#someMethod");
  }

  private static MetadataProcessor noMetadata() {
    return MetadataProcessor.forScopeAndLogSite(Metadata.empty(), Metadata.empty());
  }
}
