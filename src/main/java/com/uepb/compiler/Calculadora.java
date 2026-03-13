package com.uepb.compiler;

import com.uepb.ExprBaseVisitor;
import com.uepb.ExprParser.AtribuicaoContext;
import com.uepb.ExprParser.DeclVariavelContext;
import com.uepb.ExprParser.DeclaracaoContext;
import com.uepb.ExprParser.InputContext;
import com.uepb.ExprParser.LoopContext;
import com.uepb.ExprParser.MulDivContext;
import com.uepb.ExprParser.NumeroContext;
import com.uepb.ExprParser.ParentesesContext;
import com.uepb.ExprParser.ProgContext;
import com.uepb.ExprParser.SomaSubContext;
import com.uepb.ExprParser.UsoVariavelContext;

public class Calculadora extends ExprBaseVisitor<Void>{

    private final ScopeControl scopes = new ScopeControl();
    private final MemoryMapper mapper = new MemoryMapper();
    private final StringBuilder code = new StringBuilder();
    private int label = 0;

    private String createLabel(){
        var labelGerada = "L" + String.valueOf(label);
        label++;
        return labelGerada;
    }

    public String getCode(){
        return code.toString();
    }

    @Override
    public Void visitProg(ProgContext ctx) {
        visit(ctx.expr());
        code.append("out\n");
        code.append("hlt\n"); //EOF
        return null;
    }

    @Override
    public Void visitParenteses(ParentesesContext ctx) {
        visit(ctx.NESTED_EXPR);
        return null;
    }

    @Override
    public Void visitMulDiv(MulDivContext ctx) {
        visit(ctx.O1);
        visit(ctx.O2);
        var operador = ctx.OP.getText();

        if(operador.equals("/")){
            code.append("div\n");
        }else{
            code.append("mul\n");
        }

        return null;
    }

    @Override
    public Void visitSomaSub(SomaSubContext ctx) {
        visit(ctx.O1);
        visit(ctx.O2);
        var operador = ctx.OP.getText();

        if(operador.equals("+")){
            code.append("add\n");
        }else{
            code.append("sub\n");
        }

        return null;
    }

    @Override
    public Void visitNumero(NumeroContext ctx) {
        var numeroStr = ctx.NUMBER().getText();
        var sinal = ctx.SINAL;

        if(sinal != null && sinal.getText().equals("-")){
            code.append("push -1").append("\n");
            code.append("push ").append(numeroStr).append("\n");
            code.append("mul\n");

            return null;
        }

        code.append("push ").append(numeroStr).append("\n");

        return null;
    }

    @Override
    public Void visitUsoVariavel(UsoVariavelContext ctx) {
        var nomeVar = ctx.ID().getText();
        var tk = ctx.ID().getSymbol();
        var sinal = ctx.SINAL;
        var declaracaoOpt = scopes.lookup(nomeVar);

        if(declaracaoOpt.isEmpty()){
            throw new RuntimeException(
                "A variavel '%s' não foi declarada na linha %d e coluna %d."
                .formatted(nomeVar,tk.getLine(),tk.getCharPositionInLine())
            );
        }

        var address = declaracaoOpt.get().address();
        if(sinal != null && sinal.getText().equals("-")){
            code.append("push $").append(address).append("\n");
            code.append("lod").append("\n");
            code.append("push -1\n");
            code.append("mul\n");

            return null;
        }

        code.append("push $").append(address).append("\n");
        code.append("lod").append("\n");

        return null;
    }

    @Override
    public Void visitDeclVariavel(DeclVariavelContext ctx) {
        scopes.createScope();
        visit(ctx.listaDeclaracao());
        visit(ctx.expr());
        scopes.dropScope();

        return null;
    }

    

    @Override
    public Void visitDeclaracao(DeclaracaoContext ctx) {
        var varName = ctx.ID().getText();
        var tk = ctx.ID().getSymbol();
        var currentScope = scopes.getCurrentScope();
        var address = mapper.alloc();

        if(currentScope.exists(varName)){
            throw new RuntimeException(
                "A variavel '%s' presente na linha %d e coluna %d ja foi declarada."
                .formatted(varName,tk.getLine(),tk.getCharPositionInLine())
            );
        }

        code.append("push $").append(address).append("\n");
        visit(ctx.expr());
        currentScope.insert(varName, address);
        code.append("sto\n");

        return null;
    }

    @Override
    public Void visitInput(InputContext ctx) {
        code.append("in\n");
        return null;
    }

    @Override
    public Void visitAtribuicao(AtribuicaoContext ctx) {
        var nomeVar = ctx.ID().getText();
        var tk = ctx.ID().getSymbol();
        var declaracaoOpt = scopes.lookup(nomeVar);

        if(declaracaoOpt.isEmpty()){
            throw new RuntimeException(
                "A variavel '%s' não foi declarada na linha %d e coluna %d."
                .formatted(nomeVar,tk.getLine(),tk.getCharPositionInLine())
            );
        }

        var variavel = declaracaoOpt.get();
        var address = variavel.address();

        code.append("push $").append(address).append("\n");
        visit(ctx.expr());
        code.append("sto\n");

        code.append("push $").append(address).append("\n");
        code.append("lod").append("\n");

        return null;
    }

    @Override
    public Void visitLoop(LoopContext ctx) {
        var start = createLabel();
        var end = createLabel();
        var marker = mapper.getCurrentAddress();
        var n = mapper.alloc();
        var i = mapper.alloc();

        code.append("push $").append(n).append("\n");
        visit(ctx.N);
        code.append("sto\n");

        code.append("push $").append(i).append("\n");
        code.append("push 0\n");
        code.append("sto\n");

        code.append(start).append(":").append("\n");
        
        code.append("push $").append(i).append("\n");
        code.append("lod\n");
        code.append("push $").append(n).append("\n");
        code.append("lod\n");
        code.append("let\n");

        code.append("fjp ").append(end).append("\n");

        visit(ctx.CODE);

        code.append("push $").append(i).append("\n");
        code.append("push $").append(i).append("\n");
        code.append("lod\n");
        code.append("push 1\n");
        code.append("add\n");
        code.append("sto\n");

        code.append("ujp ").append(start).append("\n");

        code.append(end).append(":").append("\n");

        visit(ctx.OUT);

        mapper.restore(marker);

        return null;
    }

}