CLASSPATH+=:/usr/share/java/junit.jar:../antlr-2.7.5.jar:../JavaComplex.jar:../stringtemplate.jar:../colt.jar:../commons-collections-3.1.jar:../jung-1.7.1.jar:../epsgraphics.jar:../:/usr/local/maple10/java/jopenmaple.jar:/usr/local/maple10/java/externalcall.jar

ALLOBJECTS = Intrinsic.class Expression.class

SUBDIRS = parser printer

all:
	@for i in $(SUBDIRS); do \
	(cd $$i; $(MAKE) all); done
	make $(ALLOBJECTS)

Intrinsic.class: Intrinsic.java
	$(JAVAC) Intrinsic.java
Expression.class: Expression.java
	$(JAVAC) Expression.java
