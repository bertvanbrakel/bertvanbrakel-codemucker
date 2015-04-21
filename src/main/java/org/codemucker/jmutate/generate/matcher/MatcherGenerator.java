package org.codemucker.jmutate.generate.matcher;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codemucker.jmatch.PropertyMatcher;
import org.codemucker.jmutate.JMutateContext;
import org.codemucker.jmutate.JMutateException;
import org.codemucker.jmutate.ast.JSourceFile;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.generate.SmartConfig;
import org.codemucker.jmutate.generate.matcher.MatcherGenerator.GenerateMatcherOptions;
import org.codemucker.jmutate.generate.model.TypeModel;
import org.codemucker.jmutate.generate.model.pojo.PojoModel;
import org.codemucker.jmutate.generate.model.pojo.PropertyModelExtractor;
import org.codemucker.jpattern.generate.matcher.GenerateMatcher;

import com.google.inject.Inject;

/**
 * Generates the matcher for a single pojo
 */
public class MatcherGenerator extends AbstractMatchGenerator<GenerateMatcher,GenerateMatcherOptions> {

    private static final Logger LOG = LogManager.getLogger(MatcherGenerator.class);

    @Inject
    public MatcherGenerator(JMutateContext ctxt) {
    	super(ctxt,GenerateMatcher.class,GenerateMatcherOptions.class);
    }

	@Override
	public void generate(JType declaredInType, SmartConfig config,GenerateMatcherOptions options) {	
//		if(!options.keepInSync){
//			return;
//		}
		PropertyModelExtractor extractor = ctxt.obtain(PropertyModelExtractor.Builder.class)
				.includeSuperClass(options.inheritParentProperties)
				.build();
		
		JSourceFile source = declaredInType.getCompilationUnit().getSource();
		String generateFor = options.generateFor;
		if(generateFor == null || Object.class.getName().equals(generateFor)){
			throw new JMutateException("need to set the 'generateFor' in " + source.getResource().getFullPath() + " for annotation " + GenerateMatcher.class + " other than blank or Object");
		}
		
		PojoModel model = null;
		JType pojoType = ctxt.getSourceLoader().loadTypeForClass(generateFor);
		if(pojoType!= null){
			model = extractor.extractModelFromClass(pojoType);
		}
		//no source code found, let's try class
		if(model == null){
			Class<?> pojoClass = ctxt.getSourceLoader().loadClassOrNull(generateFor);
			if(pojoClass != null){
				model = extractor.extractModelFromClass(pojoClass);
			}
		}
		
		if(model == null){
			throw new JMutateException("couldn't load source or class '" + generateFor + "' to generate matcher, set in 'generateFor' in " + source.getResource().getFullPath() + " for annotation " + GenerateMatcher.class);
		}

		String superType = declaredInType.getSuperTypeFullName();
		if(superType != null){
			//TODO:check correct super class. 
		}
		
		//TOO:if not exists super!
		declaredInType.asMutator(ctxt).setExtends(PropertyMatcher.class.getName() + "<" + generateFor + ">");
		
		generateDefaultConstructor(source.getMainType(), new TypeModel(generateFor));
    	
		generateMatcher(options,model, declaredInType);
		writeToDiskIfChanged(source);
	}


	public static class GenerateMatcherOptions extends AbstractMatcherModel<GenerateMatcher> {
		public String generateFor;
    	public boolean oneTimeOnly;
    	public String matcherBaseClass;
    	public String matcherPrefix;
    	public String[] builderMethodNames;
    	
    }


}