package com.example.projetoestoque;

public class Produto {

    public static final double QUANTIDADE_MAXIMA = 1000000d;
    public static final int TAMANHO_MAXIMO_NOME = 60;

    private final int id;
    private String nome;
    private Categoria categoria;
    private double quantidade;

    public Produto(int id, String nome, Categoria categoria, double quantidade) {
        this.id = id;
        this.nome = nome;
        this.categoria = categoria;
        this.quantidade = quantidade;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public void setQuantidade(double quantidade) {
        this.quantidade = normalizar(quantidade);
    }

    public void adicionarQuantidade(double valor) {
        if (valor > 0) {
            setQuantidade(quantidade + valor);
        }
    }

    public boolean removerQuantidade(double valor) {
        if (valor <= 0 || valor > quantidade) {
            return false;
        }

        setQuantidade(quantidade - valor);
        return true;
    }

    public static double normalizar(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor < 0) {
            return 0d;
        }

        if (valor > QUANTIDADE_MAXIMA) {
            return QUANTIDADE_MAXIMA;
        }

        return Math.round(valor * 100d) / 100d;
    }

    public static String limparNome(String valor) {
        if (valor == null) {
            return "";
        }

        String limpo = valor.replaceAll("\\s+", " ").trim();

        if (limpo.length() > TAMANHO_MAXIMO_NOME) {
            limpo = limpo.substring(0, TAMANHO_MAXIMO_NOME);
        }

        return limpo;
    }
}
