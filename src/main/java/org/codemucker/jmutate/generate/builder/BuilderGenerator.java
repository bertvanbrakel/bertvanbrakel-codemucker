package org.codemucker.jmutate.generate.builder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codemucker.jfind.FindResult;
import org.codemucker.jfind.ReflectedClass;
import org.codemucker.jfind.ReflectedField;
import org.codemucker.jfind.matcher.AField;
import org.codemucker.jfind.matcher.AMethod;
import org.codemucker.jfind.matcher.AnAnnotation;
import org.codemucker.jmatch.AString;
import org.codemucker.jmatch.Matcher;
import org.codemucker.jmatch.expression.ExpressionParser;
import org.codemucker.jmatch.expression.StringMatcherBuilderCallback;
import org.codemucker.jmutate.ClashStrategyResolver;
import org.codemucker.jmutate.CodeFinder;
import org.codemucker.jmutate.JMutateContext;
import org.codemucker.jmutate.SourceTemplate;
import org.codemucker.jmutate.ast.JAccess;
import org.codemucker.jmutate.ast.JAnnotation;
import org.codemucker.jmutate.ast.JField;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.JSourceFile;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.ast.matcher.AJAnnotation;
import org.codemucker.jmutate.ast.matcher.AJField;
import org.codemucker.jmutate.ast.matcher.AJMethod;
import org.codemucker.jmutate.ast.matcher.AJModifier;
import org.codemucker.jmutate.generate.AbstractCodeGenerator;
import org.codemucker.jmutate.generate.CodeGenMetaGenerator;
import org.codemucker.jmutate.generate.GeneratorConfig;
import org.codemucker.jmutate.transform.CleanImportsTransform;
import org.codemucker.jmutate.transform.InsertFieldTransform;
import org.codemucker.jmutate.transform.InsertMethodTransform;
import org.codemucker.jmutate.util.NameUtil;
import org.codemucker.jpattern.bean.NotAProperty;
import org.codemucker.jpattern.bean.Property;
import org.codemucker.jpattern.generate.ClashStrategy;
import org.codemucker.jpattern.generate.GenerateBuilder;
import org.codemucker.lang.BeanNameUtil;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.google.common.base.Strings;
import com.google.inject.Inject;

/**
 * Generates per class builders
 */
public class BuilderGenerator extends AbstractCodeGenerator<GenerateBuilder> {

    private final Logger log = LogManager.getLogger(BuilderGenerator.class);

    static final String VOWELS_UPPER = "AEIOU";
	
    private final Matcher<Annotation> reflectedAnnotationIgnore = AnAnnotation.with().fullName(AString.matchingAntPattern("*.Ignore"));
    private final Matcher<JAnnotation> sourceAnnotationIgnore = AJAnnotation.with().fullName(AString.matchingAntPattern("*.Ignore"));
    
    private final JMutateContext ctxt;
    private ClashStrategyResolver methodClashResolver;

	private final CodeGenMetaGenerator genInfo;

    @Inject
    public BuilderGenerator(JMutateContext ctxt) {
        this.ctxt = ctxt;
        this.genInfo = new CodeGenMetaGenerator(ctxt,getClass());
    }

	@Override
	public void generate(JType optionsDeclaredInNode, GeneratorConfig options) {
		BuilderModel model = new BuilderModel(optionsDeclaredInNode, options);
        
		
		ClashStrategy methodClashDefaultStrategy = model.getClashStrategy();
		methodClashResolver = new OnlyReplaceMyManagedMethodsResolver(methodClashDefaultStrategy);
		Matcher<String> fieldMatcher = fieldMatcher(model.getFieldNames());
        
        extractAllProperties(optionsDeclaredInNode, fieldMatcher, model);
        
        //TODO:enable builder creation for 3rd party compiled classes
        generateBuilder(optionsDeclaredInNode, model);
	}

	private void extractAllProperties(JType optionsDeclaredInNode,
			Matcher<String> fieldMatcher, BuilderModel model) {
		if(model.isInheritSuperBeanProperties()){
        	CodeFinder finder = ctxt.obtain(CodeFinder.class).failOnNotFound(false);
        	String superTypeName = NameUtil.removeGenericOrArrayPart(optionsDeclaredInNode.getSuperTypeFullName());
        	while(!Object.class.getName().equals(superTypeName)){
	        	JType superType = finder.findJTypeForClass(superTypeName);
		        if(superType == null){
		        	break;
		        }
	        	extractDirectProperties(model, superType, fieldMatcher, true);
	        	superTypeName = NameUtil.removeGenericOrArrayPart(superType.getSuperTypeFullName());
        	}
        }
        extractDirectProperties(model, optionsDeclaredInNode, fieldMatcher,false);
	}

	private static <T> T getOr(T val, T defaultVal) {
        if (val == null) {
            return defaultVal;
        }
        return val;
    }

	private Matcher<String> fieldMatcher(String s){
		if(Strings.isNullOrEmpty(s)){
			return AString.equalToAnything();
		}
		return ExpressionParser.parse(s, new StringMatcherBuilderCallback());
	}

	private void generateBuilder(JType optionsDeclaredInNode,BuilderModel model) {
		boolean markGenerated = model.isMarkGenerated();
		
		JSourceFile source = optionsDeclaredInNode.getSource();
		JType pojo = source.getMainType();
		JType builder;
		
		boolean isAbstract = model.isSupportSubclassing();

		if(pojo.getSimpleName().equals(model.getBuilderTypeSimpleRaw())){
			builder = pojo;
		} else{
			builder = pojo.getChildTypeWithNameOrNull(model.getBuilderTypeSimpleRaw());
			if(builder == null){
				SourceTemplate t = ctxt.newSourceTemplate()
					.var("typeBounds", model.getPojoType().getTypeBoundsOrNull())
					.var("type", model.getBuilderTypeSimpleRaw() + Strings.nullToEmpty(model.getBuilderTypeBoundsOrNull()))
					.var("modifier", (isAbstract ?"abstract":""))
					.pl("public static ${modifier} class ${type} { }")
				;
				
				pojo.asMutator(ctxt).addType(t.asResolvedTypeNodeNamed(null));
				builder = pojo.getChildTypeWithName(model.getBuilderTypeSimpleRaw());
				genInfo.addGeneratedMarkers(builder.asAbstractTypeDecl());
			}
		}
		//generate the with() method and aliases
		generateStaticBuilderCreateMethods(model, pojo);
		//TODO:builder ctor
		//TODO:builder clone/from method
		
		//add the self() method
		if(!"this".equals(model.getBuilderSelfAccessor())){
			SourceTemplate selfMethod = ctxt.newSourceTemplate()
				.var("selfType", model.getBuilderSelfType())
				.var("selfGetter", model.getBuilderSelfAccessor())
				
				.pl("protected ${selfType} ${selfGetter} { return (${selfType})this; }");		
			
			addMethod(builder, selfMethod.asMethodNodeSnippet(),markGenerated);	
		}
		
		for (BuilderPropertyModel property : model.getProperties()) {
			generateField(markGenerated, builder, property);
			generateSetter(model, markGenerated, builder, property);
			generateCollectionAddRemove(builder, model, property);
			generateMapAddRemove(builder, model, property);
		}
		
		generateAllArgCtor(pojo, model);
		generateBuildMethod(builder, model);
		
		writeToDiskIfChanged(source);
	}

	private void generateSetter(BuilderModel model, boolean markGenerated,JType builder, BuilderPropertyModel property) {

		SourceTemplate setterMethod = ctxt.newSourceTemplate()
			.var("p.name", property.propertyName)
			.var("p.type", property.type.getFullName())
			.var("p.type_raw", property.type.getFullNameRaw())
			.var("self.type", model.getBuilderSelfType())
			.var("self.getter", model.getBuilderSelfAccessor())
			
			.pl("public ${self.type} ${p.name}(final ${p.type} val){")
			.pl("	this.${p.name} = val;")
			.pl("	return ${self.getter};")
			.pl("}");
			
		addMethod(builder, setterMethod.asMethodNodeSnippet(),markGenerated);
	}

	private void generateField(boolean markGenerated, JType builder,BuilderPropertyModel property) {
		SourceTemplate field = ctxt.newSourceTemplate()
			.var("p.name", property.propertyName)
			.var("p.type", property.type.getFullName())
			.pl("private ${p.type} ${p.name};");
			
			addField(builder, field.asFieldNodeSnippet(),markGenerated);
	}

	
	private void generateStaticBuilderCreateMethods(BuilderModel model,JType beanType) {
		if(model.isGenerateStaticBuilderMethod()){
			SourceTemplate t = ctxt
					.newSourceTemplate()
					.var("self.type", model.getBuilderTypeSimple())
					.var("typeBounds", model.getPojoType().getTypeBoundsOrEmpty());
		
			for (String name : model.getStaticBuilderMethodNames()) {
				addMethod(beanType,t.child().pl("public static ${typeBounds} ${self.type} " + name + " (){ return new ${self.type}(); }").asMethodNodeSnippet(),model.isMarkGenerated());
			}
		}
	}
	
	private void generateCollectionAddRemove(JType bean, BuilderModel model,BuilderPropertyModel property) {
		if(property.type.isCollection() && model.isGenerateAddRemoveMethodsForIndexedProperties()){
			SourceTemplate add = ctxt
				.newSourceTemplate()
				.var("p.name", property.propertyName)
				.var("p.addName", property.propertyAddName)
				.var("p.type", property.type.getObjectTypeFullName())
				.var("p.newType", property.propertyConcreteType)
				.var("p.genericPart", property.type.getGenericPartOrEmpty())
				.var("p.valueType", property.type.getIndexedValueTypeNameOrNull())
				.var("self.type", model.getBuilderSelfType())
				.var("self.getter", model.getBuilderSelfAccessor())
				
				.pl("public ${self.type} ${p.addName}(final ${p.valueType} val){")
				.pl("	if(this.${p.name} == null){ ")
				.pl("		this.${p.name} = new ${p.newType}${p.genericPart}(); ")
				.pl("	}")	
				.pl("	this.${p.name}.add(val);")
				.pl("	return ${self.getter};")
				.pl("}");
				
			addMethod(bean, add.asMethodNodeSnippet(),model.isMarkGenerated());
			
			SourceTemplate remove = ctxt
				.newSourceTemplate()
				.var("p.name", property.propertyName)
				.var("p.removeName", property.propertyRemoveName)
				.var("p.type", property.type.getObjectTypeFullName())
				.var("p.newType", property.propertyConcreteType)
				.var("p.valueType", property.type.getIndexedValueTypeNameOrNull())
				.var("self.type", model.getBuilderSelfType())
				.var("self.getter", model.getBuilderSelfAccessor())
				
				.pl("public ${self.type} ${p.removeName}(final ${p.valueType} val){")
				.pl("	if(this.${p.name} != null){ ")
				.pl("		this.${p.name}.remove(val);")
				.pl("	}")
				.pl("	return ${self.getter};")
				.pl("}");
				
			addMethod(bean, remove.asMethodNodeSnippet(),model.isMarkGenerated());
		}
	}
	
	private void generateMapAddRemove(JType bean, BuilderModel model,BuilderPropertyModel property) {
		if(property.type.isMap() && model.isGenerateAddRemoveMethodsForIndexedProperties()){
			SourceTemplate add = ctxt
				.newSourceTemplate()
				.var("p.name", property.propertyName)
				.var("p.addName", property.propertyAddName)
				.var("p.type", property.type.getObjectTypeFullName())
				.var("p.newType", property.propertyConcreteType)
				.var("p.genericPart", property.type.getGenericPartOrEmpty())
				.var("p.keyType", property.type.getIndexedKeyTypeNameOrNull())
				.var("p.valueType", property.type.getIndexedValueTypeNameOrNull())
				.var("self.type", model.getBuilderSelfType())
				.var("self.getter", model.getBuilderSelfAccessor())
					
				.pl("public ${self.type} ${p.addName}(final ${p.keyType} key,final ${p.valueType} val){")
				.pl("	if(this.${p.name} == null){ this.${p.name} = new ${p.newType}${p.genericPart}(); }")
				.pl("	this.${p.name}.put(key, val);")
				.pl("	return ${self.getter};")
				.pl("}");
				
			addMethod(bean, add.asMethodNodeSnippet(),model.isMarkGenerated());
			
			SourceTemplate remove = ctxt
				.newSourceTemplate()
				.var("p.name", property.propertyName)
				.var("p.removeName", property.propertyRemoveName)
				.var("p.type", property.type.getObjectTypeFullName())
				.var("p.newType", property.propertyConcreteType)
				.var("p.keyType", property.type.getIndexedKeyTypeNameOrNull())
				.var("self.type", model.getBuilderSelfType())
				.var("self.getter", model.getBuilderSelfAccessor())
				
				.pl("public ${self.type} ${p.removeName}(final ${p.keyType} key){")
				.pl("	if(this.${p.name} != null){ ")
				.pl("		this.${p.name}.remove(key);")
				.pl("	}")
				.pl("	return ${self.getter};")
				.pl("}");
				
			addMethod(bean, remove.asMethodNodeSnippet(),model.isMarkGenerated());
		}
	}

	private void generateBuildMethod(JType builder,BuilderModel model) {
		if(!model.isSupportSubclassing()){
			SourceTemplate buildMethod = ctxt
				.newSourceTemplate()
				.var("b.type", model.getPojoType().getSimpleName())
				.var("buildName", model.getBuildPojoMethodName())	
				.pl("public ${b.type} ${buildName}(){")
				.p("	return new ${b.type}(");
			
			boolean comma = false;
			for (BuilderPropertyModel property : model.getProperties()) {
				if(comma){
					buildMethod.p(",");
				}
				buildMethod.p( "this." + property.propertyName);
				comma = true;
			}
			buildMethod.pl(");");
			buildMethod.pl("}");
			addMethod(builder, buildMethod.asMethodNodeSnippet(),model.isMarkGenerated());
		}
	}

	private void generateAllArgCtor(JType beanType, BuilderModel model) {
		if(!beanType.isAbstract()){
			SourceTemplate beanCtor = ctxt
					.newSourceTemplate()
					.var("b.name", model.getPojoType().getSimpleNameRaw())
					.pl("private ${b.name} (");
			
			boolean comma = false;
			//args
			for (BuilderPropertyModel property : model.getProperties()) {
				if(comma){
					beanCtor.p(",");
				}
				if(model.isMarkCtorArgsAsProperties()){
					beanCtor.p("@" + Property.class.getName() + "(name=\"" + property.propertyName + "\") ");
				}
				beanCtor.p(property.type.getFullName() + " " + property.propertyName);
				comma = true;
			}
			
			beanCtor.pl("){");
			//field assignments
			for (BuilderPropertyModel property : model.getProperties()) {
				//TODO:figure out if we can set field directly, via setter, or via ctor args
				//for now assume a setter
				if(property.isFromSuperClass()){
					beanCtor.pl(property.propertySetterName + "(" + property.propertyName + ");");
				} else {
					beanCtor.pl("this." + property.propertyName + "=" + property.propertyName + ";");
				}
				comma = true;
			}
			beanCtor.pl("}");
			addMethod(beanType, beanCtor.asConstructorNodeSnippet(),model.isMarkGenerated());
		}
	}
	
	private void addField(JType type, FieldDeclaration f, boolean markGenerated) {
		if(markGenerated){
			genInfo.addGeneratedMarkers(f);
		}
		ctxt
			.obtain(InsertFieldTransform.class)
			.clashStrategy(ClashStrategy.REPLACE)
			.target(type)
			.field(f)
			.transform();
	}
	
	private void addMethod(JType type, MethodDeclaration m,boolean markGenerated) {
		if(markGenerated){
			genInfo.addGeneratedMarkers(m);
		}
		ctxt
			.obtain(InsertMethodTransform.class)
			.clashStrategy(methodClashResolver)
			.target(type)
			.method(m)
			.transform();
	}

	private void extractProperties(BuilderModel model, Class<?> requestType) {
        ReflectedClass requestBean = ReflectedClass.from(requestType);
        FindResult<Field> fields = requestBean.findFieldsMatching(AField.that().isNotStatic().isNotNative());
        log.trace("found " + fields.toList().size() + " fields");
        for (Field f : fields) {
            ReflectedField field = ReflectedField.from(f);
            if (field.hasAnnotation(reflectedAnnotationIgnore) || field.hasAnnotation(NotAProperty.class) || field.getName().startsWith("_")) {
                log("ignoring field:" + f.getName());
                continue;
            }

            String getterName = BeanNameUtil.toGetterName(field.getName(), field.getType());
            String getter = getterName + "()";
            if (!requestBean.hasMethodMatching(AMethod.with().name(getterName).numArgs(0))) {
                if (!field.isPublic()) {
                    //can't access field, lets skip
                	continue;
                }
                getter = field.getName();// direct field access
            }
            //property.propertyGetter = getter;
            BuilderPropertyModel property = new BuilderPropertyModel(model, f.getName(), f.getGenericType().getTypeName(),true);
            
            model.addProperty(property);
        }
    }

    private void extractDirectProperties(BuilderModel model, JType pojoType, Matcher<String> fieldMatcher, boolean superClass) {
        // call request builder methods for each field/method exposed
        FindResult<JField> fields = pojoType.findFieldsMatching(AJField.with().modifier(AJModifier.that().isNotStatic().isNotNative()).name(fieldMatcher));
        log("found " + fields.toList().size() + " fields");
        for (JField field: fields) {
            if (field.getAnnotations().contains(sourceAnnotationIgnore) || field.getAnnotations().contains(NotAProperty.class) || field.getName().startsWith("_")) {
                log("ignoring field:" + field.getName());
                continue;
            }
            String getterName = BeanNameUtil.toGetterName(field.getName(), NameUtil.isBoolean(field.getFullTypeName()));
            String getter = getterName + "()";
            boolean hasGetter = pojoType.hasMethodMatching(AJMethod.with().name(getterName).numArgs(0)); 
            if (!hasGetter) {
                log("no method " + getter);
                if (!field.getJModifiers().isPublic()) {
                    //can't access field, lets skip
                	continue;
                }
                getter = field.getName();// direct field access
            }
            //property.propertyGetter = getter;
            BuilderPropertyModel property = new BuilderPropertyModel(model, field.getName(), field.getFullTypeName(),superClass);
            
            property.hasField = true;
            property.finalField = field.isFinal();
            property.readOnly = field.isFinal() || (field.isAccess(JAccess.PRIVATE) && !hasGetter);
            model.addProperty(property);
        }
    }

    private void writeToDiskIfChanged(JSourceFile source) {
        if (source != null) {
            cleanupImports(source.getAstNode());
            source = source.asMutator(ctxt).writeModificationsToDisk();
        }
    }

    private void cleanupImports(ASTNode node) {
        ctxt.obtain(CleanImportsTransform.class)
            .addMissingImports(true)
            .nodeToClean(node)
            .transform();
    }

    private void log(String msg) {
        log.debug(msg);
    }

	
	private class OnlyReplaceMyManagedMethodsResolver implements ClashStrategyResolver{

		private final ClashStrategy fallbackStrategy;
		
		public OnlyReplaceMyManagedMethodsResolver(ClashStrategy fallbackStrategy) {
			super();
			this.fallbackStrategy = fallbackStrategy;
		}

		@Override
		public ClashStrategy resolveClash(ASTNode existingNode,ASTNode newNode) {
			if(genInfo.isManagedByThis(JMethod.from(existingNode).getAnnotations())){
				return ClashStrategy.REPLACE;
			}
			return fallbackStrategy;
		}
		
	}

}