/*
 * Created on 24-Aug-2005
 */
package treeutils.data;

import java.util.ArrayList;
import java.util.List;

import antlr.collections.AST;

/**
 * @author oli
 */
public class Variable extends Symbol
{
    private DataType type;
    private boolean readonly;
    private EAST lowerBound;
    private EAST upperBound;
    private List<List<EAST>> initValue;
    private EAST value;
    private EAST[] dimensions;
    private int length;
    
    
    public enum DataType
    {
        INTEGER,
        CHARACTER,
        REAL,
        LOGICAL,
        DOUBLEPRECISION,
        COMPLEX,
        DOUBLECOMPLEX,
        GENERIC,
        IMPLICIT
        ;
        
        public String toFortranType()
        {
            switch (this)
            {
            case INTEGER:
                return "integer";
            case CHARACTER:
                return "character";
            case REAL:
                return "real";
            case LOGICAL:
                return "logical";
            case DOUBLEPRECISION:
                return "double precision";
            case COMPLEX:
                return "complex";
            case DOUBLECOMPLEX:
                return "double complex";
            default:
                return "";
            }
        }
        
        public static DataType fromFortranType(String type)
        {
            if (type != null)
            {
                if (type.equals("integer"))
                    return DataType.INTEGER;
                else if (type.equals("real"))
                    return DataType.REAL;
                else if (type.equals("precision"))
                    return DataType.DOUBLEPRECISION;
                else if (type.equals("complex"))
                    return DataType.COMPLEX;
                else if (type.equals("double"))
                    return DataType.DOUBLECOMPLEX;
                else if (type.equals("logical"))
                    return DataType.LOGICAL;
                else if (type.equals("character"))
                    return DataType.CHARACTER;
                else
                    return null;
            }
            else
                return null;
        }
    }    
    
    public Variable(Symbol s)
    {
        this(s, DataType.IMPLICIT);
    }
    
    public Variable(Symbol s, EAST[] dimensions)
    {
        this(s, DataType.IMPLICIT, dimensions);
    }
    
    public Variable(Symbol s, DataType type)
    {
        this(s, type, 0);
    }
    
    public Variable (Symbol s, DataType type, int size)
    {
        
        this(s, type, initDimensions(size));
    }
    
    private static EAST[] initDimensions(int size)
    {
        EAST[] dim = new EAST[size];
        for (int i=0; i < size; i++)
        {
            dim[i] = new EAST();
            dim[i].setType(STAR);
            dim[i].setText("*");
        }
        
        return dim;
    }
    
    public Variable(Symbol s, DataType type, EAST[] dimensions)
    {
        super(s);
        readonly = true;
        setType(type);
        this.dimensions = dimensions;
        
        if (dimensions.length == 0)
        {
            initValue = new ArrayList<List<EAST>>(1);
            initValue.add(new ArrayList<EAST>(1));
            initValue.get(0).add(null);
        }
        else
        {
            int i;
            int size = 0;
            initValue = new ArrayList<List<EAST>>(dimensions.length);
            for (i=0; i < dimensions.length; i++)
            {
                try {
                    size = Integer.parseInt(dimensions[i].getText());
                } catch (NumberFormatException e) {
                    size = 10; // arbitrary size for new list
                } finally {
                    initValue.add(i, new ArrayList<EAST>(size));
                }
            }
        }
    }
    
    public boolean isInitialised()
    {
        boolean init;
        
        try {
            init = initValue.get(0).get(0) != null;
        } catch (IndexOutOfBoundsException e) {
            init = false;
        }
        
        return init;
    }

    public String toString()
    {
        StringBuilder s = new StringBuilder(name);
        s.append(":");
        s.append(type);
        
        if (dimensions.length > 0)
        {
            s.append("(").append(dimensions[0]);
            for (int i=1; i < dimensions.length; i++)
                s.append(",").append(dimensions[i]);
            s.append(")");;
        }

        if (isInitialised())
            s.append(" = ").append(initValue);
        if (lowerBound != null || upperBound != null)
        {
            s.append(", ");
            if (lowerBound != null)
                s.append("[ ").append(lowerBound);
            else
                s.append("( ");
            s.append(", ");
            if (upperBound != null)
                s.append(upperBound).append("]");
            else
                s.append(")");
        }
        
        return s.toString();
    }
    
    public boolean isScalar()
    {
        return dimensions.length == 0;
    }
    
    public boolean isVector()
    {
        return dimensions.length == 1;
    }
    
    public boolean isMatrix()
    {
        return dimensions.length == 2;
    }
    
    public static int getArrayIndex(AST array)
    {
        return Integer.parseInt(array.getFirstChild().getText());
    }
    
    public int size(int dimension)
    {
        int size;
        
        try {
            size = Integer.parseInt(dimensions[dimension].getText());
        } catch (NumberFormatException e) {
            size = -1;
        }
        
        return size;
    }

    public EAST getInitValue()
    {
        return initValue.get(0).get(0);
    }
    
    public List<EAST> getInitValues()
    {
        return initValue.get(0);
    }
    
    public List<EAST> getInitValues(int column)
    {
        return initValue.get(column);
    }

    public void setInitValue(EAST initVal)
    {
        initValue.get(0).set(0,initVal);
    }
    
    public void setInitValue(int index, EAST initVal)
    {
        if (initValue.get(0).size() <= index)
            initValue.get(0).add(index,initVal);
        else
            initValue.get(0).set(index,initVal);
    }
    
    public void setInitialValue(int[] indices, EAST initVal)
    {
        if (indices.length != 2)
            throw new IllegalArgumentException("invalid index set");
            
        initValue.get(indices[0]).set(indices[1], initVal);
    }
    
    
    
    
    // automatically generated getters/setters
    public EAST getLowerBound()
    {
        return lowerBound;
    }

    public void setLowerBound(EAST lowerBound)
    {
        this.lowerBound = lowerBound;
    }

    public boolean isReadonly()
    {
        return readonly;
    }

    public void setReadonly(boolean readonly)
    {
        this.readonly = readonly;
    }

    public EAST getUpperBound()
    {
        return upperBound;
    }

    public void setUpperBound(EAST upperBound)
    {
        this.upperBound = upperBound;
    }

    public EAST getValue()
    {
        return value;
    }

    public void setValue(EAST value)
    {
        this.value = value;
    }

    public void setDimensions(EAST[] dimensions)
    {
        this.dimensions = dimensions;
    }

    public EAST[] getDimensions()
    {
        return dimensions;
    }

    public DataType getType()
    {
        return type;
    }

    public void setType(DataType type)
    {
        this.type = type;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length)
    {
        this.length = length;
    }

}
