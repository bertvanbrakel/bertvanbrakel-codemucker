package com.bertvanbrakel.codemucker.ast;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.junit.Test;

import com.bertvanbrakel.codemucker.ast.finder.ClasspathResource;
import com.bertvanbrakel.codemucker.util.SourceUtil;
import com.bertvanbrakel.codemucker.util.SrcWriter;
import com.bertvanbrakel.test.util.TestHelper;

public class JavaSourceFileMutatorTest {

	TestHelper helper = new TestHelper();

	@Test
	public void testGetMainType() throws Exception {
		SrcWriter w = new SrcWriter();
		w.println("package foo.bar");
		w.println("public class Foo {");
		w.println("}");
		w.println("public class Alice {");
		w.println("}");
		w.println("public class Bob {");
		w.println("}");

		JavaSourceFile srcFile = newJavaSrc(w, "foo.bar.Alice");
		
		AbstractTypeDeclaration type = srcFile.getMainType();
		assertNotNull(type);
		assertEquals(type.getName().getIdentifier(), "Alice");
	}
	
	@Test
	public void testGetMainTypeAsMutable() throws Exception {
		SrcWriter w = new SrcWriter();
		w.println("package foo.bar");
		w.println("public class Foo {");
		w.println("}");
		w.println("public class Alice {");
		w.println("}");
		w.println("public class Bob {");
		w.println("}");

		JavaSourceFileMutator srcFile = newMutator(w, "foo.bar.Alice");
		
		JTypeMutator mutable = srcFile.getMainTypeAsMutable();
		AbstractTypeDeclaration type = srcFile.getJavaSourceFile().getMainType();
		
		assertNotNull(mutable);
		assertEquals(mutable.getJavaType().asType(), type);
	}
	
	private JavaSourceFileMutator newMutator(SrcWriter writer, String fqClassName) throws IOException {
		return new JavaSourceFileMutator(newJavaSrc(writer, fqClassName));
	}	
	
	private JavaSourceFile newJavaSrc(SrcWriter writer, String fqClassName) throws IOException {
		File classRootDir = helper.createTempDir();
		return SourceUtil.writeJavaSrc(writer, classRootDir, fqClassName);
	}
}