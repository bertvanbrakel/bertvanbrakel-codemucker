package org.codemucker.jmutate.pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codemucker.jfind.FindResult;
import org.codemucker.jmatch.AnInt;
import org.codemucker.jmatch.Matcher;
import org.codemucker.jmutate.SourceFilter;
import org.codemucker.jmutate.TestSourceHelper;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.ast.matcher.AJMethod;
import org.codemucker.jmutate.ast.matcher.AJType;
import org.junit.Test;


public class BeanBuilderPatternFinderTest {

	private static final Logger LOG = LogManager.getLogger(BeanBuilderPatternFinderTest.class);
	
	@Test
	public void testFindBuilders() {
		//find classes with the name 'Builder' - confidence 90%
		//classes which subclass anything called 'builder' - confidence 80%
		//classes where most methods return itself - confidence 60%
		//classes which contain a method starting with 'build' - confidence 70%
		FindResult<JType> foundBuilders = TestSourceHelper.newSourceScannerAllSrcs()
			.filter(SourceFilter.with()
				//.addIncludeTypes(JTypeMatchers.withAnnotation(GenerateBuilder.class))
				//TODO:have matchers return confidences?? then finder can add that to results..
				.typeMatches(AJType.with().fullName("*Builder"))
				//.addIncludeTypesWithMethods(JMethodMatchers.withMethodNamed("build*"))
			)
			.build()
			.findTypes();
		
		//System.out.println("found potential builders:\n" + Joiner.on("\n================================================").join(foundBuilders) );
	
		for (JType type : foundBuilders) {
			LOG.trace( type.getFullName());
			
			//builds what???
			FindResult<JMethod> methods = type.findMethodsMatching(AJMethod.with().nameMatchingAntPattern("build*"));
			for (JMethod method : methods) {
				//could do checks on the build method here. COntains args? maybe not good?
				//return null? another warning
				//no build methods? another errors
				LOG.trace("\t--> " + method.getAstNode().getReturnType2());
			}	
		}
	}
	
	@Test
	public void testFindLongCtorClasses() {
		Matcher<JMethod> methodMatcher = AJMethod.that()
				.isConstructor()
				.numArgs(AnInt.greaterOrEqualTo(3));
		
		Iterable<JMethod> found = TestSourceHelper.newSourceScannerAllSrcs()
			.filter(SourceFilter.with()
				//.addIncludeTypes(JTypeMatchers.withAnnotation(GenerateBuilder.class))
				//TODO:have matchers return confidences?? then finder can add that to results..
				.typeMatches(AJType.with().method(methodMatcher))
				.methodMatches(methodMatcher)
				//.addIncludeTypesWithMethods(JMethodMatchers.withMethodNamed("build*"))
			)	
			.build()
			.findMethods();

		for (JMethod method : found) {
		
			LOG.trace( method.getAstNode().parameters().size());
			LOG.trace( " " + method.getClashDetectionSignature());	
		}
	}
	
	@Test
	public void testFindImmutableClasses() {
		//find classes where fields are not all final
		//where the fields are modifiable via public methods (or their call chains)
		 //(so find all caller of this method, and their callers etc...)
		// -->all references to this class, and a method call is made, object db?
	
	
	
	}
}
