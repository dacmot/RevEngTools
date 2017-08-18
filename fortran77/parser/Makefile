CLASSPATH+=:/usr/share/java/junit.jar:../../antlr-2.7.5.jar:../../JavaComplex.jar:../../stringtemplate.jar:../../colt.jar:../../commons-collections-3.1.jar:../../jung-1.7.1.jar:../../epsgraphics.jar:../../:/usr/local/maple10/java/jopenmaple.jar:/usr/local/maple10/java/externalcall.jar

GRAMMAR=f77-pccts-antlr.g
ALLOBJECTS = Fortran77TokenTypes.class Fortran77Parser.class Fortran77Lexer.class LexerTest.class ParserTest.class


all: $(ALLOBJECTS)
	cp Fortran77TokenTypes.txt ../printer/
	cp Fortran77TokenTypes.txt ../../recon/
	cp Fortran77TokenTypes.txt ../../graph/
	cp Fortran77TokenTypes.txt ../../recurrence/


Fortran77Parser.java Fortran77Lexer.java Fortran77TokenTypes.java: $(GRAMMAR)
	$(JAVARUN) antlr.Tool $(GRAMMAR)
	touch Fortran77Parser.java
	touch Fortran77Lexer.java
	touch Fortran77TokenTypes.java

Fortran77Parser.class: Fortran77Parser.java
	$(JAVAC) Fortran77Parser.java
Fortran77Lexer.class: Fortran77Lexer.java
	$(JAVAC) Fortran77Lexer.java
Fortran77TokenTypes.class: Fortran77TokenTypes.java
	$(JAVAC) Fortran77TokenTypes.java
LexerTest.class: LexerTest.java
	$(JAVAC) LexerTest.java
ParserTest.class: ParserTest.java
	$(JAVAC) ParserTest.java
