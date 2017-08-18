/*
 * Created on 29-Jul-2005
 */
package fortran77.parser;

import junit.framework.TestCase;
import java.io.*;
import java.util.regex.*;
import java.text.*;
import java.util.*;

import antlr.*;


/**
 * @author oli
 */
public class LexerTest extends TestCase
{
    File[] files;
    File[] output;
    FileWriter out;
    
    private class Filter implements FileFilter
    {
        public boolean accept(File f)
        {
            Pattern p = Pattern.compile(".f$");
            Matcher m = p.matcher(f.getName());
            return m.find();
        }
    }
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        FileFilter filter = new Filter();
        File dir = new File("/home/oli/repos/masters/src/LAPACK/SRC/");
        files = dir.listFiles(filter);
        
        Date d = new Date();
        SimpleDateFormat format = new SimpleDateFormat("_yyMMdd-HHmmss");
        output = new File[files.length];
        for(int i=0; i<files.length; i++)
            output[i] = new File("output/lexer_"+ files[i].getName() + format.format(d));
    }

    public void testNextToken()
    {
        int i = 0;
        Fortran77Lexer lexer = null;
        
        while (i < files.length)
        {
            try
            {
                System.out.println("-------------------------------------");
	            System.out.println(files[i]);
	            
	            out = new FileWriter(output[i]);
	            FileInputStream input = new FileInputStream(files[i]);
	            lexer = new Fortran77Lexer(input);
	            
	            Token t;
	            while ((t = lexer.nextToken()).getText() != null)
	                out.write(
	                        t.getText() +
	                        "\t\tType:  " + t.getType() +
	                        "\tLine:  " + t.getLine() +
	                        "\tColumn: " + t.getColumn() + "\n"
	                        );
	            input.close();
	            out.close();
            }
            catch (FileNotFoundException fnfe) {
                System.err.println("Fortran77 parser: Could not read file "
                        + files[i] + ":" + fnfe.getMessage());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (TokenStreamException tse) {
                System.err.println("Fortran77 lexer: exception in "
                        + files[i]);
                tse.printStackTrace();
            }
            i++;
        }
    }
}
