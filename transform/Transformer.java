/*
 * Created on 1-Sep-2005
 */
package transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import antlr.RecognitionException;
import antlr.collections.AST;

import recurrence.CFGExtender;
import recurrence.ContextGenerator;
import recurrence.Maple;
import recurrence.SymbolicEngine;
import treeutils.data.CodeRoot;
import treeutils.data.Function;
import treeutils.data.EAST;
import treeutils.data.StatementFunction;
import treeutils.data.SubProgSymTable;
import treeutils.data.SubprogramAST;
import treeutils.data.Symbol;
import treeutils.data.Variable;
import treeutils.data.Variable.DataType;
import util.ResultRecorder;
import util.ResultRecorder.Statistics;
import fortran77.parser.Fortran77TokenTypes;
import fortran77.Expression;
import fortran77.Intrinsic;
import graph.CFGCreationException;
import graph.ControlFlowGraph;
import graph.ControlFlowGraphGenerator;



/**
 * @author oli
 */
public class Transformer implements Fortran77TokenTypes
{
    SymbolicEngine symEng;
    SubprogramAST subprogram;
    SubProgSymTable symtable;
    ContextGenerator cgen;
    private Map<String, Function> intrin;
    private Map<String, Integer> knownIds;
    
    
    public Transformer()
    {
        symEng = new Maple();
        cgen = new ContextGenerator(symEng);
        intrin = Intrinsic.getFunctionSet();
        knownIds = new HashMap<String,Integer>();
    }
    
    
    public void setSubprogram(AST t)
    {
        subprogram = (SubprogramAST) t;
        symtable = subprogram.getSymbolTable();
        symEng.setSubprogramSymbolTable(symtable);
    }
    
    /**
     * <p>This transforms all FORTRAN subprograms into subroutines of sorts,
     * with input, output and local variables. This particular transformation
     * involves removing all input parameters (and output for functions) from
     * the syntax tree and creating a specialised node containing all this
     * information. 
     * </p>
     * 
     * @param oldSub - a node of type LITERAL_program, LITERAL_subroutine or
     *    LITERAL_function that should be transformed into a subprogram node.
     * @return - the transformed node subtree
     */
    public void subprogram(EAST oldSub, AST block, SubprogramAST newSub)
    {
        newSub.setType(SUBPROGRAM);
        newSub.setText(oldSub.getText());
        newSub.setParent(oldSub.getParent());
        newSub.setChildren(((EAST) block).getFirstChild());
        
        oldSub.replaceWithSubtree(newSub);
    }

    
    public void addSubprogramParam(AST param, SubprogramAST newSub)
    {
        newSub.getSymbolTable().addUnTypedParameter(param.getText());
    }

    
    public void addFunctionOutputParam(AST name, AST outputType,
            SubprogramAST newSub)
    {
        Variable v = new Variable(new Symbol((EAST) name),
                DataType.fromFortranType(outputType.getText()));
        //newSub.getSymbolTable().addOutput(v);
        newSub.getSymbolTable().setFunctionOutput(v);
    }

    
    public void addFunctionOutputParam(AST name, SubprogramAST newSub)
    {
        Variable v = new Variable(new Symbol((EAST) name));
        //newSub.getSymbolTable().addOutput(v);
        newSub.getSymbolTable().setFunctionOutput(v);
    }
    
    
    public void implicitStatement(AST type, AST range)
    {
        symtable.addImplicitRule(DataType.fromFortranType(type.getText()),
                (EAST) range);
    }
    
    
    public void implicitStatement(AST stmt)
    {
        ((EAST) stmt).removeThisSubtree();
    }
    
    
    public void dimension(AST dimStatement)
    {
        String name;
        Variable v;
        EAST[] dimensions;
        
        EAST dim = (EAST) dimStatement;
        for (EAST array : dim.children())
        {
            name = array.getText();
            dimensions = findDimensions(array);
            
            v = symtable.getLocalVariable(name);
            if (v != null)
            {
                // symbol is typed and local, cannot be a parameter or global
                v.setDimensions(dimensions);
            }
            else
            {
                v = symtable.getParameter(name);
                if (v != null)
                {
                    // symbol is a typed parameter, cannot be local or global
                    v.setDimensions(dimensions);
                }
                else
                {
                    v = symtable.getGlobalVariable(name);
                    if (v != null)
                    {
                        // symbol is typed and global, cannot be param or local
                        v.setDimensions(dimensions);
                    }
                    else
                    {
                        // we have an untyped parameter or local
                        v = new Variable(new Symbol(array), dimensions);
                        if (symtable.isUnTypedParameter(name))
                            symtable.addTypedParameter(v);
                        else
                            symtable.addLocalVariable(v);
                    }
                }
            }
            
        }
        
        dim.removeThisSubtree();
    }
    
    
    public void commonVariable(AST commonBlock, AST varName)
    {
        EAST commBlock = (EAST) commonBlock;
        EAST var = (EAST) varName;
        
        String commonName;
        if (commonBlock == null)
            commonName = "";
        else
            commonName = commonBlock.getText();
        
        // if the variable was typed before this common statement it will
        // be part of local variables. If it is global it cannot be a parameter
        Variable v = symtable.getLocalVariable(varName.getText());
        if (v == null)
            v = new Variable(new Symbol(var), findDimensions(var));
        else
        {
            symtable.removeLocalVariable(v.getName());
            if (var.getNumberOfChildren() > 0)
                v.setDimensions(findDimensions(var));
        }
        
        CodeRoot root = commBlock.getRoot();
        root.addGlobalVariable(commonName, v);
        symtable.addGlobalVariable(commonName,v);
    }
    
    public void common(AST commonStatement)
    {
        ((EAST) commonStatement).removeThisSubtree();
    }
    
    /**
     * <p>This transformation involves removing variable type specification
     * statements from the syntax tree and adding that information to the
     * subprogram list of variables.
     * </p>
     * 
     * <p>This not only adds a type to variables but also dimensions: whether
     * it is a scalar or an array of one or more dimensions. Scalars are
     * given dimension 0, vectors 1, and matrices 2, etc. It is unknown if
     * FORTRAN 77 supported arrays of dimensions greater than two, but these
     * cases are properly handled.
     * </p>
     * 
     * @param specStatement - the variable type specification statement subtree
     * @param subprogram - the subprogram node
     */
    public void specification(EAST specStatement)
    {
        Variable v;
        int length = 1;
        Iterable<EAST> i;
        
        DataType type = DataType.fromFortranType(specStatement.getText());
        
        // in fortran, the size of a memory allocation can sometimes be
        // specified by adding a "*NUM" to the type specification.
        EAST maybeSize = specStatement.getFirstChild();
        if (maybeSize.getType() == STAR)
        {
            i = maybeSize.siblings();
            length = Integer.parseInt(maybeSize.getFirstChild().getText());
        }
        else
            i = specStatement.children();
        
        for (EAST var : i)
        {
            v = symtable.getVariable(var.getText());
            if (v == null)
            {
                v = new Variable(new Symbol(var), type, findDimensions(var));
                if (type == DataType.CHARACTER)
                    v.setLength(length);

                // we don't want to mix parameters and locals 
                if (symtable.isUnTypedParameter(var.getText()))
                    symtable.addTypedParameter(v);
                else
                    symtable.addLocalVariable(v);
            }
            else
            {
                v.setType(type);
                if (var.getNumberOfChildren() > 0)
                    v.setDimensions(findDimensions(var));
            }
        }
        
        // this should be safe for the iterator since the right pointer
        // (next sibling) is left intact.
        specStatement.removeThisSubtree();
    }
    
    
    private EAST[] findDimensions(EAST var)
    {
        int k = 0;
        EAST[] dimensions = new EAST[var.getNumberOfChildren()];
        for (EAST arg : var.children())
        {
            dimensions[k] = arg;
            k++;
        }
        
        return dimensions;
    }
    
    /**
     * <p>This method analyses the Fortran PARAMETER statement used to declare
     * local constant parameters. The transformation is similar to a
     * specification statement, in that we add the local parameter to the
     * subprogram and remove the statement's subtree from the AST. The one
     * difference is that we may not know the type explicitely, which if not
     * implicit must be declared prior to the PARAMETER statement. If the type
     * was declared then it will be part of the subprogram's local variables,
     * in which case we set the constant's type to be that of the local variable
     * and remove it from that set. Otherwise we give it an implicit type.
     * </p>
     * 
     * @param tree - the PARAMETER statement syntax tree
     */
    public void parameter(AST tree)
    {
        Variable v;   
        for (EAST constant : ((EAST) tree).children())
        {
            constant = constant.getFirstChild(); // immediate children are "="
            
            // parameter is the only statement that requires that a symbol
            // be typed before the parameter statement.
            v = symtable.getLocalVariable(constant.getText());
            if (v != null)
                v = new Variable(new Symbol(constant), v.getType());
            else
                v = new Variable(new Symbol(constant),
                        symtable.getImplicitType(constant.getText()));
            
            
            v.setInitValue(constant.getNextSibling());
            symtable.addConstant(v);
        }   
        
        ((EAST) tree).removeThisSubtree();
    }
    
    /**
     * <p>This transformation consists in changing the syntax tree's token type
     * to INTRINSIC for all arguments of the statement. We then remove it from
     * the tree.
     * </p>
     * 
     * @param tree - the INTRINSIC statement syntax tree
     */
    public void intrinsic(AST tree)
    {
        String routine;
        for (EAST intrinNode : ((EAST) tree).children())
        {
            routine = intrinNode.getText();
            symtable.addIntrinsic(intrin.get(routine));
            knownIds.put(routine, INTRINSIC);
        }
        
        ((EAST) tree).removeThisSubtree();
    }
    
    /**
     * <p>This transformation is very much akin to the intrinsic one, except
     * that the return type of an external function may have been declared
     * prior to the external statement. Another possibility is for an
     * implicitly typed function, which may be confused for a typeless
     * subroutine.
     * </p>
     * 
     * @param tree - the EXTERNAL statement syntax tree
     */
    public void external(AST tree)
    {
        String routine;
        for (EAST externNode : ((EAST) tree).children())
        {
            routine = externNode.getText();
            symtable.addExternal(routine);
            knownIds.put(routine, EXTERNAL);
        }
        
        ((EAST) tree).removeThisSubtree();
    }
    
    
    /**
     * <p>Format statements are not executable statements and thus should be
     * removed from the syntax tree, as for specification statements.
     * </p>
     * 
     * @param tree - FORMAT statement syntax tree
     */
    public void format(AST label, AST tree)
    {
        symtable.addFormat(label.getText(), (EAST) tree);
        ((EAST) tree).removeThisSubtree();
    }
    
    /**
     * <p>Data statements are used to initialise variables with values, whether
     * scalars or arrays. This transformation removes the statement and assigns
     * initial values to local variables only. If any of the data items are
     * parameters or globals, we keep the data statement as part of the syntax
     * tree. This may result in code duplication but won't make anything wrong.
     * </p>
     * 
     * @param data - the data statement subtree
     * @param items - variables to be initialised
     * @param values - values to initialise the variables with
     */
    public void dataStatement(AST data, List<AST> items, List<AST> values)
    {
        Variable v;
        EAST item;
        
        boolean removeSubtree = true;
        
        int size,index;
        int i,j=0;
        for (i=0; i < items.size(); i++)
        {
            item = (EAST) items.get(i);
            if (item.getNumberOfChildren() == 0)
            {
                v = symtable.getLocalVariable(item.getText());
                if (v != null)
                {
                    if (v.getDimensions().length == 0)
                    {
                        v.setInitValue((EAST) values.get(j));
                        j++;
                    }
                    else if (v.getDimensions().length == 1)
                    {
                        size = v.size(0);
                        if (size < 0)
                            return;
                        
                        for (index=0; index < size; index++)
                        {
                            v.setInitValue(index, (EAST) values.get(j));
                            j++;
                        }
                    }
                    else
                        return;
                }
                else
                    return;
            }
            else
                return;
        }
        
        if (removeSubtree)
            ((EAST) data).removeThisSubtree();
    }
    
    /**
     * This transformation removes statement functions from the syntax tree,
     * storing the information into the subprogram.
     * 
     * @param sfAST - the statement function's subtree
     * @param exprAST - the function's expression subtree
     */
    public void statementFunction(AST sfAST, AST exprAST)
    {
        EAST sf = (EAST) sfAST;
        EAST sfExpr = (EAST) exprAST;
        String sfName = sf.getText();
        
        // check if this is not an array
        Variable v = symtable.getParameter(sfName);
        if (v != null)
            return;
        v = symtable.getLocalVariable(sfName);
        if (v != null && v.getDimensions().length > 0)
            return;
        
        knownIds.put(sfName, STFUNC);
        
        DataType returnType;
        if (v != null)
            returnType = v.getType();
        else
            returnType = symtable.getImplicitType(sfName);
        
        EAST param = sf.getFirstChild();
        Variable[] arguments = new Variable[sf.getNumberOfChildren()];
        for(int i=0; i < arguments.length; i++)
        {
            if (symtable.isLocalVariable(param.getText()))
            {
                arguments[i] = symtable.getLocalVariable(param.getText());
                //subprogram.removeLocal(param.getText());
                // since this variable is declared it *might* be used
                // later in the program. Don't remove.
            }
            else
            {
                arguments[i] = new Variable(new Symbol(param),
                        symtable.getImplicitType(param.getText()));
            }
            
            param = param.getNextSibling();
        }
        
        symtable.addStatementFunction(
                new StatementFunction(new Symbol(sfName, STFUNC),
                        returnType, arguments, sfExpr));
        
        sf.getParent().removeThisSubtree();
    }
    
    /**
     * <p>The use of this transformation is to better identify the real
     * purpose of a symbol: to know if a symbol is a variable (scalar, vector,
     * or matrix), an external function, an intrinsic function, a statement
     * function or finally a parameter subprogram. This is needed as things in
     * Fortran need not be explicitely declared.
     * </p>
     * 
     * <p>We make an important assumption during the analysis: we assume that
     * all arrays must be explicitely declared in order to properly dimension
     * them: an implicitly declared array is considered useless.
     * 
     * @param tree - the syntax tree to search and modify
     */
    public void analyseSymbolPurpose()
    {   
        for (Variable v : symtable.getAllVariables())
            if (v.getType() == DataType.IMPLICIT)
                symtable.giveImplicitType(v);
        
        for (EAST node : subprogram)
        {
            if (node.getType() == NAME)
            {   
                Integer astType = knownIds.get(node.getText());
                if (null != astType)
                    // If symbol has already been encountered, just change type
                    node.setType(astType);
                else if (node.getNumberOfChildren() > 0)
                    // arrays, external, intrinsic
                    analyseSymbolSubtrees(node);
                else
                    // scalars
                    analyseSymbolLeaf(node);
            }
        }
        
        knownIds.clear(); // we're done for this subprogram
    }
    
    
    private void analyseSymbolSubtrees(EAST node)
    {
        String symbol = node.getText();
        
        // if we have a typed parameter, make sure that the declared
        // dimension is 0 (scalar) so it is not an array, and that the
        // type refers to the type of the function's return value
        if (symtable.isTypedParameter(symbol))
        {
            Variable v = symtable.getParameter(symbol);
            if (v.isScalar())
            {
                node.setType(PARAMSUBPROGRAM);
                symtable.addParameterSubprogram(node,
                        v.getType(), node.getNumberOfChildren());
                knownIds.put(symbol, PARAMSUBPROGRAM);
            }
        }
        // if it's an untyped parameter then right away we know it can't
        // be an array (our assumption) and so it must be an external subprogram
        else if (symtable.isUnTypedParameter(symbol))
        {
            DataType type;
            if (node.getParent().getType() == LITERAL_call)
                type = null;
            else
                type = symtable.getImplicitType(symbol);

            node.setType(PARAMSUBPROGRAM);
            symtable.addParameterSubprogram(node, type,
                    node.getNumberOfChildren());
            knownIds.put(symbol, PARAMSUBPROGRAM);
        }
        // if the symbol is unknown then we check if it is an intrinsic
        // function, otherwise we know it must be an external function
        // or subroutine since it cannot be an array (our assumption)
        else if (!symtable.isLocalVariable(symbol)
                && !symtable.isGlobalVariable(symbol))
        {
            if (intrin.containsKey(symbol))
            {
                symtable.addIntrinsic(intrin.get(symbol));
                node.setType(INTRINSIC);
                knownIds.put(symbol, INTRINSIC);
            }
            else // this should never happen; undeclared external
            {    // subprogram cause compilation error
                symtable.addExternal(symbol);
                node.setType(EXTERNAL);
                knownIds.put(symbol, EXTERNAL);
            }
        }
        // else we found an array that is typed.
    }
    
    
    private void analyseSymbolLeaf(EAST node)
    {
        String symbol = node.getText();
        
        // We can detect subroutines that have no invocation arguments
        // since they must be used in a CALL statement.
        // TODO : something is not right. It is possible to have functions
        //   without arguments. I don't know how to detect those if they
        //   were not explicitely declared. Parsing drops the parenthesis...
        if (node.getParent().getType() == LITERAL_call)
        {
            if (symtable.isParameter(node.getText()))
            {
                symtable.addExternal(symbol);
                node.setType(PARAMSUBPROGRAM);
                knownIds.put(symbol, PARAMSUBPROGRAM);
            }
            else
            {
                symtable.addExternal(symbol);
                node.setType(EXTERNAL);
                knownIds.put(symbol, EXTERNAL);
            }
        }
        // if the variable is untyped but known, then we know it is not a
        // local. At this point, a local would be typed by a
        // specification statement, or unknown.
        else if (symtable.isUnTypedParameter(symbol))
        {
               Variable v = new Variable(new Symbol(symbol),
                       symtable.getImplicitType(symbol));
               symtable.addTypedParameter(v);
        }
        // from the information gathered previously (sub program's
        // parameters and specification statements), if the variable name
        // is unknown, it means this is a local variable and we must
        // create a new variable with an implicit type.
        //
        // here we have a special case where an undeclared array is
        // unlikely (and useless). So "variables" with more than one
        // child are most likely calls to subroutines, functions or
        // intrinsics. The other case to consider is that Fortran
        // allows passing of subroutines, functions and intrinsics as
        // arguments to a subprogram.
        else if ( symtable.isUnknown(symbol)  )
        {
            Variable v = new Variable(new Symbol(symbol),
                    symtable.getImplicitType(symbol),
                    node.getNumberOfChildren());
            symtable.addLocalVariable(v);
        }
    }
    
    /**
     * <p>This routine is not a transformation method per say. It is more used
     * for data mining. Its purpose is to gather information about how
     * identifiers are used within the given subprogram.
     * </p>
     * 
     * <p>Before this analysis can be done, the purpose of all symbols must have
     * been found.
     * </p>
     * 
     * <p>During the process, we record for each
     * node of the subprogram's syntax tree which identifiers are read or
     * written to within its subtree.
     * </p>
     */
    public void analyseInputOutput()
    {
        subprogram.analyseReadWrite();
        classifySubprogramParameters();
    }
    

    private void classifySubprogramParameters()
    {
        boolean isIn, isOut;
        String symbol;
        
        Set<String> in = subprogram.getInputs();
        Set<String> out = subprogram.getOutputs();
        for (Variable v : symtable.getParameters())
        {
            symbol = v.getName();
            if (out.contains(symbol))
            {
                if (in.contains(symbol))
                {
                    // here we may have a problem where a parameter is assigned
                    // a value before it is read. This makes it an output only,
                    // even if the variable is read from later. This is because
                    // of static single assignment.
                    isIn = false;
                    isOut = true;
                    for (EAST st : subprogram.statements())
                    {
                        isIn = isIn || st.getInputs().contains(symbol);
                        isOut = isOut || st.getOutputs().contains(symbol);
                        if (isOut && !isIn)
                        {
                            symtable.addOutput(v);
                            break;
                        }
                        else if (isIn)
                        {
                            symtable.addUpdate(v);
                            break;
                        }
                    }
                }
                else
                    symtable.addOutput(v);
            }
            else if (in.contains(symbol))
                symtable.addInput(v);
        }
            
        if (symtable.isOutput(subprogram.getName()) && !symtable.isFunction())
            symtable.setFunctionOutput(
                    symtable.getParameter(subprogram.getName()));
    }
    
    
    public void controlFlowAnalysis() throws CFGCreationException
    {
        ControlFlowGraph g = null;
        ControlFlowGraphGenerator cfg = new ControlFlowGraphGenerator();
        CFGExtender ext = new CFGExtender();
        
        try {
            g = cfg.subprogram(subprogram);
            ext.extend(g);
            ResultRecorder.saveCFG(g, subprogram.getName());
            cgen.generateContext(g);
        } catch (RecognitionException re) {
            throw new CFGCreationException(re);
        }
    }
    
    /**
     * This method attempts to clump sequential assignment statements which
     * data doesn't depend on each other in such a way that they can be
     * executed in parallel. This is a very simple, and definetely not thorough
     * way of parallelising code. But it is effective and doesn't require full
     * data flow analysis.
     * 
     * Another discrepancy is that it treats arrays as whole entities. That is
     * having assignments that assign to different array indices are treated
     * as sequential.
     * 
     * @param statement
     */
    public void reduceParallelStatements()
    {
        EAST parallel;
        Set<String> prevOut = new HashSet<String>();
        List<EAST> paraStmts = new ArrayList<EAST>();
        
        EAST s;
        Iterator<EAST> i;
        boolean sameParent;
        for (EAST stmt : subprogram.statements())
        {
            sameParent = paraStmts.isEmpty() ||
                    paraStmts.get(0).getParent() == stmt.getParent();
            if ( stmt.getType() == ASSIGN
                    && sameParent
                    && stmt.getFirstChild().getType() != LABEL
                    && stmt.getParent().getType() != PARALLEL
                    && Collections.disjoint(prevOut, stmt.getInputs()))
            {
                paraStmts.add(stmt);
                prevOut.addAll(stmt.getOutputs());
            }
            else if (paraStmts.size() > 1)
            {
                parallel = Expression.parallel();
                i = paraStmts.iterator();
                s = i.next();
                s.replaceWithSubtree(parallel);
                s.setNextSibling(null); // prevent circular sibling references
                parallel.setLine(s.getLine());
                parallel.addChild(s);
                
                while (i.hasNext()) 
                {
                    s = i.next();
                    s.removeThisSubtree();
                    s.setNextSibling(null);
                    parallel.addChild(s);
                }
                
                ResultRecorder.recordStat(Statistics.PARALLEL);
                
                paraStmts.clear();
                prevOut.clear();
                if (stmt.getType() == ASSIGN)
                {
                    paraStmts.add(stmt);
                    prevOut.addAll(stmt.getOutputs());
                }
            }
            else// if (stmt.getType() != COMMENT)
            {
                if (paraStmts.size() > 0)
                {
                    paraStmts.clear();
                    prevOut.clear();
                }
            }
        }
    }
    
    
    public void initialisation(AST assign, AST var, AST value)
    {
        Variable v = symtable.getVariable(var.getText());
        if (v != null)
            v.setInitValue((EAST) value);
        
        ((EAST) assign).removeThisStatement();
        
        ResultRecorder.recordStat(Statistics.INITIALISATION);
    }
    
    
    public void arithmeticIf(AST arithIf, AST expr,
            AST label1, AST label2, AST label3)
    {
        EAST aif  = (EAST) arithIf;
        EAST e    = (EAST) expr;
        EAST l1   = (EAST) label1; 
        EAST l2   = (EAST) label2;
        EAST l3   = (EAST) label3;
        EAST zero = Expression.zero();
        
        aif.setType(LITERAL_if); // back to normal IF
        EAST then = Expression.thenBlock();
        e.setNextSibling(then); // removes 3-way labels as siblings
        EAST els = Expression.elseBlock();
        
        // check if the arithmetic expression could be replaced by a boolean one
        if (e.getType() == MINUS)
        {
            EAST value = e.getFirstChild();
            EAST sub = value.getNextSibling();
            
            EAST bool;
            boolean gt = l1.getText().equals(l2.getText());
            boolean lt = l2.getText().equals(l3.getText());
            if (gt || lt)
            {
                if (gt)
                {
                    bool = Expression.greaterThan(value, sub);
                    then.addChild(Expression.goTo(l3));
                }
                else
                {
                    bool = Expression.lessThan(value, sub);
                    then.addChild(Expression.goTo(l1));
                }
                e.replaceWithSubtree(bool);
                
                aif.addChild(els);
                els.addChild(Expression.goTo(l2));
                
                return;
            }
            
            // not a useful subtraction expression. Use standard transformation
        }
        
        EAST ltz = Expression.lessThan(e, zero);
        e.replaceWithSubtree(ltz);
        
        then.addChild(Expression.goTo(l1));
        
        EAST elseif = Expression.elseif(Expression.greaterThan(e, zero));
        aif.addChild(elseif);
        then = Expression.thenBlock();
        then.addChild(Expression.goTo(l2));
        elseif.addChild(then);
        
        aif.addChild(els);
        els.addChild(Expression.goTo(l3));
        
        ResultRecorder.recordStat(Statistics.ARITHMETICIF);
    }
    
    
    
    
    private boolean isProjectedVector(EAST a, EAST i, EAST ix)
    {
        return ! (a.getText().equals("1") && i.getText().equals(ix.getText()));
    }
    
    private EAST arrayToVector(EAST array, EAST ix, EAST i, EAST a, EAST b)
    {
        if (isProjectedVector(a, i, ix))
            return Expression.vector(array, ix, i, a, b);
        else
            return Expression.vector(array, b);
    }


    private EAST arrayToVector(EAST array, EAST i, EAST a, EAST b)
    {
        EAST newArray;
        newArray = arrayToVector(array, array.getFirstChild(), i, a, b);
        array.replaceWithSubtree(newArray);
        return newArray;
    }
    
    private EAST arrayToVectorListX(EAST array, EAST dimx)
    {
        EAST newArray = Expression.arrayOfVectorsX(array, dimx);
        array.replaceWithSubtree(newArray);
        return newArray;
    }

    private EAST arrayToVectorListY(EAST array, EAST dimy)
    {
        EAST newArray = Expression.arrayOfVectorsY(array, dimy);
        array.replaceWithSubtree(newArray);
        return newArray;
    }
    

    public boolean subtreeContainsIndex(EAST subtree, EAST index)
    {
        for (EAST node : subtree)
        {
            if (node.equals(index))
                return true;
        }
        return false;
    }
    
    private EAST arraysToVectors(EAST expr, EAST i, EAST a, EAST b)
    {
        if (expr != null)
        {   
            EAST fc = expr.getFirstChild();
            int type = expr.getType();
            
            if (type != VECTOR)
                arraysToVectors(fc, i, a, b);
            arraysToVectors(expr.getNextSibling(), i, a, b);
            
            if (type == NAME)
            {
                int num = expr.getNumberOfChildren();
                if (num == 1 && subtreeContainsIndex(fc, i))
                    return arrayToVector(expr, i, a, b);
                else if (num == 2)
                {
                    // assume at most one of the indices will contain ``i''
                    // (rule #3, and syntax tree vector shape)
                    if (subtreeContainsIndex(fc, i))
                        return arrayToVectorListX(expr, b);
                    else if (subtreeContainsIndex(fc.getNextSibling(), i))
                        return arrayToVectorListY(expr, b);
                }
            }
            else if (type == VECTOR)
            {
                if (fc.getType() == NAME && subtreeContainsIndex(fc, i))
                {
                    EAST v;
                    EAST ns = fc.getNextSibling();
                    if (ns.getType() == DIMX)
                        v = Expression.matrix(expr, ns.getFirstChild(), b);
                    else
                        v = Expression.matrix(expr, b, ns.getFirstChild());
                    
                    expr.replaceWithSubtree(v);
                    return v;
                }
                else
                    arraysToVectors(fc, i, a, b);
            }
        }
        
        return expr;
    }
    
    public void dotProduct(AST _sum, AST _x, AST _y, AST _i, AST _a, AST _b)
    {
        EAST sum = (EAST) _sum;
        EAST x=(EAST)_x, y=(EAST)_y;
        EAST i=(EAST)_i, a=(EAST)_a, b=(EAST)_b;
        
        x = arraysToVectors(x.cloneSubTree(), i, a, b);
        y = arraysToVectors(y.cloneSubTree(), i, a, b);
        
        EAST dot = Expression.dotProduct(x,y);
        sum.replaceWithStatement(dot);
        ResultRecorder.recordStat(Statistics.DOTPRODS);
    }
    
    public void arrayAssignment(AST _forall, AST _array, AST _expr,
            AST _i, AST _a, AST _b)
    {
        EAST forall=(EAST)_forall, array=(EAST)_array, expr=(EAST)_expr;
        EAST i=(EAST)_i, a=(EAST)_a, b=(EAST)_b;
        
        array = arraysToVectors(array.cloneSubTree(), i, a, b);
        expr = arraysToVectors(expr.cloneSubTree(), i, a, b);
        
        EAST assign = Expression.assignment(array, expr);
        forall.replaceWithStatement(assign);
        ResultRecorder.recordStat(Statistics.ARRAYASSIGN);
    }
}
