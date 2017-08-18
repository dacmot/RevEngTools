package aspects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;

import antlr.RecognitionException;

import edu.uci.ics.jung.graph.Graph;
//import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
//import edu.uci.ics.jung.graph.impl.SparseTree;
import fortran77.printer.CodePrinter;
//import graph.ASTVertex;
import graph.CFGraphDotFile;
import treeutils.data.EAST;
import treeutils.data.CodeRoot;
import treeutils.ReverseEngineer;

public aspect Logger extends DataRecorder
{
    private static final String symExt = "_sym.log";
    private static final File errlog = new File(outputDir + "err.log");
    
    
    public pointcut traceFileProcessing() ;
    

    
    private void logSymbolicEngineResults(String message)
    {
        try
        {
            File f = new File(outputDir + currentFilename + symExt);
            FileWriter symLogger = new FileWriter(f,true);
            symLogger.append(message + "\n++++++++++++++++++++++++++++++++\n");
            symLogger.close();
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
    }
    
    private void recordError(String err)
    {        
        try {
            FileWriter errWriter = new FileWriter(errlog, false); //append
            errWriter.append(currentFilename + forExt + ": \t" + err +"\n");
            errWriter.close();
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
    }
}
