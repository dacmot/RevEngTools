/*
 * Created on 31-May-2006
 */
package recurrence;

public class CyclicDependencyException extends Exception
{

    public CyclicDependencyException()
    {
        super();
    }

    public CyclicDependencyException(String message)
    {
        super(message);
    }

    public CyclicDependencyException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public CyclicDependencyException(Throwable cause)
    {
        super(cause);
    }

}
