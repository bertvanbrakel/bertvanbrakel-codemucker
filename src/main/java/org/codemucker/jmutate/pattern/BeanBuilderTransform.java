package org.codemucker.jmutate.pattern;

import static com.google.common.collect.Lists.newArrayList;
import static org.codemucker.lang.Check.checkNotNull;

import java.util.List;

import org.codemucker.jmatch.AbstractNotNullMatcher;
import org.codemucker.jmatch.MatchDiagnostics;
import org.codemucker.jmutate.JMutateContext;
import org.codemucker.jmutate.JMutateException;
import org.codemucker.jmutate.SourceTemplate;
import org.codemucker.jmutate.ast.JField;
import org.codemucker.jmutate.ast.JField.SingleJField;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.JModifier;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.ast.matcher.AJType;
import org.codemucker.jmutate.builder.JMethodSetterBuilder;
import org.codemucker.jmutate.transform.CleanImportsTransform;
import org.codemucker.jmutate.transform.InsertCtorTransform;
import org.codemucker.jmutate.transform.InsertMethodTransform;
import org.codemucker.jmutate.transform.InsertTypeTransform;
import org.codemucker.jmutate.transform.Transform;
import org.codemucker.jmutate.util.NameUtil;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.google.inject.Inject;

/**
 * Add or update a builder to build a bean
 */
public class BeanBuilderTransform implements Transform {

	@Inject
	private JMutateContext ctxt;

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
	    
	    ctxt.obtain(CleanImportsTransform.class)
	    	.addMissingImports(true)
	    	.nodeToClean(target)
	    	.transform();
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
                final JModifier mods = field.getModifiers();
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
	    final List<JType> builders = type.findTypesMatching(AJType.with().simpleName(builderClassName)).toList();
		if (builders.size() == 1) {
	    	builder = builders.get(0);
		} else if (builders.size() == 0) {
	    	builder = ctxt.newSourceTemplate()
	    		.setVar("builderClassName",builderClassName)
	    		.pl("public static class ${builderClassName} {} ")
	    		.asJTypeSnippet();

	    	ctxt.obtain(InsertTypeTransform.class)
	    		.target(type)
	    		.setType(builder)
	    		.transform();
	    	//we want a handle to the inserted nodes. These are copied on insert so adding anything to the
	    	//original node doesn't make it in. Hence we need to lookup the newly created
	    	//builder
	    	builder = type.findTypesMatching(AJType.with().simpleName(builderClassName)).toList().get(0);
	    } else {
	    	throw new JMutateException("expected only a single builder nameed '%s' on type %s", builderClassName, type);
	    }
	    return builder;
    }

	public BeanBuilderTransform setCtxt(final JMutateContext ctxt) {
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
	
	public static class BeanBuilderBuildMethodPattern implements Pattern {
		@Inject
		private JMutateContext ctxt;
		//rename, possibly not a bean ..?
		private JType bean;
		//rename to builder?
		private JType target;
		private List<SingleJField> fields;
		
		@Override
		public void apply() {
			checkNotNull("ctxt", ctxt);
			checkNotNull("target", target);
			checkNotNull("bean", bean);
			checkNotNull("fields", fields);

			JMethod buildMethod = createBuilderBuildMethod();
			ctxt.obtain(InsertMethodTransform.class)
				.target(target)
				.method(buildMethod)
				.transform();
		}

		private JMethod createBuilderBuildMethod() {
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
		
		public BeanBuilderBuildMethodPattern setCtxt(JMutateContext ctxt) {
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

	/**
	 * Add a constructor to a bean which assigns all the fields at once
	 */
	public static class BeanBuilderFieldCtorPattern implements Pattern {
		@Inject
		private JMutateContext ctxt;
		private JType target;
		private List<SingleJField> fields;
		private Boolean useQualifiedName = true;
		
		@Override
		public void apply() {
			checkNotNull("ctxt", ctxt);
			checkNotNull("target", target);
			checkNotNull("fields", fields);

			final MethodDeclaration ctor = createCtorFromFields();
	
		    ctxt.obtain(InsertCtorTransform.class)
		        .target(target)
		        .setCtor(ctor)
		        .transform();
		}
	
		/**
		 * Create a constructor which assigns all the fields at once
		 */
	    private MethodDeclaration createCtorFromFields() {
	        final SourceTemplate t = ctxt.newSourceTemplate();
	
		    t.setVar("ctorName", target.getSimpleName());
	
		    t.pl("public ${ctorName}(");
		    generateCtorArgs(t);
		    t.pl(") {");
		    generateFieldAssignments(t);
		    t.pl("}");

		    final MethodDeclaration ctor = t.asResolvedConstructorNode();
	        return ctor;
	    }

        private void generateFieldAssignments(final SourceTemplate t) {
            for(final SingleJField field:fields){
	            t.p("this.").p(field.getName()).p("=").p(field.getName()).pl(";");
	        }
        }

        private void generateCtorArgs(final SourceTemplate t) {
            boolean comma = false;
		    for(final SingleJField field:fields){
		    	if( comma ){
		            t.pl(",");
		        }
		        comma = true;
		       // if( field.getType().isParameterizedType())
				if (useQualifiedName) {
					t.p(NameUtil.resolveQualifiedName(field.getType()));
				} else {
					t.p(NameUtil.resolveQualifiedName(field.getType()));
				}
				t.p(" ");
		        t.p(field.getName());
		    }
        }

		public BeanBuilderFieldCtorPattern setCtxt(JMutateContext ctxt) {
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

	/**
	 * Add bean f
	 */
    public static class BeanBuilderPropertiesPattern implements Pattern {
    	
    	@Inject
    	private JMutateContext ctxt;
    	private JType target;
    	private List<SingleJField> fields;
		
    	@Override
		public void apply() {	
			checkNotNull("ctxt", ctxt);
			checkNotNull("target", target);
			checkNotNull("fields", fields);

			//set the transform options
		    final BeanPropertyTransform transform = ctxt.obtain(BeanPropertyTransform.class)
		    	.setTarget(target)
		    	.setCreateGetter(false)
		    	.setSetterReturns(JMethodSetterBuilder.RETURNS.TARGET);
	
		    //apply transform for each field
		    for(final SingleJField field:fields){
		    	transform.setPropertyType(field.getType());
				transform.setPropertyName(field.getName());
				//apply it
				transform.transform();
		    }
	    }
		
		public BeanBuilderPropertiesPattern setCtxt(JMutateContext ctxt) {
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