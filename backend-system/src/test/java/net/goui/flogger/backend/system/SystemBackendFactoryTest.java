package net.goui.flogger.backend.system;

import com.google.common.flogger.FluentLogger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SystemBackendFactoryTest {
  @BeforeClass
  public static void setTestProperties() {
    System.setProperty(
        "java.util.logging.config.file",
        ClassLoader.getSystemResource("logging.properties").getPath());
  }

  @Test
  public void test() {
    FluentLogger logger = FluentLogger.forEnclosingClass();
    logger.atInfo().every(100).log("Should skip metadata");
  }

}
