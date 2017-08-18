/*
 * Created on 17-Feb-2006
 */
package treeutils.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import treeutils.data.Variable.DataType;

import fortran77.parser.Fortran77TokenTypes;

public class SymbolTable implements Fortran77TokenTypes
{
    protected Map<String, Variable> localVariables;
    protected List<CodeBlock> subCodeBlocks;
    private Map<Character, DataType> implicitRules;
    
    
    public SymbolTable()
    {
        localVariables = new HashMap<String,Variable>();
        subCodeBlocks = new LinkedList<CodeBlock>();
        
        implicitRules = new HashMap<Character, DataType>();
        addImplicitRange('a','h',DataType.REAL);
        addImplicitRange('i','n',DataType.INTEGER);
        addImplicitRange('o','z',DataType.REAL);
    }
    
    
    
    public void addImplicitRule(DataType type, EAST range)
    {
        EAST a;
        EAST b;
        
        if (range.getType() == MINUS)
        {
            a = range.getFirstChild();
            b = a.getNextSibling();
            addImplicitRange(a.getText().charAt(0), b.getText().charAt(0),
                    type);
        }
        else
            implicitRules.put(range.getText().charAt(0), type);
    
    }
    
    private void addImplicitRange(char start, char end, DataType type)
    {
        while (start <= end)
        {
            implicitRules.put(start, type);
            start++;
        }
    }
    
    public DataType getImplicitType(String symbol)
    {
        return implicitRules.get(symbol.charAt(0));
    }
    
    public void giveImplicitType(Variable v)
    {
        v.setType(getImplicitType(v.getName()));
    }
    
    
    
    
    public void addLocalVariable(Variable v)
    {
        localVariables.put(v.getName(), v);
    }
    
    public void removeLocalVariable(String v)
    {
        localVariables.remove(v);
    }
    
    public Variable getLocalVariable(String name)
    {
        return localVariables.get(name);
    }
    
    public boolean isLocalVariable(String symbol)
    {
        return localVariables.containsKey(symbol);
    }
    
    public Collection<String> getLocalSymbols()
    {
        return localVariables.keySet();
    }
    
    public Collection<Variable> getLocalVariables()
    {
        return localVariables.values();
    }
    
    public void addSubScope(CodeBlock t)
    {
        subCodeBlocks.add(t);
    }
    
    public Iterable<CodeBlock> getSubScopes()
    {
        return subCodeBlocks;
    }
}
