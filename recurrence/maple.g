header {
package recurrence;

import treeutils.data.EAST;
}

class MapleParser extends Parser;
options {
    k = 1;
    importVocab=Fortran77;
    exportVocab=MapleTree;
    defaultErrorHandler = false;
    buildAST = true;
    codeGenMakeSwitchThreshold=999;
    codeGenBitsetTestThreshold=6;
}

parseExpr:
	STRINGDELIM! expr STRINGDELIM! ;

expr:
	// expressions
	PLUS^ expr (options {greedy=true;}: expr)+
	|
	MINUS^ expr expr
	|
	m:UNARYMINUS^ { #m.setType(MINUS); } expr
	|
	STAR^ expr (options {greedy=true;}: expr)+
	|
	POWER^ expr expr
	|
	DIV^ expr expr
	|
	RANGE^ expr expr
	|
	EQ^ expr expr
	|
	LOR^ expr expr
	|
	LAND^ expr expr
	|
	LNOT^ expr expr
	|
	LT^ expr expr
	|
	LE^ expr expr
	|
	GT^ expr expr
	|
	GE^ expr expr
	|
	{ LA(2) == FRACTION }? ICON FRACTION^ { ##.setType(DIV);} ICON
	|
	
	// functions or arrays
	sum        |
	piecewise  |
	gamma      |
	exp        |
	factorial  |
	overwrite  |
	dotproduct |
	vector     |
	intrinsics
	|
	n:NAME^ (OFTYPE! ("function"! {#n.setType(FUNCTION);} | "array"!)
		(options {greedy=true;}: expr)+ )?
	|
	
	// scalar symbols or constants
	complex | {LA(2)!=FRACTION}? ICON | RCON |
	// NAME | taken care of with functions or arrays
	FALSE | TRUE |
	
	LPAREN! expr RPAREN! |
	OPENTREE! expr CLOSETREE!
	;
	
sum :
	("sum"^ | "Sum"^) { ##.setType(SUM); }
		OFTYPE! "function"! expr range:expr ;
		
piecewise :
	"piecewise"^ { ##.setType(PIECEWISE); }
		OFTYPE! "function"! (options {warnWhenFollowAmbig=false;}: expr)+ ;

gamma :
	"GAMMA"^ { ##.setType(GAMMA); }
		OFTYPE! "function"! expr (options {warnWhenFollowAmbig=false;}: expr)? ;

exp :
	"exp"^ { ##.setType(EXP); }
		OFTYPE! "function"! expr ;
		
factorial :
	"factorial"^ { ##.setType(FACTORIAL); }
		OFTYPE! "function"! expr ;
		
overwrite :
	"overwrite"^ { ##.setType(OVERWRITE); ##.setText(".o/w."); }
		OFTYPE! "function"! expr expr ;

dotproduct :
	"dotproduct_"^ { ##.setType(DOTPROD); }
		OFTYPE! "function"! expr expr ;


dimx :
	OPENTREE! "dimx_"^ { ##.setType(DIMX); } OFTYPE! "function"! expr CLOSETREE!
	;
dimy :
	OPENTREE! "dimy_"^ { ##.setType(DIMY); } OFTYPE! "function"! expr CLOSETREE!
	;
index[AST n] :
	OPENTREE! "index_"! OFTYPE! "function"! e:expr! { n.addChild(#e); }
	CLOSETREE!
	;
proj :
	OPENTREE! "proj_"^ { ##.setType(PROJ); } OFTYPE! "function"!
		expr range:expr
	CLOSETREE!
	;
vector :
	v:"vector_"^ { #v.setType(VECTOR); } OFTYPE! "function"!
		n:NAME! { #v.setText(n.getText()); }
		(
			{LA(2)==LITERAL_dimx_}?   dimx ({LA(2)==LITERAL_dimy_}? dimy)?
			|
			{LA(2)==LITERAL_index_}?  index[#n] 
				({LA(2)==LITERAL_dimx_}? dimx | dimy)
				{ ((EAST)#v).insertFirstChild((EAST) #n); }
			|
			{LA(2)==LITERAL_proj_}?   proj
		)
		;
		


complex :
	c:"COMPLEXCONST"^ {#c.setType(CCON);} numeralconst numeralconst ;
numeralconst :
	(m:UNARYMINUS^ {#m.setType(MINUS);} )? (ICON | RCON) ;



intrinsics : (
	"trunc"^ { ##.setType(INTRINSIC); ##.setText("int"); }
		OFTYPE! "function"! expr
	|
	"max"^ { ##.setType(INTRINSIC); }
		OFTYPE! "function"! expr expr
	)
	;




class MapleLexer extends Lexer;
options {
    k=3;
    exportVocab=MapleTree;
    testLiterals=false;
    codeGenMakeSwitchThreshold=6;
    codeGenBitsetTestThreshold=6;
    charVocabulary = '\3'..'\377';
}

WS : (' ' | '\t' | '\n' | '\r' | '\b' | '\f')+
	{ $setType(Token.SKIP); } ;

// have Fortran-M equivalents
PLUS       : "`+`"   ;
MINUS      : "`-`"   ;
STAR       : "`*`"   ;
POWER      : "`^`"   ;
DIV        : "`/`"   ;
FRACION    : "/"     ;
UNARYMINUS : '-'     ;
RANGE      : "`..`"  ;
EQ         : "`=`"   ; // this is equality, not assignment
LOR        : "`or`"  ;
LAND       : "`and`" ;
LNOT       : "`not`" ;
LT         : "`<`"   ;
LE         : "`<=`"  ;
GT         : "`>`"   ;
GE         : "`>=`"  ;

// specific to this Maple output, no fortran equivalent
LPAREN      : '('    ;
RPAREN      : ')'    ;
OPENTREE    : "#{"   ;
CLOSETREE   : "#}"   ;
OFTYPE      : "::"   ;
STRINGDELIM : '"'    ;

// numerical expressions
ICON : ('0'..'9')+
	(options {greedy=true;} :
		'.' ('0'..'9')* { $setType(RCON); } (EXPON)?
	)?
	;
	
RCON : '.' ('0'..'9')+ (EXPON)? ;

NAME options {testLiterals=true;} :
	('a'..'z' | 'A'..'Z' ) (
		('_' | 'a'..'z' | 'A'..'Z' | '0'..'9')*
	)
	;

protected EXPON: 'e' ('+' | '-')? ('0'..'9')+;