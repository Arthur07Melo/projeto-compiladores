grammar Expr;

prog: expr EOF;
expr: '(' NESTED_EXPR=expr ')'                          #Parenteses
    | O1=expr OP=('*'|'/') O2=expr                      #MulDiv
    | O1=expr OP=('+'|'-') O2=expr                      #SomaSub
    | SINAL=('+'|'-')? NUMBER                           #Numero
    | SINAL=('+'|'-')? ID                               #UsoVariavel
    | 'let' listaDeclaracao '->' expr                   #DeclVariavel
    | 'loop' N=expr '{' CODE=expr '}' '->' OUT=expr     #Loop
    | ID '=' expr                                        #Atribuicao
    | 'ask'                                             #Input
;

listaDeclaracao: declaracao (',' declaracao)*;

declaracao: ID '=' expr;

NUMBER: [0-9]+('.'[0-9]+)?;
ID: [_a-zA-Z][_a-zA-Z0-9]*;
WS: [ \r\n\t] -> skip;