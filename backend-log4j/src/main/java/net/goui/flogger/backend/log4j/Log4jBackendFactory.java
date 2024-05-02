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

import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.system.BackendFactory;
import java.util.List;
import net.goui.flogger.backend.common.AbstractBackendFactory;
import net.goui.flogger.backend.common.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.lookup.StrLookup;

/**
 * Service API providing Flogger Next integration with Log4j2.
 *
 * <p>This class is configured as a service API for {@link BackendFactory} via {@code
 * META-INF/services} and will be loaded automatically if it appears in the class path of an
 * application.
 *
 * <p>To force Flogger to use this class (e.g. if multiple service APIs for {@link BackendFactory}
 * exist), set the system property {@code flogger.backend_factory} to the fully qualified name of
 * this class.
 */
public class Log4jBackendFactory extends BackendFactory {
  // Explicit since this is a service API and called during Platform initialization.
  public Log4jBackendFactory() {
    // !! DO NOT INVOKE UNKNOWN CODE HERE !!
  }

  @Override
  public LoggerBackend create(String loggingClassName) {
    return LazyFactory.INSTANCE.create(loggingClassName);
  }

  static final class LazyFactory extends AbstractBackendFactory<Log4jBackend> {
    static final LazyFactory INSTANCE = new LazyFactory();

    LazyFactory() {
      super(loadOptions(), loadSystemRoots());
    }

    @Override
    protected Log4jBackend newBackend(
        String backendName, LogMessageFormatter formatter, Options options) {
      return new Log4jBackend(backendName, formatter);
    }

    private static List<String> loadSystemRoots() {
      return List.copyOf(
          ((LoggerContext) LogManager.getContext()).getConfiguration().getLoggers().keySet());
    }

    private static Options loadOptions() {
      // Must not call any code which might risk triggering reentrant Flogger logging.
      StrLookup properties =
          ((LoggerContext) LogManager.getContext())
              .getConfiguration()
              .getStrSubstitutor()
              .getVariableResolver();
      return Options.of(properties::lookup).getOptions("flogger");
    }
  }
}
