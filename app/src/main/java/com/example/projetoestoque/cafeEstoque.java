package com.example.projetoestoque;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.Normalizer;
import java.util.Set;

public class cafeEstoque extends AppCompatActivity {

    private final List<Produto> estoque = new ArrayList<>();
    private final List<Produto> visiveis = new ArrayList<>();
    private final List<Formula> formulas = new ArrayList<>();

    private RepositorioEstoque repositorio;
    private EditText campoPesquisa;
    private ListView listaProdutos;
    private TextView textoVazio;
    private TextView rodapeResumo;
    private ProdutoAdapter adaptador;

    private int proximoId = 1;
    private int proximoIdFormula = 1;
    private boolean ordenarPorQuantidade = false;

    // Cores seguras
    private static final int COR_BRANCO = 0xFFFFFFFF;
    private static final int COR_VERMELHO = 0xFFE53935;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cafe_estoque);

        repositorio = new RepositorioEstoque(this);

        campoPesquisa = findViewById(R.id.campoPesquisa);
        listaProdutos = findViewById(R.id.listaProdutos);
        textoVazio = findViewById(R.id.textoVazio);
        rodapeResumo = findViewById(R.id.rodapeResumo);

        Button botaoAdicionar = findViewById(R.id.botaoAdicionar);
        Button botaoOrdenar = findViewById(R.id.botaoOrdenar);
        Button botaoFormulas = findViewById(R.id.botaoFormulas);

        adaptador = new ProdutoAdapter();
        listaProdutos.setAdapter(adaptador);

        try {
            RepositorioEstoque.Estado estado = repositorio.carregar();
            if (estado != null) {
                estoque.addAll(estado.produtos);
                formulas.addAll(estado.formulas);
                proximoId = estado.proximoId;
                proximoIdFormula = estado.proximoIdFormula;

                if (estado.arquivoRecuperado) {
                    avisar("Dados recuperados de falha anterior.");
                }
            }
        } catch (Exception e) {
            avisar("Erro ao iniciar dados.");
        }

        atualizarLista();

        campoPesquisa.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) { atualizarLista(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        botaoAdicionar.setOnClickListener(v -> abrirFormulario(null));
        botaoOrdenar.setOnClickListener(v -> {
            ordenarPorQuantidade = !ordenarPorQuantidade;
            atualizarLista();
            avisar(ordenarPorQuantidade ? "Ordenado por Qtd." : "Ordenado por Nome.");
        });

        botaoFormulas.setOnClickListener(v -> gerenciarFormulas());

        listaProdutos.setOnItemClickListener((p, view, pos, id) -> {
            Produto prod = itemEm(pos);
            if (prod != null) abrirFormulario(prod);
        });

        listaProdutos.setOnItemLongClickListener((p, view, pos, id) -> {
            Produto prod = itemEm(pos);
            if (prod != null) confirmarExclusao(prod);
            return true;
        });
    }

    private void gerenciarFormulas() {
        if (estoque.isEmpty() && formulas.isEmpty()) {
            avisar("Adicione itens ao estoque para criar fórmulas.");
            return;
        }

        String[] opcoes = new String[formulas.size() + 1];
        opcoes[0] = "Nova fórmula";
        for (int i = 0; i < formulas.size(); i++) {
            opcoes[i + 1] = "Aplicar: " + formulas.get(i).getNome();
        }

        new AlertDialog.Builder(this)
                .setTitle("Fórmulas")
                .setItems(opcoes, (dialog, index) -> {
                    if (index == 0) {
                        abrirEditorFormula(null);
                        return;
                    }
                    int idxFormula = index - 1;
                    if (idxFormula >= 0 && idxFormula < formulas.size()) {
                        mostrarAcoesFormula(formulas.get(idxFormula));
                    }
                })
                .show();
    }

    private void mostrarAcoesFormula(Formula formula) {
        if (formula == null) return;

        String[] opcoes = new String[] {"Aplicar", "Editar"};
        new AlertDialog.Builder(this)
                .setTitle(formula.getNome())
                .setItems(opcoes, (dialog, which) -> {
                    if (which == 0) {
                        aplicarFormula(formula);
                    } else if (which == 1) {
                        abrirEditorFormula(formula);
                    }
                })
                .show();
    }

    private void abrirEditorFormula(Formula formulaExistente) {
        if (estoque.isEmpty()) {
            avisar("Adicione itens ao estoque primeiro.");
            return;
        }

        final EditText campoNome = new EditText(this);
        campoNome.setHint("Nome da fórmula");
        campoNome.setSingleLine(true);
        campoNome.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (formulaExistente != null) {
            campoNome.setText(formulaExistente.getNome());
        }

        final AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(formulaExistente == null ? "Nova Fórmula" : "Editar Fórmula")
                .setView(campoNome)
                .setPositiveButton("Continuar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialogo.setOnShowListener(d -> {
            Button botao = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
            botao.setOnClickListener(v -> {
                String nomeFormula = Produto.limparNome(campoNome.getText().toString());
                if (nomeFormula.isEmpty()) {
                    avisar("Informe o nome da fórmula.");
                    return;
                }
                dialogo.dismiss();
                selecionarProdutosParaFormula(nomeFormula, formulaExistente);
            });
        });

        dialogo.show();
    }

    private void selecionarProdutosParaFormula(String nomeFormula, Formula formulaExistente) {
        final Map<Integer, Double> quantidadesIniciais = new HashMap<>();
        final Set<Integer> idsSelecionados = new HashSet<>();
        if (formulaExistente != null) {
            for (IngredienteFormula ing : formulaExistente.getIngredientes()) {
                double atual = quantidadesIniciais.containsKey(ing.getIdProduto()) ? quantidadesIniciais.get(ing.getIdProduto()) : 0d;
                quantidadesIniciais.put(ing.getIdProduto(), atual + ing.getQuantidade());
                idsSelecionados.add(ing.getIdProduto());
            }
        }

        final ViewGroup container = new android.widget.LinearLayout(this);
        ((android.widget.LinearLayout) container).setOrientation(android.widget.LinearLayout.VERTICAL);

        final EditText campoBusca = new EditText(this);
        campoBusca.setHint("Pesquisar produto...");
        campoBusca.setSingleLine(true);
        campoBusca.setInputType(InputType.TYPE_CLASS_TEXT);
        container.addView(campoBusca);

        final ListView listaSelecao = new ListView(this);
        listaSelecao.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        container.addView(listaSelecao);

        final List<Produto> filtrados = new ArrayList<>();
        final ArrayAdapter<Produto> adapter = new ArrayAdapter<Produto>(this, android.R.layout.simple_list_item_multiple_choice, filtrados) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                Produto p = getItem(position);
                if (p != null) {
                    text.setText(p.getNome() + " (" + formatar(p.getQuantidade()) + " L/kg disponíveis)");
                }
                return view;
            }
        };
        listaSelecao.setAdapter(adapter);

        Runnable aplicarFiltro = () -> {
            String termo = normalizarTextoPesquisa(campoBusca.getText().toString());
            filtrados.clear();
            for (Produto p : estoque) {
                String nome = normalizarTextoPesquisa(p.getNome());
                String categoria = normalizarTextoPesquisa(p.getCategoria().toString());
                if (termo.isEmpty() || nome.contains(termo) || categoria.contains(termo)) {
                    filtrados.add(p);
                }
            }
            Collections.sort(filtrados, (a, b) -> a.getNome().compareToIgnoreCase(b.getNome()));
            adapter.notifyDataSetChanged();
            for (int i = 0; i < filtrados.size(); i++) {
                listaSelecao.setItemChecked(i, idsSelecionados.contains(filtrados.get(i).getId()));
            }
        };

        listaSelecao.setOnItemClickListener((parent, view, position, id) -> {
            Produto selecionado = adapter.getItem(position);
            if (selecionado == null) return;
            if (listaSelecao.isItemChecked(position)) {
                idsSelecionados.add(selecionado.getId());
            } else {
                idsSelecionados.remove(selecionado.getId());
            }
        });

        campoBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltro.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        aplicarFiltro.run();

        final AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Selecione os produtos")
                .setView(container)
                .setPositiveButton("Próximo", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialogo.setOnShowListener(d -> {
            Button botao = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
            botao.setOnClickListener(v -> {
                List<Produto> produtosSelecionados = new ArrayList<>();
                for (Produto p : estoque) {
                    if (idsSelecionados.contains(p.getId())) {
                        produtosSelecionados.add(p);
                    }
                }

                if (produtosSelecionados.isEmpty()) {
                    avisar("Selecione ao menos um produto.");
                    return;
                }

                dialogo.dismiss();
                coletarQuantidadeIngredientes(nomeFormula, produtosSelecionados, 0, new ArrayList<>(), quantidadesIniciais, formulaExistente);
            });
        });

        dialogo.show();
    }

    private void coletarQuantidadeIngredientes(
            String nomeFormula,
            List<Produto> produtosSelecionados,
            int indice,
            List<IngredienteFormula> ingredientes,
            Map<Integer, Double> quantidadesIniciais,
            Formula formulaExistente
    ) {
        if (indice >= produtosSelecionados.size()) {
            if (formulaExistente == null) {
                int idFormula = proximoIdFormula++;
                Formula formula = new Formula(idFormula, nomeFormula);
                for (IngredienteFormula ing : ingredientes) {
                    formula.adicionarIngrediente(ing);
                }
                formulas.add(formula);
                persistir();
                avisar("Fórmula criada: " + nomeFormula);
                return;
            }

            formulaExistente.setNome(nomeFormula);
            formulaExistente.getIngredientes().clear();
            for (IngredienteFormula ing : ingredientes) {
                formulaExistente.adicionarIngrediente(ing);
            }
            persistir();
            avisar("Fórmula atualizada: " + nomeFormula);
            return;
        }

        Produto produto = produtosSelecionados.get(indice);
        final EditText campoQuantidade = new EditText(this);
        campoQuantidade.setHint("Ex: 1.5");
        campoQuantidade.setSingleLine(true);
        campoQuantidade.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (quantidadesIniciais.containsKey(produto.getId())) {
            campoQuantidade.setText(formatar(quantidadesIniciais.get(produto.getId())));
        }

        final AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Qtd. para " + produto.getNome())
                .setMessage("Disponível: " + formatar(produto.getQuantidade()) + " L/kg")
                .setView(campoQuantidade)
                .setPositiveButton(indice == produtosSelecionados.size() - 1 ? "Finalizar" : "Próximo", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialogo.setOnShowListener(d -> {
            Button botao = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
            botao.setOnClickListener(v -> {
                Double quantidade = converter(campoQuantidade.getText().toString());
                if (quantidade == null || quantidade <= 0) {
                    avisar("Informe uma quantidade válida.");
                    return;
                }
                ingredientes.add(new IngredienteFormula(produto.getId(), quantidade));
                dialogo.dismiss();
                coletarQuantidadeIngredientes(nomeFormula, produtosSelecionados, indice + 1, ingredientes, quantidadesIniciais, formulaExistente);
            });
        });

        dialogo.show();
    }

    private void aplicarFormula(Formula formula) {
        if (formula == null || formula.getIngredientes().isEmpty()) {
            avisar("Fórmula sem ingredientes.");
            return;
        }

        Map<Integer, Double> totaisNecessarios = new HashMap<>();
        for (IngredienteFormula ing : formula.getIngredientes()) {
            if (ing == null || ing.getQuantidade() <= 0) {
                avisar("Fórmula inválida: ingrediente com quantidade inválida.");
                return;
            }
            int idProduto = ing.getIdProduto();
            double totalAtual = totaisNecessarios.containsKey(idProduto) ? totaisNecessarios.get(idProduto) : 0d;
            totaisNecessarios.put(idProduto, totalAtual + ing.getQuantidade());
        }

        Map<Integer, Produto> produtosAlvo = new HashMap<>();
        for (Map.Entry<Integer, Double> entrada : totaisNecessarios.entrySet()) {
            Produto produto = encontrarProduto(entrada.getKey());
            if (produto == null) {
                avisar("Erro: item da fórmula não existe mais no estoque.");
                return;
            }
            if (produto.getQuantidade() < entrada.getValue()) {
                avisar("Erro: estoque insuficiente para " + produto.getNome() + ".");
                return;
            }
            produtosAlvo.put(entrada.getKey(), produto);
        }

        Map<Integer, Double> quantidadesOriginais = new HashMap<>();
        for (Map.Entry<Integer, Double> entrada : totaisNecessarios.entrySet()) {
            Produto produto = produtosAlvo.get(entrada.getKey());
            quantidadesOriginais.put(produto.getId(), produto.getQuantidade());
        }

        for (Map.Entry<Integer, Double> entrada : totaisNecessarios.entrySet()) {
            Produto produto = produtosAlvo.get(entrada.getKey());
            boolean removido = produto.removerQuantidade(entrada.getValue());
            if (!removido) {
                for (Map.Entry<Integer, Double> original : quantidadesOriginais.entrySet()) {
                    Produto p = produtosAlvo.get(original.getKey());
                    if (p != null) {
                        p.setQuantidade(original.getValue());
                    }
                }
                avisar("Erro ao aplicar fórmula. Nenhuma alteração foi salva.");
                return;
            }
        }

        avisar("Fórmula '" + formula.getNome() + "' aplicada!");
        atualizarLista();
        persistir();
    }

    private Produto encontrarProduto(int id) {
        for (Produto p : estoque) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    private Produto itemEm(int posicao) {
        if (posicao < 0 || posicao >= visiveis.size()) return null;
        return visiveis.get(posicao);
    }

    private void atualizarLista() {
        String termo = normalizarTextoPesquisa(campoPesquisa.getText().toString());
        visiveis.clear();
        for (Produto p : estoque) {
            String nome = normalizarTextoPesquisa(p.getNome());
            String categoria = normalizarTextoPesquisa(p.getCategoria().toString());
            if (termo.isEmpty() || nome.contains(termo) || categoria.contains(termo)) {
                visiveis.add(p);
            }
        }
        Collections.sort(visiveis, (p1, p2) -> {
            if (ordenarPorQuantidade) return Double.compare(p2.getQuantidade(), p1.getQuantidade());
            return p1.getNome().compareToIgnoreCase(p2.getNome());
        });
        adaptador.notifyDataSetChanged();
        
        if (textoVazio != null) textoVazio.setVisibility(visiveis.isEmpty() ? View.VISIBLE : View.GONE);
        if (listaProdutos != null) listaProdutos.setVisibility(visiveis.isEmpty() ? View.GONE : View.VISIBLE);
        
        double total = 0;
        for (Produto p : estoque) total += p.getQuantidade();
        if (rodapeResumo != null) {
            rodapeResumo.setText(estoque.size() + " itens | Estoque: " + formatar(total) + " L/kg");
        }
    }

    private void abrirFormulario(final Produto existente) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_produto, null);
        
        final EditText editNome = layout.findViewById(R.id.editNome);
        final Spinner spinnerCategoria = layout.findViewById(R.id.spinnerCategoria);
        final EditText editQuantidade = layout.findViewById(R.id.editQuantidade);

        ArrayAdapter<Categoria> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, Categoria.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapter);

        if (existente != null) {
            editNome.setText(existente.getNome());
            spinnerCategoria.setSelection(existente.getCategoria().ordinal());
            editQuantidade.setText(formatar(existente.getQuantidade()));
        }

        final AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(existente == null ? "Cadastrar" : "Editar")
                .setView(layout)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialogo.setOnShowListener(d -> {
            Button btn = ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE);
            btn.setOnClickListener(v -> {
                String nome = Produto.limparNome(editNome.getText().toString());
                Categoria cat = (Categoria) spinnerCategoria.getSelectedItem();
                Double qtd = converter(editQuantidade.getText().toString());

                if (nome.isEmpty() || cat == null || qtd == null) {
                    avisar("Preencha todos os campos.");
                    return;
                }

                if (existente == null) {
                    estoque.add(new Produto(proximoId++, nome, cat, qtd));
                } else {
                    existente.setNome(nome);
                    existente.setCategoria(cat);
                    existente.setQuantidade(qtd);
                }
                
                atualizarLista();
                persistir();
                dialogo.dismiss();
            });
        });

        dialogo.show();
    }

    private void confirmarExclusao(final Produto produto) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir")
                .setMessage("Deseja remover " + produto.getNome() + "?")
                .setPositiveButton("Sim", (d, b) -> {
                    estoque.remove(produto);
                    atualizarLista();
                    persistir();
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private Double converter(String texto) {
        if (texto == null) return null;
        try { 
            String n = texto.trim().replace(',', '.');
            if (n.isEmpty()) return null;
            return Double.parseDouble(n); 
        }
        catch (Exception e) { return null; }
    }

    private String formatar(double valor) {
        if (valor == Math.rint(valor)) return String.format(Locale.getDefault(), "%.0f", valor);
        return String.format(Locale.getDefault(), "%.2f", valor);
    }

    private void persistir() {
        persistir(false);
    }

    private void persistir(boolean silencioso) {
        boolean salvo;
        try {
            salvo = repositorio.salvar(estoque, formulas, proximoId, proximoIdFormula);
        } catch (Exception e) {
            salvo = false;
        }
        if (!salvo && !silencioso) {
            avisar("Erro ao salvar dados.");
        }
    }

    private String normalizarTextoPesquisa(String valor) {
        if (valor == null) return "";
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }

    private void avisar(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    @Override protected void onStop() { 
        super.onStop(); 
        persistir(true); 
    }

    private class ProdutoAdapter extends ArrayAdapter<Produto> {
        ProdutoAdapter() { super(cafeEstoque.this, 0, visiveis); }

        @NonNull
        @Override
        public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
            View v = (convertView != null) ? convertView : LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            v.setBackgroundResource(R.drawable.fundo_item);
            
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp == null) {
                lp = new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMargins(0, 0, 0, dp(8));
            }
            v.setLayoutParams(lp);
            v.setPadding(dp(16), dp(16), dp(16), dp(16));

            TextView t1 = v.findViewById(android.R.id.text1);
            TextView t2 = v.findViewById(android.R.id.text2);
            Produto p = visiveis.get(pos);
            
            t1.setText(p.getNome());
            t1.setTextColor(COR_BRANCO);
            t1.setTextSize(18f);
            
            t2.setText(p.getCategoria() + " • " + formatar(p.getQuantidade()) + " L/kg");
            t2.setTextColor(COR_VERMELHO);
            
            return v;
        }
    }
}
