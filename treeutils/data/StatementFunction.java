/*
 * Created on 5-Jan-2006
 */
package treeutils.data;

import treeutils.data.Variable.DataType;

public class StatementFunction extends Function
{
    EAST expression;
    Variable[] arguments;
    
    public StatementFunction(Symbol s, DataType returnType, Variable[] arguments,
            EAST expression)
    {
        super(s, returnType);
        this.expression = expression;
        this.arguments = arguments;
        
        argumentTypes = new DataType[arguments.length];
        for(int i=0; i < arguments.length; i++)
            argumentTypes[i] = arguments[i].getType();
    }
    
    // automatically generated
    public EAST getExpression()
    {
        return expression;
    }

    public Variable[] getArguments()
    {
        return arguments;
    }
}
