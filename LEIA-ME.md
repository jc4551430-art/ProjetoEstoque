# ProjetoEstoque (Android nativo, Java)

Aplicativo de controle de estoque com a Activity principal chamada cafeEstoque.

## Como abrir

1. Instale o Android Studio (ele ja traz o JDK e o Android SDK).
2. Descompacte esta pasta.
3. No Android Studio: File > Open e selecione a pasta ProjetoEstoque.
4. Aguarde o Gradle sincronizar. O wrapper do Gradle e regenerado automaticamente.
5. Run > Run 'app' em um emulador ou celular com Android 7.0 ou superior.

## O que o app faz

- Cadastro, edicao e exclusao de produtos (toque para editar, pressione para excluir).
- Pesquisa por nome em tempo real.
- Nove categorias de insumo.
- Quantidade em L/kg com resumo do total no topo da lista.
- Alternancia de ordenacao entre nome e quantidade.
- Salvamento automatico ao alterar dados e ao sair da tela.

## Decisoes de seguranca e robustez

- Os dados ficam em armazenamento interno privado (getFilesDir), acessivel apenas ao app.
- Persistencia em JSON em vez de serializacao Java, que permite execucao de codigo
  malicioso ao desserializar arquivos alterados.
- Gravacao atomica: escreve em arquivo temporario, sincroniza e so entao substitui o
  arquivo final, evitando perda de dados se o app for encerrado no meio da operacao.
- Arquivo corrompido ou de versao desconhecida e isolado e o app reinicia vazio, sem travar.
- Limite de 4 MB na leitura do arquivo de dados.
- Validacao de entrada: nome obrigatorio, sem duplicidade, limite de caracteres,
  quantidade numerica entre 0 e 1.000.000, rejeicao de NaN e infinito.
- IDs unicos garantidos tambem na leitura do arquivo.
- allowBackup desativado para nao copiar o estoque para backups externos.
- Nenhuma permissao declarada no manifesto e nenhum acesso a rede.

## Estrutura

- app/src/main/java/com/example/projetoestoque/cafeEstoque.java - tela principal
- app/src/main/java/com/example/projetoestoque/Produto.java - modelo e validacoes
- app/src/main/java/com/example/projetoestoque/Categoria.java - categorias
- app/src/main/java/com/example/projetoestoque/RepositorioEstoque.java - persistencia
- app/src/main/res/layout/activity_cafe_estoque.xml - layout
- app/src/test/java/com/example/projetoestoque/ExampleUnitTest.java - testes unitarios
- app/src/androidTest/java/com/example/projetoestoque/ExampleInstrumentedTest.java - testes instrumentados

## Pasta de testes do APK

- app/src/test - testes unitarios (JVM)
- app/src/androidTest - testes instrumentados (dispositivo/emulador)
