/*
 * Created on 9-Jan-2006
 */
package recurrence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.uci.ics.jung.graph.Graph;

import graph.BasicCodeBlock;
import graph.ControlFlowGraph;
import graph.FlowEdge;
import graph.BasicCodeBlock.BlockType;
import graph.BasicCodeBlock.VisitState;
import graph.FlowEdge.FlowType;

public class CFGExtender
{
    private Set<BasicCodeBlock> loops;
    private Set<BasicCodeBlock> newNodes;
    private Map<BasicCodeBlock,BasicCodeBlock> newEdges;
    private Map<FlowEdge, BasicCodeBlock> oldEdges;
    private List<FlowEdge> backEdges; // set global for speed
    private List<FlowEdge> fwdEdges; // set global for speed
    private ControlFlowGraph g;
    
    
    
    public CFGExtender()
    {
        loops = new HashSet<BasicCodeBlock>();
        newNodes = new HashSet<BasicCodeBlock>();
        newEdges = new HashMap<BasicCodeBlock,BasicCodeBlock>();
        oldEdges = new HashMap<FlowEdge, BasicCodeBlock>();
        backEdges = new LinkedList<FlowEdge>();
        fwdEdges = new LinkedList<FlowEdge>();
    }
    
    public void extend(Graph graph)
    {
        g = (ControlFlowGraph) graph;
        
        g.resetNodesVisitState();
        
        classifyNode(g.getStartNode());
        updateGraph(g);
        
        insertLoopExit(g);
        updateGraph(g);
    }

    private void updateGraph(ControlFlowGraph g)
    {
        for (BasicCodeBlock b : newNodes)
            g.addVertex(b);
        for (BasicCodeBlock b : newEdges.keySet())
            g.addEdge(new FlowEdge(b, newEdges.get(b)));
        replaceOldEdges(g);

        newNodes.clear();
        oldEdges.clear();
        newEdges.clear();
    }
    
    private void replaceOldEdges(Graph g)
    {
        FlowEdge f;
        BasicCodeBlock joinNode;
        for (FlowEdge e : oldEdges.keySet())
        {
            joinNode = oldEdges.get(e);
            f = new FlowEdge(e.getSource(), joinNode);
            f.setBranch(e.getBranch());
            f.setLabel(e.getLabel());
            
            g.addEdge(f);
            g.removeEdge(e);
        }
    }
    
    
    
    private void findLoops(BasicCodeBlock b)
    {
        b.setState(VisitState.ENCOUNTERED);
        
        for (Iterator i=b.getSuccessors().iterator(); i.hasNext(); )
            classifyNode((BasicCodeBlock) i.next());
    }
    
    private void classifyNode(BasicCodeBlock b)
    {
        if (b.getState() == VisitState.UNDISCOVERED)
        {
            FlowEdge e;
            BasicCodeBlock k;
            /* 
             * Look at incoming edges to the current node. If there is at least
             * one back edge within the incoming edges, we know this node is a
             * loop header node.
             */
            for(Iterator i = b.getInEdges().iterator(); i.hasNext() ; )
            {
                e = (FlowEdge) i.next();
                if (e.getFlowType() == FlowType.BACKWARD)
                    backEdges.add(e);
                else
                    fwdEdges.add(e);
            }
    
            if (!backEdges.isEmpty())
            {
                loops.add(b);
                k = new BasicCodeBlock("PH", BlockType.PREHEADER);
                if (g.getStartNode() == b)
                    g.setStartNode(k);
                insertJoinNode(b, k, fwdEdges);
                insertJoinNode(b, new BasicCodeBlock("PB", BlockType.POSTBODY),
                        backEdges);
            }
            
            fwdEdges.clear();
            backEdges.clear();
            
            findLoops(b);
        }
    }
    
    private void insertJoinNode(BasicCodeBlock b,
            BasicCodeBlock joinNode, List<FlowEdge> edges)
    {
        newNodes.add(joinNode);
        newEdges.put(joinNode, b);
        for (FlowEdge e : edges)
            oldEdges.put(e, joinNode);
    }
    
    private void insertLoopExit(ControlFlowGraph g)
    {
        BasicCodeBlock postBody, preHeader;
        
        for (BasicCodeBlock l : loops)
        {
            g.resetNodesVisitState();
            postBody = findPredecessor(l, BlockType.POSTBODY);
            preHeader = findPredecessor(l, BlockType.PREHEADER);
            inverseFlowBFS(postBody, preHeader);
        }
    }

    private BasicCodeBlock findPredecessor(BasicCodeBlock l, BlockType t)
    {
        BasicCodeBlock pre;
        for (Iterator i = l.getPredecessors().iterator(); i.hasNext(); )
        {
            pre = (BasicCodeBlock) i.next();
            if (pre.getType() == t)
                return pre;
        }
        return null;
    }

    private void inverseFlowBFS(BasicCodeBlock postBody,
            BasicCodeBlock preHeader)
    {
        BasicCodeBlock current=null, predecessor;
        Set<FlowEdge> unknownExits = new HashSet<FlowEdge>();
        Queue<BasicCodeBlock> q = new LinkedList<BasicCodeBlock>();
        
        postBody.setState(VisitState.ENCOUNTERED);
        q.offer(postBody);
        while (!q.isEmpty())
        {
            current = q.poll();
            current.setState(VisitState.ENCOUNTERED);
            if (!current.equals(preHeader))
            {
                analysePossibleExits(current.getOutEdges(), unknownExits);
                for(Iterator i=current.getPredecessors().iterator(); i.hasNext();)
                {
                    predecessor = (BasicCodeBlock) i.next();
                    if (predecessor.getState() == VisitState.UNDISCOVERED)
                        q.offer(predecessor);
                }
            }
        }
        
        List<FlowEdge> edgeList = new ArrayList<FlowEdge>();
        edgeList.add(null);
        for (FlowEdge e : unknownExits)
        {
            current = (BasicCodeBlock) e.getDest();
            if (current.getState() == VisitState.UNDISCOVERED)
            {
                // create a list with a single node. Unlike PB and PH we only
                // want to insert the PE node in the middle of a single edge.
                edgeList.set(0, e);
                insertJoinNode(current,
                        new BasicCodeBlock("PE",BlockType.POSTEXIT), edgeList);
            }
        }
    }
    
    private void analysePossibleExits(Set outEdges, Set<FlowEdge> unknownExits)
    {
        FlowEdge e;
        BasicCodeBlock successor;
        
        for (Iterator i = outEdges.iterator(); i.hasNext(); )
        {
            e = (FlowEdge) i.next();
            successor = (BasicCodeBlock) e.getDest();
            if (successor.getState() == VisitState.UNDISCOVERED)
                unknownExits.add(e);
        }
    }
}
