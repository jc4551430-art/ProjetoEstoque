package com.example.projetoestoque;

import androidx.annotation.NonNull;

public enum Categoria {
    HERBICIDA("Herbicida"),
    FUNGICIDA("Fungicida"),
    INSETICIDA("Inseticida"),
    ACARICIDA("Acaricida"),
    NEMATICIDA("Nematicida"),
    BIOLOGICO("Biologico"),
    ADJUVANTE("Adjuvante"),
    FERTILIZANTE_FOLIAR("Fertilizante foliar"),
    OUTROS("Outros");

    private final String descricao;

    Categoria(String descricao) {
        this.descricao = descricao;
    }

    public static Categoria porNomeSeguro(String nome) {
        if (nome == null) {
            return OUTROS;
        }

        for (Categoria categoria : values()) {
            if (categoria.name().equals(nome)) {
                return categoria;
            }
        }

        return OUTROS;
    }

    @NonNull
    @Override
    public String toString() {
        return descricao;
    }
}
