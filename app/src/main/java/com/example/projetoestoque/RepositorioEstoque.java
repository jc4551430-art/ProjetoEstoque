package com.example.projetoestoque;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RepositorioEstoque {

    private static final String ARQUIVO = "estoque_cafe.json";
    private static final String ARQUIVO_TEMPORARIO = "estoque_cafe.json.tmp";
    private static final int VERSAO = 2;
    private static final int LIMITE_BYTES = 4 * 1024 * 1024;

    private final Context contexto;

    public RepositorioEstoque(Context contexto) {
        this.contexto = contexto.getApplicationContext();
    }

    public static class Estado {
        public final List<Produto> produtos;
        public final List<Formula> formulas;
        public final int proximoId;
        public final int proximoIdFormula;
        public final boolean arquivoRecuperado;

        Estado(List<Produto> produtos, List<Formula> formulas, int proximoId, int proximoIdFormula, boolean arquivoRecuperado) {
            this.produtos = produtos;
            this.formulas = formulas;
            this.proximoId = proximoId;
            this.proximoIdFormula = proximoIdFormula;
            this.arquivoRecuperado = arquivoRecuperado;
        }
    }

    public Estado carregar() {
        File arquivo = new File(contexto.getFilesDir(), ARQUIVO);

        if (!arquivo.exists() || arquivo.length() == 0) {
            return new Estado(new ArrayList<>(), new ArrayList<>(), 1, 1, false);
        }

        if (arquivo.length() > LIMITE_BYTES) {
            return new Estado(new ArrayList<>(), new ArrayList<>(), 1, 1, true);
        }

        try {
            String conteudo = lerTexto(arquivo);
            JSONObject raiz = new JSONObject(conteudo);

            int versaoLida = raiz.optInt("versao", 0);
            
            // Migração ou versões futuras
            if (versaoLida > VERSAO) {
                return new Estado(new ArrayList<>(), new ArrayList<>(), 1, 1, true);
            }

            // Carregar Produtos
            JSONArray listaProdutos = raiz.optJSONArray("produtos");
            List<Produto> produtos = new ArrayList<>();
            int maiorId = 0;

            if (listaProdutos != null) {
                for (int i = 0; i < listaProdutos.length(); i++) {
                    JSONObject item = listaProdutos.optJSONObject(i);
                    if (item == null) continue;

                    String nome = Produto.limparNome(item.optString("nome", ""));
                    if (nome.isEmpty()) continue;

                    int id = item.optInt("id", 0);
                    Produto produto = new Produto(
                            id,
                            nome,
                            Categoria.porNomeSeguro(item.optString("categoria", null)),
                            Produto.normalizar(item.optDouble("quantidade", 0d))
                    );

                    produtos.add(produto);
                    maiorId = Math.max(maiorId, id);
                }
            }

            // Carregar Formulas
            JSONArray listaFormulas = raiz.optJSONArray("formulas");
            List<Formula> formulas = new ArrayList<>();
            int maiorIdFormula = 0;

            if (listaFormulas != null) {
                for (int i = 0; i < listaFormulas.length(); i++) {
                    JSONObject item = listaFormulas.optJSONObject(i);
                    if (item == null) continue;

                    int id = item.optInt("id", 0);
                    String nome = item.optString("nome", "");
                    Formula formula = new Formula(id, nome);

                    JSONArray listaIngredientes = item.optJSONArray("ingredientes");
                    if (listaIngredientes != null) {
                        for (int j = 0; j < listaIngredientes.length(); j++) {
                            JSONObject ingJson = listaIngredientes.optJSONObject(j);
                            if (ingJson != null) {
                                int idProd = ingJson.optInt("idProduto", 0);
                                double qtd = ingJson.optDouble("quantidade", 0);
                                formula.adicionarIngrediente(new IngredienteFormula(idProd, qtd));
                            }
                        }
                    }
                    formulas.add(formula);
                    maiorIdFormula = Math.max(maiorIdFormula, id);
                }
            }

            int proximoId = Math.max(raiz.optInt("proximoId", 1), maiorId + 1);
            int proximoIdFormula = Math.max(raiz.optInt("proximoIdFormula", 1), maiorIdFormula + 1);
            
            return new Estado(produtos, formulas, proximoId, proximoIdFormula, false);
        } catch (Exception excecao) {
            renomearArquivoInvalido(arquivo);
            return new Estado(new ArrayList<>(), new ArrayList<>(), 1, 1, true);
        }
    }

    public boolean salvar(List<Produto> produtos, List<Formula> formulas, int proximoId, int proximoIdFormula) {
        JSONObject raiz = new JSONObject();

        try {
            // Salvar Produtos
            JSONArray listaP = new JSONArray();
            for (Produto p : produtos) {
                JSONObject item = new JSONObject();
                item.put("id", p.getId());
                item.put("nome", p.getNome());
                item.put("categoria", p.getCategoria().name());
                item.put("quantidade", p.getQuantidade());
                listaP.put(item);
            }

            // Salvar Formulas
            JSONArray listaF = new JSONArray();
            for (Formula f : formulas) {
                JSONObject item = new JSONObject();
                item.put("id", f.getId());
                item.put("nome", f.getNome());
                
                JSONArray listaI = new JSONArray();
                for (IngredienteFormula ing : f.getIngredientes()) {
                    JSONObject ingJson = new JSONObject();
                    ingJson.put("idProduto", ing.getIdProduto());
                    ingJson.put("quantidade", ing.getQuantidade());
                    listaI.put(ingJson);
                }
                item.put("ingredientes", listaI);
                listaF.put(item);
            }

            raiz.put("versao", VERSAO);
            raiz.put("proximoId", proximoId);
            raiz.put("proximoIdFormula", proximoIdFormula);
            raiz.put("produtos", listaP);
            raiz.put("formulas", listaF);
        } catch (Exception excecao) {
            return false;
        }

        File destino = new File(contexto.getFilesDir(), ARQUIVO);
        File temporario = new File(contexto.getFilesDir(), ARQUIVO_TEMPORARIO);

        FileOutputStream saida = null;
        OutputStreamWriter escritor = null;

        try {
            saida = new FileOutputStream(temporario);
            escritor = new OutputStreamWriter(saida, StandardCharsets.UTF_8);
            escritor.write(raiz.toString());
            escritor.flush();
            saida.getFD().sync();
        } catch (IOException excecao) {
            fechar(escritor);
            fechar(saida);
            temporario.delete();
            return false;
        }

        fechar(escritor);
        fechar(saida);

        if (destino.exists() && !destino.delete()) {
            temporario.delete();
            return false;
        }

        if (!temporario.renameTo(destino)) {
            temporario.delete();
            return false;
        }

        return true;
    }

    private String lerTexto(File arquivo) throws IOException {
        InputStream entrada = null;
        try {
            entrada = new FileInputStream(arquivo);
            byte[] bloco = new byte[8192];
            ByteArrayOutputStream acumulado = new ByteArrayOutputStream();
            int lidos;
            while ((lidos = entrada.read(bloco)) != -1) {
                acumulado.write(bloco, 0, lidos);
            }
            return new String(acumulado.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            fechar(entrada);
        }
    }

    private void renomearArquivoInvalido(File arquivo) {
        File backup = new File(contexto.getFilesDir(), ARQUIVO + ".invalido");
        backup.delete();
        arquivo.renameTo(backup);
    }

    private static void fechar(Closeable recurso) {
        if (recurso == null) return;
        try {
            recurso.close();
        } catch (IOException ignorado) {}
    }
}
