package com.example.projetoestoque;

public class IngredienteFormula {
    private final int idProduto;
    private final double quantidade;

    public IngredienteFormula(int idProduto, double quantidade) {
        this.idProduto = idProduto;
        this.quantidade = quantidade;
    }

    public int getIdProduto() {
        return idProduto;
    }

    public double getQuantidade() {
        return quantidade;
    }
}
