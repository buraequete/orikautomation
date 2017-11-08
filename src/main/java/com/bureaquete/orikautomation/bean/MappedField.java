package com.bureaquete.orikautomation.bean;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MappedField {

	Class<?> type;
	Class<?> genericType;
	MappedField parent;
	String name;
}