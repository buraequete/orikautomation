package com.bureaquete.orikautomation.config;

import com.bureaquete.orikautomation.mapper.AutoBeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class ContextRefreshedEventListener implements ApplicationListener<ContextRefreshedEvent> {

	@Autowired
	private AutoBeanMapper beanMapper;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent cse) {
		beanMapper.init();
	}
}