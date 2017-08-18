/*
 * Created on 17-Nov-2005
 */
package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;

import antlr.RecognitionException;

import edu.uci.ics.jung.graph.Graph;
//import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
//import edu.uci.ics.jung.graph.impl.SparseTree;
import fortran77.parser.Fortran77TokenTypes;
import fortran77.printer.CodePrinter;
//import graph.ASTVertex;
import graph.CFGraphDotFile;
import treeutils.data.EAST;

public class ResultRecorder implements Fortran77TokenTypes
{
    private static final String outputDir = "output/";
    private static final String astExt = "_ast";
    private static final String cfgExt = "_cfg";
    private static final String symExt = "_sym.log";
    private static final String txtExt = ".txt";
    private static final String dotExt = ".dot";
//    private static final String epsExt = ".eps";
    private static final String forExt = ".f";
    private static final String texExt = ".tex";
    
    private static String currentFilename = "";
    
    private static File errlog = new File(outputDir + "err.log");
    
    private static boolean isNewLog = true;
    

    
    private static Map<Statistics,Integer> stats =
        new EnumMap<Statistics,Integer>(Statistics.class);
    public enum Statistics
    {
        SUBPROGS,
        TRANSFORMS,
        LOOPS,
        DOTPRODS,
        ARRAYASSIGN,
        INITIALISATION,
        PARALLEL,
        ARITHMETICIF
    }
    
    
    
    
    public static void setCurrentFilename(String filename)
    {
        currentFilename = filename;
    }
    
    public static void saveAST(EAST tree)
    {
//        String dot = outputDir + currentFilename + astExt + dotExt;
//        GraphDotFile gdf = new GraphDotFile();
//        gdf.save(createASTGraph(tree), dot);
        
        try
        {
            File f = new File(outputDir + currentFilename + astExt + txtExt);
            FileWriter writer = new FileWriter(f);
            writer.write(tree.toStringTree());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /*
    private static Graph createASTGraph(EAST tree)
    {
        ASTVertex root = new ASTVertex(tree);
        Graph g = new SparseTree(root);
        
        treeWalk(g, tree.getFirstChild(), root);
        
        return g;
    }
    
    private static void treeWalk(Graph g, EAST tree, ASTVertex parent)
    {
        if (tree != null)
        {
            if (tree.getType() == COMMENT)
                treeWalk(g, tree.getNextSibling(), parent);
            else
            {
                ASTVertex v = new ASTVertex(tree);
                g.addVertex(v);
                g.addEdge(new DirectedSparseEdge(parent, v));
                treeWalk(g, tree.getFirstChild(), v);
                treeWalk(g, tree.getNextSibling(), parent);
            }
        }
    } //*/
    
    public static void saveCFG(Graph g, String routineName)
    {
        StringBuilder dot = new StringBuilder();
        dot.append(outputDir);
        dot.append(currentFilename);
        dot.append("-");
        dot.append(routineName);
        dot.append(cfgExt);
        dot.append(dotExt);
        
        CFGraphDotFile gdf = new CFGraphDotFile();
        gdf.save(g, dot.toString());
    }
    
    public static void toFortran(EAST tree)
    {
        try
        {
            CodePrinter printer = new CodePrinter();
            File tmpl = new File("fortran77/printer/f77-code_template.stg");
            printer.setTemplates(new StringTemplateGroup(new FileReader(tmpl),
                    AngleBracketTemplateLexer.class));
            StringTemplate code = printer.program(tree);

            File f = new File(outputDir + currentFilename + forExt);
            FileWriter writer = new FileWriter(f);
            writer.write(code.toString());
            writer.close();
        }
          catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace(System.err);
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        } catch (RecognitionException re) {
            re.printStackTrace(System.err);
        }
    }
    
    public static void toLatex(EAST tree)
    {
        try
        {
            CodePrinter printer = new CodePrinter();
            File tmpl = new File(
                    "fortran77/printer/latex_fortran_m-code_template.stg");
            printer.setTemplates(new StringTemplateGroup(new FileReader(tmpl)));
            StringTemplate code = printer.program(tree);

            File f = new File(outputDir + currentFilename + texExt);
            FileWriter writer = new FileWriter(f);
            writer.write(code.toString());
            writer.close();
        }
          catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace(System.err);
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        } catch (RecognitionException re) {
            re.printStackTrace(System.err);
        }
    }
    
    public static void logSymbolicEngineResults(String message)
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
    
    public static void recordError(String err)
    {        
        try {
            FileWriter errWriter = new FileWriter(errlog, !isNewLog);
            errWriter.append(currentFilename + forExt + ": \t" + err +"\n");
            errWriter.close();
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        } finally {
            if (isNewLog)
                isNewLog = false;
        }
    }
    
    
    public static void recordStat(Statistics s)
    {
        Integer i = stats.get(s);
        if (i == null)
            i = 0;
        stats.put(s, i + 1);
    }
    public static Map<Statistics, Integer> getStats()
    {
        return stats;
    }
}
