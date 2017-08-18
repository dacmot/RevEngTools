CLASSPATH+=:/usr/share/java/junit.jar:../antlr-2.7.5.jar:../JavaComplex.jar:../stringtemplate.jar:../colt.jar:../commons-collections-3.1.jar:../jung-1.7.1.jar:../epsgraphics.jar:../:/usr/local/maple10/java/jopenmaple.jar:/usr/local/maple10/java/externalcall.jar

GRAMMAR=maple.g
ALLOBJECTS=MapleTreeTokenTypes.class MapleParser.class MapleLexer.class CFGExtender.class ContextGenerator.class Context.class Maple.class


all: $(ALLOBJECTS)

MapleParser.java MapleLexer.java MapleTreeTokenTypes.java : $(GRAMMAR)
	$(JAVARUN) antlr.Tool $(GRAMMAR)
	touch MapleParser.java
	touch MapleLexer.java
	touch MapleTreeTokenTypes.java

MapleParser.class: MapleParser.java
	$(JAVAC) MapleParser.java
MapleLexer.class: MapleLexer.java
	$(JAVAC) MapleLexer.java
MapleTreeTokenTypes.class: MapleTreeTokenTypes.java
	$(JAVAC) MapleTreeTokenTypes.java
CFGExtender.class: CFGExtender.java
	$(JAVAC) CFGExtender.java
ContextGenerator.class: ContextGenerator.java
	$(JAVAC) ContextGenerator.java
Context.class: Context.java
	$(JAVAC) Context.java
Maple.class: Maple.java
	$(JAVAC) Maple.java
