/*
 * Created on 22-Nov-2005
 */
package graph;

import antlr.RecognitionException;

public class CFGCreationException extends Exception
{
    int line;
    int column;
    
    public CFGCreationException()
    {
        super();
    }

    public CFGCreationException(String message)
    {
        super(message);
    }

    public CFGCreationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public CFGCreationException(Throwable cause)
    {
        super(cause);
    }

    public CFGCreationException(RecognitionException re)
    {
        super(re.getMessage());
        line = re.getLine();
        column = re.getColumn();
    }
    
    public String toString()
    {
        return "line " + line + ":" + column + ": " + getMessage(); 
    }
}
