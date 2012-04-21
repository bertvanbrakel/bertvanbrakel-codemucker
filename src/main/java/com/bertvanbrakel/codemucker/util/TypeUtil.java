package com.bertvanbrakel.codemucker.util;

import java.util.Collection;

import com.google.common.collect.ImmutableList;

public class TypeUtil {

	private static final Collection<String> PRIMITIVES = ImmutableList.of(
			"boolean"
			,"short"
			, "char"
			, "byte"
			, "int"
			, "float"
			, "double"
			, "String"
			, "java.lang.String"
			, "long"
	);
	
	public static boolean isPrimitive(String type){
		return PRIMITIVES.contains(type);
	}
	
	public static boolean typeValueRequiresSingleQuotes(String type){
		return type != null && "char".equals(type) || "java.lang.Character".equals(type);
	}
	
	public static boolean typeValueRequiresDoubleQuotes(String type){
		return type != null && "String".equals(type) || "java.lang.String".equals(type);
	}
	
}
