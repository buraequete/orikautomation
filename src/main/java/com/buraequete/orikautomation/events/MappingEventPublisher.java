package com.buraequete.orikautomation.events;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MappingEventPublisher {

	private final ApplicationEventPublisher applicationEventPublisher;

	public void publishMappingEndEvent() {
		applicationEventPublisher.publishEvent(new MappingEndEvent(this));
	}
}
