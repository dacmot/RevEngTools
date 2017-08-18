/*
 * Created on 18-Aug-2005
 */
package treeutils.data;

import fortran77.Expression;
import fortran77.Expression.DoLoop;
import fortran77.parser.TokenAST;
import fortran77.parser.Fortran77TokenTypes;

import antlr.collections.AST;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.Iterator;

/**
 * <p>This class implements additions to the TokenAST class. EAST stands for
 * Enhanced AST.
 * </p>
 * 
 * <p>
 * It is a datastructure that is used to represent abstract syntax tree nodes.
 * Its secret is how the tree is stored. Its existence is justified because
 * the default AST class provided by ANTLR does not support useful tree
 * modification operations.
 * </p> 
 * 
 * @author Olivier Dragon <dragonoe@mcmaster.ca>
 */
public class EAST
    extends TokenAST
    implements Iterable<EAST>, Fortran77TokenTypes

{
    private EAST parent;
    private EAST prevSibling;
    private EAST lastChild;
    private Set<String> inputs;
    private Set<String> outputs;
    
    private class SingleRootDepthFirstIterator extends DepthFirstIterator
    {
        boolean newIterator;
        
        public SingleRootDepthFirstIterator()
        {
            newIterator = true;
            stack = new Stack<EAST>();
            next = EAST.this;
        }
        
        public EAST next()
        {
            // this is to avoid visiting sibling trees; we only want to visit
            // this current tree. We can't simply set next to be the first
            // child because the iterator wouldn't include the root of the
            // tree.
            if (newIterator)
            {
                current = next;
                next = current.getFirstChild();
                newIterator = false;
            }
            else
            {
                super.next();
            }
            
            return current;
        }
    }

    private class DepthFirstIterator implements Iterator<EAST>
    {
        EAST current;
        EAST next;
        Stack<EAST> stack;
        
        public DepthFirstIterator()
        {
            stack = new Stack<EAST>();
            next = EAST.this;
        }
        
        public EAST next()
        {
            current = next;
            
            if (current.getFirstChild() == null)
            {
                if (current.getNextSibling() == null)
                    if (!stack.isEmpty())
                        do {
                            next = (EAST) stack.pop().getNextSibling();
                        } while (next == null && !stack.isEmpty());
                    else
                        next = null;
                else
                    next = (EAST) current.getNextSibling();
                    
            }
            else
            {
                next = (EAST) current.getFirstChild();
                stack.push(current);
            }

            return current;
        }
        
        public boolean hasNext()
        {
            return (next != null);
        }
        
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
        
        public Iterator<EAST> iterator()
        {
            return this;
        }
    }
    
    private class ForestDepthFirstView implements Iterable<EAST>
    {
        public Iterator<EAST> iterator()
        {
            return new DepthFirstIterator();
        }
    }
    
    private class ChildrenIterator implements Iterator<EAST>
    {
        private EAST current;
        protected EAST next;
        
        public ChildrenIterator()
        {
            next = EAST.this.getFirstChild();
        }
        
        public EAST next()
        {
            current = next;
            next = current.getNextSibling();
            return current;
        }
        
        public boolean hasNext()
        {
            return (next != null);
        }
        
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
    
    private class Children implements Iterable<EAST>
    {
        public Iterator<EAST> iterator()
        {
            return new ChildrenIterator();
        }
    }

    private class SiblingsIterator extends ChildrenIterator
    {
        public SiblingsIterator()
        {
            next = EAST.this.getNextSibling();
        }
    }
    
    private class Siblings implements Iterable<EAST>
    {
        public Iterator<EAST> iterator()
        {
            return new SiblingsIterator();
        }
    }
    
    
    /* ***********************************************************************
     * 
     * PUBLIC METHODS
     * 
     * ***********************************************************************/
    
    public EAST()
    {
        super();
        inputs = new HashSet<String>();
        outputs = new HashSet<String>();
    }
    
    public EAST(TokenAST clone)
    {
        super(clone);
        inputs = new HashSet<String>();
        outputs = new HashSet<String>();
    }
    
    public EAST(EAST clone)
    {
        super(clone);
        inputs = new HashSet<String>(clone.getInputs());
        outputs = new HashSet<String>(clone.getOutputs());
    }
    
    /**
     * The following method is used to override the superclass' which now
     * returns a node of type EAST instead of AST. This avoids a lot of
     * tedious type casting.
     */
    public EAST getFirstChild()
    {
        return (EAST) this.down;
    }

    /**
     * The following method is used to override the superclass' which now
     * returns a node of type EAST instead of AST. This avoids a lot of
     * tedious type casting.
     */
    public EAST getNextSibling()
    {
        return (EAST) this.right;
    }
    
    public EAST getLastChild()
    {
        return lastChild;
    }
    
    public EAST getPreviousSibling()
    {
        return prevSibling;
    }

    public void addChild(AST t)
    {
        addChild((EAST) t);
    }
    
    public void addChild(EAST t)
    {
        if (t != null)
        {
            t.setPreviousSibling(lastChild);
            
            if (down == null)
                setChildren(t);
            else
                lastChild.setNextSiblings(t);
        }
    }
    
    /**
     * @see setChildren(EAST forest)
     */
    public void setFirstChild(AST subtree)
    {
        setChildren((EAST) subtree);
    }

    /**
     * <p>In this routine, all children of this node are replaced by <code>
     * forest</code>. This forest can consist of a single subtree, or multiple
     * sibling subtrees.
     * </p>
     * 
     * @param forest - the new first child of this tree node
     */
    
    public void setChildren(EAST forest)
    {
        down = forest;
        lastChild = updateSiblings(forest, this);
    }
    
    /**
     * @see setNextSiblings(EAST forest)
     */
    public void setNextSibling(AST subtree)
    {
        if (subtree != null)
            setNextSiblings((EAST) subtree);
        else
            right = null;
    }

    /**
     * <p>In this routine, forest becomes this node's next sibling, replacing
     * all the previous siblings following this one. The forest's siblings,
     * if it has any, also become siblings of this node.
     * </p>
     * 
     * @param forest - the new next sibling
     */
    
    public void setNextSiblings(EAST forest)
    {   
        forest.setPreviousSibling(this);
        right = forest;
        if (parent != null)
            parent.resetLastChild(updateSiblings(forest, parent));
    }
    
    private EAST updateSiblings(EAST forest, EAST parent)
    {
        EAST last;
        do
        {
            forest.setParent(parent);
            last = forest;
            forest = forest.getNextSibling();
        }
        while (forest != null);
        
        return last;
    }
    
    private void setPreviousSibling(EAST subtree)
    {
        prevSibling = subtree;
    }
    
    private void resetLastChild(EAST subtree)
    {
        lastChild = subtree; 
    }

    public void removeChildren()
    {
        down = null;
        lastChild = null;
    }
    
    private void detach()
    {
        parent = null;
        //right = null; // issues w/ transformations inside tree parser
        prevSibling = null;
        // don't change `down', keep subtree
    }
    
    /**
     * This method is used to replace the current subtree in the larger tree
     * with a different subtree.
     * 
     * @param subtree - the new subtree to insert in place of this one.
     */
    public void replaceWithSubtree(EAST subtree)
    {
        if (right != null)
        {
            subtree.setParent(parent); // prevent setNextSibling w/ null parent
            subtree.setNextSiblings(getNextSibling());
        }
        // else do nothing
        
        if (prevSibling != null)
        {
            subtree.setPreviousSibling(prevSibling);
            prevSibling.setNextSiblings(subtree);
        }
        else if (parent != null)
            parent.setChildren(subtree);
        
        detach();
    }
    
    public void replaceWithStatement(EAST subtree)
    {
        if (down != null && down.getType() == LABEL)
            subtree.insertFirstChild(this.getFirstChild().clone());
        replaceWithSubtree(subtree);
    }
    
    public void removeThisSubtree()
    {        
        if (prevSibling != null)
            prevSibling.removeNextSibling();
        else
            parent.removeFirstChild();
        
        detach();
    }
    
    public void removeThisStatement()
    {
        if (down == null || down.getType() != LABEL)
            this.removeThisSubtree();
        else
            this.replaceWithSubtree(
                    Expression.continueStmt(this.getFirstChild().clone()));
    }
    
    /**
     * Remove the current node's first child (unless the current node has no
     * children). This removes the whole child's subtree, but only this child
     * and not any other of its siblings.
     */
    public void removeFirstChild()
    {
        EAST fc;
        if (down != null)
        {
            fc = getFirstChild();
            down = fc.getNextSibling();
            if (down != null)
            {
                getFirstChild().setPreviousSibling(null);
                fc.detach();
            }   
            else
                lastChild = null;
        }
    }
    
    /**
     * Remove the current node's following sibling (unless the current node has
     * no siblings). This removes the whole sibling's subtree, but only this
     * sibling and not all the following ones.
     */
    public void removeNextSibling()
    {
        EAST ns;
        if (right != null)
        {
            ns = getNextSibling();
            right = ns.getNextSibling();
            if (right != null)
            {
                getNextSibling().setPreviousSibling(this);
                ns.detach();
            }
            else
                parent.resetLastChild(this);
        }
    }

    /**
     * Method similar to addChild() but which inserts the subTree at the
     * beginning of the children list instead of appending it to the end.
     * 
     * @param subtree - 
     */
    public void insertFirstChild(EAST subtree)
    {
        if (subtree.getNextSibling() != null)
            throw new IllegalArgumentException();
        
        if (down != null)
        {
            subtree.setParent(this);
            subtree.setNextSiblings(getFirstChild());
            setChildren(subtree);
        }
        else
            setChildren(subtree);
    }
    
    public void insertPreviousSibling(EAST subtree)
    {
        if (subtree.getNextSibling() != null)
            throw new IllegalArgumentException();
        
        if (prevSibling != null)
            prevSibling.setNextSiblings(subtree);
        else
            parent.setChildren(subtree);

        subtree.setNextSiblings(this);
    }
    
    /**
     * Remove all siblings that follow.
     */
    public void removeFollowingSiblings()
    {
        right = null;
        parent.resetLastChild(this);
    }
    
    /**
     * This method overrides the one in BaseAST to create a string
     * representation so that it actually looks like a tree.
     */
    public String toStringTree()
    {
        return visitorPrinter(this, new StringBuilder(), 0).toString();
    }
    
    private StringBuilder visitorPrinter(AST t, StringBuilder s, int childDepth)
    {
        if (t != null)
        {
	        s.append(spaces(childDepth));
            s.append(t.toString());
            s.append("\n");
	        
	        s = visitorPrinter(t.getFirstChild() ,s, childDepth+1);
	        s = visitorPrinter(t.getNextSibling(), s, childDepth);
        }
        
        return s;
    }
    
    private String spaces(int number)
    {
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < number; i++)
            padding.append("|   ");
        return padding.toString();
    }
    
    private void clearInputsAndOutputs()
    {
        inputs.clear();
        outputs.clear();
    }
    
    public Set<String> getInputs()
    {
        return inputs;
    }
    
    public Set<String> getOutputs()
    {
        return outputs;
    }
    
    public void analyseReadWrite()
    {
        Iterator<EAST> i;
        EAST child = null;
        
        clearInputsAndOutputs();
        switch (getType())
        {
        case ASSIGN:
            // find assigned variable
            child = getFirstChild();
            if (child.getType() == LABEL)
                child = child.getNextSibling();
            
            child.getOutputs().clear();
            child.getOutputs().add(child.getText());
            addTreeRemainingInputs(child.getFirstChild()); // array indices
            addSubTreeRemainingInputs(child.getNextSibling()); // expression
            break;
            
        case LITERAL_do:
            DoLoop l = new DoLoop(this);

            EAST loopVar = l.getLoopVariable();
            loopVar.getOutputs().clear();
            loopVar.getOutputs().add(loopVar.getText());

            addSubTreeRemainingInputs(l.getInitialValue());
            addSubTreeRemainingInputs(l.getFinalValue());
            addSubTreeRemainingInputs(l.getIncrement());

            // this last child of the do-loop is the DOBLOCK
            this.getLastChild().analyseReadWrite();
            break;
            
        case LITERAL_if:
        case ELSEIF:
            child = getFirstChild(); // condition expression
            addSubTreeRemainingInputs(child); 
            child.getNextSibling().analyseReadWrite(); // THENBLOCK
            break;
        
        case SUBPROGRAM:
        case PARALLEL:
        case THENBLOCK:
        case ELSEBLOCK:
        case DOBLOCK:
        case FORALL:
            // loop through all the code block's statements
            for (i = children().iterator(); i.hasNext(); )
                i.next().analyseReadWrite();
            // THENBLOCK may have ELSEIFs or an ELSEBLOCK as siblings
            if (getType() == THENBLOCK)
                for (i = siblings().iterator(); i.hasNext(); )
                    i.next().analyseReadWrite();
            
            break;

        // speed things up a bit
        case COMMENT:
        case LITERAL_return:
        case LITERAL_continue:
        case LITERAL_end:
        case LITERAL_stop:
            break;
            
        case LITERAL_call:
            child = getFirstChild();
            if (child.getType() == LABEL)
                child = child.getNextSibling();
            
            analyseExternal(child);
            break;
            
        case NAME:
        case VECTOR:
            inputs.add(getText());
            // continue and add all other names in this subtree to inputs
        default:
            addSubTreeRemainingInputs(getFirstChild());
        }
        
        mergeChildrenRW(this);
    }

    private void addSubTreeRemainingInputs(EAST node)
    {
        if (node != null)
        {   
            clearInputsAndOutputs();
            if (node.getType() == NAME)
                node.getInputs().add(node.getText());
            
            if (node.getNumberOfChildren() > 0)
            {
//                if (node.getType() == EXTERNAL)
//                    analyseExternal(node);
//                else
//                {
                    addTreeRemainingInputs(node.getFirstChild());
                    mergeChildrenRW(node);
//                }
            }
        }
    }
    
    private void addTreeRemainingInputs(EAST node)
    {
        if (node != null)
        {   
            addSubTreeRemainingInputs(node);
            addTreeRemainingInputs(node.getNextSibling());
        }
    }
    
    private void mergeChildrenRW(EAST node)
    {
        Set<String> in = node.getInputs();
        Set<String> out = node.getOutputs();
        
        for(EAST child : node.children())
        {
            in.addAll(child.getInputs());
            out.addAll(child.getOutputs());
        }
    }
    
    private void analyseExternal(EAST subprogram)
    {
        
    }
    
    
    public boolean equalsTree(AST t)
    {
        return equalsTree((EAST) t);
    }
    
    public boolean equalsTree(EAST t)
    {
        Iterator<EAST> i = t.iterator();
        for (EAST node : this)
        {
            if ( !(i.hasNext() && node.equals(i.next())) )
                return false;
        }
        
        if (i.hasNext())
            return false;
        else
            return true;
    }
    
    /**
     * <p>The Iterator returned by this method will iteratively go through this
     * tree's nodes in a depth-first (children before siblings) manner. Nodes
     * are hence "visited". Whenever a node is encountered by the visitor
     * patterns it is returned by the Iterator's next() method.
     * </p>
     * <p>In this way, we can almost completely hide the tree structure.</p>
     * 
     * @return the depth-first iterator
     */
    public Iterator<EAST> iterator()
    {
        return new SingleRootDepthFirstIterator();
    }
    
    public Iterable<EAST> forestDepthFirstView()
    {
        return new ForestDepthFirstView();
    }
    
    /**
     * <p>The Iterator returned by this method will iteratively go through this
     * tree's children without visiting them recursively.
     * </p>
     * 
     * @return the children iterator
     */
    public Iterable<EAST> children()
    {
        return new Children();
    }
    
    /**
     * <p>The Iterator returned by this method will iteratively go through this
     * tree's siblings without visiting them recursively.
     * </p>
     * 
     * @return the siblings iterator
     */
    public Iterable<EAST> siblings()
    {
        return new Siblings();
    }
    
    public EAST clone()
    {
        return new EAST(this);
    }
    
    public EAST cloneSubTree()
    {
        EAST clone = new EAST(this);
        
        EAST fc = getFirstChild();
        if (fc != null)
            clone.setChildren(fc.cloneForest());
            
        return clone;
    }
    
    public EAST cloneForest()
    {
        EAST clone = new EAST(this);
        
        EAST fc = getFirstChild();
        if (fc != null)
            clone.setChildren(fc.cloneForest());
        
        EAST ns = getNextSibling();
        if (ns != null)
            clone.setNextSiblings(ns.cloneForest());
            
        return clone;
    }

    public CodeRoot getRoot()
    {
        EAST root = this;
        while (root.getParent() != null)
            root = root.getParent();
        
        return (CodeRoot) root;
    }
    
    public SubprogramAST getSubprogram()
    {
        EAST subp = this;
        while (subp.getParent() != null && subp.getType() != SUBPROGRAM)
            subp = subp.getParent();
        
        if (subp.getType() == SUBPROGRAM)
            return (SubprogramAST) subp;
        else
            return null;
    }
    
    // Below are automatically generated setters and getters.

    public void setParent(EAST parent)
    {
        this.parent = parent;
    }

    public EAST getParent()
    {
        return parent;
    }
}
