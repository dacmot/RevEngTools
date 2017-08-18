/*
 * Created on 9-Nov-2005
 */
package treeutils.data;

import treeutils.data.Variable.DataType;

public class Function extends Symbol
{
    DataType returnType;
    DataType[] argumentTypes;
    EAST tree;
    
    public Function(Symbol s)
    {
        this(s, (DataType) null);
    }
    
    public Function(Symbol s, DataType returnType)
    {
        this(s, returnType, new DataType[0]);
    }
    
    public Function(Symbol s, int numberOfArguments)
    {
        this(s, null, new DataType[numberOfArguments]);
    }
    
    public Function(Symbol s, DataType returnType, int numberOfArguments)
    {
        this(s, returnType, new DataType[numberOfArguments]);
    }
    
    public Function(Symbol s, DataType[] argumentTypes)
    {
        this(s, null, argumentTypes);
    }
    
    public Function(String s, DataType returnType, DataType[] argumentTypes)
    {
        super(new Symbol(s, INTRINSIC));
        setReturnType(returnType);
        this.argumentTypes = argumentTypes;
    }
    
    public Function(Symbol s, DataType returnType, DataType[] argumentTypes)
    {
        super(s);
        setReturnType(returnType);
        this.argumentTypes = argumentTypes;
    }
    
    
    
    public boolean isSubroutine()
    {
        return returnType == null;
    }
    
    public String toString()
    {
        StringBuilder s = new StringBuilder(name);
        if (argumentTypes.length > 0)
        {
            s.append("(");
            if (argumentTypes[0] != null)
            {
                s.append(argumentTypes[0]);
                for (int i=1; i< argumentTypes.length; i++)
                {
                    s.append(", ");
                    s.append(argumentTypes[i]);
                }
            }
            else
            {
                s.append(argumentTypes.length);
                s.append(" parameters");
            }
            s.append(")");
        }
        s.append(":");
        s.append(returnType);
        
        return s.toString();
    }
    
    public void setArgumentType(int argpos, DataType type)
    {
        argumentTypes[argpos] = type;
    }

    // automatically generated
    public DataType getReturnType()
    {
        return returnType;
    }

    public void setReturnType(DataType returnType)
    {
        this.returnType = returnType;
    }

    public DataType[] getArgumentTypes()
    {
        return argumentTypes;
    }

    public void setArgumentTypes(DataType[] argumentTypes)
    {
        this.argumentTypes = argumentTypes;
    }

    public EAST getTree()
    {
        return tree;
    }

    public void setTree(EAST tree)
    {
        this.tree = tree;
    }
}
