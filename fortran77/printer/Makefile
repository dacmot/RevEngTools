CLASSPATH+=:/usr/share/java/junit.jar:../../antlr-2.7.5.jar:../../JavaComplex.jar:../../stringtemplate.jar:../../colt.jar:../../commons-collections-3.1.jar:../../jung-1.7.1.jar:../../epsgraphics.jar:../../:/usr/local/maple10/java/jopenmaple.jar:/usr/local/maple10/java/externalcall.jar

GRAMMAR=f77-printer.g
ALLOBJECTS=CodePrinter.class CodePrinterTokenTypes.class


all: $(ALLOBJECTS)

CodePrinter.java CodePrinterTokenTypes.java: $(GRAMMAR)
	$(JAVARUN) antlr.Tool $(GRAMMAR)
	touch CodePrinter.java
	touch CodePrinterTokenTypes.java

CodePrinter.class: CodePrinter.java
	$(JAVAC) CodePrinter.java
CodePrinterTokenTypes.class: CodePrinterTokenTypes.java
	$(JAVAC) CodePrinterTokenTypes.java

