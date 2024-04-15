package net.goui.flogger.backend.common;

import static com.google.common.truth.Truth.assertThat;
import static net.goui.flogger.backend.common.FloggerPlugin.DEFAULT_PLUGIN_NAME;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FloggerPluginTest {
  @Test
  public void instantiate_noOptionsDefault() {
    Options opts = Options.of(s -> null);
    Map<String, Function<Options, TestPlugin>> defaultPlugins =
        Map.of(DEFAULT_PLUGIN_NAME, DefaultPlugin::new);

    TestPlugin plugin = FloggerPlugin.instantiate(TestPlugin.class, opts, defaultPlugins);
    assertThat(plugin).isInstanceOf(TestPlugin.class);
  }

  @Test
  public void instantiate_explicitClassName() {
    ImmutableMap<String, String> opts = ImmutableMap.of("impl", ImplPlugin.class.getName());
    Map<String, Function<Options, TestPlugin>> defaultPlugins =
        Map.of(DEFAULT_PLUGIN_NAME, DefaultPlugin::new);

    TestPlugin plugin =
        FloggerPlugin.instantiate(TestPlugin.class, Options.of(opts::get), defaultPlugins);
    assertThat(plugin).isInstanceOf(ImplPlugin.class);
  }

  @Test
  public void instantiate_missingClass() {
    ImmutableMap<String, String> opts = ImmutableMap.of("impl", "no.such.Class");
    Map<String, Function<Options, TestPlugin>> defaultPlugins =
        Map.of(DEFAULT_PLUGIN_NAME, DefaultPlugin::new);

    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () ->
                FloggerPlugin.instantiate(TestPlugin.class, Options.of(opts::get), defaultPlugins));
    assertThat(e).hasMessageThat().contains("No class found");
    assertThat(e).hasMessageThat().contains("no.such.Class");
  }

  @Test
  public void instantiate_invalidClass() {
    ImmutableMap<String, String> opts = ImmutableMap.of("impl", NotAPlugin.class.getName());
    Map<String, Function<Options, TestPlugin>> defaultPlugins =
        Map.of(DEFAULT_PLUGIN_NAME, DefaultPlugin::new);

    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () ->
                FloggerPlugin.instantiate(TestPlugin.class, Options.of(opts::get), defaultPlugins));
    assertThat(e).hasMessageThat().contains("FloggerPluginTest$NotAPlugin");
    assertThat(e).hasMessageThat().contains("does not implement");
    assertThat(e).hasMessageThat().contains("FloggerPluginTest$TestPlugin");
  }

  @Test
  public void instantiate_missingConstructor() {
    ImmutableMap<String, String> opts = ImmutableMap.of("impl", NoMatchingConstructor.class.getName());
    Map<String, Function<Options, TestPlugin>> defaultPlugins =
        Map.of(DEFAULT_PLUGIN_NAME, DefaultPlugin::new);

    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () ->
                FloggerPlugin.instantiate(TestPlugin.class, Options.of(opts::get), defaultPlugins));
    assertThat(e).hasMessageThat().contains("must have a public constructor");
    assertThat(e).hasMessageThat().contains("<init>(Options)");
    assertThat(e).hasMessageThat().contains("FloggerPluginTest$NoMatchingConstructor");
  }

  @Test
  public void instantiate_kaboom() {
    ImmutableMap<String, String> opts = ImmutableMap.of("impl", InitGoBoom.class.getName());
    Map<String, Function<Options, TestPlugin>> defaultPlugins =
        Map.of(DEFAULT_PLUGIN_NAME, DefaultPlugin::new);

    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () ->
                FloggerPlugin.instantiate(TestPlugin.class, Options.of(opts::get), defaultPlugins));
    assertThat(e).hasMessageThat().contains("Exception initializing Flogger plugin");
    assertThat(e).hasMessageThat().contains("FloggerPluginTest$InitGoBoom");
  }

  static final class NotAPlugin {
    public NotAPlugin(Options opts) {}
  }

  interface TestPlugin {}

  static final class DefaultPlugin implements TestPlugin {
    public DefaultPlugin(Options opts) {}
  }

  static final class ImplPlugin implements TestPlugin {
    public ImplPlugin(Options opts) {}
  }

  static final class NoMatchingConstructor implements TestPlugin {
    public NoMatchingConstructor(String unused) {}
  }

  static final class InitGoBoom implements TestPlugin {
    public InitGoBoom(Options opts) {
      throw new RuntimeException("Kaboom!!");
    }
  }
}
