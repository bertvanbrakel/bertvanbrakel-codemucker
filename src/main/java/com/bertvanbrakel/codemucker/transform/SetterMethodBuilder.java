package com.bertvanbrakel.codemucker.transform;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.bertvanbrakel.codemucker.ast.JAccess;
import com.bertvanbrakel.codemucker.ast.JMethod;
import com.bertvanbrakel.codemucker.ast.JType;
import com.bertvanbrakel.test.util.ClassNameUtil;

public class SetterMethodBuilder {

	public static enum RETURN {
		VOID, TARGET, ARG;
	}

	private boolean markedGenerated;
	private String pattern = "bean.setter";
	private String name;
	private String type;
	private RETURN returnType;
	private JType target;
	private MutationContext context;
	private JAccess access = JAccess.PUBLIC;
	
	public JMethod build(){
		checkState(context != null, "missing context");
		checkState(target != null, "missing target");
		checkState(name != null, "missing name");
		checkState(type != null, "missing type");

		return new JMethod(toMethod());
	}
	
	private MethodDeclaration toMethod(){
		SourceTemplate template = context.newSourceTemplate();
		
		String upperName = ClassNameUtil.upperFirstChar(name);
		template
			.setVar("methodName", "set" + upperName)
			.setVar("argType", type)
			.setVar("argName", name)
			.setVar("fieldName", name);

		if( markedGenerated){
			template.print("@Pattern(name=\"");
			template.print(pattern);
			template.println("\")");
		}
		template.print(access.toCode());
		template.println(" ${returnType} ${methodName}(${argType} ${argName}) {");
		template.println("this.${fieldName} = ${argName};");
		switch(returnType){
		case ARG:
			template.setVar("returnType", type);
			template.println("return this;");
			break;
		case TARGET:
			template.setVar("returnType", target.getSimpleName());
			template.println("return ${argName};");
			break;
		case VOID:
			template.setVar("returnType", "void");
			break;
		default:
			checkArgument(false,"don't know how to handle return type:" + returnType);
		}
		template.println("}");
		
		return template.asMethodNode();
	}
	
	public SetterMethodBuilder setAccess(JAccess access) {
    	this.access  = access;
    	return this;
    }

	public SetterMethodBuilder setName(String name) {
		this.name = name;
		return this;
	}

	public SetterMethodBuilder setType(String type) {
		this.type = type;
		return this;
	}

	public SetterMethodBuilder setReturnType(RETURN returnType) {
		this.returnType = returnType;
		return this;
	}

	public void setMarkedGenerated(boolean markedGenerated) {
    	this.markedGenerated = markedGenerated;
    }

	public SetterMethodBuilder setTarget(JType target) {
    	this.target = target;
    	return this;
	}

	public SetterMethodBuilder setContext(MutationContext context) {
    	this.context = context;
    	return this;
    }


}
