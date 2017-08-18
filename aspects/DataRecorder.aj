package aspects;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import treeutils.data.CodeRoot;
import treeutils.ReverseEngineer;


public abstract aspect DataRecorder
{
    private static final String forExtRegex = "(\\.[fF]([oO][rR])?)$";
    protected static final String outputDir = "output/";
    

    protected String currentFilename;
    protected String forExt;
    
    
    

    public pointcut processFile(File file) :
        execution(private CodeRoot ReverseEngineer.process(File*))
        && args(file);
    
    before(File file) : processFile(file)
    {
        currentFilename = removeFileExtension(file.getName());
        forExt = getFileExtension(file.getName());
    }


    private String removeFileExtension(String filename)
    {
        return filename.replaceAll(forExtRegex, "");
    }
    
    private String getFileExtension(String filename)
    {
        Matcher m = Pattern.compile(forExtRegex).matcher(filename);
        return m.group(1);
    }
}
