package com.bertvanbrakel.codemucker.ast;

import static com.bertvanbrakel.codemucker.ast.matcher.AMethod.withMethodNamed;
import static com.bertvanbrakel.codemucker.ast.matcher.AType.withFullName;
import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;

import com.bertvanbrakel.codemucker.ast.finder.Filter;
import com.bertvanbrakel.codemucker.ast.finder.FindResult;
import com.bertvanbrakel.codemucker.ast.finder.JSourceFinder;
import com.bertvanbrakel.codemucker.ast.finder.SearchPath;

public class JMethodTest {

	@Test
	public void test_clash_signature_generations(){
		//TODO:how to make this shorter?
		FindResult<JType> found = JSourceFinder.newBuilder()
			.setFilter(Filter.newBuilder().addIncludeTypes(withFullName(JMethodTest.class)))
			.setSearchPaths(SearchPath.newBuilder().setIncludeTestDir(true).setIncludeClassesDir(false))
			.build()
			.findTypes();
		JMethod method1 = found.toList().get(0).findMethodsMatching(withMethodNamed("signatureClassTestMethod")).toList().get(0);
		JMethod method2 = found.toList().get(0).findMethodsMatching(withMethodNamed("signatureClassTestMethod2")).toList().get(0);
		
		assertEquals("signatureClassTestMethod(java.lang.String,java.util.Collection)", method1.toClashDetectionSignature());
		assertEquals("signatureClassTestMethod2(java.lang.String[][],java.util.Collection,int)", method2.toClashDetectionSignature());
	}
	
	/**
	 * This is the method we will be looking for above and generating a signature for
	 */
	public Object signatureClassTestMethod(String bar,Collection<String> col){
		return null;
	}
	
	public Object signatureClassTestMethod2(String[][] bar,Collection<String> col, int foo){
		return null;
	}
}
