package org.codemucker.jmutate.pattern;


import java.util.Collection;

import org.codemucker.jfind.FindResult;
import org.codemucker.jmutate.MutateContext;
import org.codemucker.jmutate.JSourceFilter;
import org.codemucker.jmutate.TestSourceHelper;
import org.codemucker.jmutate.SourceTemplate;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.ast.SimpleMutateContext;
import org.codemucker.jmutate.ast.matcher.AJType;
import org.codemucker.jmutate.util.SourceAsserts;
import org.codemucker.jpattern.GenerateBuilder;
import org.junit.Test;


public class BeanBuilderTransformTest {

	MutateContext ctxt = SimpleMutateContext.with().defaults().build();	
	
	@Test
	public void test_apply_pattern(){
		FindResult<JType> types = findTypesToTransform();
		JType type = types.getFirst();
		
		whenTransformAppliedTo(type);
		
	    JType expected = generateExpect(ctxt);
	    SourceAsserts.assertAstsMatch(expected, type);
	}

	private void whenTransformAppliedTo(JType type) {
		ctxt.obtain(BeanBuilderTransform.class)
			.setTarget(type)
			.transform();
	}
	
	public JType generateExpect(MutateContext ctxt){
		SourceTemplate t=ctxt.newSourceTemplate();
		t.pl("@" + GenerateBuilder.class.getSimpleName());
		t.pl("public static class TestBuilderBean {");
		t.pl("	private String myString;");
		t.pl("	private int myInt;");
		t.pl("	private Collection<String> col;");
		
		t.pl("	public TestBuilderBean(String myString,int myInt,Collection<String> col){this.myString = myString;this.myInt=myInt;this.col=col;}");
		t.pl("	public static class Builder {");
		t.pl("		private String myString;");
		t.pl("		private int myInt;");
		t.pl("		private Collection<String> col;");
		
		t.pl("		public Builder setMyString(String myString){");
		t.pl("			this.myString = myString;");
		t.pl("			return this;");
		t.pl("		}");
		t.pl("		public Builder setMyInt(int myInt){");
		t.pl("			this.myInt = myInt;");
		t.pl("			return this;");
		t.pl("		}");
		t.pl("		public Builder setCol(Collection<String> col){");
		t.pl("			this.col = col;");
		t.pl("			return this;");
		t.pl("		}");
		t.pl("		public TestBuilderBean build(){");
		t.pl("			return new TestBuilderBean(myString,myInt,col);");
		t.pl("		}");
		t.pl("	}");
		t.pl("}");
		return t.asResolvedJTypeNamed("TestBuilderBean");	
	}

	private FindResult<JType> findTypesToTransform() {
	    FindResult<JType> found = TestSourceHelper.newTestSourcesResolvingFinder()
			.filter(JSourceFilter.with()
				//.addIncludeTypes(JTypeMatchers.withAnnotation(GenerateBuilder.class))
				.includeType(AJType.with().name(TestBuilderBean.class))
			)	
			.build()
			.findTypes();
	    return found;
    }

	@GenerateBuilder
	public static class TestBuilderBean {
		private String myString;
		private int myInt;
		private Collection<String> col;
	}
}
