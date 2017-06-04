package net.wuenschenswert.spring;

/**
 * 重新载入配置
 */
public interface ReconfigurableBean {
  void reloadConfiguration() throws Exception;
}
