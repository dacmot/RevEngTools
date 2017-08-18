header {
package graph;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

import graph.BasicCodeBlock.BlockType;
import graph.FlowEdge.BranchCondition;
import treeutils.data.EAST;
import fortran77.parser.Fortran77TokenTypes;
}

class ControlFlowGraphGenerator extends TreeParser;
options {
    defaultErrorHandler = false;
	importVocab=Fortran77;
    codeGenMakeSwitchThreshold=3;
    codeGenBitsetTestThreshold=6;
}



{
	BasicCodeBlock b;
	ControlFlowGraph g = new ControlFlowGraph();
	Map<String,BasicCodeBlock> labelTargets =
			new HashMap<String,BasicCodeBlock>();
	Map<String,List<BasicCodeBlock>> pendingJumps = 
			new HashMap<String,List<BasicCodeBlock>>();
    Set<BasicCodeBlock> exitNodes = new HashSet<BasicCodeBlock>();
	
    private boolean hasLabel(AST statement)
    {
    	if (statement.getFirstChild() != null)
	        return statement.getFirstChild().getType() == LABEL;
	    else
	    	return false;
    }
    
    private String getLabel(AST statement)
    {
    	return statement.getFirstChild().getText();
    }
    
    private void linkEndIfJumps(BasicCodeBlock block,
    	List<BasicCodeBlock> endIfJumps)
    {
    	for (BasicCodeBlock a : endIfJumps)
    		g.addEdge(new FlowEdge(a, block));
    	endIfJumps.clear();
    }
    
    private BasicCodeBlock newBlock(BasicCodeBlock a)
    {
    	if (!a.isEmpty())
    	{
			BasicCodeBlock c = new BasicCodeBlock();
			g.addVertex(c);
			g.addEdge(new FlowEdge(a, c));
			return c;
    	}
    	else
    		return a;
    }

	private void labelIncomingEdge(BasicCodeBlock a, String label,
		BranchCondition cond)
    {
    	FlowEdge e = (FlowEdge) a.getInEdges().iterator().next();
    	e.setLabel(label);
    	e.setBranch(cond);
    }
    
    private void addPendingJump(String toLabel, BasicCodeBlock jumpFrom)
    {
    	if (pendingJumps.containsKey(toLabel))
    		pendingJumps.get(toLabel).add(jumpFrom);
    	else
    	{
    		LinkedList<BasicCodeBlock> l = new LinkedList<BasicCodeBlock>();
    		l.add(jumpFrom);
    		pendingJumps.put(toLabel, l);
    	}
    }
    
    private void completePendingJumps(String toLabel, BasicCodeBlock toBlock)
    {
    	for (BasicCodeBlock a : pendingJumps.get(toLabel))
    		g.addEdge(new FlowEdge(a, toBlock));
    	pendingJumps.remove(toLabel);
    }
    
    private void joinExitNodes()
    {        
        BasicCodeBlock b = new BasicCodeBlock();
        b.setLabel("EXIT");
        b.setType(BlockType.EXIT);
        g.addVertex(b);
        
        for (BasicCodeBlock k : exitNodes)
            g.addEdge(new FlowEdge(k, b));
    }
}

subprogram returns [ControlFlowGraph graph]:
	{
		b = new BasicCodeBlock();
		g.addVertex(b);
		g.setStartNode(b);
	}
	(COMMENT)*
	#(SUBPROGRAM statements)
	
	{
		joinExitNodes();
		g.cleanSelf();
		g.annotateCFG();
		graph = g;
	}
	;
	
	
statements:
	{
		String label;
		AST currentStatement;
		BasicCodeBlock c;
	}
	(
		COMMENT |
		// first check if statement has a label
		{
			currentStatement = _t;
			if (hasLabel(currentStatement))
			{
                c = new BasicCodeBlock();
                //c.addStatement(currentStatement);
                g.addVertex(c);
                g.addEdge(new FlowEdge(b,c));
                
                label = getLabel(currentStatement);
				labelTargets.put(label, c);
				if (pendingJumps.containsKey(label))
					completePendingJumps(label, c);

                b = c;
			}
		}
		// the grammar rules
		(
			ifStmt |
			doStmt |
			returnStmt |
			stopStmt |
			gotoStmt |
			other
		)
	)*
	;


other:
	{ b.addStatement(_t); }
	~("if" | "do" | "return" | "stop" | "go" | COMMENT)
	;


ifStmt:
	{
		List<BasicCodeBlock> endIfJumps = new LinkedList<BasicCodeBlock>();
		b.addStatement(_t);
		BasicCodeBlock previousIf = b;
		boolean hasElse = false;
	}
	#("if" (LABEL)? boolExpr:~(LABEL)
		thenBlock                              { endIfJumps.add(b); }
		(previousIf=elseIfBlock[previousIf]    { endIfJumps.add(b); } )*
		(elseBlock[previousIf] { hasElse = true; endIfJumps.add(b); } )?
		
		{
			BasicCodeBlock c = new BasicCodeBlock();
			g.addVertex(c);
			linkEndIfJumps(c, endIfJumps);
			if (!hasElse)
				g.addEdge(new FlowEdge(previousIf, c, "false"));
			b = c;
		}
	)
	;

thenBlock:
	{ b = newBlock(b); labelIncomingEdge(b, "true", BranchCondition.TRUE); }
	#(THENBLOCK statements)
	;
	
elseIfBlock[BasicCodeBlock prevIf] returns [BasicCodeBlock p]:
	{
		b = newBlock(prevIf); // else-if line's block
		labelIncomingEdge(b, "false", BranchCondition.FALSE);
		b.addStatement(_t);
		p = b;
	}
	#(ELSEIF boolExpr:. thenBlock)
	;
	
elseBlock[BasicCodeBlock prevIf]:
	{
		b = newBlock(prevIf); // no branching for else statement
		labelIncomingEdge(b, "false", BranchCondition.FALSE);
		b.addStatement(_t);
	} 
	#(ELSEBLOCK statements)
	;


doStmt:
	{
		b = newBlock(b);
		b.addStatement(_t); // adding the DO only
		BasicCodeBlock doLine = b;
	}
	#("do" (LABEL)? LABELREF NAME from:. to:. (incr:~(DOBLOCK))? doBlock)
	{
		g.addEdge(new FlowEdge(b, doLine));
		b = new BasicCodeBlock();
		g.addVertex(b);
		g.addEdge(new FlowEdge(doLine, b, "exit"));
	}
	;
	
doBlock:
	{ b = newBlock(b); labelIncomingEdge(b, "loop", BranchCondition.FALSE);}
	#(DOBLOCK statements)
	;

returnStmt:
	{ b.addStatement(_t); exitNodes.add(b); }
	"return"
    // leave the new codeblock with no incoming edge
	{ b = new BasicCodeBlock(); g.addVertex(b); }
	;
	
stopStmt:
	{ b.addStatement(_t); exitNodes.add(b); }
	"stop"
    // leave the new codeblock with no incoming edge
	{ b = new BasicCodeBlock(); g.addVertex(b); }
	;
	
gotoStmt:
	go:"go"
	{
		b.addStatement(go);
		String label = getLabel(go);
		if (labelTargets.containsKey(label))
			g.addEdge(new FlowEdge(b, labelTargets.get(label)));
		else
			addPendingJump(label, b);
		
	    // leave the new codeblock with no incoming edge
		b = new BasicCodeBlock();
		g.addVertex(b);
	}
	;