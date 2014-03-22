package org.codemucker.jmutate.pattern;

import static com.google.common.collect.Lists.newArrayList;
import static org.codemucker.lang.Check.checkNotNull;

import java.util.List;

import org.codemucker.jmutate.ast.CodemuckerException;
import org.codemucker.jmutate.ast.JField;
import org.codemucker.jmutate.ast.JField.SingleJField;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.JModifiers;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.ast.matcher.AJType;
import org.codemucker.jmutate.transform.CodeMuckContext;
import org.codemucker.jmutate.transform.FixImportsTransform;
import org.codemucker.jmutate.transform.InsertCtorTransform;
import org.codemucker.jmutate.transform.InsertMethodTransform;
import org.codemucker.jmutate.transform.InsertTypeTransform;
import org.codemucker.jmutate.transform.SetterMethodBuilder;
import org.codemucker.jmutate.transform.SourceTemplate;
import org.codemucker.jmutate.transform.Transform;
import org.codemucker.jmutate.util.JavaNameUtil;
import org.codemucker.jmutate.util.TypeUtil;
import org.codemucker.match.AbstractNotNullMatcher;
import org.codemucker.match.MatchDiagnostics;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.google.inject.Inject;

/**
 * Add or update a builder to build a bean
 */
public class BeanBuilderTransform implements Transform {

	@Inject
	private CodeMuckContext ctxt;

	private JType target;

	/**
	 * WHat the name of the builder class will be
	 */
	private String builderClassName = "Builder";
	
	/**
	 * How do we collect the fields? Do we keep a local copy, set it on the bean directly via direct field access,
	 * or via the beans setter methods?
	 */
	static enum MODE {
		COPYOF,BEAN_FIELD,BEAN_METHOD;
	}

	private final MODE mode = MODE.COPYOF;

	private boolean useQualifiedName = true;
	
	@Override
	public void transform() {
		checkNotNull("ctxt", ctxt);
		checkNotNull("target", target);

		Iterable<SingleJField> fields = collectSingleFields(findFeildsToInclude(target));
	    
		final JType builder = getOrCreateBuilderClass(target);
		
		ctxt.obtain(BeanBuilderPropertiesPattern.class)
			.setSingleFields(fields)
			.setTarget(builder)
			.apply();
		
	    ctxt.obtain(BeanBuilderFieldCtorPattern.class)
	    	.setTarget(target)
	    	.setSingleFields(fields)
	    	.setUseQualifiedName(useQualifiedName)
	    	.apply();
	    
	    ctxt.obtain(BeanBuilderBuildMethodPattern.class)
	    	.setTarget(builder)
	    	.setBean(target)
	    	.setSingleFields(fields)
	    	.apply();
	    
	    ctxt.obtain(FixImportsTransform.class)
	    	.setAddMissingImports(true)
	    	.setNodeToClean(target)
	    	.apply();
	}

	private static List<SingleJField> collectSingleFields(Iterable<JField> fields) {
		List<SingleJField> singles = newArrayList();
		for(JField field:fields){
			singles.addAll(field.asSingleFields());
		}
		return singles;
	}

	private Iterable<JField> findFeildsToInclude(final JType target){
	    return target.findFieldsMatching(new AbstractNotNullMatcher<JField>() {
            @Override
            public boolean matchesSafely(final JField field, MatchDiagnostics diag) {
                final JModifiers mods = field.getJavaModifiers();
                if( mods.isFinal() || mods.isStatic() || mods.isStrictFp()){
                    return false;
                }
                //TODO:detect annotations. Depending on mode: all by default, or need explicit
                return true;
            }
        });
	}
	
    private JType getOrCreateBuilderClass(final JType type) {
	    JType builder;
	    final List<JType> builders = type.findDirectChildTypesMatching(AJType.withSimpleNameAntPattern(builderClassName)).toList();
		if (builders.size() == 1) {
	    	builder = builders.get(0);
		} else if (builders.size() == 0) {
	    	builder = ctxt.newSourceTemplate()
	    		.setVar("builderClassName",builderClassName)
	    		.pl("public static class ${builderClassName} {} ")
	    		.asJTypeSnippet();

	    	ctxt.obtain(InsertTypeTransform.class)
	    		.setTarget(type)
	    		.setType(builder)
	    		.transform();
	    	//we want a handle to the inserted nodes. These are copied on insert so adding anything to the
	    	//original node doesn't make it in. Hence we need to lookup the newly created
	    	//builder
	    	builder = type.findDirectChildTypesMatching(AJType.withSimpleNameAntPattern(builderClassName)).toList().get(0);
	    } else {
	    	throw new CodemuckerException("expected only a single builder nameed '%s' on type %s", builderClassName, type);
	    }
	    return builder;
    }

	public BeanBuilderTransform setCtxt(final CodeMuckContext ctxt) {
    	this.ctxt = ctxt;
    	return this;
    }

	public BeanBuilderTransform setTarget(final JType type) {
    	this.target = type;
    	return this;
	}
	
	public BeanBuilderTransform setUseQualifiedName(boolean useQualifiedName) {
		this.useQualifiedName = useQualifiedName;
		return this;
	}
	
	public static class BeanBuilderBuildMethodPattern {
		@Inject
		private CodeMuckContext ctxt;
		//rename, possibly not a bean ..?
		private JType bean;
		//rename to builder?
		private JType target;
		private List<SingleJField> fields;
		
		
		public void apply() {
			checkNotNull("ctxt", ctxt);
			checkNotNull("target", target);
			checkNotNull("bean", bean);
			checkNotNull("fields", fields);

			JMethod buildMethod = createBuildMethod();
			ctxt.obtain(InsertMethodTransform.class)
				.setTarget(target)
				.setMethod(buildMethod)
				.transform();
		}

		private JMethod createBuildMethod() {
			SourceTemplate t = ctxt.newSourceTemplate();
			t.setVar("beanType", bean.getSimpleName());
			t.p("public ${beanType} build(){");
			t.p("return new ${beanType}(");
			boolean comma = false;
			//TODO:use single fields!
			for (SingleJField f : fields) {
				if (comma) {
					t.p(",");
				}
				comma = true;
				t.p(f.getName());
			}
			t.p(");");
			t.p("}");
			
			JMethod buildMethod = t.asResolvedJMethod();
			return buildMethod;
		}
		
		public BeanBuilderBuildMethodPattern setCtxt(CodeMuckContext ctxt) {
			this.ctxt = ctxt;
			return this;
		}
		
		public BeanBuilderBuildMethodPattern setBean(JType bean) {
			this.bean = bean;
			return this;
		}

		public BeanBuilderBuildMethodPattern setTarget(JType target) {
			this.target = target;
			return this;
		}

		public BeanBuilderBuildMethodPattern setFields(Iterable<JField> fields) {
			List<SingleJField> singles = newArrayList();
			for(JField field:fields){
				singles.addAll(field.asSingleFields());
			}
			setSingleFields(singles);
			return this;
		}
		
		public BeanBuilderBuildMethodPattern setSingleFields(Iterable<SingleJField> fields) {
			this.fields = newArrayList(fields);
			return this;
		}
	}

	public static class BeanBuilderFieldCtorPattern {
		@Inject
		private CodeMuckContext ctxt;
		private JType target;
		private List<SingleJField> fields;
		private Boolean useQualifiedName = true;
		
		private void apply() {
			checkNotNull("ctxt", ctxt);
			checkNotNull("target", target);
			checkNotNull("fields", fields);

			final MethodDeclaration ctor = createCtorFromFields();
	
		    ctxt.obtain(InsertCtorTransform.class)
		        .setTarget(target)
		        .setCtor(ctor)
		        .transform();
		}
	
	    private MethodDeclaration createCtorFromFields() {
	        final SourceTemplate t = ctxt.newSourceTemplate();
	
		    t.setVar("ctorName", target.getSimpleName());
	
		    t.pl("public ${ctorName}(");
		    boolean comma = false;
		    
		    for(final SingleJField field:fields){
		    	if( comma ){
		            t.pl(",");
		        }
		        comma = true;
		       // if( field.getType().isParameterizedType())
				if (useQualifiedName) {
					t.p(JavaNameUtil.resolveQualifiedName(field.getType()));
				} else {
					t.p(TypeUtil.toTypeSignature(field.getType()));
				}
				t.p(" ");
		        t.p(field.getName());
		    }
		    t.pl(") {");
	
		    for(final SingleJField field:fields){
	            t.p("this.").p(field.getName()).p("=").p(field.getName()).pl(";");
	        }
	
		    t.pl("}");

		    final MethodDeclaration ctor = t.asResolvedConstructorNode();
	        return ctor;
	    }

		public BeanBuilderFieldCtorPattern setCtxt(CodeMuckContext ctxt) {
			this.ctxt = ctxt;
			return this;
		}

		public BeanBuilderFieldCtorPattern setTarget(JType target) {
			this.target = target;
			return this;
		}

		public BeanBuilderFieldCtorPattern setFields(Iterable<JField> fields) {
			List<SingleJField> singles = newArrayList();
			for(JField field:fields){
				singles.addAll(field.asSingleFields());
			}
			setSingleFields(singles);
			return this;
		}
		
		public BeanBuilderFieldCtorPattern setSingleFields(Iterable<SingleJField> fields) {
			this.fields = newArrayList(fields);
			return this;
		}

		public BeanBuilderFieldCtorPattern setUseQualifiedName(Boolean useQualaifiedName) {
			this.useQualifiedName = useQualaifiedName;
			return this;
		}
		
	}

    public static class BeanBuilderPropertiesPattern {
    	
    	@Inject
    	private CodeMuckContext ctxt;
    	private JType target;
    	private List<SingleJField> fields;
		
		public void apply() {	
			checkNotNull("ctxt", ctxt);
			checkNotNull("target", target);
			checkNotNull("fields", fields);

			//add the builder fields and setters
		    final BeanPropertyTransform pattern = ctxt.obtain(BeanPropertyTransform.class)
		    	.setTarget(target)
		    	.setCreateAccessor(false)
		    	.setSetterReturn(SetterMethodBuilder.RETURN.TARGET);
	
		    for(final SingleJField field:fields){
		    	pattern.setPropertyType(field.getType());
				pattern.setPropertyName(field.getName());
				pattern.transform();
		    }
	    }
		
		public BeanBuilderPropertiesPattern setCtxt(CodeMuckContext ctxt) {
			this.ctxt = ctxt;
			return this;
		}

		public BeanBuilderPropertiesPattern setTarget(JType target) {
			this.target = target;
			return this;
		}

		public BeanBuilderPropertiesPattern setFields(Iterable<JField> fields) {
			List<SingleJField> singles = newArrayList();
			for(JField field:fields){
				singles.addAll(field.asSingleFields());
			}
			setSingleFields(singles);
			return this;
		}
		
		public BeanBuilderPropertiesPattern setSingleFields(Iterable<SingleJField> fields) {
			this.fields = newArrayList(fields);
			return this;
		}
	    
    }
}