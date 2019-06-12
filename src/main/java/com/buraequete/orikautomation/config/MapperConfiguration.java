package com.buraequete.orikautomation.config;

import com.buraequete.orikautomation.events.MappingEventPublisher;
import com.buraequete.orikautomation.mapper.BeanMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
@PropertySource("classpath:application.yml")
public class MapperConfiguration {

	private static final String publishEventFlag = "orikautomation.publishMappingEndEvent";
	private final Environment env;

	@Bean
	public BeanMapper beanMapper(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<MappingEventPublisher> mappingEventPublisher) {
		return getPublishEventFlag()
				? mappingEventPublisher.map(BeanMapper::new).orElseGet(BeanMapper::new)
				: new BeanMapper();
	}

	private Boolean getPublishEventFlag() {
		return Optional.ofNullable(env.getProperty(publishEventFlag)).map(Boolean::new).orElse(Boolean.FALSE);
	}
}
