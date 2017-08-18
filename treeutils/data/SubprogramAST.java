/*
 * Created on 21-Aug-2005
 */
package treeutils.data;

import java.util.Iterator;
import java.util.Stack;

/**
 * @author oli
 */
public class SubprogramAST extends CodeBlock
{
    
    private SubProgSymTable symbolTable;

    private String name;
    
    private class Statements implements Iterable<EAST>
    {
        private class StatementIterator implements Iterator<EAST>
        {
            private EAST current;
            private EAST next;
            private Stack<EAST> stack;
            
            public StatementIterator()
            {
                stack = new Stack<EAST>();
                next = SubprogramAST.this.getFirstChild();
                skipComments();
            }
            
            public EAST next()
            {
                current = next;
                
                switch (current.getType())
                {   
                case LITERAL_if:
                    stack.push(current);
                    next = current.getFirstChild().getNextSibling();
                    break;
                    
                case LITERAL_do:
                    stack.push(current);
                    next = current.getLastChild();
                    break;

                case DOBLOCK:
                case THENBLOCK:
                case ELSEBLOCK:
                case PARALLEL:
                    stack.push(current);
                    next = current.getFirstChild();
                    break;
                    
                default:
                    setNextStatement();
                }
                
                return current;
            }
            
            private void setNextStatement()
            {       
                if (current.getNextSibling() != null)
                {
                    next = current.getNextSibling();
                    skipComments();
                }   
                else
                    endOfCodeBlock();
            }

            private void endOfCodeBlock()
            {
                if (!stack.isEmpty())
                {
                    do {
                        next = stack.pop().getNextSibling();
                    } while (next == null && !stack.isEmpty());
                    skipComments();
                }
                else
                    next = null;
            }
            
            private void skipComments()
            {
                while (next != null && next.getType() == COMMENT)
                    next = next.getNextSibling();
                if (next == null)
                    endOfCodeBlock();
            }
            
            public boolean hasNext()
            {
                return (next != null);
            }
            
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        }
        
        /**
         * <p>The Iterator returned by this method will iteratively go through this
         * tree but will only visit its top level statement nodes, as opposed to
         * all nodes like the depth-first iterator.
         * </p>
         * 
         * @return the statement iterator
         */
        public Iterator<EAST> iterator()
        {
            return new StatementIterator();
        }
    }
    
    public SubprogramAST()
    {
        super();
        symbolTable = new SubProgSymTable();
    }
    
    public SubprogramAST(EAST node)
    {
        super(node);
        symbolTable = new SubProgSymTable();
    }
    
    public SubProgSymTable getSymbolTable()
    {
        return symbolTable;
    }
    

    private String boxText(String text)
    {
        String[] lines = text.split("\\n");
        StringBuilder[] newLines = new StringBuilder[lines.length];
        
        // calculate the length of the longest line
        int max = 0;
        for (String line : lines)
            if (max < line.length())
                max = line.length();
        
        for (int i=0; i < lines.length; i++)
        {
            newLines[i] = new StringBuilder();
            newLines[i].append("  [ ");
            newLines[i].append(lines[i]);
            newLines[i].append(spaces(max - lines[i].length()));
            newLines[i].append(" ]\n");
        }
        
        StringBuilder boxedText = new StringBuilder();
        for (int i=0; i < newLines.length; i++)
            boxedText.append(newLines[i]);
        
        return boxedText.toString();
    }
    
    private String spaces(int number)
    {
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < number; i++)
            padding.append(" ");
        return padding.toString();
    }
    
    public String toString() {
        StringBuilder b = new StringBuilder();
        
        b.append(getText());
        b.append(" ");
        b.append(getName());
        b.append(",<SUBPROGRAM>\n");
        b.append(symbolTable.toString());
        
        return "\n" + boxText(b.toString());
    }
    
    public Iterable<EAST> statements()
    {
        return new Statements();
    }
    
    // automatically generated
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
}
