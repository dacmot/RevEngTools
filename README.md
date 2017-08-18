# RevEngTools
Reverse engineering tools for scientific computation FORTRAN code

https://macsphere.mcmaster.ca/bitstream/11375/21347/1/Dragon_Olivier_E._2006Jul_Masters..pdf

Building:
=========

 1. Set the environment variables JAVAC and JAVARUN to be the path to your java 1.5 compiler and virtual machine respectively (e.g. JAVAC=/usr/bin/javac)
 2. Set the environment variable MAPLE to maple's root directory (e.g. MAPLE=/usr/local/maple10)
 3. Run the `make` command (The ANTLR jar library is in the repository and so all that is needed to build is to have the environment variables set)

Running the reverse engineering tool:
=====================================

You will need to have Perl installed. Then all you need to do is make the rev.pl script executable, and then run it with the command `./rev.pl`
