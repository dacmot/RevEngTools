/*
 * Created on 22-Nov-2005
 */
package recon;

import antlr.RecognitionException;

public class PatternMatchException extends Exception
{
    int line;
    int column;
    
    public PatternMatchException()
    {
        super();
    }

    public PatternMatchException(String message)
    {
        super(message);
    }

    public PatternMatchException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public PatternMatchException(Throwable cause)
    {
        super(cause);
    }

    public PatternMatchException(RecognitionException re)
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
