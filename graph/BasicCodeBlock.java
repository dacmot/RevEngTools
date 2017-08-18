/*
 * Created on 14-Nov-2005
 */
package graph;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import recurrence.Context;
import treeutils.data.EAST;

import antlr.collections.AST;

import edu.uci.ics.jung.graph.impl.SimpleDirectedSparseVertex;

public class BasicCodeBlock extends SimpleDirectedSparseVertex
    implements LabelledVertex
{
    int sequenceIndex;
    String label;
    BlockType type;
    VisitState state;
    boolean modified;
    List<EAST> statements;
    Context entry;
    Map<BasicCodeBlock,Context> exit;
    
    public enum BlockType { ENTRY, BLOCK, EXIT, PREHEADER, POSTBODY, POSTEXIT };
    public enum VisitState { UNDISCOVERED, ENCOUNTERED, PROCESSED };
    
    
    public BasicCodeBlock()
    {
        this("", BlockType.BLOCK);
    }
    
    public BasicCodeBlock(String label, BlockType type)
    {
        super();
        statements = new ArrayList<EAST>();
        exit = new HashMap<BasicCodeBlock,Context>();
        
        this.label = label;
        this.type = type;
        state = VisitState.UNDISCOVERED;
    }
    
    public void addStatement(AST statement)
    {
        statements.add(((EAST) statement));
        modified = true;
    }

    public Iterable<EAST> getStatements()
    {
        return statements;
    }
    
    public int getNumberOfStatements()
    {
        return statements.size();
    }
    
    public String getFirstStatementText()
    {
        if (statements.isEmpty())
            return "EMPTY";
        else if (statements.size() == 1)
            return statements.get(0).getText();
        else
            return statements.get(0).getText() + "," +
                    statements.get(1).getText();
    }
    
    public EAST getFirstStatement()
    {
        return statements.get(0);
    }
    
    public EAST getLastStatement()
    {
        return statements.get(statements.size() - 1);
    }
    
    public String getLastStatementText()
    {
        return getLastStatement().getText();
    }
    
    public String getLabel()
    {
        if (modified)
        {
            StringBuilder s = new StringBuilder();
            
            s.append("(");
            s.append(sequenceIndex);
            s.append(")\\n");
            
            if (label.length() == 0)
            {
                for (AST node : statements)
                {
                    s.append(node.getLine());
                    s.append(": ");
                    s.append(node.getText());
                    s.append("\\l");
                }
            }
            else
                s.append(label);

            label = s.toString();
            modified = false;
        }
        
        return label;
    }
    
    public void setLabel(String label)
    {
        modified = true;
        this.label = label;
    }

    public void setSequenceIndex(int sequenceIndex)
    {
        modified = true;
        this.sequenceIndex = sequenceIndex;
    }
    
    public boolean isEmpty()
    {
        return statements.isEmpty();
    }
    
    public void addExit(BasicCodeBlock successor, Context exit)
    {
        this.exit.put(successor, exit);
    }
    
    public Context getExitTo(BasicCodeBlock successor)
    {
        return exit.get(successor);
    }
    
    public boolean hasEntryContext()
    {
        return entry != null;
    }
    
    public boolean hasExitContext()
    {
        return ! exit.isEmpty();
    }
    
    public String toString()
    {
        return statements.toString();
    }
    
    
    
    
    // generated automatically
    public int getSequenceIndex()
    {
        return sequenceIndex;
    }

    public BlockType getType()
    {
        return type;
    }

    public void setType(BlockType type)
    {
        this.type = type;
    }

    public VisitState getState()
    {
        return state;
    }

    public void setState(VisitState state)
    {
        this.state = state;
    }

    public Context getEntry()
    {
        return entry;
    }

    public void setEntry(Context entry)
    {
        this.entry = entry;
    }
}
