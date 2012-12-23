package com.bertvanbrakel.codemucker.transform;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;

import com.bertvanbrakel.codemucker.ast.CodemuckerException;
import com.bertvanbrakel.codemucker.ast.ContextNames;
import com.bertvanbrakel.codemucker.ast.JType;
import com.bertvanbrakel.codemucker.ast.matcher.AType;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class InsertTypeTransform extends AbstractNodeInsertTransform<InsertTypeTransform>{
	
	private JType type;
	
	public static InsertTypeTransform newTransform(){
		return new InsertTypeTransform();
	}
	
	@Override
	public void transform() {
		checkFieldsSet();
		checkState(type != null, "missing type");
		
		JType target = getTarget();
		
	    //TODO:detect if it exists?
		boolean insert = true;
		List<JType> found = target.findChildTypesMatching(AType.withFullName(type.getSimpleName())).toList();
		if( !found.isEmpty()){
			insert = false;
			JType existingType = found.get(0);
			switch(getClashStrategy()){
			case REPLACE:
				existingType.getAstNode().delete();
				insert = true;
				break;
			case IGNORE:
				break;
			case ERROR:
				throw new CodemuckerException("Existing type %s, not replacing with %s", existingType.getAstNode(), type);
			default:
				throw new CodemuckerException("Existing type %s, unsupported clash strategy %s", existingType.getAstNode(), getClashStrategy());
			}
		}
		if(insert){
			new NodeInserter()
				.setNodeToInsert(type.getAstNode())
				.setTargetToInsertInto(target)
				.setStrategy(getPlacementStrategy())
				.insert();
		}
	}
	
	/**
	 * Used by the DI container to set the default
	 */
	@Inject
    public void injectPlacementStrategy(@Named(ContextNames.TYPE) PlacementStrategy strategy) {
	    setPlacementStrategy(strategy);
    }

	/**
	 * The type to transform
	 * @param type
	 * @return
	 */
	public InsertTypeTransform setType(AbstractTypeDeclaration type) {
    	setType(new JType(type));
    	return this;
	}
	
	/**
	 * The type to transform
	 * @param type
	 * @return
	 */
	public InsertTypeTransform setType(JType type) {
    	this.type = type;
    	return this;
	}

	
}
