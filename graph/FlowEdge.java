/*
 * Created on 8-Dec-2005
 */
package graph;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;

public class FlowEdge extends DirectedSparseEdge
{
    private String label;
    private FlowType flowType;
    private BranchCondition branch;
    
    public enum FlowType {FORWARD, BACKWARD, TREE};
    public enum BranchCondition { TRUE, FALSE, UNCONDITIONAL }; 
    
    
    public FlowEdge(Vertex from, Vertex to)
    {
        this(from, to, "");
    }

    public FlowEdge(Vertex from, Vertex to, String label)
    {
        super(from, to);
        this.label = label;
        flowType = FlowType.FORWARD;
        
        if (label.equals(""))
            branch = BranchCondition.UNCONDITIONAL;
        else if (label.equals("exit") || label.equals("true"))
            branch = BranchCondition.TRUE;
        else if (label.equals("loop") || label.equals("false"))
            branch = BranchCondition.FALSE;
    }
    
    public FlowEdge(Vertex from, Vertex to, FlowType type)
    {
        this(from, to);
        flowType = type;
    }

    
    // automatically generated
    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public FlowType getFlowType()
    {
        return flowType;
    }

    public void setFlowType(FlowType type)
    {
        this.flowType = type;
    }

    public BranchCondition getBranch()
    {
        return branch;
    }

    public void setBranch(BranchCondition branch)
    {
        this.branch = branch;
    }
}
