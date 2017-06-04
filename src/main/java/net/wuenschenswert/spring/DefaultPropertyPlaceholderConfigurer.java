package net.wuenschenswert.spring;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.Properties;

/**
 * A property resource configurer that resolves placeholders with defaults in
 * bean property values of context definitions. It <i>pulls</i> values from a
 * properties file into bean definitions, using the default value if no value is
 * given in the properties file.
 *
 * <p>
 * The default syntax uses an equals sign ('=') to separate the placeholder name
 * from the default value. This syntax mimicks the syntax which would be used in
 * a property file.
 *
 * <p>
 * Example:
 *
 * <pre>
 * &lt;bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource"&gt;
 *   &lt;property name="driverClassName" value="${driver=com.mysql.jdbc.Driver}"/&gt;
 *   &lt;property name="url" value="jdbc:${dbname=mysql:mydb}"/&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>
 * Having a default value directly inside the bean definition file, at the place
 * where the property is set, avoids redundantly listing the placeholder names.
 * It is most useful for properties that usually keep their default value and
 * are rarely customized outside the bean definition file.
 *
 * <p>
 * The lookup order for properties and locations is exactly as described in
 * {@link PropertyPlaceholderConfigurer}. The default value is only used if none
 * of the specified properties and locations contains a value for the
 * placeholder. If no default value is specified, this class behaves exactly the
 * same as its superclass.
 *
 * @see PropertyPlaceholderConfigurer
 * @see #setDefaultSeparator
 */
public class DefaultPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

	public static final String DEFAULT_DEFAULT_SEPARATOR = "=";

	private String defaultSeparator = DEFAULT_DEFAULT_SEPARATOR;

	/**
	 * 设置默认参数分隔符号，默认为"="
	 *
	 * @see #DEFAULT_DEFAULT_SEPARATOR
	 */
	public void setDefaultSeparator(String defaultSeparator) {
		this.defaultSeparator = defaultSeparator;
	}

	/**
	 * 覆盖placeholder的替换方法
	 */
	@Override
	protected String resolvePlaceholder(String placeholderWithDefault, Properties props, int systemPropertiesMode) {
		String placeholder = getPlaceholder(placeholderWithDefault);
		String resolved = super.resolvePlaceholder(placeholder, props, systemPropertiesMode);
		if (resolved == null)
			resolved = getDefault(placeholderWithDefault);
		return resolved;
	}

	/**
	 *
	 * 从配置的placeholder中，获取真正的替换符
	 *
	 * @see #setPlaceholderPrefix
	 * @see #setDefaultSeparator
	 * @see #setPlaceholderSuffix
	 */
	protected String getPlaceholder(String placeholderWithDefault) {
		int separatorIdx = placeholderWithDefault.indexOf(defaultSeparator);
		if (separatorIdx == -1)
			return placeholderWithDefault;
		else
			return placeholderWithDefault.substring(0, separatorIdx);
	}

	/**
	 * 从配置的placeholder中，获取默认值
	 *
	 * @return the default value, or null if none is given.
	 * @see #setPlaceholderPrefix
	 * @see #setDefaultSeparator
	 * @see #setPlaceholderSuffix
	 */
	protected String getDefault(String placeholderWithDefault) {
		int separatorIdx = placeholderWithDefault.indexOf(defaultSeparator);
		if (separatorIdx == -1)
			return null;
		else
			return placeholderWithDefault.substring(separatorIdx + 1);
	}
}
