/*
 * Created on 9-Feb-2006
 */
package recurrence;

import java.util.Map;

import treeutils.data.EAST;
import treeutils.data.SubProgSymTable;

public interface SymbolicEngine
{
    public void reInit();
    public EAST solveRecurrenceEquation(String var, EAST initialValue,
        Map<String,EAST> recurrenceState) throws SymbolicException;
    public EAST getClosedForm(EAST equation, EAST e1, EAST e2, EAST e3)
        throws SymbolicException;
    public EAST numberOfIterations(EAST einit, EAST efinal, EAST eincr)
        throws SymbolicException;
    public EAST createRecurrenceValue(String var);
    public EAST simplify(EAST expr) throws SymbolicException;
    public EAST eval(EAST expr, Context c) throws SymbolicException;
    public EAST evalArray(EAST array, EAST expr, Context c)
        throws SymbolicException;
    public boolean eval(EAST expr) throws SymbolicException;
    public EAST evalAssuming(EAST expr) throws SymbolicException;
    public void setSubprogramSymbolTable(SubProgSymTable table);
}
