package treeutils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import fortran77.parser.Fortran77Lexer;
import fortran77.parser.Fortran77Parser;
import graph.CFGCreationException;
import treeutils.data.CodeRoot;
import treeutils.data.EAST;
import treeutils.data.SubprogramAST;
import transform.BadLoopException;
import transform.F77RepairAST;
import util.ResultRecorder;
import recon.PatternMatchException;
import recon.PatternMatcher;
import antlr.collections.AST;
import antlr.BaseAST;
import antlr.RecognitionException;
import antlr.TokenStreamException;

/**
 * This module hides the steps required, and their execution order, to
 * reverse engineer Fortran source code.
 * 
 * @author Olivier Dragon <dragonoe@mcmaster.ca>
 */
public class ReverseEngineer
{
    PatternMatcher m;
    SortedMap<File,EAST> jobs;
    EnhancedASTFactory EASTfactory;
    Map<String,SubprogramAST> externalSymbols;
    
    
    public ReverseEngineer()
    {
        m = new PatternMatcher();
        EASTfactory = new EnhancedASTFactory();
        externalSymbols = new HashMap<String,SubprogramAST>();
    }
    
    
    public void start(SortedMap<File, EAST> input)
    {
        jobs = input;
        for (Map.Entry<File,EAST> job : jobs.entrySet())
        {
            if (job.getValue() == null)
                process(job);
        }
    }
    
    
    private File requestFileFromUser(String routineName)
    {
        //  prompt the user to enter a filename where routineName can be found
        System.out.print("\nCould not find routine " + routineName
                + ". Please enter the location of its file (full path): ");

        BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in));

        EAST tree = null;
        File file = null;
        String filename = null;

        // try again until user enters a correct file
        while (true)
        {
            try
            {
                filename = br.readLine();
                file = new File(filename);
                
                // verify input given is correct.
                tree = createSyntaxTreeFromSource(file);
                for (EAST node : tree.children())
                    if (routineName.equals(node.getFirstChild().getText()))
                        return file;
                
                System.out.println(
                        "The file specified does not contain the subprogram "
                        + routineName + ".");
            }
              catch (IOException ioe) {
                System.out.println("Could not open file " + filename + ": "
                        + ioe.getMessage());
            } catch (RecognitionException re) {
                System.out.println("An error occured parsing the input: "
                        + re.getMessage());
            } catch (TokenStreamException tse) {
                System.out.println("An error occured lexing the input: "
                        + tse.getMessage());
            } catch (BadLoopException ble) {
                System.out.println("An error occured: " + ble.getMessage());
            } finally {
                System.out.println(
                        "Please enter the location of the file containing "
                        + routineName + " (full path): ");
            }
        }
    }

    
    public void process(String routineName)
    {
        String file;
        for (Map.Entry<File, EAST> job : jobs.entrySet())
        {
            file = removeFileExtension(job.getKey().getName());
            if (file.equalsIgnoreCase(routineName))
            {
                process(job);
                return; // we're done here
            }
        }

        // The routine was not found based on filenames initially supplied by
        // the user. We must prompt the user to enter the file.
        File userFile = requestFileFromUser(routineName);
        jobs.put(userFile, process(userFile));
    }

    
    private void process(Map.Entry<File, EAST> job)
    {
        job.setValue(process(job.getKey()));
    }
    
    private CodeRoot process(File file)
    {
        CodeRoot tree = null;
        
        try
        {
            // If an exception occurs, just skip the current file and keep
            // going with the next
            System.out.print("Processing " + file + "...");
            
            ResultRecorder.setCurrentFilename(
                    removeFileExtension(file.getName()));
            
            tree = createSyntaxTreeFromSource(file);
            
            analyseAndTransform(tree);

            ResultRecorder.saveAST(tree);
            ResultRecorder.toFortran(tree);
            ResultRecorder.toLatex(tree);
            System.out.println(" done.");
        }
          catch (RecognitionException e) {
            ResultRecorder.recordError("Fortran77 parser: " + e.toString());
            System.out.println(" error!");
        } catch (TokenStreamException e) {
            System.out.println(" error!");
            ResultRecorder.recordError("Fortran77 lexer: " + e.toString());
        } catch (BadLoopException e) {
            System.out.println(" error!");
            ResultRecorder.recordError("BadLoop: " + e.toString());
        } catch (PatternMatchException e) {
            System.out.println(" error!");
            ResultRecorder.recordError("PatternMatcher: " + e.toString());
        } catch (CFGCreationException e) {
            System.out.println(" error!");
            ResultRecorder.recordError("CFGraphGenerator: " + e.toString());
        } catch (FileNotFoundException fnfe) {
            System.out.println(" error!");
            fnfe.printStackTrace(System.out);
        } catch (NullPointerException npe) {
            System.out.println(" error!");
            npe.printStackTrace(System.out);
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
            System.exit(-1);
        }
        
        return tree;
    }
    
    
    private CodeRoot createSyntaxTreeFromSource(File file)
        throws IOException, RecognitionException,TokenStreamException,
            BadLoopException
    {
        Fortran77Lexer lexer = new Fortran77Lexer
            (new BufferedInputStream(new FileInputStream(file)));
        Fortran77Parser parser = new Fortran77Parser(lexer);
        
        // Specify custom AST node for ANTLR
        parser.setASTNodeClass("fortran77.parser.TokenAST");

        // set extra verbose AST node printing
        BaseAST.setVerboseStringConversion(true, parser.getTokenNames());
        
        // perform parsing
        parser.program();
        AST resultTree = parser.getAST();
        
        // modify the AST created by ANTLR for our purposes
        F77RepairAST.repair(resultTree);
        CodeRoot tree = EASTfactory.buildEnhancedAST(resultTree);
        
        tree.setExternals(externalSymbols);
        
        return tree;
    }
    
    
    private void analyseAndTransform(CodeRoot tree)
        throws PatternMatchException, CFGCreationException
    {
        try
        {
            m.mergeFortranSubprogramTypes(tree);
            m.abstractFortran(tree);
            for (int i=0; i<4; i++)
            {
                ResultRecorder.saveAST(tree);
                m.abstractFortran2(tree);
            }
        }
          catch (RecognitionException e) {
            throw new PatternMatchException(e);
        }
    }
    
    
    private String removeFileExtension(String filename)
    {
        return filename.replaceAll("\\.[fF]([oO][rR])?$", "");
    }
}
