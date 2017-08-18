/*
 * Created on 7-Dec-2005
 */
package treeutils.data;

public class CodeBlock extends EAST
{
    private SymbolTable symbolTable;

    public CodeBlock()
    {
        super();
        symbolTable = new SymbolTable();
    }
    
    public CodeBlock(EAST node)
    {
        super(node);
        symbolTable = new SymbolTable();
    }
    
    public SymbolTable getSymbolTable()
    {
        return symbolTable;
    }
}
