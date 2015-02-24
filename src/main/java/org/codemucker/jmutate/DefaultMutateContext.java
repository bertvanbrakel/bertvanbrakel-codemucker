package org.codemucker.jmutate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codemucker.jfind.DirectoryRoot;
import org.codemucker.jfind.Root;
import org.codemucker.jfind.Root.RootContentType;
import org.codemucker.jfind.Root.RootType;
import org.codemucker.jfind.RootResource;
import org.codemucker.jfind.Roots;
import org.codemucker.jmutate.ast.ContextNames;
import org.codemucker.jmutate.ast.DefaultToSourceConverter;
import org.codemucker.jmutate.ast.JAstFlattener;
import org.codemucker.jmutate.ast.JAstParser;
import org.codemucker.jmutate.ast.JSourceFile;
import org.codemucker.jmutate.ast.ToSourceConverter;
import org.codemucker.jmutate.generate.JAnnotationCompiler;
import org.codemucker.jmutate.util.MutateUtil;
import org.codemucker.jpattern.generate.ClashStrategy;
import org.codemucker.jtest.MavenProjectLayout;
import org.codemucker.jtest.ProjectLayout;
import org.codemucker.lang.PathUtil;
import org.codemucker.lang.annotation.Optional;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.name.Named;

@Singleton
public class DefaultMutateContext implements JMutateContext {

    private static final Logger log = LogManager.getLogger(DefaultMutateContext.class);

    private final ProjectLayout projectLayout;
    private final ProjectOptions projectOptons;
    private final ResourceLoader resourceLoader;
    private final PlacementStrategy placementStrategy;
	/**
	 * Where by default any generated code is output to
	 */
	private final Root generationRoot;
	private final JAstParser parser;
	private final DefaultCodeFormatterOptions formattingOptions;
	/**
	 * If set, generated classes will be marked as such
	 */
	private final boolean markGenerated;
	
	private Map<String	, ResourceTracker> sourceTrackers = new HashMap<>();
	
	private final ClashStrategy defaultClashStrategy;
	
	//internally created
    private final Injector injector;
    //private final ClassLoader classLoader;
    
	public static Builder with(){
		return new Builder();
	}
	
	private DefaultMutateContext(ProjectLayout layout, ProjectOptions options, Root generationRoot, JAstParser parser, boolean markGenerated, DefaultCodeFormatterOptions formatterOptions,PlacementStrategy placementStrategy, ClashStrategy defaultClashStrategy) {
        super();
        this.projectLayout = layout;
        this.projectOptons = options;
        this.formattingOptions = formatterOptions;
        this.generationRoot = generationRoot;
        this.parser = parser;
        this.markGenerated = markGenerated;
        this.placementStrategy = placementStrategy;
        this.resourceLoader = parser.getResourceLoader();
        this.defaultClashStrategy = defaultClashStrategy;
        
        injector = Guice.createInjector(Stage.PRODUCTION, new DefaultMutationModule());
    }

	@Override
    public SourceTemplate newSourceTemplate(){
    	return obtain(SourceTemplate.class);
    }

	@Override
    public SourceTemplate newTempSourceTemplate(){
        return new SourceTemplate(getParser(), newTmpSnippetRoot());
    }
	
	 private Root newTmpSnippetRoot(){
	     try {
	         File tmpDir = PathUtil.newTmpDir(projectLayout.getTmpDir(), "TmpSnippetRoot","");
	         return new DirectoryRoot(tmpDir,RootType.GENERATED,RootContentType.SRC);
	     } catch (IOException e) {
            throw new JMutateException("Couldn't create a snippet root",e);     
        }
	 }

	@Override
	public void trackChanges(ASTNode node) {
		JSourceFile source = MutateUtil.getSource(node);
		if( source != null){
			trackChanges(source);
		}
	}

	@Override
	public void trackChanges(JSourceFile source) {
		log.debug("tracking changes for " + source.getResource().getFullPath());
		getOrCreateTracker(source.getResource()).addSource(source);
	}

	@Override
	public JSourceFile getOrLoadSource(RootResource resource) {
		if(resource == null){
			return null;
		}
		ResourceTracker tracker = getOrCreateTracker(resource);
		JSourceFile source = tracker.getSource(resource);
		if(source == null){
			source = JSourceFile.fromResource(resource, getParser());
			tracker.addSource(source);
			log.debug("now tracking changes for " + source.getResource().getFullPath());
		} else {
			log.debug("already tracking change for " + source.getResource().getFullPath());
		}
		return source;
	}
	
	private ResourceTracker getOrCreateTracker(RootResource resource){
		Root root = resource.getRoot();
		String key = root.getFullPath();
		ResourceTracker tracker = sourceTrackers.get(key);
		if(tracker == null){
			tracker = new ResourceTracker(key);
			sourceTrackers.put(key,tracker);
		}
		return tracker;
	}
		
	@Override
    public JAstParser getParser() {
	    return obtain(JAstParser.class);
    }
 
	@Override
    public JCompiler getCompiler() {
        return obtain(JCompiler.class);
    }
	
	@Override
    public JAnnotationCompiler getAnnotationCompiler() {
        return obtain(JAnnotationCompiler.class);
    }
    
	@Override
    public ToSourceConverter getNodeToSourceConverter() {
	    return obtain(ToSourceConverter.class);
	};
	
	@Override
	public Root getDefaultGenerationRoot() {
	    return generationRoot;	    
	};
	
	@Override
	public ProjectLayout getProjectLayout() {
		return projectLayout;
	}
	
	@Override
	public ResourceLoader getResourceLoader() {
	    return resourceLoader;
	}
	
	@Override
	public <T> T obtain(Class<T> type){
		return injector.getInstance(type);
	}
	
    private class DefaultMutationModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(JCompiler.class).to(EclipseCompiler.class).in(Singleton.class);;
        }

        @Provides
        public CodeFormatter provideCodeFormatter() {
            return new DefaultCodeFormatter(getFormattingOptions());
        }

        private DefaultCodeFormatterOptions getFormattingOptions() {
            return formattingOptions;
        }

        @Provides
        public ClashStrategyResolver provideDefaultClashStrategyResolver() {
            return new ClashStrategyResolver.Fixed(provideDefaultClashStrategy());
        }

        @Provides
        public ClashStrategy  provideDefaultClashStrategy() {
            return defaultClashStrategy;
        }

        
        @Named(ContextNames.MARK_GENERATED)
        @Provides
        public boolean provideDefaultMarkGenerated() {
            return markGenerated;
        }

        @Provides
        public ToSourceConverter provideFlattener() {
            return obtain(DefaultToSourceConverter.class);
        }
        
        @Provides
        public JAstFlattener provideToSourceVisitor() {
            return obtain(JAstFlattener.class);
        }

        @Provides
        @Singleton
        public JMutateContext provideContext() {
            return DefaultMutateContext.this;
        }

        @Provides
        @Singleton
        public PlacementStrategy providePlacementStrategy() {
            return placementStrategy;
        }

        @Provides
        @Singleton
        public JAstParser provideParser() {
            return parser;
        }

        @Provides
        @Singleton
        public ProjectOptions provideOptions() {
            return projectOptons;
        }
        
        @Provides
        public SourceTemplate provideSourceTemplate() {
            return new SourceTemplate(provideParser(), generationRoot);
        }
        
        @Provides
        @Singleton
        public ResourceLoader provideResourceLoader() {
            return resourceLoader;
        }
        
        @Provides
        @Singleton
        public ProjectLayout provideProjectResolver() {
            return projectLayout;
        }
    }
    
    static class ResourceTracker {
    	private final String rootPath;
    	private final Map<String, JSourceFile> sources = new HashMap<>();
    	
		public ResourceTracker(String rootPath) {
			super();
			this.rootPath = rootPath;
		}
		
		public JSourceFile getSource(RootResource resource){
			String key = resource.getRelPath();
			JSourceFile source = sources.get(key);
			return source;
		}
		
		public void addSource(JSourceFile source){
			String key = source.getResource().getRelPath();
			sources.put(key, source);
		}
    }
	
	public static class Builder {
	
		private boolean markGenerated = false;
		private JAstParser parser;
		private Collection<Root> roots = new ArrayList<>();
		private DefaultCodeFormatterOptions formattingOptions;
		private Root generateRoot;
		private ProjectLayout projectLayout;
		private ProjectOptions projectOptions;
		private PlacementStrategy placementStrategy;
		private ClashStrategy clashStrategy = ClashStrategy.ERROR;
		
		private Builder(){
		}

		public Builder defaults() {
        	return this;
		}

        public DefaultMutateContext build() {
            ProjectLayout layout = getProjectLayoutOrDefault();
            Root generateTo = getGenerationRootOrDefault(layout);
            ResourceLoader resourceLoader = getResourceLoaderOrDefault(generateTo, layout);
            JAstParser parser = getParserOrDefault(resourceLoader);
            DefaultCodeFormatterOptions formatter = getFormatterOptionsOrDefault();
            PlacementStrategy placementStrategy = getPlacementStrategyOrDefault();
            ProjectOptions options = getProjectOptionsOrDefault();
            
            return new DefaultMutateContext(layout, options, generateTo, parser, markGenerated, formatter, placementStrategy, clashStrategy);
        }
		
        private JAstParser getParserOrDefault(ResourceLoader loader) {
            return parser == null ? newDefaultAstParser(loader) : parser;
        }

		private JAstParser newDefaultAstParser(ResourceLoader loader){
            return JAstParser.with()
                    .defaults()
                    .resourceLoader(loader)
                    .build();
        }
		
		private ResourceLoader getResourceLoaderOrDefault(Root generateTo,ProjectLayout layout){
		    Collection<Root> roots = new ArrayList<>();
		    roots.add(generateTo);
		    if(!this.roots.isEmpty()){
		        roots.addAll(this.roots);
		    } else {
		        roots.addAll(newDefaultRoots(layout));
		    }
            return DefaultResourceLoader.with().roots(roots).build();
        }
		
		private Collection<Root> newDefaultRoots(ProjectLayout layout){
            return Roots.with()
                    .projectLayout(layout)
                    .all()
                    .classpath(true)
                    .build();
        }
		
		private DefaultCodeFormatterOptions getFormatterOptionsOrDefault(){
		    return formattingOptions==null?newDefaultFormatter():formattingOptions;
		}
		
		private DefaultCodeFormatterOptions newDefaultFormatter(){
            return new DefaultCodeFormatterOptions(DefaultCodeFormatterConstants.getJavaConventionsSettings());
        }
		
		private Root getGenerationRootOrDefault(ProjectLayout project){
		    return generateRoot==null?newTmpSnippetRoot(project):generateRoot;
		}
		
	    private Root newTmpSnippetRoot(ProjectLayout project){
	        try {
                File tmpDir = PathUtil.newTmpDir(project.getTmpDir(), "TmpGenRoot", "");
                return new DirectoryRoot(tmpDir,RootType.GENERATED,RootContentType.SRC);
                
            } catch (IOException e) {
                throw new JMutateException("Couldn't create tmp generation root",e);
            }
	    }
	    
	    private PlacementStrategy getPlacementStrategyOrDefault(){
	        return placementStrategy==null?newDefaultPlacementStrategy():placementStrategy;
	    }
	    
	    private PlacementStrategy newDefaultPlacementStrategy(){
            return ConfigurablePlacementStrategy.with().defaults().build();
        }
        
	    private ProjectLayout getProjectLayoutOrDefault(){
	        return projectLayout==null?newDefaultProjectLayout():projectLayout;
	    }
	    
	    private ProjectLayout newDefaultProjectLayout(){
            return new MavenProjectLayout();
        }
        
	    private ProjectOptions getProjectOptionsOrDefault(){
            return projectOptions==null?newDefaultProjectOptions():projectOptions;
        }
	    
	    private ProjectOptions newDefaultProjectOptions(){
            return new DefaultProjectOptions();
        }
	    
	    @Optional
		public Builder markGenerated(boolean markGenerated) {
        	this.markGenerated = markGenerated;
        	return this;
		}

	    @Optional
        public Builder parser(JAstParser parser) {
            this.parser = parser;
            return this;
        }

        @Optional
        public Builder formattingOptions(DefaultCodeFormatterOptions formattingOptions) {
            this.formattingOptions = formattingOptions;
            return this;
        }

        @Optional
        public Builder generationRoot(Root root) {
            this.generateRoot = root;
            return this;
        }

        @Optional
        public Builder placementStrategy(ConfigurablePlacementStrategy strategy) {
            this.placementStrategy = strategy;
            return this;
        }
        
        @Optional
        public Builder projectResolver(ProjectLayout project) {
            this.projectLayout = project;
            return this;
        }

        @Optional
		public Builder clashStrategy(ClashStrategy defaultClashStrategy) {
			this.clashStrategy = defaultClashStrategy;
			return this;
        }  
	}//builder
}
