package com.uepb.compiler;

import com.uepb.ExprBaseVisitor;
import com.uepb.ExprParser.DeclVariavelContext;
import com.uepb.ExprParser.DeclaracaoContext;
import com.uepb.ExprParser.ExponenciacaoContext;
import com.uepb.ExprParser.MulDivContext;
import com.uepb.ExprParser.NumeroContext;
import com.uepb.ExprParser.ParentesesContext;
import com.uepb.ExprParser.ProgContext;
import com.uepb.ExprParser.SomaSubContext;
import com.uepb.ExprParser.UsoVariavelContext;

public class Calculadora extends ExprBaseVisitor<Double>{

    private final ScopeControl stack = new ScopeControl();

    @Override
    public Double visitProg(ProgContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Double visitParenteses(ParentesesContext ctx) {
        return visit(ctx.NESTED_EXPR);
    }

    @Override
    public Double visitExponenciacao(ExponenciacaoContext ctx) {
        var base = visit(ctx.BASE);
        var expoente = visit(ctx.EXPOENTE);
        return Math.pow(base, expoente);
    }

    @Override
    public Double visitMulDiv(MulDivContext ctx) {
        var o1 = visit(ctx.O1);
        var o2 = visit(ctx.O2);
        var operador = ctx.OP.getText();

        if(operador.equals("/")){
            if(o2 == 0) throw new ArithmeticException("Divisão por zero");

            return o1/o2;
        }else{
            return o1*o2;
        }
    }

    @Override
    public Double visitSomaSub(SomaSubContext ctx) {
        var o1 = visit(ctx.O1);
        var o2 = visit(ctx.O2);
        var operador = ctx.OP.getText();
        return operador.equals("+") ? o1+o2 : o1-o2;
    }

    @Override
    public Double visitNumero(NumeroContext ctx) {
        var numero = ctx.NUMBER().getText();
        var sinal = ctx.SINAL;
        var valor = Double.valueOf(numero);

        if(sinal != null && sinal.getText().equals("-")){
            return -valor;
        }

        return valor;
    }

    @Override
    public Double visitUsoVariavel(UsoVariavelContext ctx) {
        var nomeVar = ctx.ID().getText();
        var idSymbol = ctx.ID().getSymbol();
        var sinal = ctx.SINAL;

        var valueOpt = stack.lookup(nomeVar);

        if(valueOpt.isEmpty()){
            throw new RuntimeException(
                "A variável '%s' na linha %d e coluna %d não foi declarada"
                .formatted(nomeVar, idSymbol.getLine(), idSymbol.getCharPositionInLine())
            );
        }

        var value = valueOpt.get().value();

        if(sinal != null && sinal.getText().equals("-")){
            return -value;
        }

        return value;
    }

    @Override
    public Double visitDeclVariavel(DeclVariavelContext ctx) {
        stack.createScope();
        visit(ctx.listaDeclaracao());
        var valor = visit(ctx.expr());
        stack.dropScope();
        return valor;
    }

    @Override
    public Double visitDeclaracao(DeclaracaoContext ctx) {
        var varName = ctx.ID().getText();
        var idSymbol = ctx.ID().getSymbol();
        var currentScope = stack.getCurrentScope();

        if(currentScope.exists(varName)){
            throw new RuntimeException(
                "A variável '%s' na linha %d e coluna %d já foi declarada"
                .formatted(varName, idSymbol.getLine(), idSymbol.getCharPositionInLine())
            );
        }

        var valor = visit(ctx.expr());
        currentScope.insert(varName, valor);
        return null;
    }

}
