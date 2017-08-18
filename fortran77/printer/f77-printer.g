header {
package fortran77.printer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import treeutils.data.EAST;
import treeutils.data.Variable;
import treeutils.data.Variable.DataType;
import treeutils.data.Function;
import treeutils.data.StatementFunction;
import treeutils.data.SubprogramAST;
import treeutils.data.SubProgSymTable;
import util.ResultRecorder;
import org.antlr.stringtemplate.*;
}

class CodePrinter extends TreeParser;
options {
    defaultErrorHandler = true;
	importVocab=Fortran77;
    codeGenMakeSwitchThreshold=3;
    codeGenBitsetTestThreshold=6;
}

{
	private StringTemplate indentUnit;
	private StringTemplate nolabel;
	private StringTemplate commentLabel;
	private StringTemplate labelFormat;
	private StringTemplate continueLabel;
	private static final StringTemplate empty = new StringTemplate("");
	
	private StringTemplateGroup templates;
	private int level;
	private String indent[];
	
	private static final GreekLetterNames greekLetters = new GreekLetterNames();
	
	
	
	public void setTemplates(StringTemplateGroup t)
	{
		templates = t;
		indentUnit = template("indentUnit");
		nolabel = template("nolabel");
		commentLabel = template("commentLabel");
		labelFormat = template("labelFormat");
		continueLabel = template("continueLabel");
		
		level = -1; // fix extra indentation issue
		indent = new String[15];
		indent[0] = "";
	}
	
	
	private StringTemplate template(String name)
	{
    	return templates.getInstanceOf(name);
	}
	private StringTemplate template(Object o)
	{
		return template(o.toString());
	}
	
	
	private void indentUp()
	{
		level++;
		if (indent[level] == null)
			indent[level] = indent[level-1] + indentUnit;
	}
	
	private void indentDown()
	{
		level--;	
	}
	
	private String getIndentation()
	{
		return indent[level];	
	}
	
	
	private StringTemplate addVariableDeclarations(String attribute,
		StringTemplate tmpl, Collection<Variable> list)
		throws RecognitionException
	{
		StringTemplate spec = null;
		StringTemplate type = null;
		StringTemplate group = template(attribute);
		for (Variable var : list)
		{
			spec = template("typedef");
			spec.setAttribute("variable", var);
			spec.setAttribute("name", grVar(var.getName()));
			
			type = template(var.getType());
			if (var.getType() == DataType.CHARACTER)
				type.setAttribute("length", var.getLength());
			spec.setAttribute("type", type);
			
			if (!var.isScalar())
				for (EAST dim : var.getDimensions())
					spec.setAttribute("arraydimensions", expr(dim));
					
			group.setAttribute(attribute, spec);
		}
		if (! list.isEmpty() || attribute.equals("locals"))
			tmpl.setAttribute(attribute, group);
			
		return group;
	}
	
	
	private Collection<Variable> filterFunctionOutput(Collection<Variable> list,
		Variable fout)
	{
		Variable v;
		Iterator<Variable> i = list.iterator();
		while (i.hasNext())
		{
			v = i.next();
			if (v == fout)
				i.remove();
		}
		return list;
	}
	
	
	private StringTemplate encloseExpression(AST expression)
	{
		EAST e = (EAST) expression;
		
		int parentType;
		if (e.getParent() != null)
			parentType = e.getParent().getType();
		else
			return template("noparenthesis");
		
		switch (e.getType())
		{
			case PLUS:
				if (parentType == MINUS && e.getPreviousSibling() != null)
					return template("parenthesis");
					
			case MINUS:
			case SUM:
				if (parentType == STAR)
					return template("parenthesis");

			case STAR:
			case DIV:
			case POWER:
			case DOTPROD:
				if (parentType == FACTORIAL)
					return template("parenthesis");
				break;
				
				
			case LOR:
				if (parentType == LAND)
					return template("parenthesis");
			case LAND:
				if ( parentType == LNOT)
					return template("parenthesis");
				break;
		}
		
		// if this expression is to be powered, enclose it in parenthesis
		if (parentType == POWER)
		{
			if (e == e.getParent().getFirstChild())
				return template("parenthesis");	
		}
		
		return template("noparenthesis");
	}
	
	
	private StringTemplate gr(String name, StringTemplate t)
	{
		if (greekLetters.isGreekLetter(name))
			t = template(name);
		else
			t.setAttribute("name", name);
		return t;
	}
	private StringTemplate grVar(String name)
	{
		return gr(name, template("variable"));
	}
	private StringTemplate grFct(String name)
	{
		return gr(name, template("fct"));
	}
}


program returns [StringTemplate code = template("programUnits")]:
	#(CODEROOT (executableUnit[code])+)
	;

	
executableUnit[StringTemplate code]
{ StringTemplate u = null,c = null, s = null; } :
	( { s = template("statement"); } c = comment[s])?
	u = subprogram
	{
		if (s != null)
			s.setAttribute("statement", c);
		u.setAttribute("comment", s);
		code.setAttribute("units", u);
	}
	;

	
subprogram returns [StringTemplate code=null] { StringTemplate body=null; } :
	#(s:SUBPROGRAM 
	
	{	
		SubprogramAST subp = (SubprogramAST) s;
		SubProgSymTable syms = subp.getSymbolTable();
		
		StringTemplate init;
		if (subp.getText().equals("function"))
		{
			Variable fctOut = syms.getFunctionOutput();
			code = template("function");
			code.setAttribute("rettype", template(fctOut.getType()));
			if (fctOut.isInitialised())
			{
				init = template("eq");
				init.setAttribute("a", grVar(fctOut.getName()));
				init.setAttribute("b", fctOut.getInitValue().getText());
				code.setAttribute("retinit", init);
			}
			else
				code.setAttribute("retinit", grVar(subp.getName()));
		}
		else
			code = template(subp.getText());	
		
		// set the name of the subprogram, convert to greek letter if needed
		code.setAttribute("name", grFct(subp.getName()));
		
		// subprogram argument parameter
		StringTemplate p;
		for(Variable param : syms.getParameters())
		{ // this will only execute if there are parameters, so not for programs
			p = template("parameterdef");
			p.setAttribute("param", grVar(param.getName()));
			code.setAttribute("parameters", p);
		}
		
		body = template("subprogramBody");
		code.setAttribute("subprogramBody", body);
		
		
		addVariableDeclarations("inputs", code, syms.getInputVariables());
		addVariableDeclarations("outputs", code,
			filterFunctionOutput(syms.getOutputVariables(),
				syms.getFunctionOutput()));
		addVariableDeclarations("updates", code, syms.getUpdateVariables());
		addVariableDeclarations("globals", body, syms.getGlobalVariables());
		StringTemplate locals =
			addVariableDeclarations("locals", body, syms.getLocalVariables());
		
		
		StringTemplate gl;
		StringTemplate group = template("globals");
		for (String cb : syms.getCommonBlocks())
		{
			gl = template("common");
			gl.setAttribute("block", cb);
			for (Variable v : syms.getCommonVariables(cb))
			{
				gl.setAttribute("variables", grVar(v.getName()));
			}
			group.setAttribute("globals", gl);
		}
		if (! syms.getCommonBlocks().isEmpty())
			body.setAttribute("globals", group);
		
		StringTemplate con;
		group = template("constants");
		for (Variable v : syms.getConstants())
		{
			con = template("constant");
			con.setAttribute("const", grVar(v.getName()));
			con.setAttribute("type", template(v.getType()));
			con.setAttribute("expr", expr(v.getInitValue()));
			group.setAttribute("constants", con);	
		}
		if (! syms.getConstants().isEmpty())
			body.setAttribute("constants", group);
		
		StringTemplate ext;
		group = template("externals");
		for (Function f : syms.getExternalRoutines())
		{
			ext = template("external");
			if (f.getReturnType() != null)
				ext.setAttribute("type", template(f.getReturnType()));
			ext.setAttribute("routine", f);
			ext.setAttribute("name", grFct(f.getName()));
			group.setAttribute("externals", ext);
		}
		if (! syms.getExternalRoutines().isEmpty())
			body.setAttribute("externals", group);
		
		StringTemplate intr;
		group = template("intrinsics");
		for (Function f : syms.getIntrinsicFunctions())
		{
			intr = template("intrinsic");
			intr.setAttribute("function", grFct(f.getName()));
			group.setAttribute("intrinsics", intr);
		}
		if (! syms.getIntrinsicFunctions().isEmpty())
			body.setAttribute("intrinsics", group);
		
		StringTemplate sf;
		StringTemplate spec;
		group = template("stfuncs");
		for (StatementFunction f : syms.getStatementFunctions())
		{
			sf = template("statementFunc");
			sf.setAttribute("function", grFct(f.getName()));
			sf.setAttribute("expr", expr(f.getExpression()));
			for (Variable v : f.getArguments())
			{
				if (!syms.isLocalVariable(v.getName()))
				{
					spec = template("typedef");
					spec.setAttribute("variable", grVar(v.getName()));
					spec.setAttribute("type", template(v.getType()));
					body.setAttribute("locals", spec);
				}
				
				sf.setAttribute("params", grVar(v.getName()));
			}
			
			spec = template("typedef");
			Variable v = new Variable(f, f.getReturnType());
			spec.setAttribute("variable", v);
			spec.setAttribute("name", grVar(v.getName()));
			spec.setAttribute("type", template(f.getReturnType()));
			locals.setAttribute("locals", spec);
			
			group.setAttribute("stfuncs", sf);
		}
		if (! syms.getStatementFunctions().isEmpty())
			body.setAttribute("stfuncs", group);
		
		
		indentUp();
		StringTemplate stmt;
		StringTemplate para = template("parallel");
		boolean hasInitialisations = false;
		for (Variable v : syms.getAllVariables())
		{
			if (v.isInitialised())
			{
				hasInitialisations = true;
				
				init = template("assignment");
				init.setAttribute("var", grVar(v.getName()));
				init.setAttribute("value",  expr(v.getInitValue()));
				
				stmt = template("parallelstatement");
				stmt.setAttribute("label", nolabel);
				stmt.setAttribute("indent", getIndentation());
				stmt.setAttribute("statement", init);
				
				para.setAttribute("statements", stmt);
			}
		}
		if (hasInitialisations)
		{
    		stmt = template("initialisations");
    		stmt.setAttribute("label", nolabel);
    		stmt.setAttribute("indent", getIndentation());
    		stmt.setAttribute("statement", para);
    		body.setAttribute("statements", stmt);
		}
		indentDown();
		
	// finished processing subprogram internal data	
	}
	codeblock[body])
	;


codeblock [StringTemplate parent] { StringTemplate s = null;} :
	{ indentUp(); }
	(
	 	s = statement
		{
			parent.setAttribute("statements", s);
			s.setAttribute("indent", getIndentation());
		}
	)*
	{ indentDown(); }
	{
		if (_t != null && _t.getType() > NULL_TREE_LOOKAHEAD)
			ResultRecorder.recordError(
				"CodePrinter's tree parser cannot handle statement: "
				+ _t.toString());
	}
	;
parallel [StringTemplate st]
returns [StringTemplate p = template("parallel")]
{ StringTemplate s = null; } :
	#(PARALLEL label[st] ( s = parallelStatement
		{
			p.setAttribute("statements", s);
			s.setAttribute("indent", getIndentation());
		} )+
	)
	;
	
parallelStatement returns [StringTemplate st=template("parallelstatement")]
{ StringTemplate s = null; } :
	(
	s = assignment[st]      |
	s = arrayAssignment[st]
	)
	{ st.setAttribute("statement", s); }
	;
	
	
statement returns [StringTemplate st=template("statement")]
{ StringTemplate s = null; } :
	(
	s = comment[st]         |
	s = assignment[st]      |
	s = arrayAssignment[st] |
	s = parallel[st]        |
	s = ifStatement[st]     |
	s = doStatement[st]     |
	s = gotoStmt[st]        |
	s = returnStmt[st]      |
	s = continueStmt[st]    |
	s = callStmt[st]        |
	s = equivalenceStmt[st] |
	s = stopStmt[st]        |
	s = formatStmt[st]      |
	s = writeStmt[st]       |
	s = readStmt[st]        |
	s = openStmt[st]        |
	s = closeStmt[st]       |
	s = endStmt[st]
	)
	{ st.setAttribute("statement", s); }
	;


label [StringTemplate st] :
	(
		l:LABEL
		{ st.setAttribute("label",
			String.format( labelFormat.toString(), l.getText() )); }
		|
		// empty... no label
		{st.setAttribute("label", nolabel);}
	)
	;


comment [StringTemplate st]
returns [StringTemplate comblock = template("commentBlock")]
{ StringTemplate com = null; } :
	(options { greedy=true; } :
	c:COMMENT
		{
			com = template("comment");
			com.setAttribute("text", c.getText());
			comblock.setAttribute("comments", com);
		}
	)+
	{ st.setAttribute("label", commentLabel); }
	;

	
assignment [StringTemplate st]
returns [StringTemplate as=template("assignment")]
{StringTemplate var,e;} :
	#(ASSIGN label[st] var=varRef e=expr)
	{
		as.setAttribute("var", var);
		as.setAttribute("value", e);
	}
	;
	
assignment2 returns [StringTemplate as=template("assignment")]
{StringTemplate var,e;} :
	#(ASSIGN var=varRef e=expr)
	{
		as.setAttribute("var", var);
		as.setAttribute("value", e);
	}
	;
	
arrayAssignment [StringTemplate st]
returns [StringTemplate aas=template("arrayassign")]
{StringTemplate var,as;} :
	#(FORALL label[st] var=varRef as=assignment2)
	{
		aas.setAttribute("idx", var);
		aas.setAttribute("assign", as);
	}
	;


ifStatement [StringTemplate st]
returns [StringTemplate i = template("ifStatement")]
{ StringTemplate e,tb,eib,eb; } :
	#("if" label[st]  e=expr  tb=thenBlock
		( eib=elseIfBlock  {i.setAttribute("elseifblock",eib);} )*
		( eb=elseBlock     {i.setAttribute("elseblock",eb);} )?
	)
	{
		i.setAttribute("expr", e);
		i.setAttribute("thenblock",tb);
		i.setAttribute("indent", nolabel + getIndentation());
	}
	;
thenBlock returns [StringTemplate tb = template("thenBlock")]:
	#(THENBLOCK codeblock[tb])
	;
elseIfBlock  returns [StringTemplate eib = template("elseIfBlock")]
{StringTemplate e,tb; } :
	#(ELSEIF e=expr tb=thenBlock)
	{
		eib.setAttribute("expr", e);
		eib.setAttribute("thenblock", tb);
		eib.setAttribute("indent", nolabel + getIndentation());
	}
	;
elseBlock returns [StringTemplate eb = template("elseBlock")] :
	#(ELSEBLOCK codeblock[eb])
	{ eb.setAttribute("indent", nolabel + getIndentation()); }
	;


doStatement [StringTemplate st]
returns [StringTemplate d = template("doStatement")]
{StringTemplate init,termin,incr; } :
	#("do" label[st] l:LABELREF v:NAME init=expr termin=expr
		( incr=expr {d.setAttribute("increment", incr);} )?
			#(DOBLOCK codeblock[d])
	)
	{
		d.setAttribute("labelRef", l.getText());
		d.setAttribute("loopVar", v.getText());
		d.setAttribute("init", init);
		d.setAttribute("termination", termin);
	}
	;


gotoStmt [StringTemplate st]
returns [StringTemplate go = template("goto")] :
	#("go" label[st] lr:LABELREF)
	{ go.setAttribute("label", lr.getText()); }
	;


returnStmt [StringTemplate st]
returns [StringTemplate ret = template("return")] :
	#("return" label[st])
	;
	
	
continueStmt [StringTemplate st]
returns [StringTemplate ctu = template("continue")] :
	#("continue" label[st])
	;
	
	
callStmt [StringTemplate st]
returns [StringTemplate call = template("call")]
{StringTemplate sub;} :
	#("call" label[st] sub=externalFunction)
	{ call.setAttribute("subroutine", sub); }
	;


equivalenceStmt [StringTemplate st]
returns [StringTemplate equiv = empty] :
	"equivalence"
	;
	

stopStmt [StringTemplate st]
returns [StringTemplate stop = template("stop")] :
	#("stop" label[st])
	;


formatStmt [StringTemplate st]
returns [StringTemplate format = template("format")]
{ StringTemplate arg=null; } :
	#("format" label[st]
		((arg=scon | arg=fcon) {format.setAttribute("args", arg);} )+
	)
	;


writeStmt [StringTemplate st]
returns [StringTemplate write = template("write")]
{ StringTemplate ctl=null, io=null; } :
	#("write" label[st] LPAREN ctl=controlInfoList RPAREN)// (io=ioList)?)
	{
		write.setAttribute("ctlList", ctl);
		if (io != null)
			write.setAttribute("ioList", io);
	}
	;
controlInfoList returns [StringTemplate ctl=template("controlInfoList")]
{StringTemplate i=null;} : 
	(i=controlInfoListItem {ctl.setAttribute("items", i);} )+
	;
controlInfoListItem returns [StringTemplate ctl=template("controlInfoItem")]
{StringTemplate e=null;} :	
	ctl=expr |
	#(ASSIGN { ctl.setAttribute("ctl", _t.getText()); }
		("fmt" e=expr |
		"unit" e=expr |
		CTRLREC e=expr |
        "end" e=labelRef |
		"err" (e=labelRef | e=varRef) |
		"iostat" e=varRef)
	) { ctl.setAttribute("value", e); }
	;
	
	
readStmt [StringTemplate st] returns [StringTemplate rd=template("read")] :
	#("read" label[st] ~(LABEL));
	
	
openStmt [StringTemplate st] returns [StringTemplate op=template("open")] :
	#("open" label[st] ~(LABEL));


closeStmt [StringTemplate st] returns [StringTemplate cl=template("close")] :
	#("close" label[st] ~(LABEL));



endStmt [StringTemplate st]
returns [StringTemplate end = template("end")] :
	#("end" label[st])
	;





labelRef returns [StringTemplate lbl = null] :
	l:LABELREF { lbl = new StringTemplate(l.getText()); }
	;

varRef returns [StringTemplate v = null] :
	{_t.getNumberOfChildren()==0}?
	var:NAME
	{ v = grVar(var.getText()); }
	|
	v=arrayref
	|
	v=recvar
	|
	v=vector
	;
recvar returns [StringTemplate a=template("recvar")]
{StringTemplate t = null;} :
	n:RECVAR
	{ a.setAttribute("var", n.getText()); }
	;
arrayref returns [StringTemplate a=template("arrayref")]
{StringTemplate t = null;} :
	#(n:NAME (t=expr {a.setAttribute("indices",t);})+)
	{ a.setAttribute("name", grVar(n.getText())); }
	;
externalFunction returns [StringTemplate a=template("functioncall")]
{StringTemplate t = null;} :
	#(n:EXTERNAL (t=expr {a.setAttribute("arguments",t);})*)
	{ a.setAttribute("name", grFct(n.getText())); }
	;
intrinsicFunction returns [StringTemplate a=null]
{StringTemplate t = null;} :
	#(n:INTRINSIC { a = template(n.getText()); }
		(t=expr {a.setAttribute("arguments",t);})*)
	;
statementFunction returns [StringTemplate a=template("functioncall")]
{StringTemplate t = null;} :
	#(n:STFUNC (t=expr {a.setAttribute("arguments",t);})*)
	{ a.setAttribute("name", grFct(n.getText())); }
	;
function returns [StringTemplate a=template("functioncall")]
{StringTemplate t = null;} :
	#(n:FUNCTION (t=expr {a.setAttribute("arguments",t);})*)
	{ a.setAttribute("name", grFct(n.getText())); }
	;
vector returns [StringTemplate v = null]
{ StringTemplate ix = null, x = null, y = null; } :
	(
		{ _t.getFirstChild().getType() == NAME }?
		#(VECTOR #(n1:NAME ix=expr)
			(
				#(DIMX x=expr)
				{ v = template("vectorA"); v.setAttribute("dimX", x); }
			| 
				#(DIMY y=expr)
				{ v = template("vectorT"); v.setAttribute("dimY", y); }
			)
		{
			v.setAttribute("name", grVar(n1.getText()));
			v.setAttribute("arrayIndex", ix);
			
		}
		)
	|
		#(n2:VECTOR
			( #(DIMX x=expr) (#(DIMY y=expr))?
			{
				v = template("vector");
				v.setAttribute("name", grVar(n2.getText()));
				v.setAttribute("dimX", x);
				v.setAttribute("hasY", y != null);
				v.setAttribute("dimY", y);
			}
			
			|
			#(PROJ ix=expr #(EQ k:NAME #(RANGE x=expr y=expr)))
			{
				v = template("vectorproj");
				v.setAttribute("name", grVar(n2.getText()));
				v.setAttribute("expr", ix);
				v.setAttribute("k", grVar(k.getText()));
				v.setAttribute("a", x);
				v.setAttribute("b", y);
			}
			
			)
			
		)
	)
	;


nAryAddition returns [StringTemplate e=null]
{StringTemplate a, b;} :
	{ e = template("plus");}
	a=expr (options {greedy=true;}: b=nAryAddition
			{ e.setAttribute("a", a); e.setAttribute("b", b); }
		| { e = a; }  )
	;
nAryMultiplication returns [StringTemplate e=null]
{StringTemplate a, b;} :
	{ e = template("mult");}
	a=expr (options {greedy=true;}: b=nAryMultiplication
			{ e.setAttribute("a", a); e.setAttribute("b", b); }
		| { e = a; }  )
	;
range returns [StringTemplate r=template("range")]
{StringTemplate a, b;} :
	#(EQ n:NAME #(RANGE a=expr b=expr))
	{
		r.setAttribute("var", n.getText());
		r.setAttribute("from", a);
		r.setAttribute("to", b);
	}
	;

pieces returns [StringTemplate e=null]
{StringTemplate a, b;} :
	a=expr ((#(EQ NAME RANGE))=> b=range | b=expr)
			{
				e = template("piece");
				e.setAttribute("expr", a);
				e.setAttribute("cond", b);
			}
		
	;
operatorExpression returns [StringTemplate e=null]
{StringTemplate a,b,c,d,group=null; AST prev=null;} :
	{ group = encloseExpression(_t); }
	(
	#(COLON a=expr b=expr)
	{ e = template("colon"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(CONCATOP a=expr b=expr)
	{ e = template("concat"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(NEQV a=expr b=expr)
	{ e = template("neqv"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(EQV a=expr b=expr)
	{ e = template("eqv"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(LOR a=expr b=expr)
	{ e = template("or"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(LAND a=expr b=expr)
	{ e = template("and"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(LNOT a=expr)
	{ e = template("not"); e.setAttribute("a",a); }
	|
	#(LT a=expr b=expr)
	{ e = template("lt"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(LE a=expr b=expr)
	{ e = template("leq"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(EQ a=expr b=expr)
	{ e = template("eq"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(NE a=expr b=expr)
	{ e = template("neq"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(GT a=expr b=expr)
	{ e = template("gt"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(GE a=expr b=expr)
	{ e = template("geq"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	(#(PLUS . .))=> #(PLUS a=expr b=nAryAddition)
	{ e = template("plus"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	(#(MINUS . .))=> #(MINUS a=expr b=expr)
	{ e = template("minus"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	(#(STAR . .))=> #(STAR a=expr b=nAryMultiplication)
	{ e = template("mult"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(DIV a=expr b=expr)
	{ e = template("div"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(PLUS a=expr)
	{ e = template("unaryplus"); e.setAttribute("a",a); }
	|
	#(MINUS a=expr)
	{ e = template("unaryminus"); e.setAttribute("a",a); }
	|
	STAR // this indicates variable array dimension
	{ e = template("varArrayDim"); }
	|
	#(POWER a=expr b=expr)
	{ e = template("power"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	// The following  expressions are part of Fortran-M, the extension of
	// FORTRAN 77 used in this reverse engineering project
	#(IMPLIES a=expr b=expr)
	{ e = template("implies"); e.setAttribute("a",a); e.setAttribute("b",b); }
	|
	#(SUM c=expr #(EQ d=expr #(RANGE a=expr b=expr)))
	{ e = template("sum"); e.setAttribute("expr", c); e.setAttribute("index", d);
		e.setAttribute("a", a); e.setAttribute("b", b); }
	|
	#(PIECEWISE { e = template("piecewise"); }
		( ((. .)=> a=pieces { e.setAttribute("pieces", a); }
			| a=expr { e.setAttribute("otherwise", a); }) )+
	)
	|
	#(GAMMA { e = template("gammaFct"); }
		( a=expr {e.setAttribute("args",a);} )+ )
	|
	#(EXP a=expr)
	{ e = template("exp"); e.setAttribute("expr", a); }
	|
	#(FACTORIAL a=expr)
	{ e = template("factorial"); e.setAttribute("expr", a); }
	|
	#(DOTPROD a=vector b=vector)
	{ e=template("dotproduct"); e.setAttribute("a", a); e.setAttribute("b",b); }
	|
	#(EVAL a=expr { e=template("eval"); e.setAttribute("expr", a); }
		( b=expr { e.setAttribute("eqns", b); } )+ )
	|
	#(OVERWRITE a=expr b=expr)
	{ e=template("overwrite"); e.setAttribute("a", a); e.setAttribute("b", b); }

	)
	{ group.setAttribute("expr", e); e = group; }
	;
	

expr returns [StringTemplate e=null] {AST prev=null;} :
	e = operatorExpression
	|
	e = varRef
	|
	e = externalFunction
	|
	e = intrinsicFunction
	|
	e = statementFunction
	|
	e = function
	|
	e=hollerith |
	e=scon |
	e=ccon | e=zcon |
	e=trueconst | e=falseconst |
	{ prev = _t; } (
	ICON | RCON
	) { e = new StringTemplate(prev.getText()); }
	;



hollerith returns [StringTemplate t=template("hollerith")] :
	h:HOLLERITH
	{
		t.setAttribute("len", h.getText().length());
		t.setAttribute("string", h.getText());
	}
	;
scon returns [StringTemplate t=template("stringliteral")] :
	s:SCON
	{ t.setAttribute("string", s.getText()); }
	;
ccon returns [StringTemplate t=template("complexnumber")]
{StringTemplate re=null,im=null;} :
	#(c:CCON re=expr im=expr)
	{
		t.setAttribute("real", re);
		t.setAttribute("img", im);
	}
	;
zcon returns [StringTemplate t=template("hexnumber")] :
	h:ZCON
	{ t.setAttribute("number", h.getText()); }
	;
trueconst returns [StringTemplate t=template("trueconst")] :
	TRUE ;
falseconst  returns [StringTemplate t=template("falseconst")] :
	FALSE ;

fcon returns [StringTemplate t = null] :
	f:FCON { t = new StringTemplate(f.getText()); }
	;
