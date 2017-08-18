import java.io.File;
import java.io.FileFilter;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import treeutils.ReverseEngineer;
import treeutils.data.EAST;
import util.ResultRecorder;
import util.ResultRecorder.Statistics;

/**
 * This module hides how the program begins its execution, and how the user
 * input from the command line is used to determine the input Fortran files
 * to be reverse engineered.
 * 
 * @author Olivier Dragon <dragonoe@mcmaster.ca>
 */
public class Main
{
    public static void main(String[] args)
    {
        long starttime = System.currentTimeMillis();
        
        ReverseEngineer eng = new ReverseEngineer();
        SortedMap<File, EAST> files = new TreeMap<File,EAST>();
        
        for (String arg : args)
            getFiles(arg, files);

        if (files.isEmpty())
            System.out.println(
                    "You must specify at least one file to be parsed.");
        else
            eng.start(files);
        
        printStatistics(starttime, files.size());
    }

    private static void printStatistics(long starttime, int filesCount)
    {
        // Statistics for fun
        double time = (System.currentTimeMillis() - starttime) / 1000;
        System.out.println("\n" + filesCount + " files processed.");

        for (Map.Entry<Statistics,Integer> e
                : ResultRecorder.getStats().entrySet())
            System.out.println(e.getKey() + ": " + e.getValue());
        
        System.out.println("Execution took " + time + " sec, on average "
                + (time / filesCount) + " sec per file.");
        System.out.println("System used "
                + (Runtime.getRuntime().totalMemory() / 1024)
                + "KB of memory.");
    }
    
    /**
     * Obtain a sorted collection of files from an input specified by the user.
     * If <code>filename</code> is a file, then the collection only contains
     * that file. If it is a directory then all the fortran files in that
     * directory are 
     *  
     * @param filename
     * @return
     */
    private static void getFiles(String filename,
            Map<File, EAST> fileSet)
    {
        File f = new File(filename);
        
        if (f.exists())
        {
            if (f.isDirectory())
            {
                for (File file : f.listFiles(new Filter()))
                    fileSet.put(file, null);
            }
            else if (f.isFile())
                fileSet.put(f, null);
        }
    }
    
    private static class Filter implements FileFilter
    {
        public boolean accept(File f)
        {
            return f.getName().endsWith(".f") ||
                f.getName().endsWith(".F") ||
                f.getName().endsWith(".for") ||
                f.getName().endsWith(".FOR");
        }
    }
}
