package org.codemucker.jmutate.ast;

import static org.codemucker.lang.Check.checkNotNull;

import java.util.Collection;
import java.util.List;

import org.codemucker.jmutate.ast.finder.FindResult;
import org.codemucker.jmutate.ast.finder.FindResultImpl;
import org.codemucker.jmutate.ast.matcher.AJType;
import org.codemucker.match.Matcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.google.common.collect.Lists;

public class JCompilationUnit implements AstNodeProvider<CompilationUnit> {

	private final CompilationUnit compilationUnit;
	
	protected JCompilationUnit(CompilationUnit cu) {
		checkNotNull("compilationUnit", cu);
		this.compilationUnit = cu;
	}
	
	public static JCompilationUnit from(CompilationUnit cu){
		return new JCompilationUnit(cu);
	}
	
	public JType findMainType() {
		@SuppressWarnings("unchecked")
		List<AbstractTypeDeclaration> topTypes = compilationUnit.types();
		JType mainType = null;
		if(topTypes.isEmpty()){
			throw new CodemuckerException("no types found in compilation unit so couldn't determine source path to generate");
		} else if(topTypes.size() == 1){
			mainType = JType.from(topTypes.get(0));
		} else { //find the public class
			for(AbstractTypeDeclaration node:topTypes){
				JType type = JType.from(node);
				if(type.getModifiers().isPublic()){
					if( mainType != null){
						throw new CodemuckerException("Multiple top level types in compilation unit and more than one is public. Can't determine main type");
					}
					mainType = type;
				}
			}
			if(mainType == null){
				throw new CodemuckerException("Multiple top level types in compilation unit and could not determine one to use. Try setting one to public access");
			}
		}
		return mainType;
	}
	
	public FindResult<JType> findAllTypes(){
		return findTypesMatching(AJType.any());
	}
	
	public FindResult<JType> findTypesMatching(final Matcher<JType> matcher){
		final Collection<JType> found = Lists.newArrayList();
		ASTVisitor visitor = new BaseASTVisitor() {
			@Override
			protected boolean visitNode(ASTNode node) {
				if(JType.isTypeNode(node)){
					JType type = JType.from(node);
					if(matcher.matches(type)) {
						found.add(type);
					}
				}
				return true;
			}
		};
		visitChildren(visitor);
		return FindResultImpl.from(found);
	}
	
	private void visitChildren(final ASTVisitor visitor){
		@SuppressWarnings("unchecked")
		List<AbstractTypeDeclaration> topTypes = compilationUnit.types();
		
		for(AbstractTypeDeclaration topType:topTypes){
			topType.accept(visitor);
		}
	}

	@Override
	public CompilationUnit getAstNode() {
		return compilationUnit;
	}
}