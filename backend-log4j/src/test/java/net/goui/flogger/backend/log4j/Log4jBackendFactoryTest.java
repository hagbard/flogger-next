package net.goui.flogger.backend.log4j;

import com.google.common.flogger.MetadataKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Log4jBackendFactoryTest {
  public static final class Key {
    public static final MetadataKey<String> FOO = MetadataKey.single("foo_key", String.class);
    public static final MetadataKey<String> BAR = MetadataKey.single("bar_key", String.class);
  }

  private static final Logger logger = LogManager.getLogger();

  @Test
  public void test() {
    logger.atInfo().log("Hello World");
  }
}
