/*
 * Parser.java            
 *
 * This parser for a subset of the VC language is intended to 
 *  demonstrate how to create the AST nodes, including (among others): 
 *  [1] a list (of statements)
 *  [2] a function
 *  [3] a statement (which is an expression statement), 
 *  [4] a unary expression
 *  [5] a binary expression
 *  [6] terminals (identifiers, integer literals and operators)
 *
 * In addition, it also demonstrates how to use the two methods start 
 * and finish to determine the position information for the start and 
 * end of a construct (known as a phrase) corresponding an AST node.
 *
 * NOTE THAT THE POSITION INFORMATION WILL NOT BE MARKED. HOWEVER, IT CAN BE
 * USEFUL TO DEBUG YOUR IMPLEMENTATION.
 *
 * (09-|-April-|-2016)


program       -> func-decl
func-decl     -> type identifier "(" ")" compound-stmt
type          -> void
identifier    -> ID
// statements
compound-stmt -> "{" stmt* "}" 
stmt          -> expr-stmt
expr-stmt     -> expr? ";"
// expressions 
expr                -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
                    |  additive-expr "-" multiplicative-expr
multiplicative-expr -> unary-expr
	            |  multiplicative-expr "*" unary-expr
	            |  multiplicative-expr "/" unary-expr
unary-expr          -> "-" unary-expr
		    |  primary-expr

primary-expr        -> identifier
 		    |  INTLITERAL
		    | "(" expr ")"
 */

package VC.Parser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;
import VC.ASTs.*;






public class Parser {

  private Scanner scanner;
  private ErrorReporter errorReporter;
  private Token currentToken;
  private SourcePosition previousTokenPosition;
  private SourcePosition dummyPos = new SourcePosition();

  public Parser (Scanner lexer, ErrorReporter reporter) {
    scanner = lexer;
    errorReporter = reporter;

    previousTokenPosition = new SourcePosition();

    currentToken = scanner.getToken();
  }

// match checks to see f the current token matches tokenExpected.
// If so, fetches the next token.
// If not, reports a syntactic error.

  void match(int tokenExpected) throws SyntaxError {
    if (currentToken.kind == tokenExpected) {
      previousTokenPosition = currentToken.position;
      currentToken = scanner.getToken();
    } else {
      syntacticError("\"%\" expected here", Token.spell(tokenExpected));
    }
  }

  void accept() {
    previousTokenPosition = currentToken.position;
    currentToken = scanner.getToken();
  }

  void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
    SourcePosition pos = currentToken.position;
    errorReporter.reportError(messageTemplate, tokenQuoted, pos);
    throw(new SyntaxError());
  }

// start records the position of the start of a phrase.
// This is defined to be the position of the first
// character of the first token of the phrase.

  void start(SourcePosition position) {
    position.lineStart = currentToken.position.lineStart;
    position.charStart = currentToken.position.charStart;
  }

// finish records the position of the end of a phrase.
// This is defined to be the position of the last
// character of the last token of the phrase.

  void finish(SourcePosition position) {
    position.lineFinish = previousTokenPosition.lineFinish;
    position.charFinish = previousTokenPosition.charFinish;
  }

  void copyStart(SourcePosition from, SourcePosition to) {
    to.lineStart = from.lineStart;
    to.charStart = from.charStart;
  }

// ========================== PROGRAMS ========================

  public Program parseProgram() {

    Program programAST = null;
    List dlAST = null;
    SourcePosition programPos = new SourcePosition();
    start(programPos);

    try {
    	
    	
        dlAST = parseFuncDeclList();
        finish(programPos);
        programAST = new Program(dlAST, programPos); 
        if (currentToken.kind != Token.EOF) {
            syntacticError("\"%\" unknown type", currentToken.spelling);
        }
    }
    catch (SyntaxError s) { return null; }
    return programAST;
  }

// ========================== DECLARATIONS ========================

  List parseFuncDeclList() throws SyntaxError {
	    List dlAST = null;
	    Decl dAST = null;

	    SourcePosition funcPos = new SourcePosition();
	    start(funcPos);
	    
	    if (currentToken.kind != Token.EOF) {
	    	Type tAST = parseType();
	        Ident idAST = parseIdent();
	        if(currentToken.kind == Token.LPAREN){
	        	dAST = parseFuncDecl(tAST, idAST);
	        	if (currentToken.kind == Token.VOID || currentToken.kind == Token.FLOAT 
	        			|| currentToken.kind == Token.INT || currentToken.kind == Token.BOOLEAN) {
	        	      dlAST = parseFuncDeclList();
	        	      finish(funcPos);
	        	      dlAST = new DeclList(dAST, dlAST, funcPos);
	        	}else if (dAST != null) {
	        		  finish(funcPos);
	        	      dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), funcPos);
	        	}
	    	}else{
	    		boolean mark = true;
	    		dlAST = parseVardecl(tAST, idAST, mark);
	    		if (currentToken.kind == Token.VOID || currentToken.kind == Token.FLOAT 
	        			|| currentToken.kind == Token.INT || currentToken.kind == Token.BOOLEAN) {
	        	      DeclList R = (DeclList) dlAST;
	        	      while(!(R.DL instanceof EmptyDeclList)){
		  		  		  R = (DeclList) R.DL;
		  		  	  }
	  		  		  R.DL = parseFuncDeclList();
	        	}
	    	}
	    }
	    
	    if (dlAST == null) 
	      dlAST = new EmptyDeclList(dummyPos);

	    return dlAST;
  }

  Decl parseFuncDecl(Type tAST,Ident idAST) throws SyntaxError {

    Decl fAST = null; 
    
    SourcePosition funcPos = new SourcePosition();
    start(funcPos);

    List fplAST = parseParaList();
    Stmt cAST = parseCompoundStmt();

    finish(funcPos);
    fAST = new FuncDecl(tAST, idAST, fplAST, cAST, funcPos);
    return fAST;
  }
  
  List parseVardecl(Type tAST,Ident idAST, boolean mark) throws SyntaxError {
	  List varAST = null;
	  varAST = parseInitDeclaratorList(tAST, idAST, mark);
	  match(Token.SEMICOLON);
	  return varAST;  
  }
  

  List parseInitDeclaratorList(Type tAST, Ident idAST, boolean mark) throws SyntaxError {
	  List initdecllistAST = null;
	  SourcePosition decllistPos = new SourcePosition();
	  start(decllistPos);
	  
	  Decl declAST = parseInitDeclarator(tAST, idAST, mark);
	  if(currentToken.kind == Token.COMMA){
		  accept();
		  Ident id2AST = parseIdent();
		  List next = parseInitDeclaratorList(tAST, id2AST, mark);
		  finish(decllistPos);
		  initdecllistAST = new DeclList(declAST, next, decllistPos);
	  }else{
		  finish(decllistPos);
		  initdecllistAST = new DeclList(declAST, new EmptyDeclList(dummyPos), decllistPos);
	  }
	  return initdecllistAST;
  }
  
  Decl parseInitDeclarator(Type tAST, Ident idAST, boolean mark) throws SyntaxError {
	  //mark = true means global var, mark = false means local var
	  Decl initdeclAST = null;
	  SourcePosition declPos = new SourcePosition();
	  start(declPos);
	  
	  Type declType = parseDeclarator(tAST, idAST);
	  if(currentToken.kind == Token.EQ){
		  accept();
		  Expr initialiser = parseInitialiser();
		  finish(declPos);
		  if(mark){
			  initdeclAST = new GlobalVarDecl(declType, idAST, initialiser, declPos);
		  }else{
			  initdeclAST = new LocalVarDecl(declType, idAST, initialiser, declPos);
		  }
	  }else{
		  finish(declPos);
		  if(mark){
			  initdeclAST = new GlobalVarDecl(declType, idAST, new EmptyExpr(dummyPos), declPos);
		  }else{
			  initdeclAST = new LocalVarDecl(declType, idAST, new EmptyExpr(dummyPos), declPos);
		  }
	  }
	  return initdeclAST;
  }
  //Should consider this
  Expr parseInitialiser() throws SyntaxError {
	  Expr initAST = null;
	  SourcePosition initPos = new SourcePosition();
	  start(initPos);
	  
	  if(currentToken.kind == Token.LCURLY){
		  match(Token.LCURLY);
		  List exprlist = parseInitExprList(); 
		  finish(initPos);
		  initAST = new InitExpr(exprlist, initPos);
		  match(Token.RCURLY);
	  }else{
		  //System.out.println("Epression begin");
		  finish(initPos);
		  initAST = parseExpr();
	  }
	  return initAST;
  }
  
  List parseInitExprList() throws SyntaxError{
	  List initexprAST = null;
	  SourcePosition exprlistPos = new SourcePosition();
	  start(exprlistPos);
	  Expr exprAST = parseExpr();
	  if(currentToken.kind == Token.COMMA){
		  accept();
		  List exprlist = parseInitExprList();
		  finish(exprlistPos);
		  initexprAST = new ExprList(exprAST, exprlist, exprlistPos);
	  }else{
		  finish(exprlistPos);
		  initexprAST = new ExprList(exprAST, new EmptyExprList(dummyPos), exprlistPos);
	  }
	  
	  return initexprAST;
  }
  Type parseDeclarator(Type type, Ident idAST) throws SyntaxError {
	  Type tAST = null;
	  
	  SourcePosition declaratorPos = new SourcePosition();
	  start(declaratorPos);
	  
	  if(currentToken.kind == Token.LBRACKET){
		  accept();
		  Expr intexpr = null;
		  if(currentToken.kind == Token.INTLITERAL){
			  IntLiteral index = parseIntLiteral();
			  intexpr = new IntExpr(index, previousTokenPosition);
		  	  match(Token.RBRACKET);
		  }else{
			  intexpr = new EmptyExpr(dummyPos);
			  match(Token.RBRACKET);  
		  }
		  finish(declaratorPos);
		  tAST = new ArrayType(type,intexpr,declaratorPos);
	  }else{
		  finish(declaratorPos);
		  tAST = type;
	  }
	  return tAST;
  }

//  ======================== TYPES ==========================

  Type parseType() throws SyntaxError {
    Type typeAST = null;

    SourcePosition typePos = new SourcePosition();
    start(typePos);

    switch(currentToken.kind){
	  case Token.VOID:
		  accept();
		  finish(typePos);
		  typeAST = new VoidType(typePos);
		  break;
	  case Token.BOOLEAN:
		  accept();
		  finish(typePos);
		  typeAST = new BooleanType(typePos);
		  break;
	  case Token.INT:
		  accept();
		  finish(typePos);
		  typeAST = new IntType(typePos);
		  break;
	  case Token.FLOAT:
		  accept();
		  finish(typePos);
		  typeAST = new FloatType(typePos);
		  break;
	  default:
		  //Should error Here?
		  //syntacticError("primitive types expected here", "");
		  syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
		  break;
	} 

    return typeAST;
   }

// ======================= STATEMENTS ==============================

  Stmt parseCompoundStmt() throws SyntaxError {
    Stmt cAST = null; 

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    match(Token.LCURLY);

    // Insert code here to build a DeclList node for variable declarations
    List varlAST = parseVarDeclList();
    //System.out.println("stmtlist next:");
    List slAST = parseStmtList();  
    match(Token.RCURLY);
    finish(stmtPos);

    if (slAST instanceof EmptyStmtList && varlAST instanceof EmptyDeclList) 
      cAST = new EmptyCompStmt(stmtPos);
    else 
      cAST = new CompoundStmt(varlAST, slAST, stmtPos);
    
    return cAST;
  }
  
  List parseVarDeclList() throws SyntaxError{
	  List varlistAST = null;
	  SourcePosition varlistPos = new SourcePosition();
	  start(varlistPos);
	  
	  if (currentToken.kind == Token.VOID || currentToken.kind == Token.BOOLEAN
	    		|| currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT){
	    	
		  	Type tAST = parseType();
		  	Ident idAST = parseIdent();
		  	boolean mark = false;
		  	
		  	varlistAST = parseVardecl(tAST, idAST, mark);

		  	if (currentToken.kind == Token.VOID || currentToken.kind == Token.BOOLEAN
		    		|| currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT){
		  		//Find the most right child of the current tree
		  		//insert the new node in it
		  		DeclList R = (DeclList) varlistAST;
		  		while(!(R.DL instanceof EmptyDeclList)){
		  			R = (DeclList) R.DL;
		  		}
		  		R.DL = parseVarDeclList();
		  		
		  	}
	    }else{
	    	finish(varlistPos);
	    	varlistAST = new EmptyDeclList(dummyPos);
	    }
	  return varlistAST;
  }

  List parseStmtList() throws SyntaxError {
    List slAST = null; 

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    if (currentToken.kind != Token.RCURLY) {
      Stmt sAST = parseStmt();
      {
        if (currentToken.kind != Token.RCURLY) {
          slAST = parseStmtList();
          finish(stmtPos);
          slAST = new StmtList(sAST, slAST, stmtPos);
        } else {
          finish(stmtPos);
          slAST = new StmtList(sAST, new EmptyStmtList(dummyPos), stmtPos);
        }
      }
    }
    else
      slAST = new EmptyStmtList(dummyPos);
    
    return slAST;
  }

  Stmt parseStmt() throws SyntaxError {
    Stmt sAST = null;
    //Position ??????????????????????
    
    switch (currentToken.kind) {
    case Token.LCURLY:
    	sAST = parseCompoundStmt();
    	break;
    case Token.IF:
    	sAST = parseIfStmt();
    	break;
    case Token.FOR:
    	sAST = parseForStmt();
    	break;
    case Token.WHILE:
    	sAST = parseWhileStmt();
    	break;
    case Token.BREAK:
    	sAST = parseBreakStmt();
    	break;
    case Token.CONTINUE:
    	sAST = parseContinueStmt();
    	break;
    case Token.RETURN:
    	sAST = parseReturnStmt();
    	break;
    default:
    	//System.out.println("ExprStmt:");
    	sAST = parseExprStmt();
		break;

    }
    return sAST;
  }
  
//Expr-stmt
  Stmt parseExprStmt() throws SyntaxError {
    Stmt sAST = null;

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    if (currentToken.kind == Token.ID
            || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
            || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
            || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
            || currentToken.kind == Token.LPAREN) {
          Expr eAST = parseExpr();
          
          match(Token.SEMICOLON);
          finish(stmtPos);
          sAST = new ExprStmt(eAST, stmtPos);
    } else {
	      match(Token.SEMICOLON);
	      finish(stmtPos);
	      sAST = new ExprStmt(new EmptyExpr(dummyPos), stmtPos);
    }
    
    return sAST;
  }

//If-stmt
   Stmt parseIfStmt() throws SyntaxError {
	  Stmt sAST = null;
	  SourcePosition stmtPos = new SourcePosition();
	  start(stmtPos);
	  
	  match(Token.IF);
	  match(Token.LPAREN);
	  Expr eAST = parseExpr();
	  match(Token.RPAREN);
	  Stmt s1AST = parseStmt();
	  if(currentToken.kind == Token.ELSE){
		  match(Token.ELSE);
		  finish(stmtPos);
		  Stmt s2AST =parseStmt();
		  sAST = new IfStmt(eAST, s1AST, s2AST, stmtPos);
	  }else{
		  finish(stmtPos);
		  sAST = new IfStmt(eAST, s1AST,stmtPos);
	  }
	  return sAST;
  }
   
//For-stmt
   Stmt parseForStmt() throws SyntaxError {
	   
	  Stmt sAST = null;
	  SourcePosition stmtPos = new SourcePosition();
	  start(stmtPos);
	  
 	  match(Token.FOR);
 	  match(Token.LPAREN);
 	  Expr e1AST = new EmptyExpr(dummyPos);
 	  Expr e2AST = new EmptyExpr(dummyPos);
 	  Expr e3AST = new EmptyExpr(dummyPos);
 	  if (currentToken.kind == Token.ID
 		        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
 		        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
 		        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
 		        || currentToken.kind == Token.LPAREN) {
 		  		e1AST = parseExpr();
 	  }
 	  match(Token.SEMICOLON);
 	  if (currentToken.kind == Token.ID
 		        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
 		        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
 		        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
 		        || currentToken.kind == Token.LPAREN) {
 		  		e2AST = parseExpr();
 	  }
 	  match(Token.SEMICOLON);
 	  if (currentToken.kind == Token.ID
 		        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
 		        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
 		        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
 		        || currentToken.kind == Token.LPAREN) {
 		        e3AST = parseExpr();
 	  }
 	  match(Token.RPAREN);
 	  Stmt sAST_in = parseStmt();
 	  finish(stmtPos);
 	  sAST = new ForStmt(e1AST, e2AST, e3AST, sAST_in, stmtPos);
 	  return sAST;
   }
   
//While-stmt
   Stmt parseWhileStmt() throws SyntaxError {
	  Stmt sAST = null;
	  SourcePosition stmtPos = new SourcePosition();
	  start(stmtPos);
	  
 	  match(Token.WHILE);
 	  match(Token.LPAREN);
 	  Expr eAST = parseExpr();
 	  match(Token.RPAREN);
 	  Stmt sAST_in = parseStmt();
 	  
 	  finish(stmtPos);
 	  sAST = new WhileStmt(eAST, sAST_in, stmtPos);
 	  return sAST;
   }   
   
//Break-stmt
   Stmt parseBreakStmt () throws SyntaxError {
	  Stmt sAST = null;
	  SourcePosition stmtPos = new SourcePosition();
	  start(stmtPos);
	  
 	  match(Token.BREAK);
 	  match(Token.SEMICOLON);
 	  
 	  finish(stmtPos);
 	  sAST = new BreakStmt(stmtPos);
 	  return sAST;
 	  
   }
   
//Continue-stmt
  Stmt parseContinueStmt() throws SyntaxError {
	 Stmt sAST = null;
	 SourcePosition stmtPos = new SourcePosition();
	 start(stmtPos);
	 
     match(Token.CONTINUE);
     match(Token.SEMICOLON);
     
     finish(stmtPos);
	 sAST = new ContinueStmt(stmtPos);
	 return sAST;
   }
   
  //Return-stmt
  Stmt parseReturnStmt() throws SyntaxError{
	  Stmt sAST = null;
	  SourcePosition stmtPos = new SourcePosition();
	  start(stmtPos);
	  Expr eAST = new EmptyExpr(dummyPos);
	  match(Token.RETURN);
	  if (currentToken.kind == Token.ID
		        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
		        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
		        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
		        || currentToken.kind == Token.LPAREN) {
		        eAST = parseExpr();
	    } 
	  match(Token.SEMICOLON);
	  
	  finish(stmtPos);
	  sAST = new ReturnStmt(eAST,stmtPos);
	  return sAST;
  }
  
// ======================= PARAMETERS =======================

  List parseParaList() throws SyntaxError {
    List formalsAST = null;

    SourcePosition formalsPos = new SourcePosition();
    start(formalsPos);

    match(Token.LPAREN);
	if(currentToken.kind == Token.VOID || currentToken.kind == Token.BOOLEAN
	    	|| currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT){
		formalsAST = parseProperParaList();
		match(Token.RPAREN);
		finish(formalsPos);
	}else{
		match(Token.RPAREN);
		finish(formalsPos);
		formalsAST = new EmptyParaList (formalsPos);
	}

    return formalsAST;
  }

  List parseProperParaList() throws SyntaxError {
	  List paraListAST = null;
	  
	  SourcePosition pralistPos = new SourcePosition();
	  start(pralistPos);
	  
	  ParaDecl pAST = parseParaDecl();
	  
	  if(currentToken.kind == Token.COMMA){
		  match(Token.COMMA);
		  List next = parseProperParaList();
		  finish(pralistPos);
		  paraListAST = new ParaList(pAST, next, pralistPos);
	  }else{
		  finish(pralistPos);
		  paraListAST = new ParaList(pAST, new EmptyParaList(dummyPos), pralistPos);
	  }
	  return paraListAST;
  }
  
 //para-decl  -> type declarator
  ParaDecl parseParaDecl() throws SyntaxError {
	  ParaDecl pAST = null;
	  
	  SourcePosition pASTPos = new SourcePosition();
	  start(pASTPos);
	  
	  Type tAST = parseType();
	  Ident idAST = parseIdent();
	  Type new_tAST = parseDeclarator(tAST, idAST);
	  //System.out.println("parse");
	  
	  
	  finish(pASTPos);
	  pAST = new ParaDecl(new_tAST, idAST, pASTPos);
	  return pAST;
  }
  
  List parseArgList() throws SyntaxError {
	  List arglist = null;
	  SourcePosition ArglistPos = new SourcePosition();
	  start(ArglistPos);
	  
	  match(Token.LPAREN);
	  if (currentToken.kind == Token.ID
		        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
		        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
		        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
		        || currentToken.kind == Token.LPAREN){
		  arglist = parseProperArgList();
		  match(Token.RPAREN); 
	  }else{
		  finish(ArglistPos);
		  arglist = new EmptyArgList(dummyPos);
		  match(Token.RPAREN); 	  
	  }
	  
	  return arglist;
  }
  
  List parseProperArgList() throws SyntaxError {
	  List Arglist = null;
	  SourcePosition ArglistPos = new SourcePosition();
	  start(ArglistPos);
	  
	  Arg arg = parseArg();
	  if(currentToken.kind == Token.COMMA){
		  accept();
		  List next = parseProperArgList();
		  finish(ArglistPos);
		  Arglist = new ArgList(arg, next, ArglistPos);
	  }else{
		  finish(ArglistPos);
		  Arglist = new ArgList(arg, new EmptyArgList(dummyPos), ArglistPos);
	  }
	  
	  return Arglist;
  }
  
  Arg parseArg() throws SyntaxError {
	  Arg argAST = null;
	  SourcePosition argPos = new SourcePosition();
	  start(argPos);
	  Expr expr = parseExpr();
	  finish(argPos);
	  argAST = new Arg(expr, argPos);
	  return argAST;
  }

// ======================= EXPRESSIONS ======================


  Expr parseExpr() throws SyntaxError {
	 //System.out.println("parseExpr:");
    Expr exprAST = null;
    exprAST = parseAssignExpr();
    return exprAST;
  }
  
  Expr parseAssignExpr() throws SyntaxError {
	  Expr assAST = null;
	  SourcePosition assPos = new SourcePosition();
	  start(assPos);
	  
	  assAST = parseCondORExpr();
	  if(currentToken.kind == Token.EQ){
		  
		  acceptOperator();
		  Expr next = parseAssignExpr();
		  finish(assPos);
		  assAST = new AssignExpr(assAST, next, assPos);
	  }
	  
	  return assAST;
  }
  
  Expr parseCondORExpr() throws SyntaxError {
	  Expr condorAST = null;
	  SourcePosition condorPos = new SourcePosition();
	  start(condorPos);
	  
	  condorAST = parseCondAndExpr();
	  while(currentToken.kind == Token.OROR){
		  Operator opAST = acceptOperator();
		  Expr e2AST = parseCondAndExpr();
		  SourcePosition addPos = new SourcePosition();
	      copyStart(condorPos, addPos);
	      finish(addPos);
		  condorAST = new BinaryExpr(condorAST, opAST, e2AST, addPos);
	  }
	  return condorAST;
  }
  
  Expr parseCondAndExpr() throws SyntaxError {
	  Expr condandAST = null;
	  SourcePosition condandPos = new SourcePosition();
	  start(condandPos);
	  
	  condandAST = parseEqualityExpr();
	  while(currentToken.kind == Token.ANDAND){
		  Operator opAST = acceptOperator();
		  Expr e2AST = parseEqualityExpr();
		  
		  SourcePosition addPos = new SourcePosition();
	      copyStart(condandPos, addPos);
	      finish(addPos);
	      
		  condandAST = new BinaryExpr(condandAST, opAST, e2AST, addPos);
	  }
	  return condandAST;
  }
  
  Expr parseEqualityExpr()throws SyntaxError {
	  Expr equalAST = null;
	  SourcePosition equalPos = new SourcePosition();
	  start(equalPos);
	  equalAST = parseRelExpr();
	  while(currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ){
		  Operator opAST = acceptOperator();
		  Expr e2AST = parseRelExpr();
		  
		  SourcePosition addPos = new SourcePosition();
	      copyStart(equalPos, addPos);
	      finish(addPos);
	      equalAST = new BinaryExpr(equalAST, opAST, e2AST, addPos);
	  }
	  return equalAST;
  }
  
  Expr parseRelExpr() throws SyntaxError {
	  Expr relAST  = null;
	  SourcePosition relPos = new SourcePosition();
	  start(relPos);
	  
	  relAST = parseAdditiveExpr();
	  while(currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ
			  || currentToken.kind == Token.GT ||currentToken.kind == Token.GTEQ){
		  Operator opAST = acceptOperator();
		  Expr e2AST = parseAdditiveExpr();
		  
		  SourcePosition addPos = new SourcePosition();
	      copyStart(relPos, addPos);
	      finish(addPos);
	      relAST = new BinaryExpr(relAST, opAST, e2AST, addPos);
	       
	  }
	  return relAST;
  }
  

  Expr parseAdditiveExpr() throws SyntaxError {
    Expr exprAST = null;

    SourcePosition addStartPos = new SourcePosition();
    start(addStartPos);

    exprAST = parseMultiplicativeExpr();
    while (currentToken.kind == Token.PLUS
           || currentToken.kind == Token.MINUS) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseMultiplicativeExpr();

      SourcePosition addPos = new SourcePosition();
      copyStart(addStartPos, addPos);
      finish(addPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
    }
    return exprAST;
  }

  Expr parseMultiplicativeExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition multStartPos = new SourcePosition();
    start(multStartPos);

    exprAST = parseUnaryExpr();
    while (currentToken.kind == Token.MULT
           || currentToken.kind == Token.DIV) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseUnaryExpr();
      SourcePosition multPos = new SourcePosition();
      copyStart(multStartPos, multPos);
      finish(multPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, multPos);
    }
    return exprAST;
  }

  Expr parseUnaryExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition unaryPos = new SourcePosition();
    start(unaryPos);

    switch (currentToken.kind) {
      case Token.MINUS:case Token.PLUS:case Token.NOT:
        {
          Operator opAST = acceptOperator();
          Expr e2AST = parseUnaryExpr();
          finish(unaryPos);
          exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
        }
        break;
      
      default:
        exprAST = parsePrimaryExpr();
        break;
       
    }
    return exprAST;
  }

  Expr parsePrimaryExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition primPos = new SourcePosition();
    start(primPos);

    switch (currentToken.kind) {

      case Token.ID:
    	// Test this case: ............................
        Ident iAST = parseIdent();
        
        if(currentToken.kind == Token.LBRACKET){
        	accept();
        	Var arrAST = new SimpleVar(iAST, previousTokenPosition);
        	Expr eAST = parseExpr();
        	match(Token.RBRACKET);
        	finish(primPos);
        	exprAST = new ArrayExpr(arrAST, eAST, primPos);
        }else if(currentToken.kind == Token.LPAREN){
        	List arglist = parseArgList();
        	finish(primPos);
        	exprAST = new CallExpr(iAST, arglist, primPos);
        }else{
        	finish(primPos);
            Var simVAST = new SimpleVar(iAST, primPos);
            exprAST = new VarExpr(simVAST, primPos);
        	
        }
            
        break;

      case Token.LPAREN:
        {
          accept();
          exprAST = parseExpr();
          match(Token.RPAREN);
        }
        break;

      case Token.INTLITERAL:
          IntLiteral ilAST = parseIntLiteral();
          finish(primPos);
          exprAST = new IntExpr(ilAST, primPos);
          break;
      case Token.BOOLEANLITERAL:
		  BooleanLiteral blAST = parseBooleanLiteral();
		  finish(primPos);
	      exprAST = new BooleanExpr(blAST, primPos);
		  break;
	  case Token.FLOATLITERAL:
		  FloatLiteral flAST = parseFloatLiteral();
		  finish(primPos);
		  exprAST = new FloatExpr(flAST, primPos);
		  break;
      case Token.STRINGLITERAL:
    	  StringLiteral slAST = parseStringLiteral();
    	  finish(primPos);
    	  exprAST = new StringExpr(slAST, primPos);
    	  break;
      default:
        syntacticError("illegal primary expression", currentToken.spelling);
        break;
    }
    return exprAST;
  }

// ========================== ID, OPERATOR and LITERALS ========================

  Ident parseIdent() throws SyntaxError {

    Ident I = null; 

    if (currentToken.kind == Token.ID) {
      previousTokenPosition = currentToken.position;
      String spelling = currentToken.spelling;
      I = new Ident(spelling, previousTokenPosition);
      currentToken = scanner.getToken();
    } else 
      syntacticError("identifier expected here", "");
    return I;
  }

// acceptOperator parses an operator, and constructs a leaf AST for it

  Operator acceptOperator() throws SyntaxError {
    Operator O = null;

    previousTokenPosition = currentToken.position;
    String spelling = currentToken.spelling;
    O = new Operator(spelling, previousTokenPosition);
    currentToken = scanner.getToken();
    return O;
  }


  IntLiteral parseIntLiteral() throws SyntaxError {
    IntLiteral IL = null;

    if (currentToken.kind == Token.INTLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      IL = new IntLiteral(spelling, previousTokenPosition);
    } else 
      syntacticError("integer literal expected here", "");
    return IL;
  }

  FloatLiteral parseFloatLiteral() throws SyntaxError {
    FloatLiteral FL = null;

    if (currentToken.kind == Token.FLOATLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      FL = new FloatLiteral(spelling, previousTokenPosition);
    } else 
      syntacticError("float literal expected here", "");
    return FL;
  }

  BooleanLiteral parseBooleanLiteral() throws SyntaxError {
    BooleanLiteral BL = null;

    if (currentToken.kind == Token.BOOLEANLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      BL = new BooleanLiteral(spelling, previousTokenPosition);
    } else 
      syntacticError("boolean literal expected here", "");
    return BL;
  }
  
  StringLiteral parseStringLiteral() throws SyntaxError {
	  StringLiteral SL = null;
	  
	  if (currentToken.kind == Token.STRINGLITERAL) {
	      String spelling = currentToken.spelling;
	      accept();
	      SL = new StringLiteral(spelling, previousTokenPosition);
	    } else 
	      syntacticError("string literal expected here", "");
	  return SL;
  }

}

