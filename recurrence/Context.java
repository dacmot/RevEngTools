/*
 * Created on 1-Feb-2006
 */
package recurrence;

import java.util.HashMap;
import java.util.Map;

import fortran77.Expression;
import fortran77.parser.Fortran77TokenTypes;
import treeutils.data.EAST;

public class Context implements Fortran77TokenTypes
{
    Map<String, EAST> s;
    EAST t;
    EAST p;
    
    public Context()
    {
        s = new HashMap<String,EAST>();
        t = Expression.getTrue();
        p = Expression.getTrue();
    }
    
    public Context(Context c)
    {
        s = new HashMap<String,EAST>();
        for (Map.Entry<String,EAST> e : c.getState().entrySet())
            s.put(e.getKey(), e.getValue());
        
        t = c.getStateCondition().cloneSubTree();
        p = c.getPathCondition().cloneSubTree();
    }

    /**
     * The folding or two contexts requires the use of a gamma function. We
     * define the gamma function as a piecewise function, similar
     * to that of Fahringer and Scholz.
     * 
     * @param c2 - context to fold with this one
     * @return context resulting from folding this one with c2
     *
    public Context fold(Context c2)
    {
        EAST e,f,g;
        Context c3;
        Map<String, EAST> s2, s3;
        EAST t2, t3;
        EAST p2, p3;
        
        s2 = c2.getState();
        t2 = c2.getStateCondition();
        p2 = c2.getPathCondition();
        
        s3 = new HashMap<String,EAST>();
        for (String v : s.keySet())
        {
                e = s.get(v);
                if (e.equalsTree(s2.get(v)))
                    s3.put(v, e);
                else
                {
                    g = new EAST();
                    g.setType(GAMMA);
                    g.setText("");
                    s3.put(v, g);
                }
        }

        c3 = new Context();
        c3.setState(s3);
        //c3.setStateCondition(t3);
        //c3.setPathCondition(p3);
        
        return c3;
    } //*/
    
    
    // automatically generated
    public EAST getStateCondition()
    {
        return t;
    }

    public void setStateCondition(EAST t)
    {
        this.t = t;
    }

    public EAST getPathCondition()
    {
        return p;
    }

    public void setPathCondition(EAST p)
    {
        this.p = p;
    }

    public Map<String, EAST> getState()
    {
        return s;
    }

    public void setState(Map<String, EAST> s)
    {
        this.s = s;
    }
}
