package net.wuenschenswert.spring;

import java.util.Properties;

/**
 * 定义一个Properties的接口，这个接口定义了获取配置文件，增加和移除这个配置文件发生变更的时候需要通知的Listener
 * </p>
 * For Properties maps that notify about changes.
 * Would extend interface java.util.Properties if it were an interface.
 * Classes implementing this interface should consider extending {@link DelegatingProperties}.
 */
public interface ReloadableProperties {
  public Properties getProperties();
  void addReloadablePropertiesListener(ReloadablePropertiesListener l);
  boolean removeReloadablePropertiesListener(ReloadablePropertiesListener l);
}
