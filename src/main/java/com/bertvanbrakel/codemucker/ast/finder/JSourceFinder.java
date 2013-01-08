package com.bertvanbrakel.codemucker.ast.finder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTParser;

import com.bertvanbrakel.codemucker.ast.JAstParser;
import com.bertvanbrakel.codemucker.ast.JFindVisitor;
import com.bertvanbrakel.codemucker.ast.JMethod;
import com.bertvanbrakel.codemucker.ast.JSourceFile;
import com.bertvanbrakel.codemucker.ast.JType;
import com.bertvanbrakel.lang.IsBuilder;
import com.bertvanbrakel.test.finder.BaseRootVisitor;
import com.bertvanbrakel.test.finder.RootResource;
import com.bertvanbrakel.test.finder.Root;
import com.bertvanbrakel.test.finder.RootVisitor;
import com.bertvanbrakel.test.finder.Roots;
import com.bertvanbrakel.test.finder.matcher.LogicalMatchers;
import com.bertvanbrakel.test.finder.matcher.Matcher;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Utility class to find source files, java types, and methods.
 * 
 * Usage:
 * 
 * 
 */
public class JSourceFinder {

	private static final String JAVA_EXTENSION = "java";
	private static final JFindListener NULL_LISTENER = new JFindListener() {
		@Override
		public void onMatched(Object obj) {
		}
		
		@Override
		public void onIgnored(Object obj) {
		}
	};
	
	private final Collection<Root> roots;

	private final Matcher<Root> rootMatcher;
	private final Matcher<RootResource> resourceMatcher;
	private final Matcher<JSourceFile> sourceMatcher;
	private final Matcher<JType> typeMatcher;
	private final Matcher<JMethod> methodMatcher;
	
	private final JFindListener listener;
	
	@Inject
	private final JAstParser parser;

	public static interface JFindMatcher {
		public Matcher<Object> getObjectMatcher();
		public Matcher<Root> getRootMatcher();
		public Matcher<RootResource> getResourceMatcher();
		public Matcher<JSourceFile> getSourceMatcher();
		public Matcher<JType> getTypeMatcher();
		public Matcher<JMethod> getMethodMatcher();
	}
	
	public static interface JFindListener extends FindResult.MatchListener<Object> {
	}
	
	public static Builder builder(){
		return new Builder();
	}

	@Inject
	public JSourceFinder(
			JAstParser parser
			, Iterable<Root> classPathRoots
			, JFindMatcher matchers
			, JFindListener listener
			) {
		this(parser,
			classPathRoots,
			matchers.getObjectMatcher(),
			matchers.getRootMatcher(),
			matchers.getResourceMatcher(),			
			matchers.getSourceMatcher(),
			matchers.getTypeMatcher(),
			matchers.getMethodMatcher(),
			listener
		);
	}

	public JSourceFinder(
			JAstParser parser
			, Iterable<Root> roots
			, Matcher<Object> objectFilter
			, Matcher<Root> rootFilter
			, Matcher<RootResource> resourceFilter
			, Matcher<JSourceFile> sourceFilter
			, Matcher<JType> typeFilter
			, Matcher<JMethod> methodFilter
			, JFindListener listener
			) {
		
		checkNotNull(roots, "expect class path roots");
		
		this.parser = checkNotNull(parser, "expect parser");
		this.roots = ImmutableList.<Root>builder().addAll(roots).build();

		this.rootMatcher = join(checkNotNull(rootFilter, "expect root filter"), objectFilter);
		this.resourceMatcher = join(checkNotNull(resourceFilter, "expect resource filter"), objectFilter);
		this.sourceMatcher = join(checkNotNull(sourceFilter, "expect source filter"), objectFilter);
		this.typeMatcher = join(checkNotNull(typeFilter, "expect type filter"), objectFilter);
		this.methodMatcher = join(checkNotNull(methodFilter, "expect method filter"), objectFilter);
	
		this.listener = checkNotNull(listener, "expect find listener");
	}
	
	private static <T> Matcher<T> join(final Matcher<T> matcher,final Matcher<Object> objMatcher){
		return new Matcher<T>(){
			@Override
			public boolean matches(T found) {
				return objMatcher.matches(found) && matcher.matches(found);
			}
		};
	}
	public void visit(JFindVisitor visitor) {
		for (JSourceFile srcFile : findSources()) {
			srcFile.visit(visitor);
		}
	}
	
	public FindResult<JMethod> findMethods() {
		return findTypes().transformToMany(typeToMethods()).filter(methodMatcher, listener);
	}
	
	private Function<JType, Iterator<JMethod>> typeToMethods(){
		return new Function<JType,Iterator<JMethod>>(){
			public Iterator<JMethod> apply(JType type){
				return type.findAllJMethods().iterator();
			}
		};
	}

	public FindResult<JType> findTypes() {
		return findSources().transformToMany(sourceToTypes()).filter(typeMatcher, listener);
    }
		
	private Function<JSourceFile, Iterator<JType>> sourceToTypes(){
		return new Function<JSourceFile,Iterator<JType>>(){
			public Iterator<JType> apply(JSourceFile source){
				return source.findAllTypes().iterator();
			}
		};
	}
	
	public FindResult<JSourceFile> findSources() {
		return findResources().transform(resourceToSource()).filter(sourceMatcher, listener);
	}
	
	private Function<RootResource, JSourceFile> resourceToSource(){
		return new Function<RootResource,JSourceFile>(){
			public JSourceFile apply(RootResource resource){
				if( resource.hasExtension(JAVA_EXTENSION)){
					//TODO:catch error here and callback onerror?
					return JSourceFile.fromResource(resource, parser);
				}
				//indicate skip this item
				return null;
			}
		};
	}
	
	public FindResult<RootResource> findResources(){
		final List<RootResource> resources = Lists.newArrayListWithExpectedSize(200);	
		RootVisitor visitor = new BaseRootVisitor(){
			@Override
			public boolean visit(Root root) {
				boolean visit = rootMatcher.matches(root);
				if( visit){
					listener.onMatched(root);
				} else {
					listener.onIgnored(root);	
				}
				return visit;
			}
			@Override
			public boolean visit(RootResource resource) {
				boolean visit = resourceMatcher.matches(resource);
				if( visit){
					resources.add(resource);
					listener.onMatched(resource);
				} else {
					listener.onIgnored(resource);	
				}
				return visit;
			}
		};

		for(Root root:roots){
			root.accept(visitor);
		}

		return FindResultImpl.from(resources);
	}
	
	public FindResult<Root> findRoots() {
		return FindResultImpl.from(roots).filter(rootMatcher);
	}
	
	public static class Builder {
		private JAstParser parser;
		private List<Root> roots = newArrayList();
		
		private Matcher<Object> objectMatcher;
		private Matcher<Root> rootMatcher;
		private Matcher<RootResource> resourceMatcher;
		private Matcher<JSourceFile> sourceMatcher;
		private Matcher<JType> typeMatcher;
		private Matcher<JMethod> methodMatcher;
		
		private JFindListener listener;
		
		public JSourceFinder build(){			
			return new JSourceFinder(
				toParser()
				, roots
				, anyIfNull(objectMatcher)
				, anyIfNull(rootMatcher)
				, anyIfNull(resourceMatcher)
				, anyIfNull(sourceMatcher)
				, anyIfNull(typeMatcher)
				, anyIfNull(methodMatcher)
				, listener==null?NULL_LISTENER:listener
			);
		}

		private static <T> Matcher<T> anyIfNull(Matcher<T> matcher){
			return LogicalMatchers.anyIfNull(matcher);
		}
		
		private JAstParser toParser(){
			return parser != null ? parser : JAstParser.newDefaultJParser();
		}

		public Builder setSearchRoots(Roots.Builder searchRoots) {
        	setSearchRoots(searchRoots.build());
        	return this;
        }
		
	 	public Builder setSearchRoots(IsBuilder<? extends Iterable<Root>> rootsBuilder) {
        	setSearchRoots(rootsBuilder.build());
        	return this;
        }
	 	
	 	public Builder setSearchRoots(Iterable<Root> roots) {
        	this.roots = nullSafeList(roots);
        	return this;
        }
	 	
	 	private static <T> List<T> nullSafeList(Iterable<T> iter){
	 		if( iter == null){
	 			return newArrayList();
	 		}
	 		return newArrayList(iter);
	 	}

	 	public Builder setListener(JFindListener listener) {
        	this.listener = listener;
        	return this;
		}
	 	
	 	public Builder setFilter(Filter.Builder filter) {
        	setFilter(filter.build());
        	return this;
		}
	 	
		public Builder setFilter(IsBuilder<JFindMatcher> builder) {
        	setFilter(builder.build());
        	return this;
		}
		
		public Builder setFilter(JFindMatcher filters) {
			objectMatcher = filters.getObjectMatcher();
			rootMatcher = filters.getRootMatcher();
			resourceMatcher = filters.getResourceMatcher();			
			sourceMatcher = filters.getSourceMatcher();
			typeMatcher = filters.getTypeMatcher();
			methodMatcher = filters.getMethodMatcher();
			
        	return this;
		}

		public Builder setParser(JAstParser parser) {
        	this.parser = parser;
        	return this;
        }
	}
}
