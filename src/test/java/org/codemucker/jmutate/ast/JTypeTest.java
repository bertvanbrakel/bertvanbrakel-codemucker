package org.codemucker.jmutate.ast;

import static org.codemucker.jmatch.Assert.assertThat;
import static org.codemucker.jmatch.Assert.is;
import static org.codemucker.jmatch.Assert.isEqualTo;
import static org.codemucker.jmatch.Assert.isFalse;
import static org.codemucker.jmatch.Assert.isTrue;
import static org.codemucker.jmatch.Assert.not;

import java.util.List;

import org.codemucker.jmatch.AList;
import org.codemucker.jmutate.SourceHelper;
import org.codemucker.jmutate.ast.JField;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.JSourceFile;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.ast.SimpleCodeMuckContext;
import org.codemucker.jmutate.ast.JTypeTest.MyClass.MyChildClass1;
import org.codemucker.jmutate.ast.JTypeTest.MyClass.MyChildClass2;
import org.codemucker.jmutate.ast.JTypeTest.MyClass.MyChildClass3;
import org.codemucker.jmutate.ast.JTypeTest.MyClass.MyNonExtendingClass;
import org.codemucker.jmutate.ast.finder.FindResult;
import org.codemucker.jmutate.ast.matcher.AJField;
import org.codemucker.jmutate.ast.matcher.AJMethod;
import org.codemucker.jmutate.ast.matcher.AJType;
import org.codemucker.jmutate.transform.CodeMuckContext;
import org.codemucker.jmutate.transform.SourceTemplate;
import org.junit.Test;


public class JTypeTest {

	CodeMuckContext ctxt = new SimpleCodeMuckContext();
	
	@Test
	public void testIsAbstract() {
		assertThat(newJType("class MyClass{}").getModifiers().isAbstract(),isFalse());
		assertThat(newJType("interface MyInterface{}").getModifiers().isAbstract(),isFalse());
		assertThat(newJType("enum MyEnum{}").getModifiers().isAbstract(),isFalse());

		assertThat(newJType("abstract class MyAbstractClass{}").getModifiers().isAbstract(),isTrue());
	}
	
	@Test
	public void testIsInterface() {
		assertThat(newJType("class MyClass{}").isInterface(),isFalse());
		assertThat(newJType("enum MyEnum{}").isInterface(),isFalse());	
	
		assertThat(newJType("interface MyInterface{}").isInterface(),isTrue());
	}
	
	@Test
	public void testIsConcreteClass() {
		assertThat(newJType("class MyClass{}").isConcreteClass(),isTrue());
		
		assertThat(newJType("interface MyInterface{}").isConcreteClass(),isFalse());
		assertThat(newJType("enum MyEnum{}").isConcreteClass(),isFalse());
	}
	
	private JType newJType(String src){
		return ctxt.newSourceTemplate().println(src).asResolvedJTypeNamed(null);
	}

	@Test
	public void testIsTopLevelClass() {
		assertThat(newJType("class MyTopClass {}").isTopLevelClass(),isTrue());
		assertThat(newJType("class AnotherTopClass { class MyInnerClass{} }").isTopLevelClass(),isTrue());
		
		assertThat(newJType("class AnotherTopClass { class MyInnerClass{} }").getChildTypeWithName("MyInnerClass").isTopLevelClass(),isFalse());
	}
	
	@Test
	public void testFindJavaMethods(){
		SourceTemplate t = ctxt.newSourceTemplate();
		
		t.pl("class MyTestClass  {");
		t.pl( "public void methodA(){}" );
		t.pl( "public void methodB(){}" );
		t.pl("}");
	
		FindResult<JMethod> foundMethods = t.asResolvedJTypeNamed("MyTestClass").findAllJMethods();

		assertThat(
				foundMethods.toList(), 
				AList.of(JMethod.class)
					.inOrder()
					.containingOnly()
					.item(AJMethod.withName("methodA"))
					.item(AJMethod.withName("methodB"))
		);
	}

	@Test
	public void testFindJavaMethodsWithFilter(){
		SourceTemplate t = ctxt.newSourceTemplate();

		t.pl("class MyTestClass  {");
		t.pl( "public void getA(){}" );
		t.pl( "public void setB(){}" );
		t.pl( "public void getB(){}" );
		t.pl( "public void setA(){}" );
		t.pl("}");

		FindResult<JMethod> foundMethods = t.asResolvedJTypeNamed("MyTestClass").findMethodsMatching(AJMethod.withNameMatchingAntPattern("get*"));
	
		assertThat(
				foundMethods.toList(), 
				AList.of(JMethod.class)
					.inOrder()
					.containingOnly()
					.item(AJMethod.withName("getA"))
					.item(AJMethod.withName("getB"))
		);
	}

	@Test
	public void testFindJavaMethodsExcludingChildTypeMethods(){
		SourceTemplate t = ctxt.newSourceTemplate();

		t.pl("class MyTestClass{");
		t.pl("	public void getA(){ return; }" );
		t.pl("	private class Foo {" );
		t.pl("		public Object getB(){ return null;}//should be ignored" );
		t.pl("	}");
		t.pl("}");
		
		FindResult<JMethod> foundMethods = t.asResolvedJTypeNamed("MyTestClass").findMethodsMatching(AJMethod.withNameMatchingAntPattern("get*"));

		assertThat(
				foundMethods.toList(), 
				AList.ofOnly(AJMethod.withName("getA"))
		);
	}

	@Test
	//For a bug where performing a method search on top level children also return nested methods
	public void testFindJavaMethodsExcludingAnonymousTypeMethodsEmbeddedInMethods(){
		JType type = SourceHelper.findSourceForTestClass(getClass()).getTypeWithName(MyTestClass.class);
		FindResult<JMethod> foundMethods = type.findMethodsMatching(AJMethod.withNameMatchingAntPattern("get*"));

		assertThat(
			foundMethods.toList(), 
			AList.of(JMethod.class)
				.inOrder()
				.containingOnly()
				.item(AJMethod.withName("getA"))
				.item(AJMethod.withName("getB"))
		);
	}
	
	static class MyTestClass {
		public Foo foo = new Foo() { // should be ignored
			public int getC() {
				return 1;
			}
		};

		public Object getA() {
			return null;
		}

		public Object getB() {
			return new Foo() {
				public int getC() { // should be ignored
					return new Foo() {
						public int getC() {
							return 2;
						}// should be ignored
					}.getC();
				};
			};
		}

		private class Foo {
			public int getC() {
				return 0;
			}// should be ignored
		}
	}
	@Test
	public void testResolveFullNameAnonymousInnerClass(){
		SourceTemplate t = ctxt.newSourceTemplate();

		t.pl("class MyTestClass  {");
		t.pl( "public void getA(){ return; }" );
		t.pl( "private Foo foo = new Foo(){};//should resolve to Foo" );
		t.pl( "private class Foo {" );
		t.pl( "		public void getB(){ return;}" );
		t.pl("	}");
		t.pl("}");
		
		JField field = t.asResolvedJTypeNamed("MyTestClass").findFieldsMatching(AJField.withName("foo")).getFirst();
		
		assertThat(field.getTypeSignature(),isEqualTo("Foo"));
	}
	
	@Test
	public void testIsAnonymousClass(){
		SourceTemplate t = ctxt.newSourceTemplate();

		t.pl("class MyTestClass  {");
		t.pl( "private Foo foo = new Foo(){};//anonymous" );
		t.pl( "private class Foo {" );
		t.pl("	}");
		t.pl("}");
		
		List<JType> types = t.asResolvedJTypeNamed("MyTestClass").findAllChildTypes().toList();

		assertThat(
			types,
			is(AList.of(JType.class)
				.inOrder()
				.containingOnly()
				.item(AJType.isAnonymous())
				.item(not(AJType.isAnonymous()))
			)
		);
	}
	
	@Test
	public void testGetFullName(){
		SourceTemplate t = ctxt.newSourceTemplate();
		
		t.pl("package foo.bar;");
		
		t.pl("import java.util.Collection;");
		t.pl("import java.io.File;");

		t.pl("class MyTestClass  {");
		t.pl( "public Collection getA(){return null;}" );
		t.pl( "public Object getB(){return null;}" );
		t.pl("}");
	
		JType type = t.asResolvedSourceFileNamed("foo.bar.MyTestClass").getMainType();

		assertThat(type.getFullName(), isEqualTo("foo.bar.MyTestClass"));
	}

	@Test
	public void testHasNotAnnotationOfType(){
		SourceTemplate t = ctxt.newSourceTemplate();
		t.pl("class MyClass  {}");
	
		assertThat(t.asResolvedJTypeNamed("MyClass").hasAnnotationOfType(MyAnnotation.class),isFalse());
	}
	
	@Test
	public void testHasAnnotationOfType(){
		SourceTemplate t = ctxt.newSourceTemplate();
		t.v("a1", MyAnnotation.class);
		t.pl("@${a1}");
		t.pl("class MyClass {}");
		
		assertThat(t.asResolvedJTypeNamed("MyClass").hasAnnotationOfType(MyAnnotation.class),isTrue());
	}
	
	public @interface MyAnnotation {
		
	}
	
	@Test
	public void testImplementsClass(){
		JSourceFile source = SourceHelper.findSourceForTestClass(getClass());
		assertThat(source.getTypeWithName(MyChildClass1.class).isSubClassOf(MyExtendedClass.class),isFalse());
		assertThat(source.getTypeWithName(MyChildClass2.class).isSubClassOf(MyExtendedClass.class),isTrue());
		assertThat(source.getTypeWithName(MyChildClass3.class).isSubClassOf(MyExtendedClass.class),isTrue());
		assertThat(source.getTypeWithName(MyNonExtendingClass.class).isSubClassOf(MyExtendedClass.class),isFalse());
	}
	
	public static class MyExtendedClass {
	}
	
	class MyClass {
		class MyChildClass1 {}
		class MyChildClass2 extends MyExtendedClass {}
		class MyChildClass3 extends MyChildClass2 {}
		class MyNonExtendingClass {}
	}

	@Test
	public void testFindJavaConstructors(){
		SourceTemplate t = ctxt.newSourceTemplate();
		
		t.pl("class MyTestClass { ");
		t.pl( "MyTestClass(){}" );
		t.pl( "MyTestClass(String foo){}" );
		t.pl( "public void someMethod(){}" );
		t.pl("}");

		FindResult<JMethod> foundMethods = t.asResolvedJTypeNamed("MyTestClass").findMethodsMatching(AJMethod.isConstructor());

		assertThat(
				foundMethods.toList(), 
				is(AList.of(JMethod.class)
					.inOrder()
					.containingOnly()
					.item(AJMethod.withName("MyTestClass"))
					.item(AJMethod.withName("MyTestClass")))
		);
	}
}
