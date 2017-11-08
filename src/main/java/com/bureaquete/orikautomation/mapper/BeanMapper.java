package com.bureaquete.orikautomation.mapper;

import com.bureaquete.orikautomation.annotation.Mapped;
import com.bureaquete.orikautomation.bean.MappedField;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import ma.glasnost.orika.Converter;
import ma.glasnost.orika.Mapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.ClassMap;
import ma.glasnost.orika.metadata.ClassMapBuilder;
import ma.glasnost.orika.metadata.Type;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class BeanMapper extends ConfigurableMapper implements ApplicationContextAware {

	private MapperFactory factory;
	private ApplicationContext applicationContext;
	private NormalizedLevenshtein stringComparator = new NormalizedLevenshtein();

	public BeanMapper() {
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
		applicationContext.getBeansWithAnnotation(Mapped.class).values().stream()
				.map(bean -> bean.getClass().getSuperclass().getAnnotationsByType(Mapped.class)[0])
				.forEach(annotation -> setMapping(annotation.value()[0], annotation.value()[1]));
	}

	private void setMapping(Class<?> classA, Class<?> classB) {
		ClassMapBuilder<?, ?> classMapBuilder = factory.classMap(classA, classB);
		ClassMap<?, ?> classMap = classMapBuilder.toClassMap();
		List<MappedField> aMappings = classMap.getFieldsMapping().stream()
				.map(fm -> toMappedField(fm.getAType(), fm.getSourceExpression()))
				.collect(Collectors.toList());
		List<MappedField> bMappings = classMap.getFieldsMapping().stream()
				.map(fm -> toMappedField(fm.getBType(), fm.getDestinationExpression()))
				.collect(Collectors.toList());
		List<MappedField> aUnmapped = FieldUtils.getAllFieldsList(classA).stream()
				.filter(field -> !field.isSynthetic())
				.map(this::toMappedField)
				.filter(field -> !aMappings.contains(field))
				.collect(Collectors.toList());
		List<MappedField> bUnmapped = FieldUtils.getAllFieldsList(classB).stream()
				.filter(field -> !field.isSynthetic())
				.map(this::toMappedField)
				.filter(field -> !bMappings.contains(field))
				.collect(Collectors.toList());
		aUnmapped.forEach(a -> bUnmapped.stream()
				.collect(Collectors.toMap(b -> getSimilarity(a, b), Function.identity(), (pa, pb) -> pa))
				.entrySet().stream().sorted(Map.Entry.comparingByKey(Collections.reverseOrder()))
				.findFirst()
				.ifPresent(bEntry -> {
					if (bEntry.getKey() * 2 > 1) {
						MappedField b = bEntry.getValue();
						if (areTypesCompatible(a.getType(), b.getType())) {
							classMapBuilder.field(a.getName(), b.getName());
							if (a.getGenericType() != null && b.getGenericType() != null) {
								setMapping(a.getGenericType(), b.getGenericType());
							}
						} else {
							setMapping(a.getType(), b.getType());
						}
						bUnmapped.remove(b);
					} else {
						// get into lower layer
						bUnmapped.stream().flatMap(b -> Arrays.stream(FieldUtils.getAllFields(b.getType())).map(field -> toMappedField(field).setParent(b)))
								.collect(Collectors.toMap(b -> getSimilarity(a, b), Function.identity(), (pa, pb) -> pa))
								.entrySet().stream().sorted(Map.Entry.comparingByKey(Collections.reverseOrder()))
								.findFirst()
								.ifPresent(_bEntry -> {
									if (_bEntry.getKey() * 2 > 1) {
										MappedField b = _bEntry.getValue();
										if (a.getType().equals(b.getType())) {
											classMapBuilder.field(a.getName(), b.getParent().getName() + "." + b.getName());
										}
										bUnmapped.remove(b);
									}
								});
					}
				}));
		classMapBuilder.register();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configureFactoryBuilder(final DefaultMapperFactory.Builder factoryBuilder) {
		factoryBuilder.mapNulls(false);
	}

	/**
	 * Scans the application context and registers all Mappers and Converters found in it.
	 */
	@SuppressWarnings("rawtypes")
	private void addAllSpringBeans(final ApplicationContext applicationContext) {
		applicationContext.getBeansOfType(Mapper.class).values().forEach(this::addMapper);
		applicationContext.getBeansOfType(Converter.class).values().forEach(this::addConverter);
	}

	/**
	 * Constructs and registers a {@link ma.glasnost.orika.metadata.ClassMapBuilder} into the {@link MapperFactory} using a {@link Mapper}.
	 */
	@SuppressWarnings("rawtypes")
	private void addMapper(Mapper<?, ?> mapper) {
		factory.classMap(mapper.getAType(), mapper.getBType())
				.byDefault()
				.customize((Mapper) mapper)
				.register();
	}

	/**
	 * Registers a {@link Converter} into the {@link ma.glasnost.orika.converter.ConverterFactory}.
	 */
	private void addConverter(Converter<?, ?> converter) {
		factory.getConverterFactory().registerConverter(converter);
	}

	private Double getSimilarity(MappedField pA, MappedField pB) {
		return stringComparator.similarity(pA.getName(), pB.getName());
	}

	private MappedField toMappedField(Field field) {
		return new MappedField().setName(field.getName()).setType(field.getType())
				.setGenericType(field.getGenericType() instanceof ParameterizedType ? ((Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]) : null);
	}

	private MappedField toMappedField(Type type, String expression) {
		return new MappedField().setName(expression)
				.setType(type.getRawType())
				.setGenericType(type.getComponentType() != null ? type.getComponentType().getRawType() : null);
	}

	private boolean areTypesCompatible(Class<?> classA, Class<?> classB) {
		return classA.equals(classB) || Arrays.stream(classA.getInterfaces()).filter(Arrays.asList(classB.getInterfaces())::contains).count() > 0;
	}
}
