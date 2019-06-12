package com.buraequete.orikautomation.config;

import com.buraequete.orikautomation.events.ContextRefreshedEventListener;
import com.buraequete.orikautomation.events.MappingEventPublisher;
import com.buraequete.orikautomation.mapper.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class EventConfiguration {

	@Bean
	public ContextRefreshedEventListener contextRefreshedEventListener(BeanMapper beanMapper) {
		return new ContextRefreshedEventListener(beanMapper);
	}

	@Bean
	public MappingEventPublisher mappingEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		return new MappingEventPublisher(applicationEventPublisher);
	}
}
