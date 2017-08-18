CLASSPATH+=:/usr/share/java/junit.jar:../antlr-2.7.5.jar:../JavaComplex.jar:../stringtemplate.jar:../colt.jar:../commons-collections-3.1.jar:../jung-1.7.1.jar:../epsgraphics.jar:../:/usr/local/maple10/java/jopenmaple.jar:/usr/local/maple10/java/externalcall.jar

ALLOBJECTS=ReverseEngineer.class EnhancedASTFactory.class


all: $(ALLOBJECTS)
	@cd data; $(MAKE) all

ReverseEngineer.class: ReverseEngineer.java
	$(JAVAC) ReverseEngineer.java
EnhancedASTFactory.class: EnhancedASTFactory.java
	$(JAVAC) EnhancedASTFactory.java
