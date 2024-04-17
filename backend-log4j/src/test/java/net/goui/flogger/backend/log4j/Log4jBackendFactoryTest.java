package net.goui.flogger.backend.log4j;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.MetadataKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Log4jBackendFactoryTest {
  public static final class Keys {
    public static final MetadataKey<String> FOO = MetadataKey.single("foo_key", String.class);
    public static final MetadataKey<String> BAR = MetadataKey.repeated("bar_key", String.class);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Test
  public void test() {
    logger.atInfo().with(Keys.FOO, "Greetings").with(Keys.BAR, "World").with(Keys.BAR, "Tour").log("Hello");
  }
}
