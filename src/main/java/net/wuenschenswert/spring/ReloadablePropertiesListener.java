package net.wuenschenswert.spring;

/**
 * 定义配置文件更改监听接口
 *
 * @see PropertiesReloadedEvent
 */
public interface ReloadablePropertiesListener {
	/**
	 * @param event 配置文件更新事件
	 */
	void propertiesReloaded(PropertiesReloadedEvent event);
}
