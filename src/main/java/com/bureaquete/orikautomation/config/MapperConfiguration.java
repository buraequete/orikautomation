package com.bureaquete.orikautomation.config;

import com.bureaquete.orikautomation.mapper.BeanMapper;
import org.springframework.context.annotation.Bean;

public class MapperConfiguration {

	@Bean
	public BeanMapper beanMapper() {
		return new BeanMapper();
	}

	@Bean
	public ContextRefreshedEventListener contextRefreshedEventListener() {
		return new ContextRefreshedEventListener();
	}
}
