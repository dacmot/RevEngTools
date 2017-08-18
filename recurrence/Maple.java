/*
 * Created on 3-Feb-2006
 */
package recurrence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import treeutils.data.EAST;
import treeutils.data.SubProgSymTable;
import treeutils.data.Variable;
import util.ResultRecorder;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Engine;

import fortran77.Expression;
import fortran77.printer.CodePrinter;

public class Maple
    implements SymbolicEngine, MapleTreeTokenTypes
{   
    private Algebraic result;
    private Engine kernel;
    private CodePrinter printer;
    private StringTemplateGroup templates;
    private SubProgSymTable symtbl;
    
    
    public Maple()
    {
        try
        {
            String[] args = new String[]{"java"};
            kernel = new Engine(args, new REngineCallBacks(), null, null);
            
            printer = new CodePrinter();
            File tmpl = new File("recurrence/maple.stg");
            templates = new StringTemplateGroup(new FileReader(tmpl));
            printer.setTemplates(templates);
        }
          catch (MapleException e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        } catch (FileNotFoundException e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
    
    public void setSubprogramSymbolTable(SubProgSymTable table)
    {
        symtbl = table;
    }
    
    public void reInit()
    {
        try {
            run(template("init"));
        } catch (SymbolicException e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
    
    private StringTemplate template(String name)
    {
        return templates.getInstanceOf(name);
    }
    
    /**
     * Convert a syntax tree to a string expression that Maple understands.
     * 
     * @param expr - syntax tree expression
     * @return string expression
     */
    private StringTemplate treeToString(EAST expr)
    {
        try {
            return printer.expr(expr);
        } catch (RecognitionException e) {
            // throw an unchecked exception for debugging
            throw new NullPointerException();
        }
    }
    
    /**
     * Convert a Maple string expression that has been formatted by our
     * pretty printer (see Maple.stg) into a Fortran-M syntax tree.
     * 
     * @param expr
     * @return
     */
    private EAST stringToTree(Algebraic expr)
    {
        return stringToTree(expr.toString());
    }
    
    private EAST stringToTree(String expr)
    {
        try
        {
            MapleLexer lex = new MapleLexer(new StringReader(expr));
            MapleParser parser = new MapleParser(lex);
            parser.setASTNodeClass("treeutils.data.EAST");
            
            parser.parseExpr();
            EAST resultTree = (EAST) parser.getAST();
            
            return resultTree;
        }
          catch (RecognitionException e) {
              // throw an unchecked exception for debugging
              throw new NullPointerException();
        } catch (TokenStreamException e) {
            // throw an unchecked exception for debugging
            throw new NullPointerException();
        }
    }

    /**
     * Run a maple command or evaluate an expression.
     * 
     * @param cmd - maple command
     * @throws SymbolicException 
     * @throws MapleException 
     */
    private void run(StringTemplate cmd) throws SymbolicException
    {
        StringTemplate runcmd = template("command");
        runcmd.setAttribute("cmd", cmd);
        String cmdText = runcmd.toString();
        ResultRecorder.logSymbolicEngineResults(cmdText);
        try {
            result = kernel.evaluate(cmdText);
        } catch (MapleException e) {
            throw new SymbolicException(e.getMessage() + " *** " + cmdText, e);
        }
        ResultRecorder.logSymbolicEngineResults(result.toString());
    }
    
    private void runWithResults(StringTemplate cmd) throws SymbolicException
    {
        StringTemplate cmdresult = template("printcommandresult");
        cmdresult.setAttribute("cmd", cmd);
        run(cmdresult);
    }
    
    public EAST solveRecurrenceEquation(String var, EAST initialValue,
            Map<String,EAST> recurrenceState) throws SymbolicException
    {
        if (recurrenceState.get(var).getType() == OVERWRITE)
            return solveArrayRecurrence(var, recurrenceState);
        else
            return solveScalarRecurrence(var, initialValue, recurrenceState);
    }
    
    private EAST solveArrayRecurrence(String var,
            Map<String, EAST> recurrenceState) throws SymbolicException
    {
        runWithResults( getEquationEval(treeToString(recurrenceState.get(var)),
                getStateEquations(recurrenceState, var)) );
        runWithResults( getEquationEval(treeToString(stringToTree(result)),
                template("arrayrec")));
        return stringToTree(result);
    }

    private EAST solveScalarRecurrence(String var, EAST initialValue,
            Map<String,EAST> recurrenceState) throws SymbolicException
    {
        StringTemplate r0;
        
        StringTemplate req = template("recureq");
        req.setAttribute("var", var);
        req.setAttribute("expr", treeToString(recurrenceState.get(var)));
        
        if (initialValue == null)
            r0 = template("recurinitunknown");
        else
        {
            r0 = template("recurinitknown");
            r0.setAttribute("expr", treeToString(initialValue));
        }
        r0.setAttribute("var", var);
        
        
        StringTemplate rsolve = template("solverecur");
        rsolve.setAttribute("eq",
            getEquationEval(req, getStateEquations(recurrenceState, var)));
        rsolve.setAttribute("init",  r0);
        rsolve.setAttribute("var", var);
        rsolve.setAttribute("idx", template("recidx"));
        
        runWithResults(rsolve);
        return stringToTree(result);
    }
    
    private StringTemplate getEquationEval(StringTemplate eqn,
            StringTemplate state)
    {
        StringTemplate eval = template("evaleqn");
        
        eval.setAttribute("expr", eqn);
        eval.setAttribute("eqnset", state);
        
        return eval;
    }
    
    /*
     * Used to backsub the recurrence solutions of variables depended on
     */
    private StringTemplate getStateEquations(Map<String,EAST> state, String rv)
    {   
        StringTemplate eq, var;
        StringTemplate set = template("set");
        for(Map.Entry<String,EAST> varState : state.entrySet())
        {
            if ( !rv.equals(varState.getKey()) )
            {
                var = template("recvar");
                var.setAttribute("var", varState.getKey());
                
                eq = template("eq");
                eq.setAttribute("a", var);
                eq.setAttribute("b", treeToString(varState.getValue()));
                
                set.setAttribute("items", eq);
            }
        }
        
        return set;
    }
    
    /*
     * Used during analysis of each statements
     */
    private StringTemplate getStateEquations(Map<String,EAST> state)
    {
        StringTemplate eq;
        StringTemplate set = template("set");
        for (String var : state.keySet())
        {
            eq = template("eq");
            eq.setAttribute("a", var);
            eq.setAttribute("b", treeToString(state.get(var)));
            set.setAttribute("items", eq);
        }
        
        return set;
    }
    

    public EAST getClosedForm(EAST equation, EAST e1, EAST e2, EAST e3)
            throws SymbolicException
    {
        StringTemplate eq = template("eq");
        eq.setAttribute("a", template("recidx"));
        
        eq.setAttribute("b", doLoopNumberOfIterations(e1,e2,e3));
        
        runWithResults(getEquationEval(treeToString(equation), eq));
        return stringToTree(result);
    }
    
    private StringTemplate doLoopNumberOfIterations(EAST einit, EAST efinal,
            EAST eincr)
    {
        StringTemplate iterations = template("iterations");
        iterations.setAttribute("e1", treeToString(einit));
        iterations.setAttribute("e2", treeToString(efinal));
        iterations.setAttribute("e3", treeToString(eincr));
        
        return iterations;
    }
    
    public EAST numberOfIterations(EAST einit, EAST efinal, EAST eincr)
    throws SymbolicException
    {
        runWithResults(doLoopNumberOfIterations(einit,efinal,eincr));
        return evalAssuming(stringToTree(result));
    }
    
    
    public EAST createRecurrenceValue(String var)
    {
        EAST val = new EAST();
        val.setText(var);
        val.setType(RECVAR);
        return val;
    }
    
    public EAST simplify(EAST expr) throws SymbolicException
    {
        StringTemplate cmd = template("simplify");
        cmd.setAttribute("expr", treeToString(expr));
        run(cmd);
        return stringToTree(result);
    }
    
    public EAST eval(EAST expr, Context c)
    throws SymbolicException
    {
        StringTemplate set = getStateEquations(c.getState());
        runWithResults( getEquationEval(treeToString(expr), set) );
        return stringToTree(result);
    }
    
    public EAST evalArray(EAST array, EAST expr, Context c)
    throws SymbolicException
    {
        Map<String,EAST> filtered = new HashMap<String,EAST>(c.getState());
        filtered.remove(array.getText());
        
        StringTemplate set = getStateEquations(filtered);
        runWithResults( getEquationEval(treeToString(array), set) );
        EAST a = stringToTree(result);
        
        runWithResults( getEquationEval(treeToString(expr), set) );
        EAST ow = Expression.overwriteWith(a, stringToTree(result));
        
        return ow;
    }
    
    public boolean eval(EAST expr) throws SymbolicException
    {
        StringTemplate cmd = template("eval");
        cmd.setAttribute("expr", treeToString(expr));
        run(cmd);
        
        String value = result.toString();
        if (value.equals("true"))
            return true;
        else
            return false;
    }

    public EAST evalAssuming(EAST expr) throws SymbolicException
    {
        Variable v;
        StringTemplate as, type;
        
        boolean hasAssumptions = false;
        StringTemplate cmd = template("evalassuming");
        cmd.setAttribute("expr", treeToString(expr));
        for (EAST node : expr)
        {
            if (node.getType() == NAME)
            {
                v = symtbl.getVariable(node.getText());
                if (v != null)
                {
                    hasAssumptions = true;
                    as = template("typeassumption");
                    as.setAttribute("var", v.getName());
                
                    type = template(v.getType().toString());
                    as.setAttribute("type", type);
                
                    cmd.setAttribute("assumptions", as);
                }
            }
        }
        
        cmd.setAttribute("hasA", hasAssumptions);
        runWithResults(cmd);
        return stringToTree(result);
    }
    
    protected void finalize() throws Throwable
    {
        super.finalize();
        kernel.stop();
    }
}
