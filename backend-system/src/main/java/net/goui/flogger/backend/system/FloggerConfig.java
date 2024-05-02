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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.LogManager;

/**
 * A configuration class which intercepts the loading of {@code logging.properties} file (wherever
 * it is located) and allows Flogger to inspect it to determine the system logging roots.
 *
 * <p>Install this class via the {@code java.util.logging.config.class} system property:
 *
 * <pre>{@code
 * -Djava.util.logging.config.class=net.goui.flogger.backend.system.FloggerConfig
 * }</pre>
 */
public final class FloggerConfig {
  private static final AtomicReference<List<String>> configuredRoots = new AtomicReference<>();

  /** Expected to be invoked exactly once by LogManager before getSystemRoots() is called. */
  public FloggerConfig() throws IOException {
    String configFileName = getConfigurationFileNameMatchingLogManager();
    try (BufferedInputStream cis = new BufferedInputStream(new FileInputStream(configFileName))) {
      cis.mark(Integer.MAX_VALUE);
      // Get any general issues out the way first.
      LogManager.getLogManager().readConfiguration(cis);
      cis.reset();
      initFloggerConfiguration(cis);
    }
  }

  /** Reads and sets the Flogger configuration from a logging properties file. */
  public static void initFloggerConfiguration(InputStream is) throws IOException {
    Properties allProperties = new Properties();
    allProperties.load(is);
    initFloggerConfiguration(allProperties);
  }

  /** Reads and sets the Flogger configuration from a logging properties object. */
  public static void initFloggerConfiguration(Properties properties) {
    List<String> roots =
        properties.stringPropertyNames().stream()
            .map(FloggerConfig::toLoggerRoot)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .sorted()
            .collect(toList());
    if (!configuredRoots.compareAndSet(null, roots)) {
      throw new IllegalStateException("Flogger config class already initialized");
    }
  }

  /** Called by {@link SystemBackendFactory} to set up initial Flogger configuration. */
  static List<String> getSystemRoots() {
    List<String> roots = configuredRoots.get();
    return roots != null ? roots : List.of();
  }

  private static Optional<String> toLoggerRoot(String name) {
    // Use the same heuristic as LogManager, looking for values ending in:
    // ".level", ".handlers", ".useParentHandlers"
    // However exclude any reference to the "global" and root ("") loggers.
    if (name.endsWith(".level")
        || name.endsWith(".handlers")
        || name.endsWith(".useParentHandlers")) {
      String maybeRoot = name.substring(0, name.lastIndexOf('.'));
      if (!maybeRoot.isEmpty()
          && !maybeRoot.equals("global")
          && FloggerConfig.isValidRootName(maybeRoot)) {
        return Optional.of(maybeRoot);
      }
    }
    return Optional.empty();
  }

  private static boolean isValidRootName(String name) {
    return stream(name.split("[.]", -1)).allMatch(FloggerConfig::isValidNamePart);
  }

  private static boolean isValidNamePart(String part) {
    return !part.isEmpty()
        && Character.isJavaIdentifierStart(part.codePointAt(0))
        && part.codePoints().skip(1).allMatch(Character::isJavaIdentifierPart);
  }

  private static String getConfigurationFileNameMatchingLogManager() {
    String fname = System.getProperty("java.util.logging.config.file");
    if (fname == null) {
      fname = System.getProperty("java.home");
      if (fname == null) {
        throw new Error("Can't find java.home ??");
      }
      fname =
          Paths.get(fname, "conf", "logging.properties").toAbsolutePath().normalize().toString();
    }
    return fname;
  }
}
