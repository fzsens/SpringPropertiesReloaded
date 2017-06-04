package net.wuenschenswert.spring.example;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 */
public class Main {
	public static void main(String[] args) throws IOException {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"net/wuenschenswert/spring/example/dynamic.xml");
		System.out.println(
				"change the file src/main/resources/net/wuenschenswert/spring/example/config.properties to trigger a reload... press return when satisfied.");
		MyBean myBean = (MyBean) applicationContext.getBean("mybean");
		System.out.println(myBean.getCachesize());
		System.in.read();
		applicationContext.close();
	}
}
