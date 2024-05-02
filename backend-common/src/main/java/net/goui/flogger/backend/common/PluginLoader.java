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

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;

/** Helper class to load and configure Flogger plugins via {@link Options}. */
public final class PluginLoader {
  public static final String DEFAULT_PLUGIN_NAME = "default";

  /**
   * Loads and instantiates a Flogger plugin via the given options. The specified target class is
   * required to have a public constructor which takes a single {@link Options} argument.
   *
   * @param targetClass the plugin API to be implemented.
   * @param options options passed to the plugin class's constructor.
   * @param defaultMap map of inbuilt plugin names to plugin providers.
   * @return the instantiated plugin.
   * @throws FloggerPluginException if the plugin could not be loaded.
   */
  public static <T> T instantiate(
      Class<T> targetClass, Options options, Map<String, Function<Options, T>> defaultMap) {
    String implName = options.getString("impl", DEFAULT_PLUGIN_NAME);
    Function<Options, T> implInit = defaultMap.get(implName);
    if (implInit != null) {
      return implInit.apply(options);
    }
    try {
      Class<?> clazz = Class.forName(implName);
      if (!targetClass.isAssignableFrom(clazz)) {
        throw new FloggerPluginException(
            "Class '"
                + implName
                + "' does not implement expected plugin type: "
                + targetClass.getName());
      }
      return targetClass.cast(clazz.getDeclaredConstructor(Options.class).newInstance(options));
    } catch (ClassNotFoundException e) {
      throw new FloggerPluginException("No class found for Flogger plugin: " + implName, e);
    } catch (InvocationTargetException e) {
      throw new FloggerPluginException("Exception initializing Flogger plugin: " + implName, e);
    } catch (ReflectiveOperationException e) {
      throw new FloggerPluginException(
          "Flogger plugin classes must have a public constructor: <init>("
              + Options.class.getSimpleName()
              + "): "
              + implName,
          e);
    }
  }

  public static final class FloggerPluginException extends RuntimeException {
    FloggerPluginException(String message) {
      super(message);
    }

    FloggerPluginException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private PluginLoader() {}
}
