/*
 * Created on 7-Feb-2006
 */
package fortran77;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;

import antlr.RecognitionException;

import recurrence.UnknownSignLoopIncrException;
import treeutils.data.EAST;

import fortran77.parser.Fortran77TokenTypes;
import fortran77.printer.CodePrinter;

public class Expression implements Fortran77TokenTypes
{
    public static class DoLoop
    {
        boolean unknownSignLoopIncr;
        EAST loop;
        EAST labelref;
        EAST doVar;
        EAST e1;
        EAST e2;
        EAST e3;
        EAST cond;
        EAST entry;
        EAST iter;
        EAST body;
        
        public DoLoop(EAST doStatement)
        {   
            if (doStatement.getType() != LITERAL_do)
                throw new IllegalArgumentException("Not a do statement");
            
            loop = doStatement;
            Iterator<EAST> i = doStatement.children().iterator();
            
            do {
                labelref = i.next();
            } while(labelref.getType() != LABELREF);

            doVar = i.next();
            e1 = i.next();
            e2 = i.next();
            e3 = i.next();
            if (e3.getType() == DOBLOCK)
            {
                body = e3;
                e3 = newInteger(1);
            }
            else if (i.hasNext())
                body = i.next();
        }
        
        public EAST getLoopVariable()
        {
            return doVar;
        }
        
        public EAST getInitialValue()
        {
            return e1;
        }
        
        public EAST getFinalValue()
        {
            return e2;
        }
        
        public EAST getIncrement()
        {
            return e3;
        }
        
        public EAST getDoStatement()
        {
            return loop;
        }
        
        public EAST getCodeBlock()
        {
            return body;
        }
        
        public EAST getLabelRef()
        {
            return labelref;
        }
        
        public EAST getExitCondition() throws UnknownSignLoopIncrException
        {
            if (cond == null)
            {
                int incr = evalExpr(e3);
                if (incr > 0)
                    cond = lessThanEqualTo(doVar, e2);
                else if (incr < 0)
                    cond = greaterThanEqualTo(doVar, e2);
                else
                    unknownSignLoopIncr = true;
            }
            
            if (unknownSignLoopIncr)
                throw new UnknownSignLoopIncrException(e3.toString());
            else
                return cond;
        }
        
        public EAST getNumberOfIterations()
        {
            if (iter == null)
            {
                iter = subtract(e2, e1);
                iter = add(iter, e3);
                iter = divide(iter, e3);
                iter = trunc(iter);
            }
            
            return iter;
        }
        
        public EAST getEntryCondition()
        {
            if (entry == null)
                entry = greaterThan(getNumberOfIterations(), zero());
            return entry;
        }
        
        public String toString()
        {
            return "do " + doVar.getText() + "=" + e1.getText() + ", "
                    + e2.getText();
        }
    }
    
    
    
    private static int evalExpr(EAST expr)
    {
        String exprChar = "";
        try {
            CodePrinter printer = new CodePrinter();
            File tmpl = new File("fortran77/printer/f77-code_template.stg");
            printer.setTemplates(new StringTemplateGroup(new FileReader(tmpl),
                    AngleBracketTemplateLexer.class));
            StringTemplate code = printer.expr(expr);
            exprChar = code.toString();
        }
          catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (RecognitionException e) {
            e.printStackTrace();
        }
        
        if (Pattern.matches("^(-)?[0-9]+$", exprChar))
            return Integer.parseInt(exprChar);
        
        return 0;
    }
    
    public static boolean isPositive(EAST expr)
    {
        return evalExpr(expr) > 0;
    }
    
    public static EAST implies(EAST ifExpr, EAST thenExpr)
    {
        EAST expr = new EAST();
        expr.setType(IMPLIES);
        expr.setText(".implies.");
        expr.addChild(ifExpr.cloneSubTree());
        expr.addChild(thenExpr.cloneSubTree());
        return expr;
    }

    public static EAST negateExpr(EAST expr)
    {
        EAST neg = new EAST();
        neg.setType(LNOT);
        neg.setText(".not.");
        neg.addChild(expr.cloneSubTree());
        return neg;
    }

    public static EAST lessThan(EAST lhs, EAST rhs)
    {
        EAST lt = new EAST();
        lt.setType(LT);
        lt.setText(".lt.");
        lt.addChild(lhs.cloneSubTree());
        lt.addChild(rhs.cloneSubTree());
        return lt;
    }
    
    public static EAST lessThanEqualTo(EAST lhs, EAST rhs)
    {
        EAST leq = new EAST();
        leq.setType(LE);
        leq.setText(".le.");
        leq.addChild(lhs.cloneSubTree());
        leq.addChild(rhs.cloneSubTree());
        return leq;
    }
    
    public static EAST greaterThan(EAST lhs, EAST rhs)
    {
        EAST gt = new EAST();
        gt.setType(GT);
        gt.setText(".gt.");
        gt.addChild(lhs.cloneSubTree());
        gt.addChild(rhs.cloneSubTree());
        return gt;
    }
    
    public static EAST greaterThanEqualTo(EAST lhs, EAST rhs)
    {
        EAST geq = new EAST();
        geq.setType(GE);
        geq.setText(".ge.");
        geq.addChild(lhs.cloneSubTree());
        geq.addChild(rhs.cloneSubTree());
        return geq;
    }
    
    public static EAST equals(EAST lhs, EAST rhs)
    {
        EAST eq = new EAST();
        eq.setType(EQ);
        eq.setText(".eq.");
        eq.addChild(lhs.cloneSubTree());
        eq.addChild(rhs.cloneSubTree());
        return eq;
    }
    
    public static EAST range(EAST from, EAST to)
    {
        EAST r = new EAST();
        r.setType(RANGE);
        r.setText(":");
        r.addChild(from.cloneSubTree());
        r.addChild(to.cloneSubTree());
        return r;
    }
    
    public static EAST assignment(EAST variable, EAST value)
    {
        EAST a = new EAST();
        a.setType(ASSIGN);
        a.setText("=");
        a.addChild(variable.cloneSubTree());
        a.addChild(value.cloneSubTree());
        return a;
    }
    
    public static EAST continueStmt(EAST label)
    {
        EAST c = new EAST();
        c.setType(LITERAL_continue);
        c.setText("continue");
        c.addChild(label.cloneSubTree());
        return c;
    }
    
    public static EAST parallel()
    {
        EAST p = new EAST();
        p.setType(PARALLEL);
        p.setText("[parallel]");
        return p;
    }
    
    public static EAST ifStmt(EAST cond)
    {
        EAST i = new EAST();
        i.setType(LITERAL_if);
        i.setText("if");
        i.addChild(cond.cloneSubTree());
        return i;
    }
    
    public static EAST elseif(EAST expr)
    {
        EAST eif = new EAST();
        eif.setType(ELSEIF);
        eif.setText("elseif");
        eif.addChild(expr.cloneSubTree());
        return eif;
    }
    
    public static EAST thenBlock()
    {
        EAST then = new EAST();
        then.setType(THENBLOCK);
        then.setText("then");
        return then;
    }
    
    public static EAST elseBlock()
    {
        EAST els = new EAST();
        els.setType(ELSEBLOCK);
        els.setText("else");
        return els;
    }
    
    public static EAST and(EAST a, EAST b)
    {
        EAST and = new EAST();
        and.setType(LAND);
        and.setText(".and.");
        and.addChild(a.cloneSubTree());
        and.addChild(b.cloneSubTree());
        return and;
    }
    
    public static EAST goTo(EAST label)
    {
        EAST go = new EAST();
        go.setType(LITERAL_go);
        go.setText("go");
        go.addChild(label.cloneSubTree());
        return go;
    }
    
    public static EAST add(EAST a, EAST b)
    {
        EAST c = new EAST();
        c.setType(PLUS);
        c.setText("+");
        c.addChild(a.cloneSubTree());
        c.addChild(b.cloneSubTree());
        return c;
    }
    
    public static EAST subtract(EAST a, EAST b)
    {
        EAST c = new EAST();
        c.setType(MINUS);
        c.setText("-");
        c.addChild(a.cloneSubTree());
        c.addChild(b.cloneSubTree());
        return c;
    }
    
    public static EAST multiply(EAST a, EAST b)
    {
        EAST c = new EAST();
        c.setType(STAR);
        c.setText("*");
        c.addChild(a.cloneSubTree());
        c.addChild(b.cloneSubTree());
        return c;
    }
    
    public static EAST divide(EAST a, EAST b)
    {
        EAST c = new EAST();
        c.setType(DIV);
        c.setText("/");
        c.addChild(a.cloneSubTree());
        c.addChild(b.cloneSubTree());
        return c;
    }
    
    public static EAST dotProduct(EAST u, EAST v)
    {
        EAST p = new EAST();
        p.setType(DOTPROD);
        p.setText(".");
        p.addChild(u.cloneSubTree());
        p.addChild(v.cloneSubTree());
        return p;
    }
    
    public static EAST piecewise(EAST e1, EAST cond, EAST e2)
    {
        EAST p = new EAST();
        p.setType(PIECEWISE);
        p.setText("piecewise");
        p.addChild(e1.cloneSubTree());
        p.addChild(cond.cloneSubTree());
        p.addChild(e2.cloneSubTree());
        return p;
    }
    
    public static EAST overwriteWith(EAST array, EAST expr)
    {
        EAST o = new EAST();
        o.setType(OVERWRITE);
        o.setText(".o/w.");
        o.addChild(array.cloneSubTree());
        o.addChild(expr.cloneSubTree());
        return o;
    }
    
    public static EAST trunc(EAST expr)
    {
        EAST t = new EAST();
        t.setType(INTRINSIC);
        t.setText("int");
        t.addChild(expr.cloneSubTree());
        return t;
    }
    
    public static EAST max(EAST a, EAST b)
    {
        EAST m = new EAST();
        m.setType(INTRINSIC);
        m.setText("max");
        m.addChild(a.cloneSubTree());
        m.addChild(b.cloneSubTree());
        return m;
    }
    
    public static EAST eval(EAST expr, EAST where)
    {
        EAST e = new EAST();
        e.setType(EVAL);
        e.setText("eval");
        e.addChild(expr.cloneSubTree());
        e.addChild(where.cloneSubTree());
        return e;
    }
    
    public static EAST getTrue()
    {
        EAST t = new EAST();
        t.setType(TRUE);
        t.setText(".true.");
        return t;
    }
    
    public static EAST newVariable(String name)
    {
        EAST v = new EAST();
        v.setText(name);
        v.setType(NAME);
        return v;
    }
    
    public static EAST newInteger(int value)
    {
        EAST v = new EAST();
        v.setText("" + value);
        v.setType(ICON);
        return v;
    }
    
    public static EAST zero()
    {
        return newInteger(0);
    }

    private static EAST projection(EAST ix, EAST i, EAST a, EAST b)
    {
        EAST p = new EAST();
        p.setType(PROJ);
        p.setText("");
        p.addChild(ix.cloneSubTree());
        p.addChild(equals(i.clone(), range(a, b)));
        return p;
    }
    
    private static EAST vector(EAST x)
    {
        EAST v = new EAST();
        v.setType(VECTOR);
        v.setText(x.getText());
        return v;
    }
    
    public static EAST vector(EAST x, EAST ix, EAST i, EAST a, EAST b)
    {
        EAST v = vector(x);
        v.addChild(projection(ix, i, a, b));
        return v;
    }
    
    private static EAST dim(int type, EAST size)
    {
        EAST d = new EAST();
        d.setType(type);
        d.setText("");
        d.addChild(size.cloneSubTree());
        return d;
    }
    
    public static EAST vector(EAST x, EAST m)
    {
        EAST v = vector(x);
        v.addChild(dim(DIMX, m));
        return v;
    }
    
    public static EAST arrayOfVectorsX(EAST x, EAST dimx)
    {
        EAST a = vector(x);
        x = x.cloneSubTree();
        x.removeFirstChild();
        a.addChild(x);
        a.addChild(dim(DIMX, dimx));
        return a;
    }
    
    public static EAST arrayOfVectorsY(EAST x, EAST dimy)
    {
        EAST a = vector(x);
        x = x.cloneSubTree();
        x.getFirstChild().removeFollowingSiblings();
        a.addChild(x);
        a.addChild(dim(DIMY, dimy));
        return a;
    }
    
    public static EAST matrix(EAST x, EAST m, EAST n)
    {
        EAST v = vector(x, m);
        v.addChild(dim(DIMY, n));
        return v;
    }
    
    public static EAST arrayClosedForm(EAST array, EAST expr, EAST numiter)
    {
        EAST a = new EAST();
        a.setType(FORALL);
        a.setText("forall");
        a.addChild(Expression.newVariable("k_0"));
        
        EAST c = new EAST();
        c.setType(RANGE);
        c.setText("..");
        c.addChild(newInteger(1));
        c.addChild(numiter.cloneSubTree());
        
        EAST eq = new EAST();
        eq.setType(EQ);
        eq.setText(".eq.");
        eq.addChild(newVariable("k_0"));
        eq.addChild(c);

        EAST s = assignment(array.cloneSubTree(), piecewise(expr, eq, array));
        a.addChild(s);
        
        return a;
    }
}
