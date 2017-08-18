/*
 * Created on 17-Feb-2006
 */
package treeutils.data;

import fortran77.parser.Fortran77TokenTypes;

public class Symbol implements Fortran77TokenTypes
{
    protected int symbolType;
    protected String name;
    
    public Symbol(String text, int tokenType)
    {
        symbolType = tokenType;
        name = text;
    }
    
    public Symbol(String text)
    {
        symbolType = NAME;
        name = text;
    }
    
    public Symbol(Symbol s)
    {
        symbolType = s.getSymbolType();
        name = s.getName();
    }
    
    public Symbol(EAST node)
    {
        symbolType = node.getType();
        name = node.getText();
    }
    
    
    public int getSymbolType()
    {
        return symbolType;
    }
    public void setSymbolType(int symbolType)
    {
        this.symbolType = symbolType;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
}
