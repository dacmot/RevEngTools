header {
package recon;

import treeutils.data.EAST;
import treeutils.data.CodeRoot;
import treeutils.data.SubprogramAST;
import transform.Transformer;
import graph.CFGCreationException;
import util.ResultRecorder;
import util.ResultRecorder.Statistics;

import java.util.List;
import java.util.ArrayList;
}

class PatternMatcher extends TreeParser;
options {
    defaultErrorHandler = false; // required to detect no matching pattern
	importVocab=Fortran77;
    codeGenMakeSwitchThreshold=6;
    codeGenBitsetTestThreshold=6;
}

{
	Transformer t = new Transformer();
}


/*
 * End of headers
 * Start of patterns
 */ 

subprogramName [SubprogramAST newsub] :
	n:NAME { newsub.setName(n.getText()); } ;
subprogramParameters [SubprogramAST newsub] :
	( param:NAME {t.addSubprogramParam(param, newsub);} )* ;
subprogramBlock [EAST oldsub, SubprogramAST newsub] :
	bl:SUBPROGRAMBLOCK { t.subprogram(oldsub, bl, newsub); } ;
transformSubprogram
{
	EAST subprogram = null;
	SubprogramAST s = null;
	CodeRoot root = null;
} :
		(COMMENT)*
		{
			subprogram = (EAST) _t;
			s = new SubprogramAST();
			root = (CodeRoot) subprogram.getParent();
		}
		(
			#("subroutine" subprogramName[s] subprogramParameters[s]
				subprogramBlock[subprogram, s])
				{ root.addExternal(s); }
			|
			#("function" ( type:specTypes
								{ t.addFunctionOutputParam(_t, type, s); }
								|
								{ t.addFunctionOutputParam(_t, s); } )
				subprogramName[s]
				subprogramParameters[s]
				subprogramBlock[subprogram, s])
				{ root.addExternal(s); }
			|
			#("program" ( subprogramName[s] )? subprogramBlock[subprogram, s])
		)
	;

mergeFortranSubprogramTypes :
	#(CODEROOT (transformSubprogram)+ ) ;



// "double" is double complex, "precision" is double precision
specTypes : "real" | "complex" | "double" | "precision" | "integer" | "logical" |
	#("character" (STAR)?) ;
specChildren : (STAR)? (NAME)+ ;
specPattern
	{ EAST spec = (EAST) _t; } :
	(
		#("real" specChildren) |
		#("complex" specChildren) |
		#("double" specChildren) |
		#("precision" specChildren) |
		#("integer" specChildren) |
		#("logical" specChildren) |
		#("character" specChildren)
	)
	{ t.specification(spec); }
	;

intrinsicPattern :
	#(i:"intrinsic" (NAME)+)
	{ t.intrinsic(i); }
	;
	
externalPattern :
	#(e:"external" (NAME)+)
	{ t.external(e); }
	;

parameterPattern :
	#(p:"parameter" (#(ASSIGN NAME expr:.))+ )
	{ t.parameter(p); }
	;
	
dimensionPattern :
	#(d:"dimension" (NAME)+)
	{ t.dimension(d); }
	;

commonPattern :
	#(c:"common" (
		
		(DIV ( block:NAME DIV | DIV )
			( v1:NAME {t.commonVariable(block, v1);} )+
		)+
		|
		( v2:NAME {t.commonVariable(null, v2);} )+
		
	) )
	{ t.common(c); }
	;

equivalencePattern :
	"equivalence"
	;
	
formatPattern :
	#(f:"format" l:LABEL)  { t.format(l,f); }
	;
	
entryPattern :
	"entry"
	;

dataPattern :
	#(dat:"data" (dataEntity[dat])+) ;
dataEntity [AST dat] { List<AST> items, values; } :
	items=dataItems values=dataValues { t.dataStatement(dat, items, values); } ;
dataItems returns [List<AST> items = null] :
	#(d:DIV { items = new ArrayList<AST>(d.getNumberOfChildren()); }
		( dataItem[items] )+ ) ;
dataValues returns [List<AST> values = null] :
	#(d:DIV { values = new ArrayList<AST>(d.getNumberOfChildren()); }
		( dataValue[values] )+ ) ;
dataItem [List<AST> items] : 
	it:varRef { items.add(it); }  ;
dataValue [List<AST> values] : 
	val:expr { values.add(val); } ;





	
implicitRange : NAME | #(MINUS NAME NAME) ;
implicitPattern :
	#(im:"implicit"
		("none" | (type:specTypes { _t = type.getFirstChild(); } 
			(range:implicitRange {t.implicitStatement(type, range);} )+
		)+ )
	)
	{ t.implicitStatement(im); }
	;
	
statementFunctionPattern :
	#(ASSIGN #(sf:NAME (NAME)+) expr:.)
	{ t.statementFunction(sf, expr); }
	;

abstractFortran throws CFGCreationException :
	#(CODEROOT (processSubprogram)+ ) ;
	
processSubprogram throws CFGCreationException :
	(COMMENT)*
	#(s:SUBPROGRAM { t.setSubprogram(s); }
		{ ResultRecorder.recordStat(Statistics.SUBPROGS); }
		
		// Implicit statements
		( options { greedy=true; } :
			COMMENT |
			formatPattern |
			entryPattern |
			parameterPattern |
			implicitPattern
		)*
	
		// Other specification statements
		( options { greedy=true; } :
			COMMENT |
			formatPattern |
			entryPattern |
			parameterPattern |
			intrinsicPattern |
			externalPattern |
			dimensionPattern |
			commonPattern |
			specPattern				
		)*
	
		// Statement function statements
		( options { greedy=true; } :
			COMMENT |
			formatPattern |
			entryPattern |
			dataPattern |
			(#(ASSIGN #(NAME NAME)))=>statementFunctionPattern	
		)*
		
		// All these should really be tree grammar based, except the control
		// flow analysis which already is. If I have time I will look into
		// converting them
		{
    	 	t.analyseSymbolPurpose();
    		t.analyseInputOutput();
		}
	)
	;


abstractFortran2 throws CFGCreationException :
	#(CODEROOT (processSubprogram2)+ ) ;
	
processSubprogram2 throws CFGCreationException :
	(COMMENT)*
	#(s:SUBPROGRAM { t.setSubprogram(s); }
		executableStatementPatterns
		{
			ResultRecorder.toLatex(((EAST)s).getParent());
    		t.reduceParallelStatements();
			t.controlFlowAnalysis();
			ResultRecorder.toLatex(((EAST)s).getParent());
		}
	)
	;

executableStatementPatterns :
	(options {greedy=true;}: comment)*
	((initialisation)=> initialisations | )
	cdblk1
	;

/*
 * Executable statements
 *
 * Here you can find the patterns and transformations performed on
 * executable statements.
 */

initialisations :
	((initialisation)=> initialisation initialisations |
		((COMMENT)+ initialisation)=>COMMENT initialisations |
		// end recursion
		) ;

initialisation {AST e=null;} :
	#(assign:ASSIGN label var:NAME
		{ e = _t; } (booleanConstant | arithmeticConstant | stringConstant))
	{ t.initialisation(assign, var, e); }
	;
	
arithmeticIf :
	#(aif:AIF label e:expr l1:LABELREF l2:LABELREF l3:LABELREF)
	{ t.arithmeticIf(aif, e, l1, l2, l3); }
	;
	
assignmentPattern :
	#(ASSIGN label varRef aexpr1)
	;
	
arrayAssignmentPattern :
	#(f:FORALL label NAME
		#(ASSIGN ar:varRef #(PIECEWISE
			e:aexpr1 #(EQ ix:NAME #(RANGE a:aexpr1 b:aexpr1))
			aexpr1)
		)
	)
	{ t.arrayAssignment(f, ar, e, ix, a, b); }
	;

sumPatterns :
	#(s:SUM (
		// dot product
		(#(STAR NAME NAME))=>
		#(STAR #(x:NAME expr) #(y:NAME expr))
		#(EQ i:NAME #(RANGE a:expr b:expr))
		{ t.dotProduct(s,x,y,i,a,b); }
		
		|
		.
		)
	)
	;

// Here is a generic grammar that will look only at statements. So if statements
// and do-loops, which body contains other statements, must be visited
// recursively.

subp1 : #(SUBPROGRAM cdblk1) | comment ;

cdblk1 : (stmt1)* ;

stmt1 : ~("do" | "if" | AIF | "format" | "data" | ASSIGN | PARALLEL | FORALL) |
	ifst1                  |
	dost1                  |
	formatPattern          |
	dataPattern            |
	arithmeticIf           |
	assignmentPattern      |
	arrayAssignmentPattern |
	paraBlk
	;

ifst1 :
 	#("if" label expr 
 		then1
 		(elseIf1)*
 		(else1)?
 	);
 		
then1 : #(THENBLOCK cdblk1) ;
elseIf1 : #(ELSEIF expr then1) ;
else1 : #(ELSEBLOCK cdblk1) ;

dost1 :
 	#("do" label LABELREF NAME expr expr (expr)?
 		#(DOBLOCK cdblk1))
 	;

paraBlk : #(PARALLEL cdblk1) ;


nAryAddition1 : aexpr1 (options {greedy=true;}: nAryAddition1 | ) ;
nAryMultiplication1 : aexpr1 (options {greedy=true;}: nAryMultiplication1 | ) ;
pieces1 : aexpr1 aexpr1 ;
aexpr1 :
	(#(PLUS . .))=> #(PLUS aexpr1 nAryAddition1)
	|
	(#(MINUS . .))=> #(MINUS aexpr1 aexpr1)
	|
	(#(STAR . .))=> #(STAR aexpr1 nAryMultiplication1)
	|
	#(DIV aexpr1 aexpr1)
	|
	#(PLUS aexpr1)
	|
	#(MINUS aexpr1)
	|
	STAR
	|
	#(POWER aexpr1 aexpr1)
	|
	#(PIECEWISE ( ((aexpr1 aexpr1)=> pieces1 | aexpr1 ) )+ )
	|
	#(GAMMA aexpr1 (aexpr1)? )
	|
	#(EXP aexpr1)
	|
	#(FACTORIAL aexpr1)
	|
	#(DOTPROD vector vector)
	|
	#(EVAL aexpr1 #(EQ NAME aexpr1))
	|
	arithmeticConstant |
	indirectValue
	|
	// arithmetic expression patterns
	sumPatterns
	;

/*
 * Here you can find general statement patterns that don't have any action
 * associated with them. They can be used to "skip" certain statements by
 * including them directly. Or they can be copy/pasted (with a change of name)
 * to include actions.
 */

unit : #(CODEROOT (subprogram)+) ;

subprogram : #(SUBPROGRAM codeblock) | comment ;

label : (LABEL | ) ;
 
codeblock : (statement)* ;
 
statement :
	comment      |
	parallel     |
	assignment   |
	ifStatement  |
	doStatement  |
	gotoStmt     |
	returnStmt   |
	continueStmt |
	callStmt     |
	equivalenceStmt
	;
 	
parallel : #(PARALLEL (assignment)+) ;
 	
comment : (options {greedy=true;}: COMMENT)+ ;
 
assignment : #(ASSIGN label varRef expr);
 
ifStatement :
 	#("if" label expr 
 		thenBlock
 		(elseIfBlock)*
 		(elseBlock)?
 	);
 		
thenBlock : #(THENBLOCK codeblock) ;
elseIfBlock : #(ELSEIF expr thenBlock) ;
elseBlock : #(ELSEBLOCK codeblock) ;
 
doStatement :
 	#("do" label LABELREF NAME expr expr (expr)?
 		#(DOBLOCK codeblock))
 	;
 	
gotoStmt : #("go" label LABELREF) ;
 
returnStmt : #("return" label) ;
 
continueStmt : #("continue" label) ;
 
callStmt : #("call" label externalFunction) ;
 
equivalenceStmt : "equivalence" ;
	
dataStatement :
	#("data" (dataStatementEntity)+) ;
dataStatementItem : 
	varRef | dataImpliedDo ;
dataStatementMultiple : 
	( #(STAR (ICON | NAME) (constant | NAME) )
		| (constant | NAME) ) ;
dataStatementEntity :
	dse1 dse2 ;
dse1:   #(DIV (dataStatementItem)+) ;
dse2:   #(DIV (dataStatementMultiple)+) ;

dataImpliedDo : 
	#(LPAREN dataImpliedDoList #(ASSIGN	NAME expr expr (expr)?)) ;
dataImpliedDoList :
	(dataImpliedDoListWhat)+ ;
dataImpliedDoListWhat : 
	(varRef | dataImpliedDo ) ;


varRef : ({_t.getNumberOfChildren()==0}? NAME | arrayref | vector) ;
arrayref : #(NAME (expr)+) ;
externalFunction : #(EXTERNAL (expr)*) ;
intrinsicFunction : #(INTRINSIC (expr)*) ;
statementFunction : #(STFUNC (expr)*) ;
function : #(FUNCTION (expr)*) ;
vector :
	(
		{ _t.getFirstChild().getType() == NAME }?
		#(VECTOR #(NAME aexpr) ( #(DIMX aexpr) | #(DIMY aexpr) ) )
	|	#(VECTOR ( #(DIMX aexpr) (#(DIMY aexpr))? |
			#(PROJ aexpr #(EQ NAME #(RANGE aexpr aexpr))) ))
	)
	;

expr : (
	(indirectValue)=> indirectValue |
	vexpr |
	sexpr |
	bexpr |
	aexpr )
	;

vexpr :
	#(COLON expr expr) ;

sexpr : 
	#(CONCATOP sexpr sexpr) | stringConstant | indirectValue ;

bexpr :
	#(NEQV bexpr bexpr)
	|
	#(EQV bexpr bexpr)
	|
	#(LOR bexpr bexpr)
	|
	#(LAND bexpr bexpr)
	|
	#(LNOT bexpr)
	|
	#(IMPLIES bexpr bexpr)
	|
	#(LT aexpr aexpr)
	|
	#(LE aexpr aexpr)
	|
	#(EQ aexpr aexpr)
	|
	#(NE aexpr aexpr)
	|
	#(GT aexpr aexpr)
	|
	#(GE aexpr aexpr)
	|
	booleanConstant |
	indirectValue
	;


nAryAddition : aexpr (options {greedy=true;}: nAryAddition | ) ;
nAryMultiplication : aexpr (options {greedy=true;}: nAryMultiplication | ) ;
pieces : aexpr aexpr ;
aexpr :
	(#(PLUS . .))=> #(PLUS aexpr nAryAddition)
	|
	(#(MINUS . .))=> #(MINUS aexpr aexpr)
	|
	(#(STAR . .))=> #(STAR aexpr nAryMultiplication)
	|
	#(DIV aexpr aexpr)
	|
	#(PLUS aexpr)
	|
	#(MINUS aexpr)
	|
	STAR
	|
	#(POWER aexpr aexpr)
	|
	#(SUM aexpr #(EQ NAME #(RANGE aexpr aexpr)))
	|
	#(PIECEWISE ( ((aexpr aexpr)=> pieces | aexpr ) )+ )
	|
	#(GAMMA aexpr (aexpr)? )
	|
	#(EXP aexpr)
	|
	#(FACTORIAL aexpr)
	|
	#(DOTPROD vector vector)
	|
	#(EVAL aexpr #(EQ NAME aexpr))
	|
	arithmeticConstant |
	indirectValue
	;

indirectValue :
	varRef |
	externalFunction |
	intrinsicFunction |
	statementFunction |
	function
	;

constant : booleanConstant | arithmeticConstant | stringConstant ;

booleanConstant :
	trueconst |
	falseconst
	;

integerConstant :
	ICON |
	zcon
	;

arithmeticConstant :
	integerConstant |
	RCON |
	ccon
	;

stringConstant :
	scon |
	hollerith
	;

hollerith : HOLLERITH ;
scon : SCON ;
ccon : #(CCON expr expr) ;
zcon : ZCON ;
trueconst : TRUE ;
falseconst : FALSE ;