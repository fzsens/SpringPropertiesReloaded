package net.wuenschenswert.spring;

import java.util.List;
import java.util.ArrayList;

public class ReloadConfiguration implements Runnable {
	List<ReconfigurableBean> reconfigurableBeans;

	public void setReconfigurableBeans(List reconfigurableBeans) {
		// early type check, and avoid aliassing
		this.reconfigurableBeans = new ArrayList<ReconfigurableBean>();
		for (Object o : reconfigurableBeans) {
			this.reconfigurableBeans.add((ReconfigurableBean) o);
		}
	}

	public void run() {
		for (ReconfigurableBean bean : reconfigurableBeans) {
			try {
				bean.reloadConfiguration();
			} catch (Exception e) {
				throw new RuntimeException("while reloading configuration of " + bean, e);
			}
		}
	}
}
