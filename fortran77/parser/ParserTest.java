/*
 * Created on 30-Jul-2005
 */
package fortran77.parser;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

import junit.framework.TestCase;

/**
 * @author oli
 */
public class ParserTest extends TestCase
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
        java.util.Arrays.sort(files);
                
        Date d = new Date();
        SimpleDateFormat format = new SimpleDateFormat("_yyMMdd-HHmmss");
        output = new File[files.length];
        for(int i=0; i<files.length; i++)
            output[i] = new File("output/parser_"+ files[i].getName() + format.format(d));
    }
    
    public void testExecutableUnit()
    {
        int i = 0;
        Fortran77Lexer lexer = null;
        Fortran77Parser parser = null;
        
        while (i < files.length)
        {
            try
            {
                System.out.println("-------------------------------------");
	            System.out.println(files[i]);
	            
	            out = new FileWriter(output[i]);
	            FileInputStream input = new FileInputStream(files[i]);
	            
	            lexer = new Fortran77Lexer(input);
	            parser = new Fortran77Parser(lexer);
	            
	            parser.executableUnit();
	            AST resultTree = parser.getAST();
	            out.write(resultTree.toStringList());
	            
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
                break; // isolate errors for now
            } catch (RecognitionException re) {
                System.err.println("Fortran77 parser: exception in "
                        + files[i]);
                re.printStackTrace();
                break; // isolate errors for now
            }
            
            i++;
        }
    }
}
