/**
 * Checker.java   
 * Sun Apr 24 15:57:55 AEST 2016
 **/

package VC.Checker;

import VC.ASTs.*;
import VC.Scanner.SourcePosition;
import VC.ErrorReporter;
import VC.StdEnvironment;

public final class Checker implements Visitor {

  private String errMesg[] = {
    "*0: main function is missing",                            
    "*1: return type of main is not int",                    

    // defined occurrences of identifiers
    // for global, local and parameters
    "*2: identifier redeclared",                             
    "*3: identifier declared void",                         
    "*4: identifier declared void[]",                      

    // applied occurrences of identifiers
    "*5: identifier undeclared",                          

    // assignments
    "*6: incompatible type for =",                       
    "*7: invalid lvalue in assignment",                 

     // types for expressions 
    "*8: incompatible type for return",                
    "*9: incompatible type for this binary operator", 
    "*10: incompatible type for this unary operator",

     // scalars
     "*11: attempt to use an array/function as a scalar", 

     // arrays
     "*12: attempt to use a scalar/function as an array",
     "*13: wrong type for element in array initialiser",
     "*14: invalid initialiser: array initialiser for scalar",   
     "*15: invalid initialiser: scalar initialiser for array",  
     "*16: excess elements in array initialiser",              
     "*17: array subscript is not an integer",                
     "*18: array size missing",                              

     // functions
     "*19: attempt to reference a scalar/array as a function",

     // conditional expressions in if, for and while
    "*20: if conditional is not boolean",                    
    "*21: for conditional is not boolean",                  
    "*22: while conditional is not boolean",               

    // break and continue
    "*23: break must be in a while/for",                  
    "*24: continue must be in a while/for",              

    // parameters 
    "*25: too many actual parameters",                  
    "*26: too few actual parameters",                  
    "*27: wrong type for actual parameter",           

    // reserved for errors that I may have missed (J. Xue)
    "*28: misc 1",
    "*29: misc 2",

    // the following two checks are optional 
    "*30: statement(s) not reached",     
    "*31: missing return statement",    
  };


  private SymbolTable idTable;
  private static SourcePosition dummyPos = new SourcePosition();
  private ErrorReporter reporter;
  private boolean func_return_mark;
  // Checks whether the source program, represented by its AST, 
  // satisfies the language's scope rules and type rules.
  // Also decorates the AST as follows:
  //  (1) Each applied occurrence of an identifier is linked to
  //      the corresponding declaration of that identifier.
  //  (2) Each expression and variable is decorated by its type.

  public Checker (ErrorReporter reporter) {
    this.reporter = reporter;
    this.idTable = new SymbolTable ();
    establishStdEnvironment();
    func_return_mark = false;
  }

  public void check(AST ast) {
    ast.visit(this, null);
  }


  // auxiliary methods

  private void declareVariable(Ident ident, Decl decl) {
    IdEntry entry = idTable.retrieveOneLevel(ident.spelling);

    if (entry == null) {
      ; // no problem
    } else
      reporter.reportError(errMesg[2] + ": %", ident.spelling, ident.position);
    idTable.insert(ident.spelling, decl);
  }

// =========================== Program =============================================
  // Programs -> Fucdecl / Globalvar /Localvar

  public Object visitProgram(Program ast, Object o) {
	
    ast.FL.visit(this, null);
    Decl check_main = idTable.retrieve("main");
    //Need check null-pointer
    if(check_main != null){
    	//main should be int type
    	if(check_main.isFuncDecl()){
    		if(!((FuncDecl)check_main).T.isIntType()){
        		reporter.reportError(errMesg[1], "", ast.position);
        	}
    	}else{
        	reporter.reportError(errMesg[0], "", ast.position);
    	} 	
    }else{
    	reporter.reportError(errMesg[0], "", ast.position);
    }
    return null;
  }

  //========================== Declarations============================================

  // Always returns null. Does not use the given object.

  public Object visitFuncDecl(FuncDecl ast, Object o) {
	  //Check duplicate defined function
      IdEntry check_duplicate = idTable.retrieveOneLevel(ast.I.spelling);
	  if(check_duplicate != null){
		  reporter.reportError(errMesg[2]+": %", ast.I.spelling, ast.position);
	  }
      idTable.insert (ast.I.spelling, ast); 
     // System.out.println("function name: "+ ast.I.spelling);
    // HINT
    // Pass ast as the 2nd argument (as done below) so that the
    // formal parameters of the function can be extracted from ast when the
    // function body is later 


    ast.S.visit(this, ast);
    //Check void and return  //main function can be without return things
    if(!ast.T.isVoidType() && !func_return_mark && !ast.I.spelling.equals("main")){
    	reporter.reportError(errMesg[31]+": %", ast.I.spelling, ast.position);
    }
    func_return_mark = false;
    return null;
  }

  public Object visitDeclList(DeclList ast, Object o) {
    ast.D.visit(this, null);
    ast.DL.visit(this, null);
    return null;
  }

  public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
    return null;
  }

  public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
    declareVariable(ast.I, ast);
    //Type -> boolean, int, float, boolean[], int[], float[]
    if(ast.T.isVoidType()){
    	reporter.reportError(errMesg[3]+": %", ast.I.spelling, ast.position);
    }
    if(ast.T.isArrayType()){
    	if(((ArrayType) ast.T).T.isVoidType()){
    		reporter.reportError(errMesg[4]+": %", ast.I.spelling, ast.position);
    	}
    	//must be specified length unless specifies an initial value
    	if(!(ast.E instanceof InitExpr) && ((ArrayType) ast.T).E.isEmptyExpr()){
    		reporter.reportError(errMesg[18]+": %", ast.I.spelling, ast.position);	  		
    	}
    }
    Object r = ast.E.visit(this, ast.T);
    if(ast.T.isArrayType()){
    	if(ast.E instanceof InitExpr){
    		Integer size = (Integer)r;
    		if(((ArrayType) ast.T).E.isEmptyExpr()){
    			((ArrayType) ast.T).E = new IntExpr(new IntLiteral(size.toString(),dummyPos),dummyPos );
    		}else{
    			Integer d_size = Integer.parseInt(((IntExpr)((ArrayType) ast.T).E).IL.spelling);
    			if(d_size < size){
    				reporter.reportError(errMesg[16]+": %", ast.I.spelling, ast.E.position);
    			}
    		}
    	}else if(!ast.E.isEmptyExpr()){
    		reporter.reportError(errMesg[15]+": %", ast.I.spelling, ast.E.position);
    	}
    }else{
    	if(ast.T.assignable(ast.E.type)){
    		if(!ast.T.equals(ast.E.type)){
    			ast.E = i2f(ast.E);
    		}
    	}else{
    		reporter.reportError(errMesg[6], "", ast.E.position);
    	}
    }
    return null; 
  }

  public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
    declareVariable(ast.I, ast);
    if(ast.T.isVoidType()){
    	reporter.reportError(errMesg[3]+": %", ast.I.spelling, ast.position);
    }
    if(ast.T.isArrayType()){
    	if(((ArrayType) ast.T).T.isVoidType()){
    		reporter.reportError(errMesg[4]+": %", ast.I.spelling, ast.position);
    	}
    	//must be specified length unless specifies an initial valu
    	if(!(ast.E instanceof InitExpr) && ((ArrayType) ast.T).E.isEmptyExpr()){
    		reporter.reportError(errMesg[18]+": %", ast.I.spelling, ast.position);	  		
    	}
    }
    Object r = ast.E.visit(this, ast.T);
    if(ast.T.isArrayType()){
    	if(ast.E instanceof InitExpr){
    		Integer size = (Integer)r;
    		if(((ArrayType) ast.T).E.isEmptyExpr()){
    			((ArrayType) ast.T).E = new IntExpr(new IntLiteral(size.toString(),dummyPos),dummyPos );
    		}else{
    			Integer d_size = Integer.parseInt(((IntExpr)((ArrayType) ast.T).E).IL.spelling);
    			if(d_size < size){
    				reporter.reportError(errMesg[16]+": %", ast.I.spelling, ast.E.position);
    			}
    		}
    	}else if(!ast.E.isEmptyExpr()){
    		reporter.reportError(errMesg[15]+": %", ast.I.spelling, ast.E.position);
    	}
    }else{
    	if(ast.T.assignable(ast.E.type)){
    		if(!ast.T.equals(ast.E.type)){
    			ast.E = i2f(ast.E);
    		}
    	}else{
    		reporter.reportError(errMesg[6], "", ast.E.position);
    	}
    }
    return null ;//need change
  }
  //======================= Statements =======================================

  public Object visitCompoundStmt(CompoundStmt ast, Object o) {
    idTable.openScope();
    // visit parameter list of a function here, because the scope of parameter is plus 1
    if(o instanceof FuncDecl){
    	((FuncDecl) o).PL.visit(this, null);
    }
    ast.DL.visit(this, null);
    ast.SL.visit(this, o);
    idTable.closeScope();
    return null;
  }

  public Object visitStmtList(StmtList ast, Object o) {
    ast.S.visit(this, o);
    if (ast.S instanceof ReturnStmt && ast.SL instanceof StmtList)
      reporter.reportError(errMesg[30], "", ast.SL.position);
    ast.SL.visit(this, o);
    return null;
  }

	
	/****   statements     ****/
	@Override //if-statement
	public Object visitIfStmt(IfStmt ast, Object o) {
	
		Type condition_type = (Type) ast.E.visit(this, null);
		//condition may need change here ?null
		if(condition_type == null){
			reporter.reportError(errMesg[20]+"(found: null)", "", ast.E.position);
		}else{ 
			if(!condition_type.isBooleanType()){
				reporter.reportError(errMesg[20]+"(found: %)", condition_type.toString(), ast.E.position);
			}
		}
		ast.S1.visit(this, o);
		ast.S2.visit(this, o);
		return null;
	}
	
	@Override //for-statement
	public Object visitForStmt(ForStmt ast, Object o) {
		ast.E1.visit(this, null);
		Type condition_type = (Type) ast.E2.visit(this,null);
		if(condition_type == null){
			reporter.reportError(errMesg[21]+"(found: null)", "", ast.E2.position);
		}else{
			if(!condition_type.isBooleanType() && !ast.E2.isEmptyExpr()){
				reporter.reportError(errMesg[21]+"(found: %)", condition_type.toString(), ast.E2.position);
			}
		}
		
		ast.E3.visit(this,null);
		ast.S.visit(this, o);
		return null;
	}
	
	@Override //while-statement
	public Object visitWhileStmt(WhileStmt ast, Object o) {
		Type condition_type = (Type) ast.E.visit(this, null);
		if(condition_type == null){
			reporter.reportError(errMesg[22]+"(found: null)", "", ast.position);
		}else{
			if( !condition_type.isBooleanType()){
				reporter.reportError(errMesg[22]+"(found: %)", condition_type.toString(), ast.position);
			}
		}
		
		ast.S.visit(this, o);
		return null;
	}
	
	public boolean isInWhileFor(AST ast){
		//Check an object whether is in the while or for
		boolean result = false;
		while(ast != null){
			if(ast instanceof WhileStmt || ast instanceof ForStmt){
				result = true;
				break;
			}else{
				ast = ast.parent;
			}
		}
		return result;
	}
	
	@Override //Break-statement
	public Object visitBreakStmt(BreakStmt ast, Object o) {
		boolean in = isInWhileFor(ast.parent);
		if(!in){
			reporter.reportError(errMesg[23], "", ast.position);
		}
		return null;
	}
	
	@Override //Continue-Stament
	public Object visitContinueStmt(ContinueStmt ast, Object o) {
		boolean in = isInWhileFor(ast.parent);
		if(!in){
			reporter.reportError(errMesg[24], "", ast.position);
		}
		return null;
	}
	
	@Override //Return-Statement
	public Object visitReturnStmt(ReturnStmt ast, Object o) {
		Type funcType = ((FuncDecl) o).T;
		Type returnType = (Type) ast.E.visit(this, null);
		func_return_mark = true;
		if((!funcType.isVoidType() && ast.E.isEmptyExpr()) || (funcType.isVoidType() && !ast.E.isEmptyExpr())){
			reporter.reportError(errMesg[8], "", ast.position);
		}else if(!funcType.isVoidType() && !ast.E.isEmptyExpr()){
			if(funcType.assignable(returnType)){
				if(!funcType.equals(returnType)){
					ast.E = i2f(ast.E);
				}
			}else{
				reporter.reportError(errMesg[8], "", ast.position);
			}
		}
		//what about if return doesn't exist
		return null;
	}
	
  public Object visitExprStmt(ExprStmt ast, Object o) {
    ast.E.visit(this, o);
    return null;
  }

  public Object visitEmptyStmt(EmptyStmt ast, Object o) {
    return null;
  }

  public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
    return null;
  }

  public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
  	return null;
  }
  //================================ Expressions =======================================

  // Returns the Type denoting the type of the expression. Does
  // not use the given object.
  public Expr i2f(Expr expr){
	  Expr new_expr = new UnaryExpr(new Operator("i2f", expr.position), expr, expr.position);
	  new_expr.type = StdEnvironment.floatType;
	  new_expr.parent = expr.parent;
	  expr.parent = new_expr;
	  return new_expr;
  }
  //initialiser expression List->{expr, expr,...}, this must be after array type.
  public Object visitInitExpr(InitExpr ast, Object o) {
		if(!((Type) o).isArrayType()){
			reporter.reportError(errMesg[14], "", ast.position);
			ast.type = StdEnvironment.errorType;
			return ast.type;
		}
		// pass array type to each expr in initialiser.
		ast.type = (Type) o;
		return ast.IL.visit(this, ((ArrayType) ast.type).T);
	}
	
	
	public Object visitExprList(ExprList ast, Object o) {
		Type oType = (Type) o;
		ast.E.visit(this, null);
		if(oType.assignable(ast.E.type)){
			if(!oType.equals(ast.E.type)){
				ast.E = i2f(ast.E);
			}
		}else{
			reporter.reportError(errMesg[13]+": at position %", Integer.toString(ast.index), ast.E.position);
		}
		Integer count = null;
		if(ast.EL.isEmpty()){
			count = new Integer(ast.index+1);
		}else{
			((ExprList) ast.EL).index = ast.index +1;
			count = (Integer) ast.EL.visit(this, o);
		}
		return count;
	}

	public Object visitEmptyExprList(EmptyExprList ast, Object o) {
		return null;
	}

	public Object visitUnaryExpr(UnaryExpr ast, Object o) {
		Type eType = (Type) ast.E.visit(this, null);
		String operater = ast.O.spelling;
		if(operater.equals("+") || operater.equals("-")){
			if(eType.isIntType() || eType.isFloatType()){
				ast.type = eType;
			}else{
				reporter.reportError(errMesg[10]+": %", ast.O.spelling, ast.E.position);
				ast.type = StdEnvironment.errorType;
			}
		}else if(operater.equals("!")){
			if(eType.isBooleanType()){
				ast.O.spelling = "i" + ast.O.spelling;
				ast.type = eType;
			}else{
				reporter.reportError(errMesg[10]+": %", ast.O.spelling, ast.E.position);
				ast.type = StdEnvironment.errorType;
			}
		}
		
		if(ast.type.isBooleanType()){
			ast.O.spelling = "i" + ast.O.spelling;
		}else if(ast.type.isFloatType()){
			ast.O.spelling = "f" + ast.O.spelling;
		}
		return ast.type;
	}
	
	@Override
	public Object visitBinaryExpr(BinaryExpr ast, Object o) {
		Type e1Type = (Type) ast.E1.visit(this, null);
		Type e2Type = (Type) ast.E2.visit(this, null);
		//System.out.println("e1Type is: "+ e1Type.toString());
		//System.out.println("e2Type is: "+e2Type.toString());
		String operater = ast.O.spelling;
		if(e1Type.isIntType() && e2Type.isFloatType()){
			ast.E1 = i2f(ast.E1);
			ast.type = StdEnvironment.floatType;
		}else if(e2Type.isIntType() && e1Type.isFloatType()){
			ast.E2 = i2f(ast.E2);
			ast.type = StdEnvironment.floatType;
		}else if(e1Type.equals(e2Type) && !e1Type.isErrorType() && !e2Type.isErrorType()){
			ast.type = e1Type;
		}else if(e1Type.isErrorType() && e2Type.isErrorType()){
			ast.type = e1Type;
		}else{
			reporter.reportError(errMesg[9]+": %", ast.O.spelling, ast.O.position);
			ast.type = StdEnvironment.errorType;
		}
	
		if(!ast.type.isErrorType()){
			if(operater.equals("&&") || operater.equals("||") || operater.equals("!")){
				//Only boolean is allowed in this case
				if(ast.type.isBooleanType()){
					ast.O.spelling = "i" + ast.O.spelling;
				}else{
					reporter.reportError(errMesg[9]+": %", ast.O.spelling, ast.O.position);
					ast.type = StdEnvironment.errorType;
				}
			}
			if(operater.equals("+") || operater.equals("-") || operater.equals("*")||operater.equals("/")){
				if(ast.type.isIntType()){
					ast.O.spelling = "i" + ast.O.spelling;
				}else if(ast.type.isFloatType()){
					ast.O.spelling = "f" + ast.O.spelling;
				}else{
					reporter.reportError(errMesg[9]+": %", ast.O.spelling, ast.O.position);
					ast.type = StdEnvironment.errorType;
				}
			}
			if(operater.equals("<")|operater.equals("<=") ||operater.equals(">")|operater.equals(">=")){
				if(ast.type.isIntType()){
					ast.O.spelling = "i" + ast.O.spelling;
					ast.type = StdEnvironment.booleanType;
				}else if(ast.type.isFloatType()){
					ast.O.spelling = "f" + ast.O.spelling;
					ast.type = StdEnvironment.booleanType;
				}else{
					reporter.reportError(errMesg[9]+": %", ast.O.spelling, ast.O.position);
					ast.type = StdEnvironment.errorType;
				}
			}
			if(operater.equals("==") || operater.equals("!=")){
				if(ast.type.isIntType() || ast.type.isBooleanType()){
					ast.O.spelling = "i" + ast.O.spelling;
					ast.type = StdEnvironment.booleanType;
				}else if(ast.type.isFloatType()){
					ast.O.spelling = "f" + ast.O.spelling;
					ast.type = StdEnvironment.booleanType;
				}else{
					reporter.reportError(errMesg[9]+": %", ast.O.spelling, ast.O.position);
					ast.type = StdEnvironment.errorType;
				}
			}
		}
		
		return ast.type;
	}
	

	public Object visitSimpleVar(SimpleVar ast, Object o) {
		Decl ident = idTable.retrieve(ast.I.spelling);
		//Not declare
		if(ident == null){
			reporter.reportError(errMesg[5]+": %", ast.I.spelling, ast.I.position);
			ast.type = StdEnvironment.errorType;
		}else if(ident.isFuncDecl()){
			reporter.reportError(errMesg[11]+": %", ast.I.spelling, ast.I.position);
			ast.type = StdEnvironment.errorType;
		}else{
			ast.type = ident.T;
		}
		//should be an actual argument
		if(ast.type.isArrayType() && ast.parent instanceof VarExpr && !(ast.parent.parent instanceof Arg)){
			reporter.reportError(errMesg[11]+": %", ast.I.spelling, ast.position);
			ast.type = StdEnvironment.errorType;
		}
		return ast.type;
	}
	
	// array expr -> a[expr]; ArrayExpr(Var, Expr)
	public Object visitArrayExpr(ArrayExpr ast, Object o) {
		//System.out.println("Array Expression: _____________"+);
		Type idType = (Type) ast.V.visit(this, null);
		
		//check array type
		if(idType.isArrayType()){
			ast.type = ((ArrayType) idType).T;
		}else{
			reporter.reportError(errMesg[12], "", ast.V.position);
			ast.type = StdEnvironment.errorType;
		}
		// index should be int
		Type exprType = (Type) ast.E.visit(this,null);
		if(!exprType.isIntType()){
			reporter.reportError(errMesg[17], "", ast.position);
			ast.type = StdEnvironment.errorType;
		}
		return ast.type;
	}
	
	// call function -> func(a, b);
	public Object visitCallExpr(CallExpr ast, Object o) {
		Decl funcId = idTable.retrieve(ast.I.spelling);
		
		if(funcId == null){
			//function doesn't exist 
			reporter.reportError(errMesg[5]+": %", ast.I.spelling, ast.position);
			ast.type = StdEnvironment.errorType;
		}else if(funcId.isFuncDecl()){
			ast.AL.visit(this, ((FuncDecl) funcId).PL);
			ast.type = funcId.T;
		}else{
			//Ident is not a function 
			reporter.reportError(errMesg[19]+": %", ast.I.spelling, ast.position);
			ast.type = StdEnvironment.errorType;
		}
		return ast.type;
	}
	
	@Override
	public Object visitAssignExpr(AssignExpr ast, Object o) {
		ast.E1.visit(this, o);
		ast.E2.visit(this, o);
		//left value E1 cannot be a function
		if(ast.E1 instanceof VarExpr){
			Decl ident = idTable.retrieve(((SimpleVar)((VarExpr) ast.E1).V).I.spelling);
			if(ident != null){
				if(ident.isFuncDecl()){
					reporter.reportError(errMesg[7]+": %", ((SimpleVar)((VarExpr) ast.E1).V).I.spelling, ast.E1.position);
					ast.type = StdEnvironment.errorType;
				}
			}// The error of 'var undeclared' will be checked in SimpleVar, don't need report error here.
			
		}else if(!(ast.E1 instanceof ArrayExpr)){
			reporter.reportError(errMesg[7], "", ast.E1.position);
			ast.type = StdEnvironment.errorType;
		}
		//check E1.type = E2.type
		if(ast.E1.type.assignable(ast.E2.type)){
			if(!ast.E1.type.equals(ast.E2.type)){
				ast.E2 = i2f(ast.E2);
				//--------check ----------
				ast.type = StdEnvironment.floatType;
			}else{
				ast.type = ast.E1.type;
			}
		}else{
			reporter.reportError(errMesg[6], "", ast.E1.position);
			ast.type = StdEnvironment.errorType;
		}
		return ast.type;
	}
	
  //This one may need to change ---about return + void type function
  public Object visitEmptyExpr(EmptyExpr ast, Object o) {
    ast.type = StdEnvironment.errorType;
    return ast.type;
  }

  public Object visitBooleanExpr(BooleanExpr ast, Object o) {
    ast.type = StdEnvironment.booleanType;
    return ast.type;
  }

  public Object visitIntExpr(IntExpr ast, Object o) {
    ast.type = StdEnvironment.intType;
    return ast.type;
  }

  public Object visitFloatExpr(FloatExpr ast, Object o) {
    ast.type = StdEnvironment.floatType;
    return ast.type;
  }

  public Object visitStringExpr(StringExpr ast, Object o) {
    ast.type = StdEnvironment.stringType;
    return ast.type;
  }

  public Object visitVarExpr(VarExpr ast, Object o) {
    ast.type = (Type) ast.V.visit(this, null);
    return ast.type;
  }


  //===================== Parameters =======================

 // Always returns null. Does not use the given object.

  public Object visitParaList(ParaList ast, Object o) {
    ast.P.visit(this, null);
    ast.PL.visit(this, null);
    return null;
  }

  public Object visitParaDecl(ParaDecl ast, Object o) {
     declareVariable(ast.I, ast);

    if (ast.T.isVoidType()) {
      reporter.reportError(errMesg[3] + ": %", ast.I.spelling, ast.I.position);
    } else if (ast.T.isArrayType()) {
	     if (((ArrayType) ast.T).T.isVoidType())
	        reporter.reportError(errMesg[4] + ": %", ast.I.spelling, ast.I.position);
    }
    return null;
  }

  public Object visitEmptyParaList(EmptyParaList ast, Object o) {
    return null;
  }

  //============================== Arguments ==================================

  // Your visitor methods for arguments go here

  // Types 

  // Returns the type predefined in the standard environment. 

  public Object visitArgList(ArgList ast, Object o) {
  	List formal_list = (List) o;
  	if(formal_list.isEmptyParaList()){
  		reporter.reportError(errMesg[25], "", ast.position);
  	}else{
  		ast.A.visit(this, ((ParaList) formal_list).P);
  		ast.AL.visit(this, ((ParaList) formal_list).PL);
  	}
  	return null;
  }

  public Object visitEmptyArgList(EmptyArgList ast, Object o) {
	  List formal_list = (List) o;
	  if(!formal_list.isEmptyParaList()){
		  reporter.reportError(errMesg[26], "", ast.parent.position);
	  }
	  return null;
	}
  
  public Object visitArg(Arg ast, Object o) {
	
  	Type actual_type = (Type) ast.E.visit(this, null);
  	Type formal_type = ((Decl) o).T;
  	
	//ArrayType need to compare the type of array
	if(formal_type.isArrayType() && actual_type.isArrayType()){
  		if(!((ArrayType) formal_type).T.assignable(((ArrayType) actual_type).T)){
  			reporter.reportError(errMesg[27]+": %", ((Decl) o).I.spelling, ast.E.position);
  		}
  	}else if(!formal_type.isArrayType() && !actual_type.isArrayType()){
  		if(formal_type.assignable(actual_type)){
  			if(!formal_type.equals(actual_type)){
  	  			ast.E = i2f(ast.E);
  	  		}
  		}else{
  			reporter.reportError(errMesg[27]+": %", ((Decl) o).I.spelling, ast.E.position);
  		}
  	}else{
  		reporter.reportError(errMesg[27]+": %", ((Decl) o).I.spelling, ast.E.position);
  	}
  	
  	return null;
  }
  
  public Object visitErrorType(ErrorType ast, Object o) {
    return StdEnvironment.errorType;
  }

  public Object visitBooleanType(BooleanType ast, Object o) {
    return StdEnvironment.booleanType;
  }

  public Object visitIntType(IntType ast, Object o) {
    return StdEnvironment.intType;
  }

  public Object visitFloatType(FloatType ast, Object o) {
    return StdEnvironment.floatType;
  }

  public Object visitStringType(StringType ast, Object o) {
    return StdEnvironment.stringType;
  }

  public Object visitVoidType(VoidType ast, Object o) {
    return StdEnvironment.voidType;
  }
  public Object visitArrayType(ArrayType ast, Object o) {
  	return ast;
  }
  // Literals, Identifiers and Operators

  public Object visitIdent(Ident I, Object o) {
    Decl binding = idTable.retrieve(I.spelling);
    if (binding != null)
      I.decl = binding;
    return binding;
  }

  public Object visitBooleanLiteral(BooleanLiteral SL, Object o) {
    return StdEnvironment.booleanType;
  }

  public Object visitIntLiteral(IntLiteral IL, Object o) {
    return StdEnvironment.intType;
  }

  public Object visitFloatLiteral(FloatLiteral IL, Object o) {
    return StdEnvironment.floatType;
  }

  public Object visitStringLiteral(StringLiteral IL, Object o) {
    return StdEnvironment.stringType;
  }

  public Object visitOperator(Operator O, Object o) {
    return null;
  }

  // Creates a small AST to represent the "declaration" of each built-in
  // function, and enters it in the symbol table.

  private FuncDecl declareStdFunc (Type resultType, String id, List pl) {

    FuncDecl binding;

    binding = new FuncDecl(resultType, new Ident(id, dummyPos), pl, 
           new EmptyStmt(dummyPos), dummyPos);
    idTable.insert (id, binding);
    return binding;
  }

  // Creates small ASTs to represent "declarations" of all 
  // build-in functions.
  // Inserts these "declarations" into the symbol table.

  private final static Ident dummyI = new Ident("x", dummyPos);

  private void establishStdEnvironment () {

    // Define four primitive types
    // errorType is assigned to ill-typed expressions

    StdEnvironment.booleanType = new BooleanType(dummyPos);
    StdEnvironment.intType = new IntType(dummyPos);
    StdEnvironment.floatType = new FloatType(dummyPos);
    StdEnvironment.stringType = new StringType(dummyPos);
    StdEnvironment.voidType = new VoidType(dummyPos);
    StdEnvironment.errorType = new ErrorType(dummyPos);

    // enter into the declarations for built-in functions into the table

    StdEnvironment.getIntDecl = declareStdFunc( StdEnvironment.intType,
	"getInt", new EmptyParaList(dummyPos)); 
    StdEnvironment.putIntDecl = declareStdFunc( StdEnvironment.voidType,
	"putInt", new ParaList(
	new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 
    StdEnvironment.putIntLnDecl = declareStdFunc( StdEnvironment.voidType,
	"putIntLn", new ParaList(
	new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 
    StdEnvironment.getFloatDecl = declareStdFunc( StdEnvironment.floatType,
	"getFloat", new EmptyParaList(dummyPos)); 
    StdEnvironment.putFloatDecl = declareStdFunc( StdEnvironment.voidType,
	"putFloat", new ParaList(
	new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 
    StdEnvironment.putFloatLnDecl = declareStdFunc( StdEnvironment.voidType,
	"putFloatLn", new ParaList(
	new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 
    StdEnvironment.putBoolDecl = declareStdFunc( StdEnvironment.voidType,
	"putBool", new ParaList(
	new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 
    StdEnvironment.putBoolLnDecl = declareStdFunc( StdEnvironment.voidType,
	"putBoolLn", new ParaList(
	new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 

    StdEnvironment.putStringLnDecl = declareStdFunc( StdEnvironment.voidType,
	"putStringLn", new ParaList(
	new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 

    StdEnvironment.putStringDecl = declareStdFunc( StdEnvironment.voidType,
	"putString", new ParaList(
	new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
	new EmptyParaList(dummyPos), dummyPos)); 

    StdEnvironment.putLnDecl = declareStdFunc( StdEnvironment.voidType,
	"putLn", new EmptyParaList(dummyPos));

  }


}
