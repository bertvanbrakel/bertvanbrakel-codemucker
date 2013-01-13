package org.codemucker.jmutate.ast;

import static org.codemucker.jmatch.Assert.assertThat;
import static org.codemucker.jmatch.Assert.isEqualTo;

import java.util.Collection;
import java.util.List;

import org.codemucker.jmatch.AList;
import org.codemucker.jmatch.Assert;
import org.codemucker.jmutate.SourceHelper;
import org.codemucker.jmutate.ast.JAnnotation;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.ast.SimpleCodeMuckContext;
import org.codemucker.jmutate.ast.matcher.AnAnnotation;
import org.codemucker.jmutate.transform.CodeMuckContext;
import org.codemucker.jmutate.transform.SourceTemplate;
import org.eclipse.jdt.core.dom.Annotation;
import org.junit.Test;

import com.bertvanbrakel.codemucker.annotation.Generated;

public class JAnnotationTest {

	private CodeMuckContext context = new SimpleCodeMuckContext();
	
	@Test
	public void test_findAnnotations(){
		SourceTemplate t = context.newSourceTemplate();
		t.v("a1", MyAnnotation1.class);
		t.v("a2", MyAnnotation2.class);
		t.pl("import ${a1};");
		t.pl("import ${a2};");
		t.pl("@${a1}");
		t.pl("class MyClass {");
		t.pl("	@${a2}");
		t.pl(" 	class MySubClass {");
		t.pl("	}");
		t.pl("}");
		
		JType type = t.asResolvedSourceFileNamed("MyClass").getMainType();
		
		List<Annotation> found = JAnnotation.findAnnotations(type.getAstNode());

		Assert.that(found, 
			AList.of(Annotation.class)
				.inOrder()
				.containingOnly()
				.item(AnAnnotation.withFqn(MyAnnotation1.class))
				.item(AnAnnotation.withFqn(MyAnnotation2.class))
		);
	}

	@Test
	public void test_findAnnotations_depth(){
		SourceTemplate t = context.newSourceTemplate();
		t.v("a1", MyAnnotation1.class);
		t.v("a2", MyAnnotation2.class);
		t.pl("import ${a1};");
		t.pl("import ${a2};");
		t.pl("@${a1}");
		t.pl("class MyClass {");
		t.pl("	@${a2}");
		t.pl(" 	class MySubClass {");
		t.pl("	}");
		t.pl("}");
		
		JType type = t.asResolvedSourceFileNamed("MyClass").getMainType();
		
		List<Annotation> found = JAnnotation.findAnnotations(type.getAstNode(), JAnnotation.DIRECT_DEPTH);

		assertThat(found, AList.ofOnly(AnAnnotation.withFqn(MyAnnotation1.class)));
	}

	public @interface MyAnnotation1 {
		
	}
	
	public @interface MyAnnotation2 {
		
	}
	
	@Test
	public void test_resolveSimpleName(){
		JType type = SourceHelper.findSourceForTestClass(JAnnotationTest.class).getTypeWithName(TestBean.class);
		Collection<Annotation> annons = type.getAnnotations();
		
		assertThat(annons.size(),isEqualTo(1));
	
		Annotation anon = annons.iterator().next();
		JAnnotation ja = JAnnotation.from(anon);
		
		assertThat(ja.getQualifiedName(),isEqualTo(Generated.class.getName()));
		assertThat(ja.isOfType(Generated.class));	
	}
	
	public static class TestBean {
		
		@Generated
		public void myMethod(){
			
		}
	}
}
