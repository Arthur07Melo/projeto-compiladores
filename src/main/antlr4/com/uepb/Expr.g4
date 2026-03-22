grammar Expr;

prog: (expr ';'?)* EOF;

expr: '(' NESTED_EXPR=expr ')'                                                            #Parenteses
    | O1=expr OP=('*'|'/') O2=expr                                                        #MulDiv
    | O1=expr OP=('+'|'-') O2=expr                                                        #SomaSub
    | O1=expr OP=('=='|'!='|'<'|'<='|'>'|'>=') O2=expr                                    #Comparacao
    | O1=expr '&&' O2=expr                                                                #AndLogico
    | O1=expr '||' O2=expr                                                                #OrLogico
    | '!' O1=expr                                                                         #NotLogico
    | SINAL=('+'|'-')? NUMBER                                                             #Numero
    | SINAL=('+'|'-')? ID                                                                 #UsoVariavel
    | 'if' COND=expr '{' THEN=loopBody '}' ('else' '{' ELSE=loopBody '}')?                        #IfElse
    | 'while' COND=expr '{' BODY=loopBody '}'                                             #WhileLoop
    | 'for' '(' INIT=atribuicaoVar ';' COND=expr ';' STEP=expr ')' '{' BODY=loopBody '}'  #ForLoop
    | atribuicaoVar                                                                       #Atribuicao
    | 'input'                                                                             #Input
    | 'print' '(' expr ')'                                                                #Output
;

loopBody: (expr ';'?)+;

atribuicaoVar: 'RECEBA' ID '=' expr;

NUMBER: [0-9]+('.'[0-9]+)?;
ID: [_a-zA-Z][_a-zA-Z0-9]*;
WS: [ \r\n\t] -> skip;