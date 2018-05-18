package com.buraequete.orikautomation.bean;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MappedField {

	Class<?> type;
	Class<?> genericType;
	MappedField parent;
	Boolean nested;
	List<String> reference;
	String name;
}