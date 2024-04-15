package net.goui.flogger.backend.common;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;

public final class FloggerPlugin {
  public static final String DEFAULT_PLUGIN_NAME = "default";

  public static <T> T instantiate(Class<T> targetClass, Options options, Map<String, Function<Options, T>> defaultMap) {
    String implName = options.getString("impl", DEFAULT_PLUGIN_NAME);
    Function<Options, T> implInit = defaultMap.get(implName);
    if (implInit != null) {
      return implInit.apply(options);
    }
    try {
      Class<?> clazz = Class.forName(implName);
      if (!targetClass.isAssignableFrom(clazz)) {
        throw new RuntimeException(
            "Class '"
                + implName
                + "' does not implement expected plugin type: "
                + targetClass.getName());
      }
      return targetClass.cast(clazz.getDeclaredConstructor(Options.class).newInstance(options));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("No class found for Flogger plugin: " + implName, e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Exception initializing Flogger plugin: " + implName, e);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(
          "Flogger plugin classes must have a public constructor: <init>("
              + Options.class.getSimpleName()
              + "): "
              + implName,
          e);
    }
  }
}
