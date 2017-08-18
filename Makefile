CLASSPATH+=:/usr/share/java/junit.jar:antlr-2.7.5.jar:JavaComplex.jar:stringtemplate.jar:colt.jar:commons-collections-3.1.jar:jung-1.7.1.jar:epsgraphics.jar:.:/usr/local/maple10/java/jopenmaple.jar:/usr/local/maple10/java/externalcall.jar

TESTJAVA=$(ANTLRJAVA) junit.textui.TestRunner

SUBDIRS = fortran77 recurrence graph recon transform treeutils util


all:
	@for i in $(SUBDIRS); do \
	(cd $$i; $(MAKE) all); done
	@$(MAKE) Main.class

Main.class: Main.java
	$(JAVAC) Main.java

ps:
	@for i in output/*.dot; do \
	dot -Tps $$i -o `echo "$$i" | sed s/[.]dot//`.ps; done

png:
	@for i in output/*.dot; do \
	dot -Tpng $$i -o `echo "$$i" | sed s/.dot//`.png; done

test: lextest parsetest

lextest:
	$(TESTJAVA) parser.f77.LexerTest

parsetest:
	$(TESTJAVA) parser.f77.ParserTest

clean: cleanout
	rm -f `find . -name "*.class"`
	rm -f `find . -name "Fortran77*"`
	rm -f `find . -name "PatternMatcher*"`
	rm -f `find . -name "CodePrinter*"`
	rm -f `find . -name "ControlFlowGraphGenerator*"`
	rm -f `find . -name "MapleLexer*"`
	rm -f `find . -name "MapleParser*"`
	rm -f `find . -name "MapleTree*"`

cleanout:
	rm -f output/*
