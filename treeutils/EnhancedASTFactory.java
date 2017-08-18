/*
 * Created on 21-Aug-2005
 */
package treeutils;

import treeutils.data.*;
import fortran77.parser.Fortran77TokenTypes;
import fortran77.parser.TokenAST;
import antlr.collections.AST;

/**
 * This module hides how an enhanced AST (EAST) is created from another AST.
 * We chose to put this knowledge in a seperate module, independent from the
 * grammar since it may change.
 * 
 * @author Olivier Dragon <dragonoe@mcmaster.ca>
 */
public class EnhancedASTFactory implements Fortran77TokenTypes
{
    CodeRoot root;
    
    public CodeRoot buildEnhancedAST(AST ast)
    {
        root = new CodeRoot(createEASTNode(ast));
        
        // tree root has no siblings. Only walk children.
        walk(ast.getFirstChild(), root);
        
        return root;
    }
    
    private void walk(AST ast, EAST parent)
    {
        if (ast != null)
        {
            EAST me = createEASTNode(ast);
            parent.addChild(me);
            
	        walk(ast.getFirstChild(), me);
	        walk(ast.getNextSibling(), parent);
        }   
    }
    
    private EAST createEASTNode(AST node)
    {
        EAST result = new EAST();
        result.initialize((TokenAST) node);
        
        return result;
    }
}

