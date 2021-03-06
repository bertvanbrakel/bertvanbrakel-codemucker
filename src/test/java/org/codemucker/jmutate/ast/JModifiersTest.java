package org.codemucker.jmutate.ast;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.junit.Test;

public class JModifiersTest {

	@Test
	public void testIsPublic() throws IOException {
		JModifier mods = newJavaModifiersWithKeywords(ModifierKeyword.ABSTRACT_KEYWORD, ModifierKeyword.NATIVE_KEYWORD, ModifierKeyword.PUBLIC_KEYWORD);
		
		assertTrue(mods.isPublic());
		
		assertFalse(mods.isPackagePrivate());
		assertFalse(mods.isProtected());
		assertFalse(mods.isPrivate());
	}
	
	@Test
	public void testIsPackagePrivate() throws IOException {
		JModifier mods = newJavaModifiersWithKeywords();
		
		assertTrue(mods.isPackagePrivate());
		
		assertFalse(mods.isPublic());
		assertFalse(mods.isProtected());
		assertFalse(mods.isPrivate());
	}
	
	@Test
	public void testIsProtected() throws IOException {
		JModifier mods = newJavaModifiersWithKeywords(ModifierKeyword.ABSTRACT_KEYWORD, ModifierKeyword.NATIVE_KEYWORD, ModifierKeyword.PROTECTED_KEYWORD);
		
		assertTrue(mods.isProtected());
		
		assertFalse(mods.isPublic());
		assertFalse(mods.isPackagePrivate());
		assertFalse(mods.isPrivate());
	}
	
	@Test
	public void testIsPrivate() throws IOException {
		JModifier mods = newJavaModifiersWithKeywords(ModifierKeyword.ABSTRACT_KEYWORD, ModifierKeyword.NATIVE_KEYWORD, ModifierKeyword.PRIVATE_KEYWORD);
		
		assertTrue(mods.isPrivate());
		
		assertFalse(mods.isPublic());
		assertFalse(mods.isPackagePrivate());
		assertFalse(mods.isProtected());
	}
	
	@Test
	public void testIsAbstract() throws IOException {
		JModifier mods1 = newJavaModifiersWithKeywords(ModifierKeyword.ABSTRACT_KEYWORD, ModifierKeyword.FINAL_KEYWORD);
		JModifier mods2 = newJavaModifiersWithKeywords(ModifierKeyword.FINAL_KEYWORD);
		
		assertTrue(mods1.isAbstract());
		assertFalse(mods2.isAbstract());
	}
	
	@Test
	public void testSetAccess() throws IOException {
		for( JAccess access:new JAccess[]{ JAccess.PRIVATE, JAccess.PROTECTED, JAccess.PUBLIC } ){
			JModifier mods = newJavaModifiersWithKeywords();
			
			assertFalse(mods.isAccess(access));
			mods.setAccess(access);
			assertTrue(mods.isAccess(access));
		}
		//package
		JModifier mods = newJavaModifiersWithKeywords(ModifierKeyword.PUBLIC_KEYWORD);
		
		assertFalse(mods.isAccess(JAccess.PACKAGE));
		mods.setAccess(JAccess.PACKAGE);
		assertTrue(mods.isAccess(JAccess.PACKAGE));
	}

	private static JModifier newJavaModifiersWithKeywords(ModifierKeyword... keywords){
		AST ast = AST.newAST(AST.JLS4);
		JModifier mods = new JModifier(ast, toModifiers(ast,keywords));
		
		return  mods;
	}
	
	private static List<IExtendedModifier> toModifiers(AST ast, ModifierKeyword... keywords){
		List<IExtendedModifier> mods = new ArrayList<IExtendedModifier>();
		for( ModifierKeyword keyword:keywords){
			mods.add(ast.newModifier(keyword));
		}
		return mods;
	}
}
