/*
 * Created on 17-Nov-2005
 */
package graph;

import treeutils.data.EAST;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;

public class ASTVertex extends DirectedSparseVertex implements LabelledVertex
{
    EAST subtree;
    
    public ASTVertex(EAST subtree)
    {
        super();
        this.subtree = (EAST) subtree.clone();
    }
    
    public String getLabel()
    {
        String label = subtree.getLine() + ": " + subtree.toString();
        
        label = label.replaceAll("( )*\\n( )*", "\\\\l");
        
        return label;
    }
}
