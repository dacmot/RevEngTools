/*
 * Created on 17-Nov-2005
 */
package graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.GraphFile;
import graph.BasicCodeBlock.BlockType;
import graph.FlowEdge.FlowType;

public class CFGraphDotFile implements GraphFile
{    
    private static final String indent = "  ";
    private static final String edge = " -> ";
    private static final String eol = ";\n";
    
    private Map<BasicCodeBlock.BlockType, String> cfgShapes;
    private Map<FlowType, String> edgeStyles;
    
    public CFGraphDotFile()
    {
        cfgShapes = new EnumMap<BasicCodeBlock.BlockType, String>
                (BasicCodeBlock.BlockType.class);
        cfgShapes.put(BlockType.ENTRY, "invtriangle");
        cfgShapes.put(BlockType.BLOCK, "box");
        cfgShapes.put(BlockType.EXIT, "ellipse");
        cfgShapes.put(BlockType.PREHEADER, "circle");
        cfgShapes.put(BlockType.POSTBODY, "circle");
        cfgShapes.put(BlockType.POSTEXIT, "circle");
        
        edgeStyles = new EnumMap<FlowType, String>(FlowType.class);
        edgeStyles.put(FlowType.TREE, "bold");
        edgeStyles.put(FlowType.FORWARD, "solid");
        edgeStyles.put(FlowType.BACKWARD, "dashed");
    }
    
    public Graph load(String filename)
    {
        throw new UnsupportedOperationException();
    }
    
    public void save(Graph g, String filename)
    {
        try
        {
            File f = new File(filename);
            FileWriter w = new FileWriter(f);
            
            w.write(createDotFile(g));
            w.close();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    private  String createDotFile(Graph g)
    {
        FlowEdge e;
        BasicCodeBlock src, dest;
        Map<BasicCodeBlock,String> nameTable =
            new HashMap<BasicCodeBlock,String>();
        
        createNodeNames(g, nameTable);
        StringBuilder s = new StringBuilder();
        
        s.append("digraph G {\n");
        
        for (Iterator i = g.getEdges().iterator(); i.hasNext(); )
        {
            e = (FlowEdge) i.next();
            src = (BasicCodeBlock) e.getSource();
            dest = (BasicCodeBlock) e.getDest();
            
            s.append(indent);
            s.append(nameTable.get(src));
            s.append(edge);
            s.append(nameTable.get(dest));
            s.append(createEdgeAttributes(e));
            s.append(eol);
        }
        
        for (Iterator i = g.getVertices().iterator(); i.hasNext(); )
        {
            src = (BasicCodeBlock) i.next();
            
            s.append(indent);
            s.append(nameTable.get(src));
            s.append(createNodeAttributes(src));
            s.append(eol);
        }
        
        s.append("}\n");
        
        return s.toString();
    }
    
    private void createNodeNames(Graph g, Map<BasicCodeBlock,String> nameTable)
    {
        String name;
        Integer index;
        BasicCodeBlock b;
        Map<String,Integer> collisions = new HashMap<String,Integer>();
        
        for (Iterator i = g.getVertices().iterator(); i.hasNext(); )
        {
            b = (BasicCodeBlock) i.next();
            name = createNodeName(b.getLabel());
            if (collisions.containsKey(name))
            {
                index = collisions.get(name);
                index = new Integer(index.intValue() + 1);
            }
            else
                index = new Integer(0);
            collisions.put(name,index);
            
            name = name + index;
            nameTable.put(b, name);
        }
    }
    
    private String createNodeName(String name)
    {
        name = name.replaceAll("=", "ASSIGN");
        name = name.replaceAll("[\\\\][nlr]", "_");
        name = name.replaceAll("(\\W)+", "_");
        name = name.replaceAll("(\\s)+", "_");
        name = name.replaceAll("\\d+", "");
        
        // final cleanup
        name = name.replaceAll("(_)+", "_");
        name = name.replaceAll("^_", "");
        name = name.replaceAll("_$", "");
        
        if (name.length() > 20)
            return name.substring(0,19);
        else
            return name;
    }
    
    private String createNodeAttributes(BasicCodeBlock v)
    {
        StringBuilder s = new StringBuilder("[shape=");
        s.append(cfgShapes.get(v.getType()));
        s.append(",label=\"");
        s.append(v.getLabel());
        s.append("\"]");
        return s.toString();
    }
    
    private String createEdgeAttributes(FlowEdge e)
    {
        StringBuilder s = new StringBuilder("[style=");
        s.append(edgeStyles.get(e.getFlowType()));
        s.append(",label=\"");
        s.append(e.getLabel());
        s.append("\"]");
        return s.toString();
    }
}
