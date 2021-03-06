package org.codemucker.jmutate.example;

import static org.codemucker.jmatch.Logical.not;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.codemucker.jfind.FindResult;
import org.codemucker.jfind.ReflectedClass;
import org.codemucker.jfind.Roots;
import org.codemucker.jfind.matcher.AMethod;
import org.codemucker.jmatch.AString;
import org.codemucker.jmatch.Expect;
import org.codemucker.jmatch.ObjectMatcher;
import org.codemucker.jmutate.SourceFilter;
import org.codemucker.jmutate.SourceScanner;
import org.codemucker.jmutate.TestSourceHelper;
import org.codemucker.jmutate.ast.BaseASTVisitor;
import org.codemucker.jmutate.ast.JAccess;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.ast.matcher.AJMethod;
import org.codemucker.jmutate.ast.matcher.AJType;
import org.codemucker.jmutate.builder.AbstractBuilder;
import org.codemucker.lang.IBuilder;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

public class EnforcerExampleTest 
{
	@Test
	public void baseASTVisitorOverridesAllParentVisitMethods(){
		JType baseVisitor = TestSourceHelper.findSourceForClass(BaseASTVisitor.class).getMainType();
		List<String> haveMethodSigs = baseVisitor
				.findMethodsMatching(AJMethod.with().name(AString.equalToAny("visit", "endVisit")))
				.transform(new Function<JMethod, String>() {
						@Override
						public String apply(JMethod m) {		
							return m.getClashDetectionSignature();
						}
					})	
				.toList();
		
		ReflectedClass astVisitor = new ReflectedClass(ASTVisitor.class);
		List<String> expectMethodSigs = astVisitor
				.findMethodsMatching(AMethod.with().name(AString.equalToAny("visit", "endVisit")))
				.transform(new Function<Method, String>() {
					@Override
					public String apply(Method m) {
						StringBuilder sb = new StringBuilder();
						for(Class<?> t : m.getParameterTypes()){
							if(sb.length() > 0){
								sb.append(',');
							}
							sb.append(t.getName());
						}
						return m.getName() + "(" + sb.toString() + ")";
					}
				})
				.toList();
		expectMethodSigs.removeAll(haveMethodSigs);
		
		if(expectMethodSigs.size() > 0){
			Assert.fail("expected " + BaseASTVisitor.class.getName() + " to override [" +  Joiner.on(',').join(expectMethodSigs) + "]");
		}
	}
	
	@Test
	public void ensureMatcherBuildersAreCorrectlyNamed()
	{
		FindResult<JType> matchers = SourceScanner.with()
			.scanRoots(Roots.with().mainSrcDir(true))
			.filter(SourceFilter.with()
				.typeMatches(AJType.that().isASubclassOf(ObjectMatcher.class).isNotAbstract()))
			.build()
			.findTypes();
		
		List<String> failMsgs = new ArrayList<>();
		
		for(JType t : matchers){
			FindResult<JMethod> methods = t.findMethodsMatching(AJMethod.with()
				.access(JAccess.PUBLIC)
				.name(not(AString.equalToAny("with", "that", "any", "none", "all"))));

			for(JMethod m : methods){
				if(m.getModifiers().isStatic()){
					failMsgs.add("FAILED: " + t.getFullName() + "." + m.getFullSignature() + " ==>isStatic");
				}
				if(AString.startingWithIgnoreCase("with").matches(m.getName())){
					failMsgs.add("FAILED: " + t.getFullName() + "." + m.getFullSignature() + " ==>starts with 'with'");
				}
			}
		}
		if(failMsgs.size() > 0){
			Assert.fail(Joiner.on("\n").join(failMsgs));
		}
	}

	@Test
	public void ensureBuildersAreCorrectlyNamed(){
	    BuilderFinderEnforcer.with().build().invoke();
	}
	
    static class BuilderFinderEnforcer {
	        
        public static Builder with(){
            return new Builder();
        }

        public void invoke(){

            FindResult<JType> foundBuilders = SourceScanner.with()
                    .scanRoots(Roots.with()
                        .mainSrcDir(true)
                        .testSrcDir(true))
                    .filter(SourceFilter.with()
                        .typeMatches(AJType.with().simpleName("*Builder"))
                        .typeMatches(AJType.that().isASubclassOf(IBuilder.class))
                        .typeMatches(AJType.that().isASubclassOf(AbstractBuilder.class))
                        .typeMatches(AJType.with().method(AJMethod.with().nameMatchingAntPattern("build*"))))
                    .build()
                    .findTypes();
            
            for (JType builderType : foundBuilders) {            
                Expect
                	.with().debugEnabled(true)
                    .that(builderType)
                    .is(ABuilderPattern.with()
                        .defaults()
                        .ignoreMethod(AJMethod.with().name("copyOf").isNotVoidReturn())
                        .ignoreMethod(AJMethod.with().name("describeTo").isVoidReturn()));
            }
        }
        //TODO:add build options
        public static class Builder implements IBuilder<BuilderFinderEnforcer>{
            @Override
            public BuilderFinderEnforcer build() {
                return new BuilderFinderEnforcer();
            }           
        }
    }   
}
