package com.uepb.compiler;

import java.util.LinkedList;
import java.util.Optional;

public class ScopeControl {
    private final LinkedList<SymbolTable> stack;

    public ScopeControl(){
        stack = new LinkedList<>();
        createScope();
    }

    public void createScope(){
        stack.push(new SymbolTable());
    }

    public void dropScope(){
        stack.pop();
    }

    public SymbolTable getCurrentScope(){
        return stack.peek();
    }

    public Optional<Variables> lookup(String name){
        return stack.stream()
            .filter((table) -> table.exists(name))
            .map((table) -> table.get(name))
            .findFirst();
    }

}
