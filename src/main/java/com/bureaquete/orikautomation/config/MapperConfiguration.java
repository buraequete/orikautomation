package com.bureaquete.orikautomation.config;

import com.bureaquete.orikautomation.mapper.AutoBeanMapper;
import com.bureaquete.orikautomation.mapper.SimpleBeanMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfiguration {

	@Bean
	public AutoBeanMapper beanMapper() {
		return new AutoBeanMapper();
	}

	@Bean
	public SimpleBeanMapper simpleBeanMapper() {
		return new SimpleBeanMapper();
	}

	@Bean
	public ContextRefreshedEventListener contextRefreshedEventListener() {
		return new ContextRefreshedEventListener();
	}
}
