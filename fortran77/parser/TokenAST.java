/*
 * Created on 20-Sep-2005
 */
package fortran77.parser;

import antlr.BaseAST;
import antlr.Token;
import antlr.CommonToken;
import antlr.collections.AST;

/**
 * <p>This class is used to replaced the ANTLR class CommonAST used by
 * default when building trees. It provides an implementation to the abstract
 * class BaseAST. 
 * </p>
 * 
 * <p>Its secret, just like CommonAST, is how a tree node is constructed from
 * a lexical token, and how the token information is stored.
 * </p>
 * 
 * @author Olivier Dragon <dragonoe@mcmaster.ca>
 */
public class TokenAST extends BaseAST implements Cloneable
{
    protected Token myToken;
    
    /*
     * The constructors must ensure that myToken is never null.
     */
    public TokenAST()
    {
        myToken = new CommonToken();
    }
    
    public TokenAST(Token t)
    {
        this();
        initialize(t);
    }
    
    public TokenAST(TokenAST node)
    {
        this();
        initialize(node);
    }
    
    
    public void initialize(Token t)
    {
        // for safety, ensure that myToken is never null.
        if (t != null)
        {
            myToken.setText(t.getText());
            myToken.setType(t.getType());
            myToken.setColumn(t.getColumn());
            myToken.setLine(t.getLine());
        }
    }
    
    public void initialize(int type, String txt)
    {
        myToken.setType(type);
        myToken.setText(txt);
    }
    
    public void initialize(AST node)
    {
        myToken.setText(node.getText());
        myToken.setType(node.getType());
        myToken.setColumn(node.getColumn());
        myToken.setLine(node.getLine());
    }
    
    public void initialize(TokenAST node)
    {
        initialize((AST) node);
        myToken.setFilename(node.getFilename());
    }
    
    public void setText(String txt)
    {
        myToken.setText(txt);
    }
    
    public String getText()
    {
        return myToken.getText();
    }
    
    public void setType(int type)
    {
        myToken.setType(type);
    }
    
    public int getType()
    {
        return myToken.getType();
    }
    
    public String getFilename()
    {
        return myToken.getFilename();
    }
    
    public int getLine()
    {
        return myToken.getLine();
    }
    
    public void setLine(int line)
    {
        myToken.setLine(line);
    }
    
    public int getColumn()
    {
        return myToken.getColumn();
    }
    
    public void setColumn(int column)
    {
        myToken.setColumn(column);
    }
    
    public TokenAST clone()
    {        
        return new TokenAST(this);
    }
}
