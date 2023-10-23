package net.goui.flogger.backend.common;

import java.lang.reflect.InvocationTargetException;

public class FloggerPlugin {
  public static <T> T instantiate(Class<T> targetClass, String className, Options options) {
    try {
      Class<?> clazz = Class.forName(className);
      if (!targetClass.isAssignableFrom(clazz)) {
        throw new RuntimeException(
            "Class '"
                + className
                + "' does not match expected Flogger plugin target type: "
                + targetClass.getName());
      }
      return targetClass.cast(clazz.getDeclaredConstructor(Options.class).newInstance(options));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("No class found for Flogger plugin: " + className, e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Exception initializing Flogger plugin: " + className, e);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(
          "Flogger plugin must have a public constructor: <init>("
              + Options.class.getSimpleName()
              + ")",
          e);
    }
  }
}
