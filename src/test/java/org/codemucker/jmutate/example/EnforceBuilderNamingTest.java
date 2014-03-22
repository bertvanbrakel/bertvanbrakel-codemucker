package org.codemucker.jmutate.example;

import static org.codemucker.jmatch.Logical.all;
import static org.codemucker.jmutate.ast.matcher.AJMethod.isNotConstructor;
import static org.codemucker.jmutate.ast.matcher.AJMethod.withAccess;

import java.util.List;

import org.codemucker.jfind.Roots;
import org.codemucker.jmutate.ast.JAccess;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.ast.finder.Filter;
import org.codemucker.jmutate.ast.finder.FindResult;
import org.codemucker.jmutate.ast.finder.JSourceFinder;
import org.codemucker.jmutate.ast.matcher.AJMethod;
import org.codemucker.jmutate.ast.matcher.AJType;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;

public class EnforceBuilderNamingTest 
{
	@Test
	@Ignore
	public void testEnsureBuildersAreCorrectlyNamed()
	{
		Iterable<JType> builders = JSourceFinder.builder()
				.setSearchRoots(Roots.builder()
					.setIncludeMainSrcDir(true)
					.setIncludeTestSrcDir(true)
				)
				.setFilter(Filter.builder()
					.addIncludeTypes(AJType.withSimpleNameAntPattern("*Builder"))
					.addExcludeTypes(AJType.isAbstract())
				)
				.build()
				.findTypes();
		
		List<String> ignoreMethodsNamed = Lists.newArrayList("build","builder","copyOf");
		
		for (JType builder : builders) {			
			FindResult<JMethod> buildMethod = builder.findMethodsMatching(AJMethod.withNameMatchingAntPattern("build"));
			Assert.assertFalse("expect to find a build method on " + builder.getFullName(), buildMethod.isEmpty());
			//TODO:check return type of builder
			//TODO:check no args
			//TODO:check annotated?
			
			String builderTypeName = builder.getSimpleName();
			
			System.out.println("builder: " + builder.getFullName());
			
			for( JMethod method : builder.findAllJMethods().filter(all(withAccess(JAccess.PUBLIC),isNotConstructor()))){
				//System.out.println("method: " + method.getAstNode().getReturnType2() + " " +  method.getClashDetectionSignature());
				
				if( !ignoreMethodsNamed.contains(method.getName()) && !method.getName().startsWith("build")){
					//not getter?
					//has set in it?
					
//					if( !method.getName().startsWith("set") && !method.getName().startsWith("with") && !method.getName().startsWith("add")){
//						String msg = String.format("Expected builder method %s.%s to start with 'set'", method.getJType().getFullName(), method.getName());
//						Assert.fail(msg);		
//					}
//					
					if( !method.getAstNode().getReturnType2().toString().equals(builderTypeName)){
						String msg = String.format("Expected builder method %s.%s to return the enclosing builder '%s' but got '%s' for \n\n method \n%s\n in parent \n%s ", 
									method.getJType().getFullName(),
									method.getClashDetectionSignature(),
									builderTypeName,
									method.getAstNode().getReturnType2().toString(),
									method.getAstNode(),
									method.getType()
								);
						Assert.fail(msg);		
					}
					//TODO:check return type is builder!
				}
			}
		}		
	}
}
