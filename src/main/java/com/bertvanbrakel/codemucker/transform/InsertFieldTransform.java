package com.bertvanbrakel.codemucker.transform;

import static com.google.common.base.Preconditions.checkState;

import org.eclipse.jdt.core.dom.FieldDeclaration;

import com.bertvanbrakel.codemucker.ast.CodemuckerException;
import com.bertvanbrakel.codemucker.ast.ContextNames;
import com.bertvanbrakel.codemucker.ast.JField;
import com.bertvanbrakel.codemucker.ast.finder.FindResult;
import com.bertvanbrakel.codemucker.ast.matcher.AField;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public final class InsertFieldTransform extends AbstractNodeInsertTransform<InsertFieldTransform>{

	private JField field;
	
	public static InsertFieldTransform newTransform(){
		return new InsertFieldTransform();
	}
	
	@Override
	public void transform() {
		checkFieldsSet();
		checkState(field != null,"missing field");
		
	    boolean insert = true;
		for( String fieldName:field.getNames()){
			//TODO:unwrap single field decl with multiple field (all same type/assignment)
			FindResult<JField> found = getTarget().findFieldsMatching(AField.withName(fieldName));
			if(!found.isEmpty()){
				insert = false;
				JField existingField = found.getFirst();
				switch(getClashStrategy()){
				case REPLACE:
					existingField.getAstNode().delete();
					insert = true;
					break;
				case IGNORE:
					break;
				case ERROR:
					throw new CodemuckerException("Existing field %s, not replacing with %s", existingField.getAstNode(), field);
				default:
					throw new CodemuckerException("Existing field %s, unsupported clash strategy %s", existingField.getAstNode(), getClashStrategy());
				}
			}
		}
		if( insert){
			new NodeInserter()
                .setTargetToInsertInto(getTarget())
                .setNodeToInsert(field.getAstNode())
                //TODO:allow to override this? want to make this a non greedy class!
                .setStrategy(getPlacementStrategy())
                .insert();
		}
	}

	/**
	 * Used by the DI container to set the default
	 */
	@Inject
    public void injectPlacementStrategy(@Named(ContextNames.FIELD) PlacementStrategy strategy) {
	    setPlacementStrategy(strategy);
    }
	
	/**
	 * The field to transform
	 * 
	 * @param field
	 * @return
	 */
	public InsertFieldTransform setField(FieldDeclaration field) {
    	setField(new JField(field));
    	return this;
	}
	
	/**
	 * The field to transform
	 * @param field
	 * @return
	 */
	public InsertFieldTransform setField(JField field) {
    	this.field = field;
    	return this;
	}
}
