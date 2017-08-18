CLASSPATH+=:/usr/share/java/junit.jar:../antlr-2.7.5.jar:../JavaComplex.jar:../stringtemplate.jar:../colt.jar:../commons-collections-3.1.jar:../jung-1.7.1.jar:../epsgraphics.jar:../:/usr/local/maple10/java/jopenmaple.jar:/usr/local/maple10/java/externalcall.jar

GRAMMAR=patterns.g
ALLOBJECTS=PatternMatcherTokenTypes.class PatternMatcher.class PatternMatchException.class


all: $(ALLOBJECTS)

PatternMatcher.java PatternMatcherTokenTypes.java: $(GRAMMAR)
	$(JAVARUN) antlr.Tool $(GRAMMAR)
	touch PatternMatcher.java
	touch PatternMatcherTokenTypes.java

PatternMatcher.class: PatternMatcher.java
	$(JAVAC) PatternMatcher.java
PatternMatcherTokenTypes.class: PatternMatcherTokenTypes.java
	$(JAVAC) PatternMatcherTokenTypes.java
PatternMatchException.class: PatternMatchException.java
	$(JAVAC) PatternMatchException.java
