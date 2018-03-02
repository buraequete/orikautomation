package com.buraequete.orikautomation.events;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MappingEventListener implements ApplicationListener<MappingEndEvent> {

	@Override
	public void onApplicationEvent(MappingEndEvent event) {
	}
}
