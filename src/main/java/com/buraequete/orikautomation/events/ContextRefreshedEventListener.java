package com.buraequete.orikautomation.events;

import com.buraequete.orikautomation.mapper.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContextRefreshedEventListener implements ApplicationListener<ContextRefreshedEvent> {

	private final BeanMapper beanMapper;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent cse) {
		beanMapper.init();
	}
}
