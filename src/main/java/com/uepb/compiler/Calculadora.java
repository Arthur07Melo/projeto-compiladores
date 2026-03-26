package com.uepb.compiler;

import com.uepb.ExprBaseVisitor;
import com.uepb.ExprParser.AtribuicaoVarContext;
import com.uepb.ExprParser.ComparacaoContext;
import com.uepb.ExprParser.ExponenciacaoContext;
import com.uepb.ExprParser.ForLoopContext;
import com.uepb.ExprParser.IfElseContext;
import com.uepb.ExprParser.InputContext;
import com.uepb.ExprParser.MulDivContext;
import com.uepb.ExprParser.NumeroContext;
import com.uepb.ExprParser.OutputContext;
import com.uepb.ExprParser.ParentesesContext;
import com.uepb.ExprParser.ProgContext;
import com.uepb.ExprParser.SomaSubContext;
import com.uepb.ExprParser.UsoVariavelContext;
import com.uepb.ExprParser.WhileLoopContext;
import com.uepb.ExprParser.AndLogicoContext;
import com.uepb.ExprParser.OrLogicoContext;
import com.uepb.ExprParser.NotLogicoContext;

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
        scopes.createScope();
        for (var expr : ctx.expr()) {
            visit(expr);
        }
        code.append("hlt\n");
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
    public Void visitComparacao(ComparacaoContext ctx) {
        var operador = ctx.OP.getText();

        switch(operador) {
            case "==":
                visit(ctx.O1);
                visit(ctx.O2);
                code.append("equ\n");
                break;
            case "!=":
                visit(ctx.O1);
                visit(ctx.O2);
                code.append("equ\n");
                code.append("not\n");
                break;
            case "<":
                visit(ctx.O1);
                visit(ctx.O2);
                code.append("let\n");
                break;
            case "<=":
                visit(ctx.O1);
                visit(ctx.O2);
                code.append("let\n");
                code.append("not\n");
                break;
            case ">":
                visit(ctx.O1);
                visit(ctx.O2);
                code.append("grt\n");
                break;
            case ">=":
                visit(ctx.O1);
                visit(ctx.O2);
                code.append("grt\n");
                code.append("not\n");
                break;
        }

        return null;
    }

    @Override
    public Void visitAndLogico(AndLogicoContext ctx) {
        visit(ctx.O1);
        visit(ctx.O2);
        code.append("and\n");
        return null;
    }

    @Override
    public Void visitOrLogico(OrLogicoContext ctx) {
        visit(ctx.O1);
        visit(ctx.O2);
        code.append("or\n");
        return null;
    }

    @Override
    public Void visitNotLogico(NotLogicoContext ctx) {
        visit(ctx.O1);
        code.append("not\n");
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
    public Void visitInput(InputContext ctx) {
        code.append("in\n");
        return null;
    }

    @Override
    public Void visitOutput(OutputContext ctx) {
        visit(ctx.expr());  // Evaluate the expression to print
        code.append("out\n");
        return null;
    }

    @Override
    public Void visitAtribuicaoVar(AtribuicaoVarContext ctx) {
        var nomeVar = ctx.ID().getText();
        var currentScope = scopes.getCurrentScope();

        // Try to find existing variable
        var declaracaoOpt = scopes.lookup(nomeVar);
        int address;

        if(declaracaoOpt.isEmpty()){
            // Variable doesn't exist - allocate and declare it
            address = mapper.alloc();
            currentScope.insert(nomeVar, address);
        } else {
            // Variable exists - use its address
            address = declaracaoOpt.get().address();
        }

        visit(ctx.expr());
        code.append("push $").append(address).append("\n");
        code.append("sto\n");

        return null;
    }

    @Override
    public Void visitExponenciacao(ExponenciacaoContext ctx) {
        // Save current memory state
        var memoryMarker = mapper.getCurrentAddress();
        
        // Allocate temporary memory addresses
        var baseAddr = mapper.alloc();
        var expAddr = mapper.alloc();
        var resAddr = mapper.alloc();

        var loopLabel = createLabel();
        var endLabel = createLabel();

        // Store exponent
        code.append("push $").append(expAddr).append("\n");
        visit(ctx.O2);
        code.append("sto\n");

        // Store base
        code.append("push $").append(baseAddr).append("\n");
        visit(ctx.O1);
        code.append("sto\n");

        // Initialize result = 1
        code.append("push $").append(resAddr).append("\n");
        code.append("push 1\n");
        code.append("sto\n");

        // Start loop
        code.append(loopLabel).append(":\n");
        
        // Check if exponent == 0, jump to end if true
        code.append("push $").append(expAddr).append("\n");
        code.append("lod\n");
        code.append("push 0\n");
        code.append("equ\n");
        code.append("tjp ").append(endLabel).append("\n");

        // result = result * base
        code.append("push $").append(resAddr).append("\n");
        code.append("push $").append(resAddr).append("\n");
        code.append("lod\n");
        code.append("push $").append(baseAddr).append("\n");
        code.append("lod\n");
        code.append("mul\n");

        code.append("sto\n");

        // exponent = exponent - 1
        code.append("push $").append(expAddr).append("\n");
        code.append("push $").append(expAddr).append("\n");
        code.append("lod\n");
        code.append("push 1\n");
        code.append("sub\n");
        code.append("sto\n");

        code.append("ujp ").append(loopLabel).append("\n");
        code.append(endLabel).append(":\n");
        
        // Load final result
        code.append("push $").append(resAddr).append("\n");
        code.append("lod\n");
        
        // Restore memory state
        mapper.restore(memoryMarker);
        
        return null;
    }

    @Override
    public Void visitIfElse(IfElseContext ctx) {
        var elseLabel = createLabel();
        var endLabel = createLabel();

        // Evaluate condition
        visit(ctx.COND);
        
        // Jump to else if condition is false
        code.append("fjp ").append(elseLabel).append("\n");
        
        // Execute then branch - iterate through all expressions
        for (var thenExpr : ctx.THEN.expr()) {
            visit(thenExpr);
        }
        
        // Jump to end after then branch
        code.append("ujp ").append(endLabel).append("\n");
        
        // Else branch
        code.append(elseLabel).append(":\n");
        if(ctx.ELSE != null) {
            for (var elseExpr : ctx.ELSE.expr()) {
                visit(elseExpr);
            }
        }
        
        // End label
        code.append(endLabel).append(":\n");

        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopContext ctx) {
        var startLabel = createLabel();
        var endLabel = createLabel();

        // Start label
        code.append(startLabel).append(":\n");
        
        // Evaluate condition
        visit(ctx.COND);
        
        // Jump to end if condition is false
        code.append("fjp ").append(endLabel).append("\n");
        
        // Execute body - iterate through all expressions in loopBody
        for (var bodyExpr : ctx.BODY.expr()) {
            visit(bodyExpr);
        }
        
        // Jump back to start
        code.append("ujp ").append(startLabel).append("\n");
        
        // End label
        code.append(endLabel).append(":\n");

        return null;
    }

    @Override
    public Void visitForLoop(ForLoopContext ctx) {
        var startLabel = createLabel();
        var condLabel = createLabel();
        var endLabel = createLabel();

        // Initialize
        visit(ctx.INIT);
        
        // Jump to condition check
        code.append("ujp ").append(condLabel).append("\n");
        
        // Start label (body)
        code.append(startLabel).append(":\n");
        
        // Execute body - iterate through all expressions in loopBody
        for (var bodyExpr : ctx.BODY.expr()) {
            visit(bodyExpr);
        }
        
        // Step/increment
        visit(ctx.STEP);
        
        // Condition check
        code.append(condLabel).append(":\n");
        visit(ctx.COND);
        code.append("fjp ").append(endLabel).append("\n");
        
        // Jump back to body
        code.append("ujp ").append(startLabel).append("\n");
        
        // End label
        code.append(endLabel).append(":\n");

        return null;
    }

}