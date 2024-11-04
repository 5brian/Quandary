package parser;

import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;

import interpreter.Interpreter;

%%

%public
%class Lexer
%cup
%implements sym
%char
%line
%column

%{
    StringBuffer string = new StringBuffer();
    public Lexer(java.io.Reader in, ComplexSymbolFactory sf){
	this(in);
	symbolFactory = sf;
    }
    ComplexSymbolFactory symbolFactory;

  private Symbol symbol(String name, int sym) {
      return symbolFactory.newSymbol(name, sym, new Location(yyline+1,yycolumn+1,yyline+1), new Location(yyline+1,yycolumn+yylength(),yycolumn+1));
  }
  
  private Symbol symbol(String name, int sym, Object val) {
      Location left = new Location(yyline + 1, yycolumn + 1, yyline + 1);
      Location right = new Location(yyline + 1, yycolumn + yylength(), yycolumn + 1);
      return symbolFactory.newSymbol(name, sym, left, right, val);
  } 
  /*private Symbol symbol(String name, int sym, Object val, int buflength) {
      Location left = new Location(yyline + 1, yycolumn + yylength() - buflength, yychar + yylength() - buflength);
      Location right = new Location(yyline + 1, yycolumn + yylength(), yychar + yylength());
      return symbolFactory.newSymbol(name, sym, left, right, val);
  }*/      
  private void error(String message) {
    System.out.println("Error at line "+ (yyline + 1) + ", column " + (yycolumn + 1) + " : " + message);
  }
%} 

%eofval{
     return symbolFactory.newSymbol("EOF", EOF, new Location(yyline + 1, yycolumn + 1, yychar), new Location(yyline + 1, yycolumn + 1, yychar + 1));
%eofval}


IntLiteral = 0 | [1-9][0-9]*

Identifier = [a-zA-Z_][a-zA-Z0-9_]*

new_line = \r|\n|\r\n;

white_space = {new_line} | [ \t\f]

%%

<YYINITIAL>{
/* This is where tokens are recognized. Every token recognized by the scanner corresponds to a terminal in the parser's grammar. */

/* int literal token */
{IntLiteral} { return symbol("Intconst", INTCONST, Long.parseLong(yytext())); }

/* other tokens (you can add more tokens here) */
"+"               { return symbol("+",  PLUS); }
"-"               { return symbol("-",  MINUS); }
"("               { return symbol("(",  LPAREN); }
")"               { return symbol(")",  RPAREN); }
"return"          { return symbol("return", RETURN); }
"*"               { return symbol("*",  TIMES); }
";"               { return symbol(";",  SEMICOLON); }
"="               { return symbol("=",  EQ); }
"{"               { return symbol("{",  LBRACE); }
"}"               { return symbol("}",  RBRACE); }
"if"              { return symbol("if", IF); }
"else"            { return symbol("else", ELSE); }
"while"           { return symbol("while", WHILE); }
"print"           { return symbol("print", PRINT); }
"int"             { return symbol("int", INT); }
"<="              { return symbol("<=", LEQ); }
">="              { return symbol(">=", GEQ); }
"=="              { return symbol("==", EQEQ); }
"!="              { return symbol("!=", NEQ); }
"<"               { return symbol("<",  LT); }
">"               { return symbol(">",  GT); }
"&&"              { return symbol("&&", AND); }
"||"              { return symbol("||", OR); }
"!"               { return symbol("!",  NOT); }
","               { return symbol(",", COMMA); }
"int"             { return symbol("int", INT); }
"Ref"             { return symbol("Ref", REF); }
"Q"               { return symbol("Q", Q); }
"nil"             { return symbol("nil", NIL); }
"mutable"         { return symbol("mutable", MUTABLE); }
"."               { return symbol(".",  DOT); }

{Identifier} { return symbol("IDENT", IDENT, yytext()); }

/* You shouldn't need to modify anything below this */

/* comments */
"/*" [^*] ~"*/" | "/*" "*"+ "/"
                  { /* ignore comments */ }

{white_space}     { /* ignore */ }

}

/* error fallback */
[^]               { /*error("Illegal character <" + yytext() + ">");*/ Interpreter.fatalError("Illegal character <" + yytext() + ">", Interpreter.EXIT_PARSING_ERROR); }
