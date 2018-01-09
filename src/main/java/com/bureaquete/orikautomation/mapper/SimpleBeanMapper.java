package com.bureaquete.orikautomation.mapper;

import ma.glasnost.orika.Converter;
import ma.glasnost.orika.Mapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SimpleBeanMapper extends ConfigurableMapper implements ApplicationContextAware {

	private MapperFactory factory;
	private ApplicationContext applicationContext;

	public SimpleBeanMapper() {
		super(false);
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void init() {
		super.init();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure(MapperFactory factory) {
		this.factory = factory;
		addAllSpringBeans(applicationContext);
	}

	protected void configureFactoryBuilder(DefaultMapperFactory.Builder factoryBuilder) {
		factoryBuilder.mapNulls(false).build();
	}

	private void addAllSpringBeans(final ApplicationContext applicationContext) {
		applicationContext.getBeansOfType(Mapper.class).values().forEach(this::addMapper);
		applicationContext.getBeansOfType(Converter.class).values().forEach(this::addConverter);
	}

	private <T, S> void addMapper(Mapper<T, S> mapper) {
		factory.classMap(mapper.getAType(), mapper.getBType()).byDefault().customize(mapper).register();
	}

	private <T, S> void addConverter(Converter<T, S> converter) {
		factory.getConverterFactory().registerConverter(converter);
	}
}
