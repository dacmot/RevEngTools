/*
 * Created on 17-Feb-2006
 */
package treeutils.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import treeutils.data.Variable.DataType;

public class SubProgSymTable extends SymbolTable
{   
    // input variables are program calling parameters that are only read
    // from in the subprogram
    private Map<String, Variable> inputVariables;
    // output variables are program calling parameters that are only written
    // to in the subprogram
    private Map<String, Variable> outputVariables;
    // update variables are program calling parameters which are both read from
    // and written to in the subprogram
    private Map<String, Variable> updateVariables;
    // this is the set of all program calling parameters
    private Map<String, Variable> paramVariables;
    // global variables are variables declared in a common statement.
    // Common statements have named blocks of variables, which is why this
    // datastructure maps common blocks to a set of variables.
    private Map<String, Map<String,Variable>> globalVariables;
    // constants are memory locations with a defined value which are only read
    // from
    private Map<String, Variable> constants;
    // external subprograms are subroutines and functions that are defined by
    // the user in the current file or in a different one.
    private Map<String, Function> externals;
    // intrinsics are functions that are part of the Fortran language 
    private Map<String, Function> intrinsics;
    // statement functions are simple local functions, somewhat like a macro
    private Map<String, StatementFunction> stFunctions;
    // format statements are non-executable statements specifying formatting
    // of I/O strings
    private Map<String, EAST> formats;
    // this variable is only defined in the case of functions, and is the
    // function's return variable/value.
    private Variable functionOutput;
    // unTypedParameters are parameters to a function or subroutine. They have
    // not been typed by a specification statement, or using an implicit rule.
    // This set should be empty before doing any analysis or transformations
    // past the specification statements patterns. This is due to the fact that
    // variables that do not have an explicitly defined type using a
    // specification statement are typed using the implicit rules.
    private Set<String> unTypedParameters;
    
    
    public SubProgSymTable()
    {
        super();

        inputVariables    = new HashMap<String, Variable>();
        outputVariables   = new HashMap<String, Variable>();
        updateVariables   = new HashMap<String, Variable>();
        paramVariables    = new LinkedHashMap<String, Variable>();
        globalVariables   = new HashMap<String, Map<String,Variable>>();
        constants         = new HashMap<String, Variable>();
        externals         = new HashMap<String, Function>();
        intrinsics        = new HashMap<String, Function>();
        stFunctions       = new HashMap<String, StatementFunction>();
        formats           = new HashMap<String, EAST>();
        unTypedParameters = new HashSet<String>();
    }
    
    
    /*
     * Routines to add symbols to the table
     */
    
    public void addUnTypedParameter(String var)
    {
        unTypedParameters.add(var);
        
        // this is done to keep the parameter order. "paramVariables" is
        // not a standard HashMap, but a LinkedHashMap because the order
        // in which parameters are passed to a subprogram is very important.
        // The "null" value will get overwritten by addTypedParameter, while
        // keeping the order in which they were originially found.
        paramVariables.put(var, null);
    }
    
    public void addTypedParameter(Variable var)
    {
        paramVariables.put(var.getName(), var);
        unTypedParameters.remove(var.getName());
    }
    
    public void addGlobalVariable(String commonName, Variable v)
    {
        Map<String,Variable> comblock = globalVariables.get(commonName);
        
        if (comblock == null)
        {
            // This must be linked as the order the variables appear
            // in the common statement is important
            comblock = new LinkedHashMap<String,Variable>();
            globalVariables.put(commonName, comblock);
        }
        
        comblock.put(v.getName(), v);
    }
    
    public void addInput(Variable var)
    {
        inputVariables.put(var.getName(), var);
    }
    
    public void addOutput(Variable var)
    {
        outputVariables.put(var.getName(), var);
    }
    
    public void addUpdate(Variable var)
    {
        updateVariables.put(var.getName(), var);
    }
    
    public void addConstant(Variable cons)
    {
        // this check needs to be done because the type of a name is not
        // declared in a parameter statement, but may have been declared in a
        // type specification statement
        String constantName = cons.getName();
        Variable v = localVariables.get(constantName);
        if (v != null)
        {
            cons.setType(v.getType());
            localVariables.remove(constantName);
        }
        else
            giveImplicitType(cons);

        constants.put(constantName, cons);
    }
    
    public void addExternal(String sym)
    {   
        Variable v = localVariables.get(sym);
        if (v != null)
        {
            externals.put(sym, new Function(new Symbol(sym, EXTERNAL),
                    v.getType()));
            localVariables.remove(sym);
        }
        else
            externals.put(sym, new Function(new Symbol(sym, EXTERNAL)));
    }
    
    /*
     * Contrary to the external function, we already know what the type of an
     * intrinsic is so no need to check if the symbol has been typed.
     */
    public void addIntrinsic(Function f)
    {
        intrinsics.put(f.getName(), f);
        localVariables.remove(f.getName());
    }
    
    
    public void addStatementFunction(StatementFunction sf)
    {
        stFunctions.put(sf.getName(), sf);
        localVariables.remove(sf.getName());
    }
    
    
    public void addFormat(String label, EAST statement)
    {
        formats.put(label, statement);
    }
    
    
    public void addParameterSubprogram(EAST node, DataType type,  int size)
    {
        // Function f = new Function(new Symbol(node), type, size);
        // paramVariables.put(node.getText(), null);
        // TODO: this is a parameter, but not a variable. Not sure what to do
        //   with it...
    }
    
    
    
    /*
     * Set existance tests
     */
    
    public boolean isUnknown(String symbol)
    {
        return ! (
                paramVariables.containsKey(symbol) ||
                isGlobalVariable(symbol) ||
                localVariables.containsKey(symbol) ||
                constants.containsKey(symbol) ||
                externals.containsKey(symbol) ||
                intrinsics.containsKey(symbol) ||
                stFunctions.containsKey(symbol) ||
                isFunctionOutput(symbol)
                );
    }
    
    public boolean isUnTypedParameter(String symbol)
    {
        return unTypedParameters.contains(symbol);
    }
    
    public boolean isTypedParameter(String symbol)
    {
        return paramVariables.get(symbol) != null;
    }
    
    public boolean isGlobalVariable(String symbol)
    {
        for (Map<String,Variable> comblock : globalVariables.values())
            if (comblock.containsKey(symbol))
                return true;
        
        return false;
    }
    
    public boolean isParameter(String symbol)
    {
        return paramVariables.containsKey(symbol);
    }
    
    public boolean isInput(String symbol)
    {
        return inputVariables.containsKey(symbol);
    }
    
    public boolean isOutput(String symbol)
    {
        return outputVariables.containsKey(symbol);
    }
    
    public boolean isFunctionOutput(String variable)
    {
        if (isFunction())
            return variable.equals(functionOutput.getName());
        else
            return false;
    }
    
    public boolean isExternalRoutine(String routine)
    {
        return externals.containsKey(routine);
    }
    
    public boolean isConstant(String symbol)
    {
        return constants.containsKey(symbol);
    }
    
    public int getNumberOfInputs()
    {
        return inputVariables.size();
    }
    
    public int getNumberOfOutputs()
    {
        return outputVariables.size();
    }
    
    public boolean isFunction()
    {
        return null != functionOutput;
    }

    public void setFunctionOutput(Variable functionOutput)
    {
        this.functionOutput = functionOutput;
        //inputVariables.remove(functionOutput.getName());
        //paramVariables.remove(functionOutput.getName());
    }
    
    public Variable getParameter(String name)
    {
        return paramVariables.get(name);
    }
    
    public Variable getConstant(String name)
    {
        return constants.get(name);
    }
    
    public EAST getFormat(String label)
    {
        return formats.get(label);
    }
    
    public Variable getGlobalVariable(String name)
    {
        Variable v;
        
        for (Map<String,Variable> comblock : globalVariables.values())
        {
            v = comblock.get(name);
            if (v != null)
                return v;
        }
        
        return null;
    }
    
    public Variable getVariable(String name)
    {
        Variable v = paramVariables.get(name);
        if (v != null)
            return v;
        
        v = localVariables.get(name);
        if (v != null)
            return v;
        
        v = constants.get(name);
        if (v != null)
            return v;
        
        if (functionOutput != null && name.equals(functionOutput.getName()))
            return functionOutput;
        
        for (Map<String, Variable> comblock : globalVariables.values())
        {
            v = comblock.get(name);
            if (v != null)
                return v;
        }
        
        return null;
    }
    
    
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        
        if (!inputVariables.isEmpty())
        {
            b.append("  Inputs:\n");
            for (Variable i : inputVariables.values())
            {
                b.append("    ");
                b.append(i);
                b.append("\n");
            }
        }
        if (!outputVariables.isEmpty())
        {
            b.append("  Outputs:\n");
            for (Variable o : outputVariables.values())
            {
                b.append("    ");
                b.append(o);
                b.append("\n");
            }
        }
        if (!updateVariables.isEmpty())
        {
            b.append("  Updates:\n");
            for (Variable u : updateVariables.values())
            {
                b.append("    ");
                b.append(u);
                b.append("\n");
            }
        }
        if (isFunction())
            b.append("  Function Output:\n    ").append(functionOutput)
                .append("\n");
        if (!globalVariables.isEmpty())
        {
            b.append("  Global Variables:\n");
            for (Map<String,Variable> comblock : globalVariables.values())
            {
                for (Variable g : comblock.values())
                {
                    b.append("    ");
                    b.append(g);
                    b.append("\n");
                }
            }
        }
        if (!localVariables.isEmpty())
        {
            b.append("  Local Variables:\n");
            for (Variable v : localVariables.values())
            {
                b.append("    ");
                b.append(v);
                b.append("\n");
            }
        }
        if (!constants.isEmpty())
        {
            b.append("  Local Constants:\n");
            for (Variable c : constants.values())
            {
                b.append("    ");
                b.append(c);
                b.append("\n");
            }
        }
        if (!intrinsics.isEmpty())
        {
            b.append("  Intrinsic Functions:\n");
            for (Function f : intrinsics.values())
            {
                b.append("    ");
                b.append(f);
                b.append("\n");
            }
        }
        if (!externals.isEmpty())
        {
            StringBuilder ps = new StringBuilder("  Parameter Subprograms:\n");
            b.append("  External Subprograms:\n");
            for (Function e : externals.values())
            {
                Variable v = paramVariables.get(e.getName());
                if (v != null)
                {
                    ps.append("    ");
                    ps.append(v);
                    ps.append("\n");
                }
                else
                {
                    b.append("    ");
                    b.append(e);
                    b.append("\n");
                }
            }
            if (ps.length() > 25) // length of "  Parameter Subprograms:\n"
                b.append(ps);
        }
        if (!stFunctions.isEmpty())
        {
            b.append("  Statement Functions:\n");
            for (StatementFunction st : stFunctions.values())
            {
                b.append("    ");
                b.append(st);
                b.append("\n");
            }
        }
        
        return b.toString();
    }
    
    
    private class AllVariables implements Iterator<Variable>, Iterable<Variable>
    {
        boolean done;
        private Iterator<Iterator<Variable>> varIter;
        private Iterator<Variable> current;
        private List<Iterator<Variable>> variables;
        
        public AllVariables()
        {
            variables = new ArrayList<Iterator<Variable>>(3);
            
            variables.add(getParameters().iterator());
            variables.add(getGlobalVariables().iterator());
            variables.add(getLocalVariables().iterator());
            
            varIter = variables.iterator();
            current = varIter.next();
        }
        
        public boolean hasNext()
        {
            if (current.hasNext())
                return true;
            else
            {
                while (varIter.hasNext())
                {
                    current = varIter.next();
                    if (current.hasNext())
                        return true;
                }
                
                if (!done && functionOutput != null)
                {
                    done = true;
                    return true;
                }

                return false;
            }
        }
        
        public Variable next()
        {
            Variable v;
            if (!done)
            {
                v = current.next();
                if (v == null)
                    throw new NullPointerException();
            }
            else
            {
                v = functionOutput;
                if (v == null)
                    throw new NullPointerException();
            }
            
            return v;
        }
        
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
        
        public Iterator<Variable> iterator()
        {
            return new AllVariables();
        }
    }
    
    public Iterable<Variable> getAllVariables()
    {
        return new AllVariables();
    }
    
    public Collection<String> getParameterSymbols()
    {
        return paramVariables.keySet();
    }
    
    public Collection<Variable> getParameters()
    {
        ArrayList<Variable> a = new ArrayList<Variable>(paramVariables.size());
        for (Map.Entry<String,Variable> v : paramVariables.entrySet())
            if( ! unTypedParameters.contains(v.getKey()) )
                a.add(v.getValue());
        
        return a;
    }
    
    public Collection<Variable> getGlobalVariables()
    {
        List<Variable> iter = new ArrayList<Variable>();
        for (Map<String,Variable> comblock : globalVariables.values())
            iter.addAll(comblock.values());
        
        return iter;
    }
    
    public Collection<String> getCommonBlocks()
    {
        return globalVariables.keySet();
    }
    
    public Collection<Variable> getCommonVariables(String commonBlock)
    {
        return globalVariables.get(commonBlock).values();
    }
    
    public Collection<Variable> getInputVariables()
    {
        return inputVariables.values();
    }
    
    public Collection<Variable> getOutputVariables()
    {
        return outputVariables.values();
    }
    
    public Collection<Variable> getUpdateVariables()
    {
        return updateVariables.values();
    }
    
    public Collection<Variable> getConstants()
    {
        return constants.values();
    }
    
    public Collection<Function> getExternalRoutines()
    {
        return externals.values();
    }
    
    public Collection<Function> getIntrinsicFunctions()
    {
        return intrinsics.values();
    }
    
    public Collection<StatementFunction> getStatementFunctions()
    {
        return stFunctions.values();
    }

    
    // automatically generated
    public Variable getFunctionOutput()
    {
        return functionOutput;
    }
}
