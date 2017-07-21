/**
 **	Scanner.java                        
 **/

package VC.Scanner;

import VC.ErrorReporter;

public final class Scanner { 

  private SourceFile sourceFile;
  private boolean debug;

  private ErrorReporter errorReporter;
  private StringBuffer currentSpelling;
  private char currentChar;
  private SourcePosition sourcePos;
  private int cur_line, finish_line, cur_column_start, cur_column_finish;
  private int spell_len;
// =========================================================

  public Scanner(SourceFile source, ErrorReporter reporter) {
    sourceFile = source;
    errorReporter = reporter;
    currentChar = sourceFile.getNextChar();
    debug = false;
    //sourcePos = new SourcePosition(1, 1, 1);
    // you may initialise your counters for line and column numbers here
    cur_line = 1;
    finish_line =1;
    cur_column_start = 1;
    cur_column_finish = 1;
    spell_len = 0;
    sourcePos = new SourcePosition(1,1,1);
  }

  public void enableDebugging() {
    debug = true;
  }

  // accept gets the next character from the source program.

  private void accept() {
	currentSpelling.append(currentChar);
	
    currentChar = sourceFile.getNextChar();
    cur_column_finish = cur_column_finish+1;
  // you may save the lexeme of the current token incrementally here
  // you may also increment your line and column counters here
    
    
  }

  // inspectChar returns the n-th character after currentChar
  // in the input stream. 
  //
  // If there are fewer than nthChar characters between currentChar 
  // and the end of file marker, SourceFile.eof is returned.
  // 
  // Both currentChar and the current position in the input stream
  // are *not* changed. Therefore, a subsequent call to accept()
  // will always return the next char after currentChar.

  private char inspectChar(int nthChar) {
    return sourceFile.inspectChar(nthChar);
  }

  private int nextToken() {
  // Tokens: separators, operators, literals, identifiers and keyworods
      //????There is no '#'
    switch (currentChar) {
    // separators 
    case '{':
		accept();
		return Token.LCURLY;
    case '}':
    	accept();
    	return Token.RCURLY;
    case '(':
    	accept();
    	return Token.LPAREN;
    case ')':
    	accept();
    	return Token.RPAREN;
    case '[':
    	accept();
    	return Token.LBRACKET;
    case ']':
    	accept();
    	return Token.RBRACKET;
    case ';':
    	accept();
    	return Token.SEMICOLON;
    case ',':
    	accept();
    	return Token.COMMA;
    //operators ??????What if put 2 operatorts together?????? like ++ -- +- else
    case '+':
    	accept();
    	return Token.PLUS;
    case '-':
    	accept();
    	return Token.MINUS;
    case '*': //Consider this : comments or multiple ?????????????????
    	accept();
    	return Token.MULT;
    case '/':
    	accept();
    	return Token.DIV;
    case '!':
    	if(inspectChar(1) == '='){
    		accept();
    		accept();
    		return Token.NOTEQ;
    	}else{
    		accept();
        	return Token.NOT;
    	}	
    case '=':
    	if(inspectChar(1) == '='){
    		accept();
    		accept();
    		return Token.EQEQ;
    	}else{
    		accept();
        	return Token.EQ;
    	}	   	
    case '<':
    	if(inspectChar(1) == '='){
    		accept();
    		accept();
        	return Token.LTEQ;
    	}else{
    		accept();
        	return Token.LT;
    	}	   	
    case '>':
    	if(inspectChar(1) == '='){
    		accept();
    		accept();
        	return Token.GTEQ;
    	}else{
    		accept();
        	return Token.GT;
    	}
    case '&':
    	accept();
    	if (currentChar == '&') {
      		accept();
      		return Token.ANDAND;
      	} else {
      		return Token.ERROR;
        } 
    case '|':	
       	accept();
      	if (currentChar == '|') {
      		accept();
      		return Token.OROR;
      	} else {
      		return Token.ERROR;
        }
    case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
    	accept();
    	int flag = 0;
    	boolean escape = false;
    	while(!escape){
    		switch(currentChar){
    		case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
    			accept();
    			flag = 1;
    			break;
    		case '.':
    			accept();
    			//System.out.println("Accept '.' 1");
    			flag = 2;
    			boolean escape2 = false;
    			while(!escape2){
    				switch(currentChar){
        			case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
        				//System.out.println("Accept 'digit' :"+currentChar);
        				accept();
            			break;
        			case 'e':case 'E':
        				flag = exponent(flag);
            			escape2 = true;
            			escape = true;
            			break;
        			default:
        				escape2 = true;
            			escape = true;
            			break;
        			}
    			} 
    			break;
    		case 'e':case 'E':
    			flag = exponent(flag);
    			escape = true;
    			break;
    		default:
    			escape = true;
    			break;
    		}
    	}
    	//System.out.println("Flag is :"+flag);
    	if(flag == 0){
    		return Token.INTLITERAL;
    	}else if(flag == 1){
    		return Token.INTLITERAL;
    	}else if(flag == 2){
    		return Token.FLOATLITERAL;
    	}
    	  	
    case '.':
        //  attempting to recognise a float
		accept();
		escape = false;
		flag = 0;
		while(!escape){
			switch(currentChar){
			//case: .7899
			case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
				accept();
				flag = 1;
    			break;
			case 'e':case 'E':
				if(currentSpelling.length() <2){
					escape = true;
					break;
				}else{
					flag = exponent(flag);
					escape = true;
					break;
				}
			default:
    			escape = true;
    			break;
			}
		}
		if(flag == 1 || flag == 2){
			return Token.FLOATLITERAL;
		}else{
			return Token.ERROR;
		}

    case '"':
    	currentChar = sourceFile.getNextChar();
        cur_column_finish = cur_column_finish+1;
    	escape = false;
    	char temp;
    	//int flag_s = 1;
    	while(!escape){
    		//System.out.println("now is: "+currentChar);
    		switch(currentChar){
    		case '\\':
    			switch(inspectChar(1)){
    			// accept case: \b , \f, \n .....\\
    			case 'b':
    				temp = '\b';
    				currentSpelling.append(temp);
    				currentChar = sourceFile.getNextChar();
    			    currentChar = sourceFile.getNextChar();
    			    cur_column_finish = cur_column_finish+2;    				
    				break;   			
    			case 'f':
    				temp = '\f';
    				currentSpelling.append(temp);
    				currentChar = sourceFile.getNextChar();
    			    currentChar = sourceFile.getNextChar();
    			    cur_column_finish = cur_column_finish+2;    				
    				break;
    			case 'n':
    				temp = '\n';
    				currentSpelling.append(temp);
    				currentChar = sourceFile.getNextChar();
    			    currentChar = sourceFile.getNextChar();
    			    cur_column_finish = cur_column_finish+2;    				
    				break;
    			case 'r':
    				temp = '\r';
    				currentSpelling.append(temp);
    				currentChar = sourceFile.getNextChar();
    			    currentChar = sourceFile.getNextChar();
    			    cur_column_finish = cur_column_finish+2;    				
    				break;
    			case 't': 
    				temp = '\t';
    				currentSpelling.append(temp);
    				currentChar = sourceFile.getNextChar();
    			    currentChar = sourceFile.getNextChar();
    			    cur_column_finish = cur_column_finish+2;    				
    				break;
    			case '\'':
    				temp = '\'';
    				currentSpelling.append(temp);
    				currentChar = sourceFile.getNextChar();
    			    currentChar = sourceFile.getNextChar();
    			    cur_column_finish = cur_column_finish+2;    				
    				break;
    			case '\"':
    				temp = '\"';
    				currentSpelling.append(temp);
    				currentChar = sourceFile.getNextChar();
    			    currentChar = sourceFile.getNextChar();
    			    cur_column_finish = cur_column_finish+2;    				
    				break;	
    			case '\\':
    				temp = '\\';
    				currentSpelling.append(temp);
    				currentChar = sourceFile.getNextChar();
    			    currentChar = sourceFile.getNextChar();
    			    cur_column_finish = cur_column_finish+2;    				
    				break;
    			default:
    				accept();
    				String error = "\\"+ String.valueOf(currentChar);
    				sourcePos.lineStart = cur_line;
            		sourcePos.lineFinish = cur_line;
            		sourcePos.charStart = cur_column_start;
            		sourcePos.charFinish = cur_column_finish-1;
            		errorReporter.reportError(error+": illegal escape character", "", sourcePos);
    			}
    			break;
        	case '"':
        		currentChar = sourceFile.getNextChar();
        	    cur_column_finish = cur_column_finish+1;
    			escape = true;
    			return Token.STRINGLITERAL;
        	case SourceFile.eof:
        		escape = true;
        		sourcePos.lineStart = cur_line;
        		sourcePos.lineFinish = cur_line;
        		sourcePos.charStart = cur_column_start;
        		sourcePos.charFinish = cur_column_start;
        		errorReporter.reportError(currentSpelling+": unterminated string", "", sourcePos);
        		return Token.STRINGLITERAL;
        	case '\n':
        		escape = true;
        		sourcePos.lineStart = cur_line;
        		sourcePos.lineFinish = cur_line;
        		sourcePos.charStart = cur_column_start;
        		sourcePos.charFinish = cur_column_start;
        		errorReporter.reportError(currentSpelling+": unterminated string", "", sourcePos);
        		return Token.STRINGLITERAL;
        	default:
        		//System.out.println("now is: "+currentChar);
        		accept(); 
        		break;
        	}
    	}	
    	break;
    // ....
    case SourceFile.eof:	
		currentSpelling.append(Token.spell(Token.EOF));
		cur_column_finish = cur_column_start+1;
		return Token.EOF;
    default:
    	if(Character.isLetter(currentChar)|| currentChar == '_'){
    		accept();
    		while(Character.isLetterOrDigit(currentChar) || currentChar == '_') {
				accept();
			}
    		//Check keywords:  int float ....
    		for(int i = 0; i <= 10; i++) {
    			if(Token.spell(i).equals(currentSpelling)) {
    				return i;
    			}
    		}
    		//Check boolean-literal: true false
    		if(currentSpelling.toString().equals("true") || currentSpelling.toString().equals("false")) {
    			return Token.BOOLEANLITERAL;
    		} else {
    			return Token.ID;
    		}
    	}else{
    		//Other conditions which are error
    		break;
    	}
 	
    }

    accept(); 
    return Token.ERROR;
  }
  
  int exponent(int flag){
	  switch(inspectChar(1)){
		case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
			accept();
			boolean escape2 = false;
			while(!escape2){
				switch(currentChar){
				case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
					accept();
					break;
				default:
					escape2 = true;
					break;
				}
			}
			flag = 2;
			break;
		case '-':case '+':
			switch(inspectChar(2)){
			case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
				accept();
				accept();
				escape2 = false;
				while(!escape2){
					switch(currentChar){
					case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
						accept();
						break;
					default:
						escape2 = true;
						break;
					}
				}
				flag = 2;
				break;
			default:
				break;
			}
		default:
			break;
		}	
	  return flag;
  }
  void skipSpaceAndComments() {
	  //Comments case 1: /* */ case 2:// (case: /* .....? )(case: / .....)
	  //space: case 1: ' '; case 2: '\n'; case 3: '\t'
	  //flag = 1, case: /* .....
	  //flag = 0, new begin with any 
	  //flag = 2 terminal of comments
	  int flag = 0;
	  while(flag != 2  ){
		  switch (currentChar) {
		  case '/':
			  //System.out.println("Check / begin");
			  if(flag == 1){
				  currentChar = sourceFile.getNextChar();
				  cur_column_finish = cur_column_finish + 1;
			  }else{
				  switch(inspectChar(1)){
				  case '*':
					  //System.out.println("begin *: "+currentChar);
					  currentChar = sourceFile.getNextChar();
					  currentChar = sourceFile.getNextChar();
					  cur_column_finish = cur_column_finish + 2; 
					  while(currentChar != '*' && currentChar != '\u0000' && currentChar != '\n' && currentChar != '\t'){
						  
						  currentChar = sourceFile.getNextChar();
						  cur_column_finish = cur_column_finish + 1;
						  //System.out.println("inside begin *: "+currentChar);
						  
						  
					  }
					  //System.out.println("After *: "+cur_column);
					  flag = 1;
					  break;
				  case '/':
					  while(currentChar!='\n'&& currentChar != '\u0000'){
						  currentChar = sourceFile.getNextChar();
						  
					  }
					  break;
				  default:
					  flag = 2;
					  break;
				  }
			  }  
			  break;
		  case '\n':
			  //System.out.println("ii");
			  while(currentChar == '\n'){
				  currentChar = sourceFile.getNextChar();
				  if(flag == 1){
					  finish_line = finish_line+1;
					  cur_column_finish = 1;
				  }else{
					  cur_line = cur_line+1;
					  finish_line = cur_line;
					  cur_column_start = 1;
					  cur_column_finish = 1;
				  }
				  
			  } 

			  break;
		  case ' ':
			  while(currentChar == ' '){
				  currentChar = sourceFile.getNextChar();
				  cur_column_finish = cur_column_finish + 1;
				  if(flag != 1){
					  cur_column_start = cur_column_finish;
				  }
			  }
			  //System.out.println("This is end of space: "+currentChar);
			  break;
		  case '\t':
			  while(currentChar == '\t'){
				  currentChar = sourceFile.getNextChar();
				  cur_column_finish = cur_column_finish+8-cur_column_finish%8+1;  

				  if(flag != 1){
					  cur_column_start = cur_column_finish;
				  }
			  }
			  break;
		  case '*':
			  // case 1: */  case 2: *? ----------error if the end--------0
			  if(flag == 1){
				  if(inspectChar(1) == '/' ){
					  currentChar = sourceFile.getNextChar();
					  currentChar = sourceFile.getNextChar();
					  cur_column_finish = cur_column_finish + 2;
					  cur_line = finish_line;
					  //System.out.println(cur_column);
					  flag = 0;
				  }else{
					  currentChar = sourceFile.getNextChar();
					  cur_column_finish = cur_column_finish + 1;
				  }
			  }else{
				  flag = 2;
			  }		  
			  break;
		  case SourceFile.eof:
			  if(flag == 1){
				  //System.out.println("Error :"+currentChar);
				  //cur_line = finish_line;
				  sourcePos.lineStart = cur_line;
				  sourcePos.lineFinish = cur_line;
				  sourcePos.charStart = cur_column_start;
				  sourcePos.charFinish = cur_column_start;
				  errorReporter.reportError(": unterminated comment", "", sourcePos);
			  }
			  cur_line = finish_line;
			  flag = 2;
			  break;
		  default:
			  if(flag == 1){
				  currentChar = sourceFile.getNextChar();
				  cur_column_finish = cur_column_finish + 1;
			  }else{
				  flag = 2;
			  }
			  break;
		  }  
	  }

  }

  public Token getToken() {
    Token tok;
    int kind;
    //System.out.println("This is a new getToken process");
    //System.out.println("This is currentChar: "+currentChar);
    // skip white space and comments
    currentSpelling = new StringBuffer("");
   skipSpaceAndComments();
   cur_column_start = cur_column_finish;

   

   

   // You must record the position of the current token somehow
   
   
   
   kind = nextToken();
   
   sourcePos.lineStart = cur_line;
   sourcePos.lineFinish = cur_line;
   sourcePos.charStart = cur_column_start;
   sourcePos.charFinish = cur_column_finish-1;

   tok = new Token(kind, currentSpelling.toString(), sourcePos);
   spell_len = currentSpelling.length();
   cur_column_start = cur_column_finish;
   
   // * do not remove these three lines
   if (debug)
     System.out.println(tok);
   return tok;
   
   }
  
}
