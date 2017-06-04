package net.wuenschenswert.spring;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

/**
 * 表示一个Reloadable配置文件实现类，内部包含有配置发生变化时候，需要调用的监听器
 *
 * @see DelegatingProperties
 *
 * @see ReloadablePropertiesListener
 *
 *      Useful base class for implementing {@link ReloadableProperties}.
 */
public class ReloadablePropertiesBase extends DelegatingProperties implements ReloadableProperties {

	/**
	 * properties发生变化的时候，需要通知的Listener
	 */
	private List<ReloadablePropertiesListener> listeners = new ArrayList<ReloadablePropertiesListener>();
	/**
	 * 内部的委托properties
	 */
	private Properties internalProperties;

	/**
	 * @param listeners listeners
	 */
	public void setListeners(List<ReloadablePropertiesListener> listeners) {
		this.listeners = listeners;
	}

	protected Properties getDelegate() {
		synchronized (this) {
			return internalProperties;
		}
	}

	public Properties getProperties() {
		return getDelegate();
	}

	public void addReloadablePropertiesListener(ReloadablePropertiesListener l) {
		listeners.add(l);
	}

	public boolean removeReloadablePropertiesListener(ReloadablePropertiesListener l) {
		return listeners.remove(l);
	}

	/**
	 * 通知监听器，properties 发生了变化
	 *
	 * @param oldProperties
	 */
	protected void notifyPropertiesChanged(Properties oldProperties) {
		PropertiesReloadedEvent event = new PropertiesReloadedEvent(this, oldProperties);
		for (ReloadablePropertiesListener listener : listeners) {
			listener.propertiesReloaded(event);
		}
	}

	/**
	 * @param properties 设置对象
	 */
	protected void setProperties(Properties properties) {
		Properties oldProperties = internalProperties;
		synchronized (this) {
			internalProperties = properties;
		}
		notifyPropertiesChanged(oldProperties);
	}
}
