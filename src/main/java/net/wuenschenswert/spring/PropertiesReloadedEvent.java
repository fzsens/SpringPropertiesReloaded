package net.wuenschenswert.spring;

import java.util.Properties;

/**
 * event
 */
public class PropertiesReloadedEvent {
  final ReloadableProperties target;
  final Properties oldProperties;

  /**
   *
   * @param target ReloadableProperties
   * @param oldProperties oldProperties
   */
  public PropertiesReloadedEvent(ReloadableProperties target, Properties oldProperties) {
    this.target = target;
    this.oldProperties = oldProperties;
  }

  public ReloadableProperties getTarget() {
    return target;
  }

  public Properties getOldProperties() {
    return oldProperties;
  }
}
