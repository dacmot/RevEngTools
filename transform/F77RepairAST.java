/*
 * Created on 29-Aug-2005
 */
package transform;

import fortran77.parser.Fortran77TokenTypes;
import fortran77.parser.TokenAST;
import antlr.collections.AST;

import java.util.regex.*;

/**
 * @author oli
 */
public class F77RepairAST implements Fortran77TokenTypes
{
    public static void repair(AST root) throws BadLoopException
    {
        cleanCode(root);
        fixDoLoopSubtree(root);
    }
    
    /*
     * Put these up as state variables for speed
     */
    private final static Pattern commentFilter =
        Pattern.compile("^[^ ][ ]{0,6}([^\\n\\r]+)");
    private static Matcher m = commentFilter.matcher("");
    
    /**
     * Cleaning the comments consists of doing 2 things: removing the leading
     * comment line character and white spaces, and the trailing new line.
     * 
     * @param tree - the syntax tree to process
     */
    private static void cleanCode(AST tree)
    {
        if (tree != null)
        {
            switch (tree.getType())
            {
            case COMMENT:
                tree.setText(clean(tree.getText()));
                break;
            case SCON:
            case HOLLERITH:
                break;
            default:
                tree.setText(tree.getText().toLowerCase());
            }
            
            cleanCode(tree.getFirstChild());
            cleanCode(tree.getNextSibling());
        }
    }
    
    private static String clean(String comment)
    {
        m.reset(comment);
        if (m.find())
            return m.group(1);
        else
            return comment;
    }
    
    /**
     * <p>In FORTRAN, do-loops are difficult to recognise as a whole
     * structure including a block of code (body of the loop). This is
     * because the loop body is ended by a statement with a label
     * that must match the do statement's code label. For example:</p>
     * 
     * <code>
     *       DO 10 I = 1,3
     *         ...
     *    10 CONTINUE
     * </code>
     * 
     * <p>We must then do a pass over the parser generated AST and group
     * the loop body statements, which is what this method does.</p>
     * 
     * @param root - Root of the tree to be checked for do-loops and
     *   transformed
     */
    private static void fixDoLoopSubtree(AST root) throws BadLoopException
    {
        findDoLoop(root);
    }
    
    private static void findDoLoop(AST subTree) throws BadLoopException
    {
        if (subTree != null)
        {
            // The second condition is to ensure that only DO loops with
            // a label reference are constructed. DO loops with an END DO
            // are handled within the parser itself.
            if (subTree.getType() == LITERAL_do && hasLabelReference(subTree))
                buildDoLoopBody(subTree);
            
            findDoLoop(subTree.getFirstChild());
            findDoLoop(subTree.getNextSibling());
        }
    }
    
    /**
     * <p>This method takes all the siblings, up to the matching continue
     * statement, of the do-loop node and adds them to a CODEBLOCK node. It
     * also removes those nodes from the siblings list of the do-loop and
     * recursively does all this with nested do-loop structures.</p>
     * 
     * @param doLoop - the subtree starting with a DO node.
     */
    private static void buildDoLoopBody(AST doLoop) throws BadLoopException
    {
        AST innerNode;
        TokenAST blockNode;
        int doLabelRef;
        int labelDef;
        
        blockNode = new TokenAST();
        blockNode.setText("[doLoopBody]");
        blockNode.setType(DOBLOCK);
        blockNode.setLine(doLoop.getLine());
        blockNode.setColumn(doLoop.getColumn());

        // Note that because the statements inside the loop's body are already
        // siblings there is no need to addChild() for every node. In fact that
        // will most certainly send addChild() into an infinite loop.
        //
        // Instead we will cut off the continue statement from its siblings
        // and set the first of the loop body's statement as the first child.
        innerNode = doLoop.getNextSibling();
        blockNode.setFirstChild(innerNode);
        
        doLabelRef = getLabelReference(doLoop);
        labelDef = getLabelDefinition(innerNode);
        while (innerNode != null && labelDef != doLabelRef)
        {
            innerNode = innerNode.getNextSibling();
            labelDef = getLabelDefinition(innerNode);
        }

        // if the while loop above exited because innerNode == null, then
        // it because we have searched all of the subtree and there are no
        // matching continue statements.
        if (innerNode != null)
        {
            doLoop.addChild(blockNode);
            doLoop.setNextSibling(innerNode.getNextSibling());
            innerNode.setNextSibling(null);
        }
        else
            throw new BadLoopException((TokenAST) doLoop);
    }

    private static boolean hasLabelReference(AST doLoop)
    {
        AST child = doLoop.getFirstChild();
        while (child != null)
        {
            if (child.getType() == LABELREF)
                return true;
            child = child.getNextSibling();
        }
        return false;
    }
    
    private static int getLabelReference(AST subTree)
    {
        if (subTree != null)
        {
            subTree = subTree.getFirstChild();
            while (subTree != null && subTree.getType() != LABELREF)
                subTree = subTree.getNextSibling();
            
            if (subTree == null)
                return 0;
            else
                return Integer.parseInt(subTree.getText());
        }
        else
            return 0;
    }
    
    private static int getLabelDefinition(AST subTree)
    {
        if (subTree != null)
        {
            subTree = subTree.getFirstChild();
            if (subTree != null && subTree.getType() == LABEL)
                return Integer.parseInt(subTree.getText());
            else
                return 0;
        } else
            return 0;
    }
    
    
}
