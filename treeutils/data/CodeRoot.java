/*
 * Created on 12-Apr-2006
 */
package treeutils.data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CodeRoot extends EAST
{
    Map<String, SubprogramAST> externals;
    Map<String, Map<String, Variable>> common;
    
    public CodeRoot(EAST clone)
    {
        super(clone);
        externals   = null;
        common      = new HashMap<String, Map<String, Variable>>();
    }

    public void setExternals(Map<String, SubprogramAST> externals)
    {
        this.externals = externals;
    }
    
    public void addExternal(SubprogramAST s)
    {
        externals.put(s.getName(), s);
    }
    
    public SubprogramAST getExternal(String name)
    {
        return externals.get(name);
    }
    
    public void addGlobalVariable(String commonName, Variable v)
    {
        Map<String,Variable> comblock = common.get(commonName);
        
        if (comblock == null)
        {
            // This must be linked as the order the variables appear
            // in the common statement is important
            comblock = new LinkedHashMap<String,Variable>();
            common.put(commonName, comblock);
        }
        
        // because it is allowed to change the name of variables in the same
        // common block, we only keep the first one added. For subprogram-
        // specific globals, they will have their own name
        if ( !comblock.containsKey(v.getName()) )
            comblock.put(v.getName(), v);
    }
    
    public Variable getGlobalVariable(String name)
    {
        Variable v;
        
        for (Map<String,Variable> comblock : common.values())
        {
            v = comblock.get(name);
            if (v != null)
                return v;
        }
        
        return null;
    }
}
