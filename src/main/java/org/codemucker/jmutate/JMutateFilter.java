package org.codemucker.jmutate;

import org.codemucker.jfind.FindResult;
import org.codemucker.jfind.IncludeExcludeMatcherBuilder;
import org.codemucker.jfind.MatcherToFindFilterAdapter;
import org.codemucker.jfind.Root;
import org.codemucker.jfind.RootResource;
import org.codemucker.jmatch.Description;
import org.codemucker.jmatch.MatchDiagnostics;
import org.codemucker.jmatch.Matcher;
import org.codemucker.jmutate.JMutateScanner.SourceMatcher;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.JSourceFile;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.lang.IBuilder;

public class JMutateFilter implements SourceMatcher {

	private final FindResult.Filter<Object> objectFilter;
	private final FindResult.Filter<Root> rootFilter;
	private final FindResult.Filter<RootResource> resourceFilter;
	private final FindResult.Filter<JSourceFile> sourceFilter;
	private final FindResult.Filter<JType> typeMatcher;
	private final FindResult.Filter<JMethod> methodFilter;
	
    public static Builder with() {
        return new Builder();
    }

	private JMutateFilter(
			FindResult.Filter<Object> objectFilter
			, FindResult.Filter<Root> rootFilter
			, FindResult.Filter<RootResource> resourceMatcher
			, FindResult.Filter<JSourceFile> sourceMatcher
			, FindResult.Filter<JType> typeMatcher
			, FindResult.Filter<JMethod> methodMatcher) {
        super();
        
        this.objectFilter = objectFilter;
        this.rootFilter = rootFilter;
        this.resourceFilter = resourceMatcher;
        this.sourceFilter = sourceMatcher;
        this.typeMatcher = typeMatcher;
        this.methodFilter = methodMatcher;
    }
	
	@Override
	public FindResult.Filter<Object> getObjectMatcher() {
		return objectFilter;
	}

	@Override
	public FindResult.Filter<Root> getRootMatcher() {
		return rootFilter;
	}

	@Override 
	public FindResult.Filter<RootResource> getResourceMatcher() {
		return resourceFilter;
	}

	@Override
	public FindResult.Filter<JSourceFile> getSourceMatcher() {
		return sourceFilter;
	}

	@Override
	public FindResult.Filter<JType> getTypeMatcher() {
		return typeMatcher;
	}

	@Override
	public FindResult.Filter<JMethod> getMethodMatcher() {
		return methodFilter;
	}	

	public static class Builder implements IBuilder<SourceMatcher> {
		
		private static final FindResult.Filter<Object> ANY = new FindResult.Filter<Object>(){

			@Override
			public boolean matches(Object found) {
				return true;
			}

			@Override
			public boolean matches(Object actual, MatchDiagnostics ctxt) {
				return true;
			}
			
			@Override
			public void onMatched(Object result) {}

			@Override
			public void onIgnored(Object result) {}

			@Override
			public void describeTo(Description desc) {
				desc.text("anything");
			}

			@Override
			public void onError(Object record, Exception e) throws Exception {
			}
	    };
	    
	    private IncludeExcludeMatcherBuilder<Root> roots = IncludeExcludeMatcherBuilder.builder();
		private IncludeExcludeMatcherBuilder<RootResource> resources = IncludeExcludeMatcherBuilder.builder();
		private IncludeExcludeMatcherBuilder<JSourceFile> sources = IncludeExcludeMatcherBuilder.builder();
		private IncludeExcludeMatcherBuilder<JMethod> methods = IncludeExcludeMatcherBuilder.builder();
		private IncludeExcludeMatcherBuilder<JType> types = IncludeExcludeMatcherBuilder.builder();

		private Builder(){
			//prevent instantiation outside of builder method
		}
		
		public SourceMatcher build(){
			return new JMutateFilter(
				 ANY	
				, toFilter(roots.build())
				, toFilter(resources.build())
				, toFilter(sources.build())
				, toFilter(types.build())
				, toFilter(methods.build())
			);	
		}
			
		private <T> FindResult.Filter<T> toFilter(Matcher<T> matcher){
			return MatcherToFindFilterAdapter.from(matcher);
		}
		
		public Builder includeRoot(Matcher<Root> matcher) {
            roots.addInclude(matcher);
            return this;
        }
		
		public Builder includeResource(Matcher<RootResource> matcher) {
			resources.addInclude(matcher);
			return this;
		}
	
		public Builder excludeRoot(Matcher<Root> matcher) {
            roots.addExclude(matcher);
            return this;
        }
        
		public Builder excludeResource(Matcher<RootResource> matcher) {
			resources.addExclude(matcher);
			return this;
		}
	
		public Builder includeSource(Matcher<JSourceFile> matcher) {
			sources.addInclude(matcher);
			return this;
		}
	
		public Builder excludeSource(Matcher<JSourceFile> matcher) {
			sources.addExclude(matcher);
			return this;
		}
		
		public Builder includeType(Matcher<JType> matcher){
			types.addInclude(matcher);
			return this;
		}
		
		public Builder excludeType(Matcher<JType> matcher){
			types.addExclude(matcher);
			return this;
		}
		
		public Builder includeMethods(Matcher<JMethod> matcher){
			methods.addInclude(matcher);
			return this;
		}
		
		public Builder excludeMethods(Matcher<JMethod> matcher){
			methods.addExclude(matcher);
			return this;
		}
	}
	
}