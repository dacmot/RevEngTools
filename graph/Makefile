CLASSPATH+=:/usr/share/java/junit.jar:../antlr-2.7.5.jar:../JavaComplex.jar:../stringtemplate.jar:../colt.jar:../commons-collections-3.1.jar:../jung-1.7.1.jar:../epsgraphics.jar:../:/usr/local/maple10/java/jopenmaple.jar:/usr/local/maple10/java/externalcall.jar

GRAMMAR=cfg.g
ALLOBJECTS=ControlFlowGraphGeneratorTokenTypes.class ControlFlowGraphGenerator.class CFGraphDotFile.class BasicCodeBlock.class ControlFlowGraph.class


all: $(ALLOBJECTS)

ControlFlowGraphGenerator.java ControlFlowGraphGeneratorTokenTypes.java: $(GRAMMAR)
	$(JAVARUN) antlr.Tool $(GRAMMAR)
	touch ControlFlowGraphGenerator.java
	touch ControlFlowGraphGeneratorTokenTypes.java

BasicCodeBlock.class: BasicCodeBlock.java
	$(JAVAC) BasicCodeBlock.java

ControlFlowGraphGenerator.class: ControlFlowGraphGenerator.java
	$(JAVAC) ControlFlowGraphGenerator.java
ControlFlowGraphGeneratorTokenTypes.class: ControlFlowGraphGeneratorTokenTypes.java
	$(JAVAC) ControlFlowGraphGeneratorTokenTypes.java

CFGraphDotFile.class: CFGraphDotFile.java
	$(JAVAC) CFGraphDotFile.java

ControlFlowGraph.class: ControlFlowGraph.java
	$(JAVAC) ControlFlowGraph.java
