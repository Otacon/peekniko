/** Taken from "The Definitive ANTLR 4 Reference" by Terence Parr */

// Derived from https://json.org

// Only the lexer: formatting and highlighting work from the tokens alone.
lexer grammar JSONLexer;

LBRACE: '{';
COMMA: ',';
RBRACE: '}';
COLON: ':';
LBRACKET: '[';
RBRACKET: ']';
BOOLEAN: 'true' | 'false';
NULL: 'null';

// An object's key: a string followed by a colon. The lexer takes the longest match, so every string followed by a
// colon lexes as a KEY, and STRING is only ever a value. The token ends with the colon: the key itself is everything
// up to its last quote.
KEY: STRING_LITERAL [ \t\n\r]* ':';
STRING: STRING_LITERAL;
fragment STRING_LITERAL: '"' (ESC | SAFECODEPOINT)* '"';
fragment ESC: '\\' (["\\/bfnrt] | UNICODE);
fragment UNICODE: 'u' HEX HEX HEX HEX;
fragment HEX: [0-9a-fA-F];
fragment SAFECODEPOINT: ~["\\\u0000-\u001F];
NUMBER: '-'? INT ('.' [0-9]+)? EXP?;
fragment INT: '0' | [1-9] [0-9]*;
fragment EXP: [Ee] [+-]? [0-9]+;
WS: [ \t\n\r]+ -> skip;
