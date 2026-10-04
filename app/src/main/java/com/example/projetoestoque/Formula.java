package com.example.projetoestoque;

import java.util.ArrayList;
import java.util.List;

public class Formula {
    private final int id;
    private String nome;
    private final List<IngredienteFormula> ingredientes;

    public Formula(int id, String nome) {
        this.id = id;
        this.nome = nome;
        this.ingredientes = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public List<IngredienteFormula> getIngredientes() {
        return ingredientes;
    }

    public void adicionarIngrediente(IngredienteFormula ingrediente) {
        ingredientes.add(ingrediente);
    }
}
