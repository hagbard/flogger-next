package net.goui.flogger.backend.system;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.MetadataKey;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SystemBackendFactoryTest {
  public static final class Key {
    public static final MetadataKey<String> FOO = MetadataKey.single("foo", String.class);
    public static final MetadataKey<String> BAR = MetadataKey.single("bar", String.class);
  }

  @BeforeClass
  public static void setTestProperties() {
    System.setProperty(
        "java.util.logging.config.file",
        ClassLoader.getSystemResource("logging.properties").getPath());
  }

  @Test
  public void testLogging() {
    FluentLogger logger = FluentLogger.forEnclosingClass();
    logger.atInfo().every(100).with(Key.FOO, "<handled>").with(Key.BAR, "<default>").log("Hello World");
  }
}
