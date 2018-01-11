package com.buraequete.orikautomation.mapper;

import com.buraequete.orikautomation.annotation.Mapped;
import com.buraequete.orikautomation.annotation.MultiMapped;
import com.buraequete.orikautomation.bean.MappedField;
import com.google.common.collect.ArrayTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import ma.glasnost.orika.Mapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.metadata.ClassMapBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;

public class BeanMapper extends ConfigurableMapper implements ApplicationContextAware {

	private MapperFactory factory;
	private ApplicationContext applicationContext;
	private NormalizedLevenshtein comparator = new NormalizedLevenshtein();
	private static final Double threshold = 2D / 3D;

	private Map<Class, Class> customMapperMap = Maps.newHashMap();

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
		applicationContext.getBeansWithAnnotation(MultiMapped.class).values().stream()
				.flatMap(bean -> Stream.of(bean.getClass().getSuperclass().getAnnotationsByType(MultiMapped.class)[0].value()))
				.forEach(annotation -> setMapping(annotation.value()[0], annotation.value()[1]));
		applicationContext.getBeansWithAnnotation(Mapped.class).values().stream()
				.map(bean -> bean.getClass().getSuperclass().getAnnotationsByType(Mapped.class)[0])
				.forEach(annotation -> setMapping(annotation.value()[0], annotation.value()[1]));
	}

	private void addAllSpringBeans(final ApplicationContext applicationContext) {
		applicationContext.getBeansOfType(Mapper.class).values().forEach(this::addMapper);
	}

	private <T, S> void addMapper(Mapper<T, S> mapper) {
		customMapperMap.put(mapper.getAType().getRawType(), mapper.getBType().getRawType());
		customMapperMap.put(mapper.getBType().getRawType(), mapper.getAType().getRawType());
		factory.classMap(mapper.getAType(), mapper.getBType()).byDefault().customize(mapper).register();
	}

	private void setMapping(Class<?> classA, Class<?> classB) {
		ClassMapBuilder<?, ?> classMapBuilder = factory.classMap(classA, classB);
		Table<MappedField, MappedField, Double> table = ArrayTable.create(getAllFields(classA), getAllFields(classB));
		table.cellSet().forEach(cell -> table.put(Objects.requireNonNull(cell.getRowKey()),
				Objects.requireNonNull(cell.getColumnKey()),
				getSimilarity(cell.getRowKey(), cell.getColumnKey())));
		table.rowMap().forEach((a, rowSet) -> {
			MappedField b = rowSet.entrySet().stream().max(Comparator.comparingDouble(Map.Entry::getValue)).get().getKey();
			Map.Entry<MappedField, Double> maxA = table.column(b).entrySet().stream().max(Comparator.comparingDouble(Map.Entry::getValue)).get();
			if (maxA.getKey().equals(a) && maxA.getValue() > threshold) {
				if (areTypesCompatible(a.getType(), b.getType())) {
					classMapBuilder.field(getFinalName(a), getFinalName(b));
					if (Objects.nonNull(a.getGenericType()) && Objects.nonNull(b.getGenericType())) {
						setMapping(a.getGenericType(), b.getGenericType());
					}
				} else if (a.getNested() && b.getNested() && areNestedTypesCompatible(a.getType(), b.getType())) {
					classMapBuilder.field(getFinalName(a), getFinalName(b));
					setMapping(a.getType(), b.getType());
				}
			} else if (isCustomized(a.getType(), b.getType()) || isCustomized(a.getGenericType(), b.getGenericType())) {
				classMapBuilder.field(getFinalName(a), getFinalName(b));
			}
		});
		classMapBuilder.register();
	}

	private Double getSimilarity(MappedField a, MappedField b) {
		String nameA = a.getName();
		String nameB = b.getName();
		Double offset = isFragmentMatching(nameA, nameB) || isFragmentMatching(nameB, nameA) ? 1D / 2D : 0D;
		return Stream.of(comparator.similarity(nameA, nameB),
				comparator.similarity(StringUtils.capitalize(nameA), nameB),
				comparator.similarity(nameA, StringUtils.capitalize(nameB)),
				comparator.similarity(getFinalName(a), getFinalName(b)),
				comparator.similarity(nameA, getFinalName(b)),
				comparator.similarity(getFinalName(a), nameB))
					   .max(Comparator.comparingDouble(Double::doubleValue)).get() + offset;
	}

	private boolean isFragmentMatching(String a, String b) {
		return Stream.of(StringUtils.splitByCharacterTypeCamelCase(a)).anyMatch(b::equalsIgnoreCase);
	}

	private List<MappedField> getAllFields(Class<?> clazz) {
		List<Field> mainFieldList = FieldUtils.getAllFieldsList(clazz).stream()
				.filter(field -> !field.isSynthetic() && !Modifier.isFinal(field.getModifiers()))
				.collect(Collectors.toList());
		List<MappedField> fields = mainFieldList.stream().map(this::toMappedField).collect(Collectors.toList());
		List<MappedField> flattenedFields = mainFieldList.stream()
				.filter(field -> !field.getType().isEnum())
				.map(this::toMappedField)
				.filter(MappedField::getNested)
				.flatMap(nestedField -> FieldUtils.getAllFieldsList(nestedField.getType()).stream()
						.filter(field -> !field.isSynthetic() && !Modifier.isFinal(field.getModifiers()) && !clazz.equals(field.getType()))
						.map(field -> toMappedField(field).setParent(nestedField)))
				.collect(Collectors.toList());
		fields.addAll(flattenedFields);
		return fields;
	}

	private String getFinalName(MappedField field) {
		return (Objects.isNull(field.getParent()) ? "" : field.getParent().getName() + ".") + field.getName();
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
			   || getAllAncestors(field.getDeclaringClass()).contains(field.getType().getDeclaringClass());
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
		return classA.equals(classB) || (classA.isEnum() && classB.isEnum()) || isCustomized(classA, classB)
			   || Arrays.stream(classA.getInterfaces()).anyMatch(Arrays.asList(classB.getInterfaces())::contains);
	}

	private boolean areNestedTypesCompatible(Class<?> classA, Class<?> classB) {
		return Collection.class.isAssignableFrom(classA) == Collection.class.isAssignableFrom(classB);
	}

	private boolean isCustomized(Class<?> classA, Class<?> classB) {
		return Objects.nonNull(classA) && Objects.nonNull(classB) && classB.equals(customMapperMap.get(classA));
	}
}
