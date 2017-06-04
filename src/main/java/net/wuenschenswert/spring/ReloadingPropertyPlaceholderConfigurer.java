package net.wuenschenswert.spring;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.*;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.io.IOException;

/**
 * BeanDefinitions 解析配置属性
 *
 */
public class ReloadingPropertyPlaceholderConfigurer extends DefaultPropertyPlaceholderConfigurer
		implements InitializingBean, DisposableBean, ReloadablePropertiesListener, ApplicationContextAware {

	/** Default reloading placeholder prefix: "#{" */
	public static final String DEFAULT_RELOADING_PLACEHOLDER_PREFIX = "#{";

	/** Default reloading placeholder suffix: "}" */
	public static final String DEFAULT_RELOADING_PLACEHOLDER_SUFFIX = "}";

	// -- un-protect:
	// simulate the missing protected getter for some private superclass
	// properties we need to read here.

	private String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

	private String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

	private String beanName;

	private BeanFactory beanFactory;
	private Properties[] propertiesArray;

	@Override
	public void setProperties(Properties properties) {
		setPropertiesArray(new Properties[] { properties });
	}

	@Override
	public void setPropertiesArray(Properties[] propertiesArray) {
		this.propertiesArray = propertiesArray;
		super.setPropertiesArray(propertiesArray);
	}

	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.placeholderPrefix = placeholderPrefix;
		super.setPlaceholderPrefix(placeholderPrefix);
	}

	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.placeholderSuffix = placeholderSuffix;
		super.setPlaceholderSuffix(placeholderPrefix);
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
		super.setBeanName(beanName);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		super.setBeanFactory(beanFactory);
	}

	// --- end of un-protected

	/**
	 * the application context is needed to find the beans again during
	 * reconfiguration
	 */
	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	private String reloadingPlaceholderPrefix = ReloadingPropertyPlaceholderConfigurer.DEFAULT_RELOADING_PLACEHOLDER_PREFIX;

	private String reloadingPlaceholderSuffix = ReloadingPropertyPlaceholderConfigurer.DEFAULT_RELOADING_PLACEHOLDER_SUFFIX;

	public void setReloadingPlaceholderPrefix(String reloadingPlaceholderPrefix) {
		this.reloadingPlaceholderPrefix = reloadingPlaceholderPrefix;
	}

	public void setReloadingPlaceholderSuffix(String reloadingPlaceholderSuffix) {
		this.reloadingPlaceholderSuffix = reloadingPlaceholderSuffix;
	}

	/**
	 * BeanDefinition解析获取值
	 */
	protected String parseStringValue(String strVal, Properties props, Set visitedPlaceholders)
			throws BeanDefinitionStoreException {

		//对应本次解析的Spring Bean
		DynamicProperty dynamic = null;

		// replace reloading prefix and suffix by "normal" prefix and suffix.
		// remember all the "dynamic" placeholders encountered.
		StringBuffer buf = new StringBuffer(strVal);
		int startIndex = strVal.indexOf(this.reloadingPlaceholderPrefix);
		while (startIndex != -1) {
			int endIndex = buf.toString().indexOf(this.reloadingPlaceholderSuffix,
					startIndex + this.reloadingPlaceholderPrefix.length());
			if (endIndex != -1) {
				if (currentBeanName != null && currentPropertyName != null) {
					String placeholder = buf.substring(startIndex + this.placeholderPrefix.length(), endIndex);
					placeholder = getPlaceholder(placeholder);
					if (dynamic == null)
						dynamic = getDynamic(currentBeanName, currentPropertyName, strVal);
                    // 添加以来关系，内部通过HashMap placeholderToDynamics 维护关系
					addDependency(dynamic, placeholder);
				} else {
					logger.warn("dynamic property outside bean property value - ignored: " + strVal);
				}
				buf.replace(endIndex, endIndex + this.reloadingPlaceholderSuffix.length(), placeholderSuffix);
				buf.replace(startIndex, startIndex + this.reloadingPlaceholderPrefix.length(), placeholderPrefix);
				startIndex = endIndex - this.reloadingPlaceholderPrefix.length() + this.placeholderPrefix.length()
						+ this.placeholderSuffix.length();
				startIndex = strVal.indexOf(this.reloadingPlaceholderPrefix, startIndex);
			} else
				startIndex = -1;
		}
		// then, business as usual. no recursive reloading placeholders please.
		return super.parseStringValue(buf.toString(), props, visitedPlaceholders);
	}

	private DynamicProperty getDynamic(String currentBeanName, String currentPropertyName, String orgStrVal) {
		DynamicProperty dynamic = new DynamicProperty(currentBeanName, currentPropertyName, orgStrVal);
		DynamicProperty found = (DynamicProperty) dynamicProperties.get(dynamic);
		if (found != null)
			return found;
		dynamicProperties.put(dynamic, dynamic);
		return dynamic;
	}

	private Properties lastMergedProperties;

	protected Properties mergeProperties() throws IOException {
		Properties properties = super.mergeProperties();
		this.lastMergedProperties = properties;
		return properties;
	}

	/**
	 * 监听时间出发重新加载
	 */
	public void propertiesReloaded(PropertiesReloadedEvent event) {
		Properties oldProperties = lastMergedProperties;
		try {
			Properties newProperties = mergeProperties();
            //确定发生变化的Properties
			Set<String> placeholders = placeholderToDynamics.keySet();
			Set<DynamicProperty> allDynamics = new HashSet<DynamicProperty>();
			for (String placeholder : placeholders) {
				String newValue = newProperties.getProperty(placeholder);
				String oldValue = oldProperties.getProperty(placeholder);
				if (newValue != null && !newValue.equals(oldValue) || newValue == null && oldValue != null) {
					if (logger.isInfoEnabled())
						logger.info("Property changed detected: " + placeholder
								+ (newValue != null ? "=" + newValue : " removed"));
					List<DynamicProperty> affectedDynamics = placeholderToDynamics.get(placeholder);
					allDynamics.addAll(affectedDynamics);
				}
			}
			// sort affected bean properties by bean name and say hello.
			Map<String, List<DynamicProperty>> dynamicsByBeanName = new HashMap<String, List<DynamicProperty>>();
			Map<String, Object> beanByBeanName = new HashMap<String, Object>();
			for (DynamicProperty dynamic : allDynamics) {
				String beanName = dynamic.getBeanName();
				List<DynamicProperty> l = dynamicsByBeanName.get(beanName);
				if (l == null) {
					dynamicsByBeanName.put(beanName, (l = new ArrayList<DynamicProperty>()));
					Object bean = null;
					try {
						// 获取收到影响的bean
						bean = applicationContext.getBean(beanName);
						beanByBeanName.put(beanName, bean);
					} catch (BeansException e) {
						// keep dynamicsByBeanName list, warn only once.
						logger.error("Error obtaining bean " + beanName, e);
					}
					try {
                        // 前置切面
						if (bean instanceof ReconfigurationAware)
							((ReconfigurationAware) bean).beforeReconfiguration(); // hello!
					} catch (Exception e) {
						logger.error("Error calling beforeReconfiguration on " + beanName, e);
					}
				}
				l.add(dynamic);
			}
			// for all affected beans...
			Collection<String> beanNames = dynamicsByBeanName.keySet();
			for (String beanName : beanNames) {
				Object bean = beanByBeanName.get(beanName);
				if (bean == null) // problems obtaining bean, earlier
					continue;
                // Spring BeanWrapper
				BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
				// for all affected properties...
				List<DynamicProperty> dynamics = dynamicsByBeanName.get(beanName);
				for (DynamicProperty dynamic : dynamics) {
					String propertyName = dynamic.getPropertyName();
					String unparsedValue = dynamic.getUnparsedValue();

					// obtain an updated value, including dependencies
					String newValue;
					removeDynamic(dynamic);
					currentBeanName = beanName;
					currentPropertyName = propertyName;
					try {
						newValue = parseStringValue(unparsedValue, newProperties, new HashSet());
					} finally {
						currentBeanName = null;
						currentPropertyName = null;
					}
					if (logger.isInfoEnabled())
						logger.info("Updating property " + beanName + "." + propertyName + " to " + newValue);
					try {
                        //赋值
						beanWrapper.setPropertyValue(propertyName, newValue);
					} catch (BeansException e) {
						logger.error("Error setting property " + beanName + "." + propertyName + " to " + newValue, e);
					}
				}
			}
            // 后置切面
			for (String beanName : beanNames) {
				Object bean = beanByBeanName.get(beanName);
				try {
					if (bean instanceof ReconfigurationAware)
						((ReconfigurationAware) bean).afterReconfiguration();
				} catch (Exception e) {
					logger.error("Error calling afterReconfiguration on " + beanName, e);
				}
			}
		} catch (IOException e) {
			logger.error("Error trying to reload properties: " + e.getMessage(), e);
		}
	}

	/**
	 * 动态的属性
	 */
	static class DynamicProperty {
		final String beanName;
		final String propertyName;
		final String unparsedValue;
		List<String> placeholders = new ArrayList<String>();

		public DynamicProperty(String beanName, String propertyName, String unparsedValue) {
			this.beanName = beanName;
			this.propertyName = propertyName;
			this.unparsedValue = unparsedValue;
		}

		public void addPlaceholder(String placeholder) {
			placeholders.add(placeholder);
		}

		public String getUnparsedValue() {
			return unparsedValue;
		}

		public String getBeanName() {
			return beanName;
		}

		public String getPropertyName() {
			return propertyName;
		}

		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			final DynamicProperty that = (DynamicProperty) o;

			if (beanName != null ? !beanName.equals(that.beanName) : that.beanName != null)
				return false;
			if (propertyName != null ? !propertyName.equals(that.propertyName) : that.propertyName != null)
				return false;

			return true;
		}

		public int hashCode() {
			int result;
			result = (beanName != null ? beanName.hashCode() : 0);
			result = 29 * result + (propertyName != null ? propertyName.hashCode() : 0);
			return result;
		}
	}

	private Map<DynamicProperty, DynamicProperty> dynamicProperties = new HashMap<DynamicProperty, DynamicProperty>();

	/**
	 * map with placeholder and dynamicProperty set
	 */
	private Map<String, List<DynamicProperty>> placeholderToDynamics = new HashMap<String, List<DynamicProperty>>();

	/**
	 * 绑定DynamicProperty和placeHolder的关系
	 *
	 * @param dynamic
	 * @param placeholder
	 */
	private void addDependency(DynamicProperty dynamic, String placeholder) {
		List<DynamicProperty> l = placeholderToDynamics.get(placeholder);
		if (l == null) {
			l = new ArrayList<DynamicProperty>();
			placeholderToDynamics.put(placeholder, l);
		}
		if (!l.contains(dynamic))
			l.add(dynamic);
		dynamic.addPlaceholder(placeholder);
	}

	private void removeDynamic(DynamicProperty dynamic) {
		List<String> placeholders = dynamic.placeholders;
		for (String placeholder : placeholders) {
			List<DynamicProperty> l = placeholderToDynamics.get(placeholder);
			l.remove(dynamic);
		}
		dynamic.placeholders.clear();
		dynamicProperties.remove(dynamic);
	}

	private String currentBeanName;
	private String currentPropertyName;

	/** copy & paste, just so we can insert our own visitor. */
	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
			throws BeansException {

		BeanDefinitionVisitor visitor = new ReloadingPropertyPlaceholderConfigurer.PlaceholderResolvingBeanDefinitionVisitor(
				props);
		String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
		for (int i = 0; i < beanNames.length; i++) {
			// Check that we're not parsing our own bean definition,
			// to avoid failing on unresolvable placeholders in properties file
			// locations.
			if (!(beanNames[i].equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
				this.currentBeanName = beanNames[i];
				try {
					BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(beanNames[i]);
					try {
						visitor.visitBeanDefinition(bd);
					} catch (BeanDefinitionStoreException ex) {
						throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanNames[i],
								ex.getMessage());
					}
				} finally {
					currentBeanName = null;
				}
			}
		}
	}

	public void afterPropertiesSet() {
		for (Properties properties : propertiesArray) {
			if (properties instanceof ReloadableProperties) {
				((ReloadableProperties) properties).addReloadablePropertiesListener(this);
			}
		}
	}

	public void destroy() throws Exception {
		for (Properties properties : propertiesArray) {
			if (properties instanceof ReloadableProperties) {
				((ReloadableProperties) properties).removeReloadablePropertiesListener(this);
			}
		}
	}

	/**
	 *
	 */
	private class PlaceholderResolvingBeanDefinitionVisitor extends BeanDefinitionVisitor {

		private final Properties props;

		public PlaceholderResolvingBeanDefinitionVisitor(Properties props) {
			this.props = props;
		}

		protected void visitPropertyValues(MutablePropertyValues pvs) {
			PropertyValue[] pvArray = pvs.getPropertyValues();
			for (PropertyValue pv : pvArray) {
				currentPropertyName = pv.getName();
				try {
					Object newVal = resolveValue(pv.getValue());
					if (!ObjectUtils.nullSafeEquals(newVal, pv.getValue())) {
						pvs.addPropertyValue(pv.getName(), newVal);
					}
				} finally {
					currentPropertyName = null;
				}
			}
		}

		protected String resolveStringValue(String strVal) throws BeansException {
			return parseStringValue(strVal, this.props, new HashSet());
		}
	}
}
