package com.bureaquete.orikautomation.bean;

import java.util.Objects;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MappedField {

	Class<?> type;
	Class<?> genericType;
	MappedField parent;
	String name;

	public String getName() {
		return (Objects.isNull(parent) ? "" : parent.name + ".") + this.name;
	}
}