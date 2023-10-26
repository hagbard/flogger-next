package net.goui.flogger.backend.common;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class FloggerPlugin {
  public static <T> T instantiate(Class<T> targetClass, Options options, Map<String, Function<Options, T>> defaultMap) {
    String implName = options.getString("impl", "default");
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
                + "' does not match expected Flogger plugin target type: "
                + targetClass.getName());
      }
      return targetClass.cast(clazz.getDeclaredConstructor(Options.class).newInstance(options));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("No class found for Flogger plugin: " + implName, e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Exception initializing Flogger plugin: " + implName, e);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(
          "Flogger plugin must have a public constructor: <init>("
              + Options.class.getSimpleName()
              + ")",
          e);
    }
  }
}
