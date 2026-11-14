lexer grammar FormUrlEncodedLexer;

AMPERSAND: '&';
LINE_BREAK: '\r'? '\n' | '\r';
EQUALS: '=';
PLUS: '+';
TEXT: ~[&=+\r\n]+;
