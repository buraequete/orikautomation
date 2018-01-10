package com.bureaquete.orikautomation.mapper;

import com.bureaquete.orikautomation.annotation.Mapped;
import com.bureaquete.orikautomation.bean.MappedField;
import com.google.common.collect.ArrayTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import ma.glasnost.orika.Converter;
import ma.glasnost.orika.Mapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.metadata.ClassMapBuilder;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;

public class AutoBeanMapper extends ConfigurableMapper implements ApplicationContextAware {

	private MapperFactory factory;
	private ApplicationContext applicationContext;
	private NormalizedLevenshtein comparator = new NormalizedLevenshtein();
	private boolean customMappersEnabled = false;

	public AutoBeanMapper() {
		super(false);
	}

	public AutoBeanMapper(boolean customMappersEnabled) {
		super(false);
		this.customMappersEnabled = customMappersEnabled;
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
		if (customMappersEnabled) {
			addAllSpringBeans(applicationContext);
		}
		applicationContext.getBeansWithAnnotation(Mapped.class).values().stream()
				.flatMap(bean -> Stream.of(bean.getClass().getSuperclass().getAnnotationsByType(Mapped.class)))
				.forEach(annotation -> setMapping(annotation.value()[0], annotation.value()[1]));
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

	private void setMapping(Class<?> classA, Class<?> classB) {
		ClassMapBuilder<?, ?> classMapBuilder = factory.classMap(classA, classB);
		Table<MappedField, MappedField, Double> table = ArrayTable.create(getAllFields(classA), getAllFields(classB));
		table.cellSet().forEach(cell -> table.put(Objects.requireNonNull(cell.getRowKey()),
				Objects.requireNonNull(cell.getColumnKey()), getSimilarity(cell.getRowKey(), cell.getColumnKey())));
		table.rowMap().forEach((a, rowSet) -> {
			MappedField b = rowSet.entrySet().stream().max(Comparator.comparingDouble(Map.Entry::getValue)).get().getKey();
			Map.Entry<MappedField, Double> maxA = table.column(b).entrySet().stream().max(Comparator.comparingDouble(Map.Entry::getValue)).get();
			if (maxA.getKey().equals(a) && maxA.getValue() * 2 > 1) {
				if (areTypesCompatible(a.getType(), b.getType())) {
					classMapBuilder.field(getFinalName(a), getFinalName(b));
					if (Objects.nonNull(a.getGenericType()) && Objects.nonNull(b.getGenericType())) {
						setMapping(a.getGenericType(), b.getGenericType());
					}
				} else if (a.getNested() || b.getNested()) {
					classMapBuilder.field(getFinalName(a), getFinalName(b));
					setMapping(a.getType(), b.getType());
				}
			}
		});
		classMapBuilder.register();
	}

	private Double getSimilarity(MappedField a, MappedField b) {
		return Stream.of(comparator.similarity(a.getName(), b.getName()), comparator.similarity(getFinalName(a), getFinalName(b)),
				comparator.similarity(a.getName(), getFinalName(b)), comparator.similarity(getFinalName(a), b.getName()))
				.max(Comparator.comparingDouble(Double::doubleValue)).get();
	}

	private List<MappedField> getAllFields(Class<?> clazz) {
		List<MappedField> fields = FieldUtils.getAllFieldsList(clazz).stream()
				.filter(field -> !field.isSynthetic() && !Modifier.isFinal(field.getModifiers()))
				.map(this::toMappedField)
				.collect(Collectors.toList());
		fields.addAll(FieldUtils.getAllFieldsList(clazz).stream()
				.filter(field -> !field.isSynthetic() && !Modifier.isFinal(field.getModifiers()) &&
								 !field.getType().isEnum() && getAllAncestors(clazz).contains(field.getType().getDeclaringClass()))
				.map(this::toMappedField)
				.flatMap(nestedField -> FieldUtils.getAllFieldsList(nestedField.getType()).stream()
						.filter(field -> !field.isSynthetic() && !clazz.equals(field.getType()))
						.map(field -> toMappedField(field).setParent(nestedField)))
				.collect(Collectors.toList()));
		return fields;
	}

	private String getFinalName(MappedField field) {
		return (Objects.isNull(field.getParent()) ? "" : field.getParent().getName() + ".") + field.getName();
	}

	private List<Class<?>> getAllAncestors(Class<?> clazz) {
		List<Class<?>> result = Lists.newArrayList();
		Class<?> temp = clazz;
		while (!temp.equals(Object.class)) {
			result.add(temp);
			temp = temp.getSuperclass();
		}
		return result;
	}

	private MappedField toMappedField(Field field) {
		return new MappedField()
				.setName(field.getName())
				.setType(field.getType())
				.setNested(isNested(field))
				.setGenericType(extractGenericType(field));
	}

	private boolean isNested(Field field) {
		return Stream.of(field.getAnnotationsByType(Valid.class)).count() > 0
			   || field.getDeclaringClass().equals(field.getType().getDeclaringClass());
	}

	private Class<?> extractGenericType(Field field) {
		if (field.getGenericType() instanceof ParameterizedType) {
			Type genericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
			if (!(genericType instanceof TypeVariableImpl)) {
				return (Class<?>) genericType;
			}
		}
		return null;
	}

	private boolean areTypesCompatible(Class<?> classA, Class<?> classB) {
		return classA.equals(classB) || (classA.isEnum() && classB.isEnum()) ||
			   Arrays.stream(classA.getInterfaces()).anyMatch(Arrays.asList(classB.getInterfaces())::contains);
	}
}
