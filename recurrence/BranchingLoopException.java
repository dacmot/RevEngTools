/*
 * Created on 7-Feb-2006
 */
package recurrence;

public class BranchingLoopException extends Exception
{

    public BranchingLoopException()
    {
        super();
    }

    public BranchingLoopException(String message)
    {
        super(message);
    }

    public BranchingLoopException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public BranchingLoopException(Throwable cause)
    {
        super(cause);
    }

}
