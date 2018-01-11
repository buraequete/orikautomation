package com.buraequete.orikautomation.config;

import com.buraequete.orikautomation.mapper.BeanMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
