package com.buraequete.orikautomation.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class MappingEventPublisher {

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	public void publishMappingEndEvent() {
		applicationEventPublisher.publishEvent(new MappingEndEvent(this));
	}
}
