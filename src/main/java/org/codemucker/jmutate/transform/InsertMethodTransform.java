package org.codemucker.jmutate.transform;

import static com.google.common.base.Preconditions.checkState;

import org.codemucker.jmutate.ast.CodemuckerException;
import org.codemucker.jmutate.ast.ContextNames;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.finder.FindResult;
import org.codemucker.jmutate.ast.matcher.AJMethod;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public final class InsertMethodTransform extends AbstractNodeInsertTransform<InsertMethodTransform>{

	private JMethod method;

	public static InsertMethodTransform newTransform(){
		return new InsertMethodTransform();
	}
	
	@Override
	public void transform() {
		checkFieldsSet();
		checkState(method != null, "missing method");
		
	    //TODO:detect if it exists?
		boolean insert = true;
		FindResult<JMethod> found = getTarget().findMethodsMatching(AJMethod.withNameAndArgSignature(method));
		if(!found.isEmpty()){
			insert = false;
			JMethod existingMethod = found.getFirst();
			switch(getClashStrategy()){
			case REPLACE:
				existingMethod.getAstNode().delete();
				insert = true;
				break;
			case IGNORE:
				break;
			case ERROR:
				throw new CodemuckerException("Existing method %s, not replacing with %s", existingMethod.getAstNode(), method);
			default:
				throw new CodemuckerException("Existing method %s, unsupported clash strategy %s", existingMethod.getAstNode(), getClashStrategy());
			}
		}
		if(insert){
			new NodeInserter()
				.setNodeToInsert(method.getAstNode())
				.setTargetToInsertInto(getTarget())
				.setStrategy(getPlacementStrategy())
				.insert();
		}
	}

	/**
	 * Used by the DI container to set the default
	 */
	@Inject
    public void injectPlacementStrategy(@Named(ContextNames.METHOD) PlacementStrategy strategy) {
	    setPlacementStrategy(strategy);
    }

	
	/**
	 * The method to transform
	 * @param method
	 * @return
	 */
	public InsertMethodTransform setMethod(MethodDeclaration method) {
    	setMethod(JMethod.from(method));
    	return this;
	}
	
	/**
	 * The method to transform
	 * @param newMethod
	 * @return
	 */
	public InsertMethodTransform setMethod(JMethod newMethod) {
    	this.method = newMethod;
    	return this;
	}
}