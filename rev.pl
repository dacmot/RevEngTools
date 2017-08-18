#!/usr/bin/perl -w

use strict;

my $classpath = "$ENV{CLASSPATH}:antlr-2.7.5.jar:stringtemplate.jar:jung-1.7.1.jar:commons-collections-3.1.jar:colt.jar:$ENV{MAPLE}/java/jopenmaple.jar:$ENV{MAPLE}/java/externalcall.jar";

unless (@ARGV) {
	@ARGV = ('../../src/LAPACK/BLAS/SRC/caxpy.f');}

system($ENV{JAVARUN}, "-cp", $classpath, "-Djava.library.path=$ENV{MAPLE}/bin.IBM_INTEL_LINUX", "Main", @ARGV); 
