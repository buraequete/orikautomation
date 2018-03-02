package com.buraequete.orikautomation.events;

import org.springframework.context.ApplicationEvent;

public class MappingEndEvent extends ApplicationEvent {

	public MappingEndEvent(Object source) {
		super(source);
	}
}
