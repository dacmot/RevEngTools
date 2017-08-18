/*
 * Created on 21-Sep-2005
 */
package math;

public class Matrix<T>
{
    private static final int DEFAULT_COLSIZE = 10;
    private static final int DEFAULT_ROWSIZE = 10;
    
    Object[][] m;
    
    public Matrix()
    {
        m = new Object[DEFAULT_COLSIZE][DEFAULT_ROWSIZE];
    }
    
    public Matrix(int rows, int columns)
    {
        m = new Object[rows][columns];
    }
    
    public T get(int i, int j)
    {
        return (T) m[i][j];
    }
    
    public void put(T item, int i, int j)
    {
        m[i][j] = item;
    }
    
    public int rowSize()
    {
        return m.length;
    }
    
    public int colSize()
    {
        return m[0].length;
    }
}
