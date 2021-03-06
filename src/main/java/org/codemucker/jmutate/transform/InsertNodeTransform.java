package org.codemucker.jmutate.transform;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.codemucker.jmutate.JMutateException;
import org.codemucker.jmutate.PlacementStrategy;
import org.codemucker.jmutate.ast.JType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

import com.google.common.base.Preconditions;

/**
 * Inserts generic nodes using an insertion placement strategy
 */
public class InsertNodeTransform {

	private List<ASTNode> nodesToInsertInto;
	private AST ast;
	private ASTNode nodeToInsert;
	private PlacementStrategy strategy;
	
	/**
	 * Insert the node.
	 * 
	 * @return the node which was inserted
	 */
	public ASTNode insert() {
		checkNotNull(nodesToInsertInto, "expect nodes to insert into");
		checkNotNull(nodeToInsert, "expect node to insert");
		checkNotNull(strategy,"expect strategy");
		
		ASTNode clonedNode = ASTNode.copySubtree(ast, nodeToInsert);

		int index = strategy.findIndexToPlaceInto(clonedNode, nodesToInsertInto);
		if (index < 0) {
			throw new JMutateException("Insertion strategy %s couldn't find an index to insert %s into", strategy, nodeToInsert);
		}
		if(index > nodesToInsertInto.size()){
			index = nodesToInsertInto.size();
		}
		nodesToInsertInto.add(index, clonedNode);
		return clonedNode;
	}

	public InsertNodeTransform target(JType javaType) {
    	this.nodesToInsertInto = javaType.getBodyDeclarations();
    	this.ast = javaType.getAstNode().getAST();
    	return this;
	}

	public InsertNodeTransform nodesToInsertInto(AST ast, List<ASTNode> nodesToInsertInto) {
    	this.nodesToInsertInto = Preconditions.checkNotNull(nodesToInsertInto, "expect non null list of nodes to insert into");
    	this.ast = Preconditions.checkNotNull(ast,"expect non null ast");
    	return this;
	}

	public InsertNodeTransform nodeToInsert(ASTNode childNode) {
    	this.nodeToInsert = Preconditions.checkNotNull(childNode,"expect non null node to insert");
    	return this;
	}

	public InsertNodeTransform placementStrategy(PlacementStrategy strategy) {
    	this.strategy = Preconditions.checkNotNull(strategy, "expect non null strategy");
    	return this;
    }	
}
