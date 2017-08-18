/*
 * Created on 16-Nov-2005
 */
package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.utils.Pair;
import graph.BasicCodeBlock.BlockType;
import graph.BasicCodeBlock.VisitState;
import graph.FlowEdge.BranchCondition;
import graph.FlowEdge.FlowType;

public class ControlFlowGraph extends DirectedSparseGraph
    implements Iterable<BasicCodeBlock>
{
    int sequenceCounter;
    BasicCodeBlock cfgStart;
    Set<BasicCodeBlock> deadVertices;
    Set<Edge> deadEdges;
    LinkedList<BasicCodeBlock> topologicalOrder;
    
    public ControlFlowGraph()
    {
        super();
        deadVertices = new HashSet<BasicCodeBlock>();
        deadEdges = new HashSet<Edge>();
        topologicalOrder = new LinkedList<BasicCodeBlock>();
    }

    public void setStartNode(BasicCodeBlock v)
    {
        cfgStart = v;
    }
    
    public BasicCodeBlock getStartNode()
    {
        return cfgStart;
    }
    
    public boolean isStartNode(BasicCodeBlock b)
    {
        return cfgStart == b;
    }
    
    private class Neighbours {
        public Neighbours(BasicCodeBlock from, BasicCodeBlock to)
            { this.from = from; this.to = to; }
        public BasicCodeBlock from;
        public BasicCodeBlock to;
    }
    
    public void cleanSelf()
    {
        Iterator j;
        FlowEdge edge;
        BasicCodeBlock v, succ, pred;
        Set<BasicCodeBlock> deadCode = new HashSet<BasicCodeBlock>();
        List<Neighbours> newNeighbours = new ArrayList<Neighbours>();
        List<String> neighboursLabel = new ArrayList<String>();
        

        
        // mark vertices for deletion
        // need to separate "marking" and "deleting" of nodes because of the
        // iterators' concurrent modification exceptions
        for(Iterator i = getVertices().iterator(); i.hasNext() ;)
        {
            v = (BasicCodeBlock) i.next();
            if (v.inDegree() == 0 && v.outDegree() == 0 && v != cfgStart)
                deadCode.add(v);
            else if (v.isEmpty() && (v.inDegree() == 0 || v.outDegree() == 0)
                    && v.getType() != BlockType.EXIT)
                deadVertices.add(v);
            else if (v.isEmpty() && v.inDegree() > 0 && v.outDegree() == 1)
            {
                deadVertices.add(v);
                succ = findNonEmptySuccessor(v);
                for (j = v.getPredecessors().iterator(); j.hasNext();)
                {
                    pred = (BasicCodeBlock) j.next();
                    if (!pred.isEmpty())
                    {
                        newNeighbours.add(new Neighbours(pred, succ));
                        edge = (FlowEdge) pred.findEdge(v);
                        neighboursLabel.add(edge.getLabel());
                    }
                }
            }
            else if (v.inDegree() == 0 && v.outDegree() > 0
                    && !v.equals(cfgStart))
                deadCode.add(v);
        }
        
        // delete dead nodes and subgraphs
        for (BasicCodeBlock x : deadCode)
            deleteSubGraph(x);
        
        BasicCodeBlock from,to;
        for(int i=0; i < newNeighbours.size(); i++)
        {
            from = newNeighbours.get(i).from;
            to = newNeighbours.get(i).to;
            if (!from.getSuccessors().contains(to))
                addEdge(new FlowEdge(from, to, neighboursLabel.get(i)));
        }

        removeEdges(deadEdges);
        removeVertices(deadVertices);
        
        deadEdges.clear();
        deadVertices.clear();
        deadCode.clear();
    }
    
    private void deleteSubGraph(BasicCodeBlock start)
    {
        BasicCodeBlock v;
        
        deadEdges.addAll(start.getOutEdges());
        deadVertices.add(start);
        
        for (Iterator i = start.getSuccessors().iterator(); i.hasNext() ;)
        {
            v = (BasicCodeBlock) i.next();
            if (!deadVertices.contains(v))
                deleteSubGraph(v);
        }
    }
    
    /**
     * <p>This routine only works with (and assumes that) empty vertices with
     * only one successor.</p>
     * 
     * @param v - an empty vertex
     * @return first non-empty vertex successor of v
     */
    private BasicCodeBlock findNonEmptySuccessor(BasicCodeBlock v)
    {   
        do {
            v = (BasicCodeBlock) v.getSuccessors().iterator().next();
        } while (v.isEmpty());
        
        return v;
    }
    
    public void annotateCFG()
    {
        topologicalOrder.clear();
        resetNodesVisitState();
        sequenceCounter = this.getVertices().size();
        dfsVisit(cfgStart);
    }
    
    private void dfsVisit(BasicCodeBlock b)
    {
        FlowEdge e, d;
        BasicCodeBlock succ, next;
        
        b.setState(VisitState.ENCOUNTERED);
        
        for (Iterator i = b.getSuccessors().iterator(); i.hasNext() ;)
        {
            succ = (BasicCodeBlock) i.next();

            e = (FlowEdge) b.findEdge(succ);
            // always process branches leading to a post-exit node first.
            if (i.hasNext() && succ.getType() != BlockType.POSTEXIT)
            {
                next = (BasicCodeBlock) i.next();
                d = (FlowEdge) b.findEdge(next);
                // process post-exit or true branch first. This yields a
                // reverse post-order where the non-post exit or true branches
                // come first.
                if (next.getType() == BlockType.POSTEXIT
                        || e.getBranch() == BranchCondition.FALSE)
                {
                    classifyEdge(next,d);
                }
                else
                {
                    classifyEdge(succ,e);
                    succ = next;
                    e = d;
                }
            }
            
            classifyEdge(succ, e); // TRUE or UNCONDITIONAL branch
        }
        
        b.setState(VisitState.PROCESSED);
        b.setSequenceIndex(sequenceCounter);
        topologicalOrder.addFirst(b);
        sequenceCounter--;
    }

    private void classifyEdge(BasicCodeBlock succ, FlowEdge e)
    {
        switch (succ.getState())
        {
        case UNDISCOVERED:
            e.setFlowType(FlowType.TREE);
            dfsVisit(succ);
            break;
        case ENCOUNTERED:
            e.setFlowType(FlowType.BACKWARD);
            break;
        case PROCESSED:
            e.setFlowType(FlowType.FORWARD);
        }
    }
    
    public void resetNodesVisitState()
    {
        for(Iterator i=getVertices().iterator(); i.hasNext(); )
            ((BasicCodeBlock) i.next()).setState(VisitState.UNDISCOVERED);
    }
    
    public String toString()
    {
        FlowEdge e;
        Pair p;
        BasicCodeBlock v;
        StringBuilder s = new StringBuilder();
        
        s.append("{");
        for (Iterator i = getEdges().iterator(); i.hasNext(); )
        {
            e = (FlowEdge) i.next();
            p = e.getEndpoints();
            v = (BasicCodeBlock) p.getFirst();
            s.append(v.getStatements());
            s.append("-->");
            v = (BasicCodeBlock) p.getSecond();
            s.append(v.getStatements());
            s.append(", ");
        }
        s.append("}");
        
        return s.toString();
    }
    
    /**
     * Iterator that gives the nodes in reverse post-order (topological).
     */
    public Iterator<BasicCodeBlock> iterator()
    {
        return topologicalOrder.iterator();
    }
    
    public Iterator<BasicCodeBlock> iterator(BasicCodeBlock n)
    {
        BasicCodeBlock b;
        
        Iterator<BasicCodeBlock> i = iterator();
        while(i.hasNext())
        {
            b = i.next();
            if (b.equals(n))
                break;
        }
        
        return i;
    }
}
