package com.uepb.compiler;

import com.uepb.ExprBaseVisitor;
import com.uepb.ExprParser.AtribuicaoStringContext;
import com.uepb.ExprParser.AtribuicaoVarContext;
import com.uepb.ExprParser.ComparacaoContext;
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
        visit(ctx.O1);
        visit(ctx.O2);
        var operador = ctx.OP.getText();

        // For now, we'll use subtraction to simulate comparisons
        // This is a simplified approach - you may need to adjust based on your pcode interpreter
        switch(operador) {
            case "==":
                code.append("equ\n");
                break;
            case "!=":
                code.append("neq\n");
                break;
            case "<":
                code.append("let\n");
                break;
            case "<=":
                code.append("leq\n");
                break;
            case ">":
                code.append("grt\n");
                break;
            case ">=":
                code.append("geq\n");
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

        code.append("push $").append(address).append("\n");
        visit(ctx.expr());
        code.append("sto\n");

        return null;
    }

    @Override
    public Void visitStringLiteral(com.uepb.ExprParser.StringLiteralContext ctx) {
        var stringValor = ctx.STRING().getText();
        // Remove quotes and push each character
        var cleanString = stringValor.substring(1, stringValor.length() - 1);
        
        for (int i = 0; i < cleanString.length(); i++) {
            code.append("push ").append((int)cleanString.charAt(i)).append("\n");
        }
        
        return null;
    }

    @Override
    public Void visitAtribuicaoString(AtribuicaoStringContext ctx) {
        var nomeVar = ctx.ID().getText();
        var stringValor = ctx.STRING().getText();
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

        // Remove quotes from string and store as individual characters
        var cleanString = stringValor.substring(1, stringValor.length() - 1);
        
        // Store each character of the string
        for (int i = 0; i < cleanString.length(); i++) {
            code.append("push $").append(address + i).append("\n");
            code.append("push ").append((int)cleanString.charAt(i)).append("\n");
            code.append("sto\n");
        }

        // Store string length at the base address
        code.append("push $").append(address).append("\n");
        code.append("push ").append(cleanString.length()).append("\n");
        code.append("sto\n");

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