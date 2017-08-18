/*
 * Created on 1-Feb-2006
 */
package recurrence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import treeutils.data.EAST;
import treeutils.data.SubProgSymTable;
import treeutils.data.Variable;
import util.ResultRecorder;
import util.ResultRecorder.Statistics;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.SimpleDirectedSparseVertex;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;
import fortran77.Expression;
import fortran77.Expression.DoLoop;
import fortran77.parser.Fortran77TokenTypes;
import graph.BasicCodeBlock;
import graph.ControlFlowGraph;
import graph.FlowEdge;
import graph.BasicCodeBlock.BlockType;
import graph.BasicCodeBlock.VisitState;
import graph.FlowEdge.FlowType;

public class ContextGenerator implements Fortran77TokenTypes
{
    private Stack<LoopData> currentLoop;
    private SymbolicEngine symsengine;
    private ControlFlowGraph graph;
    
    private class LoopData
    {
        public Context preLoopContext;
        public DependencyGraph dependencyGraph;
        public DoLoop doLoop;
        public Map<String, EAST> recArrays;
        public BasicCodeBlock preHeader;
        public BasicCodeBlock loopHeader;
        public BasicCodeBlock postBody;
        public List<BasicCodeBlock> postExits; 
        
        public LoopData(BasicCodeBlock preHeader)
        {
            postExits = new ArrayList<BasicCodeBlock>(5);
            recArrays = new HashMap<String, EAST>();
            
            this.preHeader = preHeader;
            loopHeader = (BasicCodeBlock)
                    preHeader.getSuccessors().iterator().next();
            
            Iterator i = loopHeader.getPredecessors().iterator();
            postBody = (BasicCodeBlock) i.next();
            if (postBody == preHeader)
                postBody = (BasicCodeBlock) i.next();
            
            // only works with do-loops. Needed for updateLoopHeader()
            Iterator doSucc = loopHeader.getSuccessors().iterator();
            BasicCodeBlock postExit = (BasicCodeBlock) doSucc.next();
            if ( postExit.getType() != BlockType.POSTEXIT )
                postExit = (BasicCodeBlock) doSucc.next();
            postExits.add(postExit);
        }
    }
    
    
    private class DependencyGraph
    {
        private Graph g;
        private Map<String, Vertex> vertices;
        LinkedList<String> topologicalOrdering;
        
        private static final String recvarName = "recvar";
        private static final String visitState = "visit";
        
        public DependencyGraph(Set<String> recurrenceVariables)
        {
            g = new DirectedSparseGraph();
            vertices = new HashMap<String, Vertex>();
            
            Vertex v;
            for (String var : recurrenceVariables)
            {
                v = new SimpleDirectedSparseVertex();
                v.addUserDatum(recvarName, var, UserData.SHARED);
                
                vertices.put(var, v);
                g.addVertex(v);
            }
        }
        
        public void addDependency(String dependent, String independent)
        {
            // only check dependencies for recurrence variables
            if (vertices.containsKey(independent))
            {
                Vertex indep = vertices.get(independent);
                Vertex dep = vertices.get(dependent);
                String u = (String) indep.getUserDatum(recvarName);
                String v = (String) dep.getUserDatum(recvarName);
                
                try {
                    // don't add self dependence. Taken care of by rsolving.
                    if ( ! u.equals(v) )
                        g.addEdge(new DirectedSparseEdge(dep, indep));
                } catch (Exception e) {
                    // this means this edge (dependency) is already in the graph
                    // just skip
                }
            }
        }
        
        private void topologicalSort() throws CyclicDependencyException
        {
            if (topologicalOrdering == null)
            {
                topologicalOrdering = new LinkedList<String>();
                
                Vertex v;
                Iterator i;
                for (i = g.getVertices().iterator(); i.hasNext(); )
                {
                    v = (Vertex) i.next();
                    v.addUserDatum(visitState, VisitState.UNDISCOVERED,
                            UserData.SHARED);
                }
                
                for (i = g.getVertices().iterator(); i.hasNext(); )
                {
                    v = (Vertex) i.next();
                    if ( ((VisitState) v.getUserDatum(visitState))
                            == VisitState.UNDISCOVERED )
                        dfsVisit(v);
                }
            }
        }
        
        private void dfsVisit(Vertex u) throws CyclicDependencyException
        {
            u.setUserDatum(visitState, VisitState.ENCOUNTERED, UserData.SHARED);
            
            Vertex v;
            VisitState s;
            String from, to;
            for(Iterator i = u.getSuccessors().iterator(); i.hasNext(); )
            {
                v = (Vertex) i.next();
                s = (VisitState) v.getUserDatum(visitState);
                if ( s == VisitState.UNDISCOVERED )
                    dfsVisit(v);
                else if (s == VisitState.ENCOUNTERED)
                {
                    from = (String) u.getUserDatum(recvarName);
                    to = (String) v.getUserDatum(recvarName);
                    if (! from.equals(to))
                        throw new CyclicDependencyException(from + "-->" + to
                                + " *** " + toString());
                }
                
            }

            u.setUserDatum(visitState, VisitState.PROCESSED,
                    UserData.SHARED);
            topologicalOrdering.addLast((String) u.getUserDatum(recvarName));
        }
        
        /**
         * Iterates through the graph's nodes in topological order.
         * @throws CyclicDependencyException 
         */
        public Iterable<String> topologicalOrder()
                throws CyclicDependencyException
        {
            topologicalSort();
            return topologicalOrdering;
        }
        
        public String toString()
        {
            Edge e;
            Pair p;
            Vertex v;
            StringBuilder s = new StringBuilder();
            
            s.append("{");
            for (Iterator i = g.getEdges().iterator(); i.hasNext(); )
            {
                e = (Edge) i.next();
                p = e.getEndpoints();
                v = (Vertex) p.getFirst();
                s.append(v.getUserDatum(recvarName));
                s.append("-->");
                v = (Vertex) p.getSecond();
                s.append(v.getUserDatum(recvarName));
                s.append(", ");
            }
            s.append("}");
            
            return s.toString();
        }
    }
    
    
    
    public ContextGenerator(SymbolicEngine e)
    {
        currentLoop = new Stack<LoopData>();
        symsengine = e;
    }
    
    
    public void generateContext(ControlFlowGraph g)
    {
        BasicCodeBlock b;
        
        graph = g;
        graph.annotateCFG();
        symsengine.reInit();
        // reverse post-order iterator (topological)
        // find loops
        Iterator<BasicCodeBlock> i = graph.iterator();
        while(i.hasNext())
        {
            b = i.next();
            if (b.getType() == BlockType.PREHEADER)
                generateLoopContext(i,b);
        }
        
//        ** See generateLoopContext()
//        for (BasicCodeBlock b : g)
//        {
//            generateContextEntry(b);
//            generateContextExit(b);
//        }
    }

    
    private void generateLoopContext(Iterator<BasicCodeBlock> i,
            BasicCodeBlock b)
    {
        try 
        {
            // generate context for current loop's pre-header
            generateContextEntry(b);
            generateContextExit(b);
            // do the rest of the loop
            do {
                b = i.next();
                if (b.getType() == BlockType.PREHEADER) // nested loop
                {
                    // handle the inner loop
                    generateLoopContext(i, b);
                    // don't process the outer loop, leave that to the next
                    // iteration through the reverse engineering process.
                    break;
                }   
                else
                {
                    generateContextEntry(b);
                    testArrayAssignment(b);
                    generateContextExit(b);
                }
            // this exit condition wrongly assumes that in general loops have
            // only one exit point. For our case it is ok as we don't treat
            // loops with multiple exits.
            } while (i.hasNext() && b.getType() != BlockType.POSTEXIT);
            
        } // return on exception and keep searching for loops
          catch (BranchingLoopException ble) {
            ResultRecorder.recordError("BranchingLoopException: "
                    + ble.getMessage());
        } catch (UnknownSignLoopIncrException e) {
            ResultRecorder.recordError("UnknownSignLoopIncrException: "
                    + e.getMessage());
        } catch (UnsupportedStatementException e) {
            ResultRecorder.recordError("UnsupportedStatementException: "
                    + e.getMessage());
        } catch (ArrayAssignmentException e) {
            ResultRecorder.recordError("ArrayAssignmentException: "
                    + e.getMessage());
        } catch (CyclicDependencyException e) {
            ResultRecorder.recordError("CyclicDependencyException: "
                    + e.getMessage());
        } catch (SymbolicException e) {
            ResultRecorder.recordError("SymbolicException: "
                    + e.getMessage());
        }
        finally
        {
            currentLoop.pop();           
        }
    }
    
    
    private void generateContextEntry(BasicCodeBlock n)
    throws BranchingLoopException, SymbolicException, ArrayAssignmentException
    {
        if (n.getPredecessors().size() == 0)
            initStartNode(n);
        // This next statement differs from the usual method because we
        // generate context information only for loops.
        // Therefore a pre-header node has no predecessor context information
        // and we must deal with it as a special case.
        else if (n.getType() != BlockType.PREHEADER)
            n.setEntry(foldAllPredecessors(n));
        
        
        if (n.getType() == BlockType.PREHEADER)
            initPreHeader(n);
        else if (n.getType() == BlockType.POSTEXIT)
            updatePostExit(n);
    }
    
    
    private void generateContextExit(BasicCodeBlock n)
    throws UnknownSignLoopIncrException, UnsupportedStatementException,
    ArrayAssignmentException, CyclicDependencyException, SymbolicException
    {
        BasicCodeBlock succ = null;
        
        // this is not quite right, but since we don't care about branches
        // it really doesn't matter. Loops get updated after post-body.
        Context exit = evalExitContext(n, n.getEntry());
        
        for(Iterator i = n.getSuccessors().iterator(); i.hasNext(); )
        {
            succ = (BasicCodeBlock) i.next();
            n.addExit(succ, exit);
        }
        
        if (n.getType() == BlockType.POSTBODY)
            updatePreHeader(n, n.getExitTo(succ));
    }

    
    private Context evalExitContext(BasicCodeBlock n, Context entry)
            throws UnknownSignLoopIncrException, UnsupportedStatementException,
            ArrayAssignmentException, SymbolicException
    {
        Context c = null;
        switch (n.getType())
        {
        case BLOCK:
            c = evalBlockContext(n, entry);
            break;
        case PREHEADER:
        case POSTBODY:
        case POSTEXIT:
            c = new Context(entry); // no change
        }
        return c;
    }
    
    
    private Context evalBlockContext(BasicCodeBlock n, Context entry)
            throws UnknownSignLoopIncrException, UnsupportedStatementException,
            ArrayAssignmentException, SymbolicException
    {
        Context exit = null;
        for(EAST stmt : n.getStatements())
        {
            exit = evalStatementContext(entry, stmt);
            
            // blocks can contain multiple statements, unlike the method
            // outlined in F&R. Therefore, we must update the entry context for
            // the next statement. Intermediate exit contexts are discarded.
            entry = exit;
        }
        
        return exit;
    }

    
    private Context evalStatementContext(Context entry, EAST stmt)
        throws UnsupportedStatementException, ArrayAssignmentException,
            SymbolicException
    {
        Context exit = null;
        
        EAST variable;
        switch (stmt.getType())
        {
        case PARALLEL:
            for (EAST s : stmt.children())
            {
                exit = evalStatementContext(entry, s);
                entry = exit;
            }
            break;
        case ASSIGN:
            variable = stmt.getFirstChild();
            if (variable.getType() == LABEL)
                variable = variable.getNextSibling();
            
            exit = sideEffect(stmt, entry);
            break;
        case LITERAL_do:
            LoopData loopData = currentLoop.peek();
            if (loopData.doLoop == null)
            {
                DoLoop l = new DoLoop(stmt);
                loopData.doLoop = l;
                
                loopData.preLoopContext.getState().put(
                        l.getLoopVariable().getText(), l.getInitialValue());
            }
            exit = new Context(entry); // no change (yet)
            break;
        /*
         * we don't usually handle if statements but we make an exception
         * for those that enclose the whole body of the loop. These can be
         * abstracted as part of the outer loop entry condition. This enables
         * us to process nested loops.
         */
        case LITERAL_if:
            if (staticIfStatement(stmt, entry.getState().keySet()))
            {
                EAST cond = stmt.getFirstChild();
                if (cond.getType() == LABEL)
                    cond = cond.getNextSibling();

                exit = new Context(entry);
                exit.setStateCondition(
                        Expression.and(exit.getStateCondition(), cond));
            }
            else
                throw new UnsupportedStatementException(stmt.toString()
                        + ", in " + stmt.getSubprogram().getName());
            
            break;
            
        case LITERAL_continue:
            exit = new Context(entry); // no change
            break;
        default:
            throw new UnsupportedStatementException(stmt.toString()
                    + ", in " + stmt.getSubprogram().getName());
        }
        
        return exit;
    }
    
    private boolean staticIfStatement(EAST stmt, Set<String> recVars)
    {
        EAST parent = stmt.getParent();
        int numChildren = parent.getNumberOfChildren();
        boolean isOnlyStatement = numChildren == 1
                || (numChildren == 2
                        && parent.getLastChild().getType() == LITERAL_continue);
        return parent.getType() == DOBLOCK
                && isOnlyStatement
                && ! exprContainsRecVar(stmt.getFirstChild(), recVars);
    }
    
    private Context sideEffect(EAST stmt, Context entry)
            throws SymbolicException, ArrayAssignmentException
    {
        // expression for new state
        EAST expr = stmt.getLastChild();
        
        // variable that changes state
        EAST var = stmt.getFirstChild();
        if (var.getType() == LABEL)
            var = var.getNextSibling();
        
        EAST newExpr = null;
        if (var.getNumberOfChildren() == 0)
            newExpr = symsengine.eval(expr, entry);
        else
        {
            boolean containsRecVar = false;
            for (EAST index : var.children())
            {
                if (exprContainsRecVar(index, entry.getState().keySet()))
                {
                    containsRecVar = true;
                    newExpr = symsengine.evalArray(var, expr, entry);
                    break;
                }
            }
            
            // array is static with respect to the loop. Index expressions don't
            // contain recurrence variables.
            if (! containsRecVar)
//                throw new ArrayAssignmentException("array " + var.getText()+
//                        " is static with respect to the loop"
//                        + " in " + stmt.getSubprogram().getName());
                newExpr = symsengine.eval(
                        flattenStaticRecArray(expr, var.getText()), entry);
        }
        
        Context exit = new Context(entry);
        exit.getState().put(var.getText(), newExpr);
        
        return exit;
    }
    
    /*
     * For recurrence an array which is static with respect to the loop, we
     * treat it as scalars by carrying out the symbolic analysis without its
     * indices.
     */
    private EAST flattenStaticRecArray(EAST expr, String name)
    {
        expr = expr.cloneSubTree();
        for (EAST node : expr)
            if (node.getType() == NAME && name.equals(node.getText()))
                node.replaceWithSubtree(Expression.newVariable(name));
        
        return expr;
    }
    
    private EAST expandStaticRecArrays(EAST expr)
    {
        EAST array;
        Map<String,EAST> arrays = currentLoop.peek().recArrays;
        expr = expr.cloneSubTree();
        
        for (EAST node : expr)
        {
            array = arrays.get(node.getText());
            if (array != null && node.getNumberOfChildren() == 0)
                node.replaceWithSubtree(array);
        }
        
        return expr;
    }
    
    /**
     * Setup first node in the control flow graph since it has no predecessors.
     * Its entry context is the initialisation of all state variables and 
     * conditions.
     */
    private void initStartNode(BasicCodeBlock n)
    {
        // only doing loops so not implemented
    }
    
    /**
     * 1. Find all recurrence variables. This is essential to the algorithm
     *    because if we don't know which variables are recurrence ones we
     *    can't properly model the recurrence equation. 
     * 2. Initialise the recurrence variables (eg. v) to a recurrence value
     *    (eg. v(k)).
     * 3. Initialise the dependancy graph for the recurrence variables.
     */
    private void initPreHeader(BasicCodeBlock ph) throws ArrayAssignmentException
    {
        currentLoop.push(new LoopData(ph));
        
        Context currentrec = new Context();
        ph.setEntry(currentrec);

        findRecurrenceVariables(ph);
        analysePrePreHeader(ph, currentrec.getState().keySet());
    }
    
    /*
     * Try to find initial values for recurrence variables
     */
    private void analysePrePreHeader(BasicCodeBlock ph, Set<String> recvars)
    throws ArrayAssignmentException
    {
        LoopData loopData = currentLoop.peek();
        loopData.preLoopContext = new Context();

        
        Set preds = ph.getPredecessors();
        if (preds.size() == 1)
        {
            BasicCodeBlock pred = (BasicCodeBlock) preds.iterator().next();
            if (! checkPreHeaderStmts(pred.getStatements(), recvars))
                loopData.preLoopContext = new Context();
        } 
        else if (preds.isEmpty() && graph.isStartNode(ph))
        {
            Map<String, EAST> state =
                    loopData.preLoopContext.getState();
            SubProgSymTable stbl = loopData.loopHeader.getFirstStatement()
                    .getSubprogram().getSymbolTable();
            
            for (Variable v : stbl.getAllVariables())
                if (v.isScalar() && v.isInitialised())
                    state.put(v.getName(), v.getInitValue());
        }
    }
    
    private boolean checkPreHeaderStmts(Iterable<EAST> statements,
            Set<String> recvars) throws ArrayAssignmentException
    {
        Context c = currentLoop.peek().preLoopContext;
        for (EAST stmt : statements)
        {
            switch (stmt.getType())
            {
            case ASSIGN:
                if (isComplexExprOrWithArray(stmt.getLastChild(), recvars))
                    return false;
                try { c = sideEffect(stmt, c); }
                    catch (SymbolicException e) { return false; }
                break;
            case PARALLEL:
                if (! checkPreHeaderStmts(stmt.children(), recvars))
                    return false;
                break;
            default:
                return false;
            }
        }
        return true;
    }


    private boolean isComplexExprOrWithArray(EAST ph, Set<String> recvars)
    {
        int type;
        boolean isRecArray, isRecVar;
        for (EAST node : ph)
        {
            type = node.getType();
            isRecVar = recvars.contains(node.getText());
            isRecArray = (type == NAME &&
                    node.getNumberOfChildren() > 0 && ! isRecVar);
            if (type == EXTERNAL || type == PARAMSUBPROGRAM || isRecArray)
                return false;
        }
        return true;
    }


    private void findRecurrenceVariables(BasicCodeBlock n)
    {
        EAST rv;
        BasicCodeBlock b = null;
        
        // get the iterator at the position of the current node (PREHEADER)
        Iterator<BasicCodeBlock> i = graph.iterator(n);

        Map<String, EAST> recState = n.getEntry().getState();
        while(i.hasNext())
        {
            b = i.next();
            if (b.getType() == BlockType.POSTBODY)
                break;
            
            if (b.getType() == BlockType.PREHEADER)
                skipInnerLoops(i);
            else
            {
                for (EAST stmt : b.getStatements())
                {
                    stmt.analyseReadWrite();
                    for (String name : stmt.getOutputs())
                    {
                        rv = symsengine.createRecurrenceValue(name);
                        recState.put(name, rv);
                    }
                }
            }
        }
    }
    
    
    private void skipInnerLoops(Iterator<BasicCodeBlock> i)
    {
        BasicCodeBlock b;
        do {
            b = i.next();
            if (b.getType() == BlockType.PREHEADER)
                skipInnerLoops(i);
        } while (b.getType() != BlockType.POSTBODY && i.hasNext());
    }
    
    /**
     * Try to obtain close forms for recurrence relation (Fahringer and Scholz)
     * 
     * Computing the closed form of a recurrence relation involves finding
     * the last iteration (max k) based on a recurrence variable and the exit
     * condition. This is not always possible, but should be for do-loops.
     * @throws SymbolicException 
     */
    private void updatePostExit(BasicCodeBlock n) throws SymbolicException
    {
        DoLoop loop = currentLoop.peek().doLoop;
        
        Map<String, EAST> state = n.getEntry().getState();
        EAST loopBody = loop.getCodeBlock();
        EAST doStmt = loop.getDoStatement();
        
        // results of symbolic analysis are all parallel if there are no
        // circular dependencies.
        EAST result = Expression.parallel();
        
        // find closed form for all variables and perform the transformations
        getClosedForm(loop, state, loopBody, result);
        
        // process the do-loop's iteration variable
        EAST newExpression = getClosedFormExpression(loop.getLoopVariable(),
                state, loop);
        result.addChild(newExpression);
        
        // enclose the loop body within an if-statement in case the loop
        // does not execute
        EAST cond = symsengine.evalAssuming(
                Expression.and(n.getEntry().getStateCondition(),
                        loop.getEntryCondition()));
        EAST ifStmt = Expression.ifStmt(cond);
        EAST then = Expression.thenBlock();
        doStmt.insertPreviousSibling(ifStmt);
        result.setNextSibling(null);
        then.addChild(result);
        ifStmt.addChild(then);

        // cleanup
        if (loopBody.getNumberOfChildren() == 0)
            doStmt.removeThisStatement();
        
        ResultRecorder.recordStat(Statistics.LOOPS);
    }


    private void getClosedForm(DoLoop loop, Map<String, EAST> state,
            EAST codeBlock, EAST result) throws SymbolicException
    {
        EAST var;
        EAST newExpression;
        for(EAST stmt : codeBlock.children())
        {
            if (stmt.getType() == PARALLEL)
                getClosedForm(loop, state, stmt, result);
            else if (stmt.getType() == LITERAL_if)
            {
                getClosedForm(loop, state, stmt.getLastChild(), result);
                stmt.removeThisSubtree();
            }
            else if (stmt.getType() == LITERAL_continue)
            {
                // do not keep continue statements. As we do not allow for
                // branches inside the loop body and Fortran prohibits jumpts
                // from outside a loop inside (including the terminal statement)
                // we can safely remove all continue statements inside loops.
                stmt.removeThisSubtree();
            }
            else
            {
                String label = null;
                var = stmt.getFirstChild();
                if (var.getType() == LABEL)
                {
                    label = var.getText();
                    var = var.getNextSibling();
                }
                

                newExpression = getClosedFormExpression(var, state, loop);
                result.addChild(newExpression);
                
                if (label != null
                        && label.equals(loop.getLabelRef().getText()))
                    stmt.removeThisSubtree();
                else
                    stmt.removeThisStatement();
            }
        }

        if (codeBlock.getNumberOfChildren() == 0)
            codeBlock.removeThisSubtree();
    }
    
    private EAST getClosedFormExpression(EAST var, Map<String,EAST> state,
            DoLoop l)
    throws SymbolicException
    {
        if (state.get(var.getText()).getType() == OVERWRITE)
            return getArrayClosedForm(var, state, l);
        else
            return getScalarClosedForm(var, state, l);
    }
    
    private EAST getScalarClosedForm(EAST var, Map<String,EAST> state,
            DoLoop l)
    throws SymbolicException
    {
        EAST cfexpr;
        
        try
        {
            cfexpr = symsengine.getClosedForm(state.get(var.getText()),
                    l.getInitialValue(),
                    l.getFinalValue(),
                    l.getIncrement());
        }
        catch (SymbolicException e)
        {
            cfexpr = Expression.max(l.getNumberOfIterations(),
                    Expression.zero());
            cfexpr = symsengine.evalAssuming(cfexpr);
            cfexpr = Expression.equals(
                    Expression.newVariable("k_"), cfexpr);
            cfexpr = Expression.eval(state.get(var.getText()), cfexpr);
        }

        correctVariableNames(cfexpr, state);
        cfexpr = expandStaticRecArrays(Expression.assignment(var, cfexpr));
        
        return cfexpr;
    }
    
    private EAST getArrayClosedForm(EAST var, Map<String,EAST> state,
            DoLoop l)
    throws SymbolicException
    {
        EAST ow = state.get(var.getText());
        ow = ow.getFirstChild();
        EAST owexpr = ow.getNextSibling(); 
        
        EAST cfexpr = Expression.arrayClosedForm(ow, owexpr,
                symsengine.numberOfIterations(
                        l.getInitialValue(),
                        l.getFinalValue(),
                        l.getIncrement()));
        
        correctVariableNames(cfexpr, state);
        return cfexpr;
    }
    
    
    private void correctVariableNames(EAST expr, Map<String,EAST> state)
    {
        String zeroName, name;
        for (EAST atom : expr)
        {
            zeroName = atom.getText();
            if (atom.getType() == NAME)
            {
                if (zeroName.endsWith("0"))
                {
                    name = zeroName.substring(0, zeroName.length() - 1);
                    if (state.containsKey(name))
                        atom.setText(name);
                } // else if (other maple oddities)
            }
        }
    }

    
    private void updateDependencies(Context n)
    {
        Map<String, EAST> state = n.getState();
        DependencyGraph g = new DependencyGraph(state.keySet());
        
        int type;
        for (Map.Entry<String, EAST> assign : state.entrySet())
        {
            for (EAST atom : assign.getValue())
            {
                type = atom.getType();
                if (type == NAME || type == FUNCTION || type == RECVAR)
                    g.addDependency(assign.getKey(), atom.getText());
            }
        }
        
        currentLoop.peek().dependencyGraph = g;
    }
    
    /**
     * Update the recurrence relations after encountering a POSTBODY node.
     * 
     * The method outlined in Fahringer and Scholz specifies that this
     * routine should insert the recurrence relations for all recurrence
     * variables into the pre-header node.
     * 
     * Instead we keep a stack of recurrence contexts that define the current
     * loop.
     * 
     * We assume that we are only dealing with do-loops.
     * @throws CyclicDependencyException 
     * @throws SymbolicException 
     * @throws ArrayAssignmentException 
     */
    private void updatePreHeader(BasicCodeBlock n, Context r)
    throws CyclicDependencyException, SymbolicException,
    ArrayAssignmentException
    {
        LoopData loopData = currentLoop.peek();
        
        // first must analyse the do-loop's loop variable's incrementation
        DoLoop l = loopData.doLoop;
        EAST assign = Expression.assignment(l.getLoopVariable(),
                Expression.add(l.getLoopVariable(), l.getIncrement()));
        r = sideEffect(assign, r);
        
        // figure out the data dependencies
        updateDependencies(r);
        
        // then  solve the recurrence equations in order of data dependency
        Map<String, EAST> pState = loopData.preLoopContext.getState();
        Map<String, EAST> rState = r.getState();
        for (String var : loopData.dependencyGraph.topologicalOrder())
        {
            rState.put(
                    var, symsengine.solveRecurrenceEquation(
                            var, pState.get(var), rState));
        }
        
        // finally update the loop header's exit to the POSTEXIT node. We can
        // do this because we assume the loop only has one exit point.
        // `n' is POSTBODY. Its only successor is the loop header. 
        updateLoopHeader(n, r);
    }


    private void updateLoopHeader(BasicCodeBlock n, Context r)
    {   
        BasicCodeBlock postExit =
            currentLoop.peek().postExits.get(0); // assume only one exit
        currentLoop.peek().loopHeader.addExit(postExit, r);
    }
    
    
    private Context foldAllPredecessors(BasicCodeBlock n)
        throws BranchingLoopException
    {
        Iterator i;
        FlowEdge e;
        Context c1 = null;//, c2;
        BasicCodeBlock pred;

        // find first non-backward edge
        for (i = n.getPredecessors().iterator(); i.hasNext(); )
        {
            pred = (BasicCodeBlock) i.next();
            e = (FlowEdge) pred.findEdge(n);
            if (e.getFlowType() != FlowType.BACKWARD)
                c1 = new Context(pred.getExitTo(n));
        }
        
        // fold remaining non-backward edges
        while (i.hasNext())
        {
            // currently we don't handle branching loops completely.
            pred = (BasicCodeBlock) i.next();
            e = (FlowEdge) pred.findEdge(n);
            if (e.getFlowType() != FlowType.BACKWARD)
            {
                // If we have a branch to fold we assume it is a static
                // if statement inside a loop.
                if (pred.getStatements().iterator().hasNext()
                        && pred.getFirstStatement().getType() != LITERAL_if)
                {
                    c1 = new Context(pred.getExitTo(n));
                    return c1;
                }
                else
                    throw new BranchingLoopException();
            }
                
//            c2 = ((BasicCodeBlock) i.next()).getExitTo(n);
//            c1 = c1.fold(c2);
        }
        
        return c1;
    }
    
    

    private void testArrayAssignment(BasicCodeBlock b)
    throws ArrayAssignmentException
    {
        searchForArrayAssignments(b.getStatements(),
                b.getEntry().getState().keySet());
    }
    
    /*
     * Test #1 and #2:
     * Search for array assignments, then look for expressions in all
     * all assignment statements.
     */
    private void searchForArrayAssignments(Iterable<EAST> statements,
            Set<String> recVars)
    throws ArrayAssignmentException
    {
        String arrayName;
        EAST arrayNode;
        Map<String,EAST> arrays = currentLoop.peek().recArrays;

        for (EAST stmt : statements)
        {
            if (stmt.getType() == PARALLEL)
                searchForArrayAssignments(stmt.children(), recVars);
            else if (stmt.getType() == ASSIGN)
            {
                arrayNode = stmt.getFirstChild();
                if (arrayNode.getType() == LABEL)
                    arrayNode = arrayNode.getNextSibling();
                
                if (arrayNode.getType() == NAME && checkDimension(arrayNode))
                {
                    checkMatrix(arrayNode, recVars);
                    
                    arrayName = arrayNode.getText();
                    if (arrays.containsKey(arrayName))
                        throw new ArrayAssignmentException("array " + arrayName
                                + " is assigned to multiple times");
                    else
                        arrays.put(arrayName, arrayNode.cloneSubTree());
                }
                else if (arrayNode.getType() == VECTOR)
                {
                    arrayName = arrayNode.getText();                    
                    if (arrays.containsKey(arrayName))
                        throw new ArrayAssignmentException("array " + arrayName
                                + " is assigned to multiple times");
                    else
                        arrays.put(arrayName, arrayNode.cloneSubTree());
                }
            }
            // else // don't test non-assignment statements. They will get
                    // caught by the statement analysis.
        }
        
        
        // do it again, this time only checking assigned expressions for
        // array references. Don't check for parallel block again as those have
        // been recursively checked already.
        EAST assignedVar;
        for (EAST stmt : statements)
        {
            if (stmt.getType() == ASSIGN)
            {
                assignedVar = stmt.getFirstChild();
                if (assignedVar.getType() == LABEL)
                    assignedVar = assignedVar.getNextSibling();
                
                checkExpressionForArrayReferences(assignedVar.getText(),
                        assignedVar.getNextSibling(), recVars);
            }
        }
    }


    /*
     * Test #1 and #2:
     * Check array references in all assignment expressions.
     */
    private void checkExpressionForArrayReferences(String recarray, EAST expr,
            Set<String> recVars)
    throws ArrayAssignmentException
    {
        EAST array;
        Map<String,EAST> arrays = currentLoop.peek().recArrays;
        for (EAST atom : expr)
        {
            array = arrays.get(atom.getText());
            if (atom.getType() == NAME
                    && checkDimension(atom)
                    && array != null
                    && atom.getParent().getType() != VECTOR)
            {
                if (!recarray.equals(atom.getText()))
                    throw new ArrayAssignmentException(
                        "read from array " + atom.getText()
                        + " outside its assignment statement");
                else if (!atom.equalsTree(array))
                    throw new ArrayAssignmentException("indices for array "
                            + atom.getText() + "are different: [#1] "
                            + array.toStringList() + ", [#2] "
                            + atom.toStringList());
            }
            else if (atom.getType() == VECTOR && array != null)
            {
                if (!recarray.equals(atom.getText()))
                    throw new ArrayAssignmentException(
                        "read from array " + atom.getText()
                        + " outside its assignment statement");
            }
        }
    }

    /*
     * Test #3:
     * Ensure that only one of a matrix' array indices contains recurrence
     * variables.
     */
    private void checkMatrix(EAST matrix, Set<String> recVars)
    throws ArrayAssignmentException
    {
        if (matrix.getNumberOfChildren() == 2)
        {
            EAST ix = matrix.getFirstChild();
            EAST iy = ix.getNextSibling();
    
            if (exprContainsRecVar(ix, recVars)
                    && exprContainsRecVar(iy, recVars))
            {
                throw new ArrayAssignmentException("matrix " + matrix.getText()
                        + " has multiple recurrence indices.");
            }
        }
    }
    
    private boolean exprContainsRecVar(EAST expr, Set<String> recVars)
    {
        for (EAST node : expr)
        {
            if (recVars.contains(node.getText()))
                return true;
        }
        return false;
    }
    
    /*
     * Test #4:
     * Check that there are no arrays with dimensions larger than 2.
     */
    private boolean checkDimension(EAST array)
    throws ArrayAssignmentException
    {
        int dim = array.getNumberOfChildren();
        if (dim > 2)
            throw new ArrayAssignmentException("array " + array.getText()
                    + " has " + dim + " dimensions.");
        else if (dim == 1 || dim == 2)
            return true;
        else
            return false;
    }
}
