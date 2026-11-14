/** Taken from "The Definitive ANTLR 4 Reference" by Terence Parr */

// Derived from https://json.org

grammar JSON;

json: value EOF;
obj: LBRACE pair (COMMA pair)* RBRACE | LBRACE RBRACE;
pair: STRING COLON value;
arr: LBRACKET value (COMMA value)* RBRACKET | LBRACKET RBRACKET;
value: STRING | NUMBER | obj | arr | BOOLEAN | NULL;

LBRACE: '{';
COMMA: ',';
RBRACE: '}';
COLON: ':';
LBRACKET: '[';
RBRACKET: ']';
BOOLEAN: 'true' | 'false';
NULL: 'null';

STRING: '"' (ESC | SAFECODEPOINT)* '"';
fragment ESC: '\\' (["\\/bfnrt] | UNICODE);
fragment UNICODE: 'u' HEX HEX HEX HEX;
fragment HEX: [0-9a-fA-F];
fragment SAFECODEPOINT: ~["\\\u0000-\u001F];
NUMBER: '-'? INT ('.' [0-9]+)? EXP?;
fragment INT: '0' | [1-9] [0-9]*;
fragment EXP: [Ee] [+-]? [0-9]+;
WS: [ \t\n\r]+ -> skip;
