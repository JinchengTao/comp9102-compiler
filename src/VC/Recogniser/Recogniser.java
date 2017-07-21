/***
 * *
 * * Recogniser.java            
 * *
 ***/

/* At this stage, this parser accepts a subset of VC defined	by
 * the following grammar. 
 *
 * You need to modify the supplied parsing methods (if necessary) and 
 * add the missing ones to obtain a parser for the VC language.
 *
 * (17---March---2017)

program       -> func-decl

// declaration

func-decl     -> void identifier "(" ")" compound-stmt

identifier    -> ID

// statements 
compound-stmt -> "{" stmt* "}" 
stmt          -> continue-stmt
    	      |  expr-stmt
continue-stmt -> continue ";"
expr-stmt     -> expr? ";"

// expressions 
expr                -> assignment-expr
assignment-expr     -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
multiplicative-expr -> unary-expr
	            |  multiplicative-expr "*" unary-expr
unary-expr          -> "-" unary-expr
		    |  primary-expr

primary-expr        -> identifier
 		    |  INTLITERAL
		    | "(" expr ")"
*/

package VC.Recogniser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;

public class Recogniser {

  private Scanner scanner;
  private ErrorReporter errorReporter;
  private Token currentToken;

  public Recogniser (Scanner lexer, ErrorReporter reporter) {
    scanner = lexer;
    errorReporter = reporter;

    currentToken = scanner.getToken();
  }

// match checks to see f the current token matches tokenExpected.
// If so, fetches the next token.
// If not, reports a syntactic error.

  void match(int tokenExpected) throws SyntaxError {
    if (currentToken.kind == tokenExpected) {
      currentToken = scanner.getToken();
    } else {
      syntacticError("\"%\" expected here", Token.spell(tokenExpected));
    }
  }

 // accepts the current token and fetches the next
  void accept() {
    currentToken = scanner.getToken();
  }

  void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
    SourcePosition pos = currentToken.position;
    errorReporter.reportError(messageTemplate, tokenQuoted, pos);
    throw(new SyntaxError());
  }


// ========================== PROGRAMS ========================

  public void parseProgram() {

    try {
    	//
    	while(currentToken.kind != Token.EOF){
	    	parseType();
	    	parseIdent();
	    	if(currentToken.kind == Token.LPAREN){
	    		parseFuncDecl();
	    	}else{
	    		parseVarDecl();
	    	}
	    	//Need check where to put LOgic not understsand
    	}
        if (currentToken.kind != Token.EOF) {
            syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
        }
    }
    catch (SyntaxError s) {  }
  }

// ========================== DECLARATIONS ========================

  void parseFuncDecl() throws SyntaxError {

    parseParaList();
    parseCompoundStmt();
  }
  void parseVarDecl() throws SyntaxError {
	  
	  parseInitDeclaratorList();  
	  match(Token.SEMICOLON);
  }
  void parseInitDeclaratorList() throws SyntaxError {
	  if(currentToken.kind == Token.LBRACKET){
		  match(Token.LBRACKET);
		  if(currentToken.kind == Token.INTLITERAL){
			  parseIntLiteral();
		  	  match(Token.RBRACKET);
		  }else{
			  match(Token.RBRACKET);  
		  }
	  } 
	  if(currentToken.kind == Token.EQ){
		  match(Token.EQ);
		  parseInitialiser();
	  }
	  while(currentToken.kind == Token.COMMA){
		  match(Token.COMMA);
		  parseInitDeclarator();
	  }
  }
  void parseInitDeclarator() throws SyntaxError {
	  parseDeclarator();
	  if(currentToken.kind == Token.EQ){
		  match(Token.EQ);
		  parseInitialiser();
	  }
  }
  void parseDeclarator() throws SyntaxError {
	  parseIdent();
	  if(currentToken.kind == Token.LBRACKET){
		  match(Token.LBRACKET);
		  if(currentToken.kind == Token.INTLITERAL){
			  parseIntLiteral();
		  	  match(Token.RBRACKET);
		  }else{
			  match(Token.RBRACKET);  
		  }
	  }  
  }
  void parseInitialiser() throws SyntaxError {
	  if(currentToken.kind == Token.LCURLY){
		  match(Token.LCURLY);
		  parseProperArgList();
		  match(Token.RCURLY);
	  }else{
		  //System.out.println("Epression begin");
		  parseExpr();
	  }
	  
  }
// =======================  Type ====================================
  void parseType() throws SyntaxError {
	  switch(currentToken.kind){
	  case Token.VOID:
		  match(Token.VOID);
		  break;
	  case Token.BOOLEAN:
		  match(Token.BOOLEAN);
		  break;
	  case Token.INT:
		  match(Token.INT);
		  break;
	  case Token.FLOAT:
		  match(Token.FLOAT);
		  break;
	  default:
		  //Should error Here?
		  //syntacticError("primitive types expected here", "");
		  syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
		  break;
	  }
  }
// ======================= STATEMENTS ==============================


  void parseCompoundStmt() throws SyntaxError {

    match(Token.LCURLY);   
    parseVarDeclList();
    parseStmtList();    
    match(Token.RCURLY);
  }

 // Here, a new nontermial has been introduced to define { stmt } *
  void parseStmtList() throws SyntaxError {

    while (currentToken.kind != Token.RCURLY) 
      parseStmt();
  }

  void parseVarDeclList() throws SyntaxError {

	    while (currentToken.kind == Token.VOID || currentToken.kind == Token.BOOLEAN
	    		|| currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT){
	    	//System.out.println("Var-decl list");
	    	parseType();
	    	parseIdent();
	    	parseVarDecl();
	    }
	  }
  void parseStmt() throws SyntaxError {
	  //Not finish
    switch (currentToken.kind) {
    case Token.LCURLY:
    	parseCompoundStmt();
    	break;
    case Token.IF:
    	parseIfStmt();
    	break;
    case Token.FOR:
    	parseForStmt();
    	break;
    case Token.WHILE:
    	parseWhileStmt();
    	break;
    case Token.BREAK:
    	parseBreakStmt();
    	break;
    case Token.CONTINUE:
    	parseContinueStmt();
    	break;
    case Token.RETURN:
    	parseReturnStmt();
    	break;
    default:
    	parseExprStmt();
		break;

    }
  }
  //If-stmt
  void parseIfStmt() throws SyntaxError {

	  match(Token.IF);
	  match(Token.LPAREN);
	  parseExpr();
	  match(Token.RPAREN);
	  parseStmt();
	  if(currentToken.kind == Token.ELSE){
		  match(Token.ELSE);
		  parseStmt();
	  }
  }
  //For-stmt
  void parseForStmt() throws SyntaxError {
	  match(Token.FOR);
	  match(Token.LPAREN);
	  if (currentToken.kind == Token.ID
		        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
		        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
		        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
		        || currentToken.kind == Token.LPAREN) {
		        parseExpr();
	  }
	  match(Token.SEMICOLON);
	  if (currentToken.kind == Token.ID
		        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
		        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
		        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
		        || currentToken.kind == Token.LPAREN) {
		        parseExpr();
	  }
	  match(Token.SEMICOLON);
	  if (currentToken.kind == Token.ID
		        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
		        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
		        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
		        || currentToken.kind == Token.LPAREN) {
		        parseExpr();
	  }
	  match(Token.RPAREN);
	  parseStmt();
  }
  //While-stmt
  void parseWhileStmt() throws SyntaxError {
	  
	  match(Token.WHILE);
	  match(Token.LPAREN);
	  parseExpr();
	  match(Token.RPAREN);
	  parseStmt();
  }
  //Break-stmt
  void parseBreakStmt () throws SyntaxError {
	  
	  match(Token.BREAK);
	  match(Token.SEMICOLON);
  }
 //Continue-stmt
  void parseContinueStmt() throws SyntaxError {

    match(Token.CONTINUE);
    match(Token.SEMICOLON);

  }
  //Return-stmt
  void parseReturnStmt() throws SyntaxError{
	  
	  match(Token.RETURN);
	  if (currentToken.kind == Token.ID
		        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
		        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
		        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
		        || currentToken.kind == Token.LPAREN) {
		        parseExpr();
	    } 
	  match(Token.SEMICOLON);
	  
  }
  //Expr-stmt Check ????????????????????????????????????????
  void parseExprStmt() throws SyntaxError {
	  //Logic needs to be checked
    if (currentToken.kind == Token.ID
        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
        || currentToken.kind == Token.LPAREN) {
        parseExpr();
        match(Token.SEMICOLON);
    } else {
      match(Token.SEMICOLON);
    }
  }


// ======================= IDENTIFIERS ======================

 // Call parseIdent rather than match(Token.ID). 
 // In Assignment 3, an Identifier node will be constructed in here.


  void parseIdent() throws SyntaxError {

    if (currentToken.kind == Token.ID) {
      currentToken = scanner.getToken();
    } else 
      syntacticError("identifier expected here", "");
  }

// ======================= OPERATORS ======================

 // Call acceptOperator rather than accept(). 
 // In Assignment 3, an Operator Node will be constructed in here.

  void acceptOperator() throws SyntaxError {

    currentToken = scanner.getToken();
  }


// ======================= EXPRESSIONS ======================

  void parseExpr() throws SyntaxError {
    parseAssignExpr();
  }


  void parseAssignExpr() throws SyntaxError {

    //parseAdditiveExpr();
	  parseCondORExpr();
	  while(currentToken.kind == Token.EQ){
		  acceptOperator();
		  parseCondORExpr();
	  }
  }
  void parseCondORExpr() throws SyntaxError {
	  parseCondAndExpr();
	  while(currentToken.kind == Token.OROR){
		  acceptOperator();
		  parseCondAndExpr();
	  }
  }
  void parseCondAndExpr() throws SyntaxError {
	  parseEqualityExpr();
	  while(currentToken.kind == Token.ANDAND){
		  acceptOperator();
		  parseEqualityExpr();
	  }
  }
  void parseEqualityExpr()throws SyntaxError {
	  parseRelExpr();
	  while(currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ){
		  acceptOperator();
		  parseRelExpr();
	  }
  }
  void parseRelExpr() throws SyntaxError {
	  parseAdditiveExpr();
	  while(currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ
			  || currentToken.kind == Token.GT ||currentToken.kind == Token.GTEQ){
		  acceptOperator();
		  parseAdditiveExpr();
	  }
  }
  void parseAdditiveExpr() throws SyntaxError {

    parseMultiplicativeExpr();
    while (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS) {
      acceptOperator();
      parseMultiplicativeExpr();
    }
  }

  void parseMultiplicativeExpr() throws SyntaxError {

    parseUnaryExpr();
    while (currentToken.kind == Token.MULT || currentToken.kind == Token.DIV) {
      acceptOperator();
      parseUnaryExpr();
    }
  }

  void parseUnaryExpr() throws SyntaxError {

    switch (currentToken.kind) {
      case Token.MINUS:
        {
          acceptOperator();
          parseUnaryExpr();
        }
        break;
      case Token.PLUS:
    	  acceptOperator();
    	  parseUnaryExpr();
    	  break;
      case Token.NOT:
    	  acceptOperator();
    	  parseUnaryExpr();
    	  break;
      default:
        parsePrimaryExpr();
        break;
       
    }
  }

  void parsePrimaryExpr() throws SyntaxError {

    switch (currentToken.kind) {

      case Token.ID:
        parseIdent();
        if(currentToken.kind == Token.LBRACKET){
        	match(Token.LBRACKET);
        	parseExpr();
        	match(Token.RBRACKET);
        }else if(currentToken.kind == Token.LPAREN){
        	parseArgList();
        }
        break;
      case Token.LPAREN:
		{
		  accept();
		  parseExpr();
		  match(Token.RPAREN);
		}
		break;
	  case Token.BOOLEANLITERAL:
		  parseBooleanLiteral();
		  break;
	  case Token.FLOATLITERAL:
		  parseFloatLiteral();
		  break;
      case Token.INTLITERAL:
    	  parseIntLiteral();
    	  break;
      case Token.STRINGLITERAL:
    	  parseStringLiteral();
    	  break;
      default:
        syntacticError("illegal parimary expression", currentToken.spelling);
        break;
       
    }
  }
// =========================  void ============================
  void parseVoid() throws SyntaxError {
	  if (currentToken.kind == Token.VOID) {
	      currentToken = scanner.getToken();
	    } else 
	      syntacticError("void type expected here", "");
  }
// ========================== LITERALS ========================

  // Call these methods rather than accept().  In Assignment 3, 
  // literal AST nodes will be constructed inside these methods. 

  void parseIntLiteral() throws SyntaxError {

    if (currentToken.kind == Token.INTLITERAL) {
      currentToken = scanner.getToken();
    } else 
      syntacticError("integer literal expected here", "");
  }

  void parseFloatLiteral() throws SyntaxError {

    if (currentToken.kind == Token.FLOATLITERAL) {
      currentToken = scanner.getToken();
    } else 
      syntacticError("float literal expected here", "");
  }

  void parseBooleanLiteral() throws SyntaxError {

    if (currentToken.kind == Token.BOOLEANLITERAL) {
      currentToken = scanner.getToken();
    } else 
      syntacticError("boolean literal expected here", "");
  }
  void parseStringLiteral() throws SyntaxError {
	  if (currentToken.kind == Token.STRINGLITERAL) {
	      currentToken = scanner.getToken();
	    } else 
	      syntacticError("string literal expected here", "");
  }
//=========================== parameters =======================
  void parseParaList() throws SyntaxError {
	  match(Token.LPAREN);
	  if(currentToken.kind == Token.VOID || currentToken.kind == Token.BOOLEAN
	    		|| currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT){
		  parseProperParaList();
	  }
	  match(Token.RPAREN);
  }
  void parseProperParaList() throws SyntaxError {
	  parseParaDecl();
	  while(currentToken.kind == Token.COMMA){
		  match(Token.COMMA);
		  parseParaDecl();
	  }
  }
  void parseParaDecl() throws SyntaxError {
	  parseType();
	  parseDeclarator();
  }
  void parseArgList() throws SyntaxError {
	  match(Token.LPAREN);
	  if (currentToken.kind == Token.ID
		        || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.BOOLEANLITERAL 
		        || currentToken.kind == Token.STRINGLITERAL || currentToken.kind == Token.FLOATLITERAL
		        || currentToken.kind == Token.MINUS || currentToken.kind == Token.PLUS ||  currentToken.kind == Token.NOT
		        || currentToken.kind == Token.LPAREN){
		  parseProperArgList();
	  }		  
	  match(Token.RPAREN);  
  }

  void parseProperArgList() throws SyntaxError {
	  parseExpr();
	  while(currentToken.kind == Token.COMMA){
		  match(Token.COMMA);
		  parseExpr();
	  }
  }
 
}
