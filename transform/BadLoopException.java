/*
 * Created on 2-Sep-2005
 */
package transform;

import fortran77.parser.TokenAST;

/**
 * @author oli
 */
public class BadLoopException extends Exception
{
    public BadLoopException()
    {
        super();
    }
    public BadLoopException(String arg0)
    {
        super(arg0);
    }
    public BadLoopException(String arg0, Throwable arg1)
    {
        super(arg0, arg1);
    }
    public BadLoopException(Throwable arg0)
    {
        super(arg0);
    }
    
    
    public BadLoopException(TokenAST doLoop)
    {
        super(doLoop.getFilename() +
                "(Line "+ doLoop.getLine() +", Col "+ doLoop.getColumn() +
                ") : Missing label definition statement for DO-loop.");
    }
}
