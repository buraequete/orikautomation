package com.buraequete.orikautomation.config;

import com.buraequete.orikautomation.events.MappingEventPublisher;
import com.buraequete.orikautomation.mapper.BeanMapper;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfiguration {

	@Bean
	public BeanMapper beanMapper(Optional<MappingEventPublisher> mappingEventPublisher) {
		return mappingEventPublisher.map(BeanMapper::new).orElseGet(BeanMapper::new);
	}
}
