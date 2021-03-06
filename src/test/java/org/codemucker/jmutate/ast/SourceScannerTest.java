package org.codemucker.jmutate.ast;

import static org.codemucker.jmatch.Assert.assertThat;
import static org.codemucker.jmatch.Assert.is;
import static org.codemucker.jmatch.Assert.isFalse;

import java.util.List;

import org.codemucker.jfind.FindResult;
import org.codemucker.jfind.MatchListener;
import org.codemucker.jmatch.AList;
import org.codemucker.jmatch.Expect;
import org.codemucker.jmutate.SourceFilter;
import org.codemucker.jmutate.SourceScanner;
import org.codemucker.jmutate.TestSourceHelper;
import org.codemucker.jmutate.ast.matcher.AJMethod;
import org.codemucker.jmutate.ast.matcher.AJType;
import org.junit.Test;

public class SourceScannerTest {

	@Test
	public void testFindClassesWithMethodMatch() throws Exception {
		SourceScanner scanner = TestSourceHelper.newSourceScannerAllSrcs()
			.filter(SourceFilter.where()
				.typeMatches(AJType.with().method(AJMethod.with().nameMatchingAntPattern("testFindClassesWithMethodMatch")))
			)
			.build();
		JType type = scanner.findTypes().getFirst();
		
		assertThat(type, is(AJType.with().name(SourceScannerTest.class)));
	}
	
	@Test
	public void testFindClassesExtending() throws Exception {
		SourceScanner scanner = TestSourceHelper.newSourceScannerAllSrcs()
			.filter(SourceFilter.with()
				.typeMatches(AJType.with().isASubclassOf(MyClass.class)))
			.build();

		Expect
			.that(scanner.findTypes())
			.is(AList.of(JType.class)
				.inAnyOrder()
				.withOnly()
				.item(AJType.with().name(MySubClass.class))
				.item(AJType.with().name(MySubSubClass.class))
				.item(AJType.with().name(org.codemucker.jmutate.ast.SourceScannerTest.SomeClass.SomeEmbeddedSubClass.class)));
	}
	
	@Test
	public void testFindClassesWithAnnotations() throws Exception {
		SourceScanner finder = TestSourceHelper.newSourceScannerAllSrcs()
			.filter(SourceFilter.with()
				.typeMatches(AJType.with().annotation(MyAnnotation.class))
			)
			.listener(new MatchListener<Object>() {
				@Override
				public void onMatched(Object result) {
				}
				
				@Override
				public void onError(Object record, Throwable t) throws Throwable {					
				}

				@Override
				public void onIgnored(Object result) {
					if( result instanceof JType){
						
						JType src = (JType)result;
						
						if( src.getFullName().contains("ClassWithAnnotation")){
							System.out.println("ignored=" + src.getFullName());
							List<JAnnotation> as = src.getAnnotations().getAllDirect();
							for(JAnnotation a:as){
								System.out.println("a=" + a.getAstNode());
							}
							System.out.println("getAnnotation=" + src.getAnnotations().getOrNull(MyAnnotation.class));
							System.out.println("hasAnnotation=" + src.getAnnotations().contains(MyAnnotation.class));
						}
					}
				}
			})
			.build();
		/*boolean found = false;
		List<JType> foundTypes = finder.findTypes().toList();
		*/
		Expect
		    .with().debugEnabled(true)
			.that(finder.findTypes())
			.is(AList.of(JType.class)
				.inAnyOrder()
				.withOnly()
				.item(AJType.with().name(ClassWithAnnotation.class)));
			
		/*for( JType type:foundTypes){
			assertThat(type.getSimpleName(), is(AString.equalTo(ClassWithAnnotation.class.getSimpleName())));
			boolean hasAnon = type.hasAnnotationOfType(MyAnnotation.class,true);
			
			assertThat(hasAnon,isTrue());
			found = true;
		}
		assertThat(foundTypes.size(),is(AnInt.equalTo(1)));
		assertThat(found,isTrue());
		*/
	}
	
	@Test
	public void testFindWithMethods(){
		SourceScanner finder = TestSourceHelper.newSourceScannerAllSrcs().build();
		
		FindResult<JMethod> methods = finder.findMethods();
		assertThat(methods.isEmpty(),isFalse());
	}

	public static @interface MyAnnotation {

	}

	@MyAnnotation
	private static class ClassWithAnnotation {

	}

	private static class ClassWithNoAnnotation {

	}

	private static class MyClass {
	}
	
	private static class MySubClass extends MyClass {
	}
	
	private static class MySubSubClass extends MySubClass {
	}
	
	private static class SomeClass {
		public static class SomeEmbeddedSubClass extends MySubClass {
			
		}
		
		public static class IgnoreMe {
			
		}
	}
}
