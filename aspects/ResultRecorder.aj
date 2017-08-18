package aspects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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

public aspect ResultRecorder extends DataRecorder
{
    private static final String astExt = "_ast";
    private static final String cfgExt = "_cfg";
    private static final String txtExt = ".txt";
    private static final String dotExt = ".dot";
    
    
        
    public pointcut recordRevengResults(CodeRoot tree) :
        call(private void ReverseEngineer.analyseAndTransform(CodeRoot))
        && args(tree);
    
    after(CodeRoot tree) : recordRevengResults(tree)
    {
        saveAST(tree);
        toFortran(tree);
    }
    


    public void saveAST(EAST tree)
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
    

    private void saveCFG(Graph g, String routineName)
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
    
    
    private void toFortran(EAST tree)
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
}
