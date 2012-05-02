package com.bertvanbrakel.codemucker.util;

import com.bertvanbrakel.test.util.ClassNameUtil;

public class BeanNameUtils {

	public static String toSetterNameIs(String name){
		return "is" + ClassNameUtil.upperFirstChar(name);
	}
	
	public static String toSetterName(String name){
		return "set" + ClassNameUtil.upperFirstChar(name);
	}
	
	public static String toGetterName(String name){
		return "get" + ClassNameUtil.upperFirstChar(name);
	}
	
}
