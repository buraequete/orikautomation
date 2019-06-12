package com.buraequete.orikautomation.dto;

import java.util.List;
import lombok.Data;

@Data
public class MappedField {

	Class<?> type;
	Class<?> genericType;
	MappedField parent;
	Boolean nested;
	List<String> reference;
	String name;
}