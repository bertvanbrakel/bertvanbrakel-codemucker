package org.codemucker.jmutate.generate;

import java.lang.annotation.Annotation;

import org.codemucker.jmutate.ast.JField;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.JType;
import org.eclipse.jdt.core.dom.ASTNode;

public abstract class AbstractCodeGenerator<T extends Annotation> implements CodeGenerator<T> {

    @Override
    public final void generate(ASTNode node, T options) {
        if (JType.is(node)) {
            generate(JType.from(node), options);
        } else if (JField.is(node)) {
            generate(JField.from(node), options);
        } else if (JMethod.is(node)) {
            generate(JMethod.from(node), options);
        }
    }

    protected void generate(JType applyToNode, T options) {
    }

    protected void generate(JMethod applyToNode, T options) {
    }

    protected void generate(JField applyToNode, T options) {
    }

}