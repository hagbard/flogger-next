/*******************************************************************************
 * Copyright (c) 2024, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

package net.goui.flogger.backend.log4j;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.MetadataKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Log4jBackendFactoryTest {
  public static final class Key {
    public static final MetadataKey<String> FOO = MetadataKey.single("foo_key", String.class);
    public static final MetadataKey<String> BAR = MetadataKey.repeated("bar_key", String.class);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Test
  public void test() {
    logger
        .atInfo()
        .with(Key.FOO, "Greetings")
        .with(Key.BAR, "World")
        .with(Key.BAR, "Tour")
        .log("Hello");
  }
}
