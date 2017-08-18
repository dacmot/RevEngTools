/*
 * Created on 11-Nov-2005
 */
package fortran77;

import java.util.Map;
import java.util.HashMap;

import treeutils.data.Function;
import treeutils.data.Variable.DataType;

public class Intrinsic
{   
    public static Map<String,Function> getFunctionSet()
    {
        HashMap<String, Function> intrin = new HashMap<String, Function>();
        
        //         NAME                   NAME      RETURN VALUE TYPE                        ARGUMENT TYPES
        intrin.put("abs",    new Function("abs",    DataType.REAL,            new DataType[]{DataType.REAL}));
        intrin.put("aimag",  new Function("aimag",  DataType.REAL,            new DataType[]{DataType.COMPLEX}));
        intrin.put("cmplx",  new Function("cmplx",  DataType.COMPLEX,         new DataType[]{DataType.GENERIC}));
        intrin.put("conjg",  new Function("conjg",  DataType.COMPLEX,         new DataType[]{DataType.COMPLEX}));
        intrin.put("dble",   new Function("dble",   DataType.DOUBLEPRECISION, new DataType[]{DataType.GENERIC}));
        intrin.put("dcmplx", new Function("dcmplx", DataType.COMPLEX,         new DataType[]{DataType.DOUBLEPRECISION}));
        intrin.put("dconjg", new Function("dconjg", DataType.COMPLEX,         new DataType[]{DataType.COMPLEX}));
        intrin.put("dimag",  new Function("dimag",  DataType.DOUBLEPRECISION, new DataType[]{DataType.COMPLEX}));
        intrin.put("ichar",  new Function("ichar",  DataType.INTEGER,         new DataType[]{DataType.CHARACTER}));
        intrin.put("int",    new Function("int",    DataType.INTEGER,         new DataType[]{DataType.GENERIC}));
        intrin.put("log",    new Function("log",    DataType.GENERIC,         new DataType[]{DataType.GENERIC}));
        intrin.put("log10",  new Function("log10",  DataType.GENERIC,         new DataType[]{DataType.GENERIC}));
        intrin.put("max",    new Function("max",    DataType.INTEGER,         new DataType[]{DataType.INTEGER, DataType.INTEGER}));
        intrin.put("min",    new Function("min",    DataType.INTEGER,         new DataType[]{DataType.INTEGER, DataType.INTEGER}));
        intrin.put("mod",    new Function("mod",    DataType.INTEGER,         new DataType[]{DataType.INTEGER, DataType.INTEGER}));
        intrin.put("nint",   new Function("nint",   DataType.INTEGER,         new DataType[]{DataType.GENERIC}));
        intrin.put("real",   new Function("real",   DataType.REAL,            new DataType[]{DataType.COMPLEX}));
        intrin.put("sign",   new Function("sign",   DataType.REAL,            new DataType[]{DataType.REAL, DataType.REAL}));
        intrin.put("sqrt",   new Function("sqrt",   DataType.REAL,            new DataType[]{DataType.REAL}));
        
        return intrin;
    }
}
