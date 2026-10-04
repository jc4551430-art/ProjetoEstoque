# ProjetoEstoque (Android nativo, Java)

Aplicativo de controle de estoque com a Activity principal chamada cafeEstoque.

## Objetivo

Este projeto foi desenvolvido para praticar conceitos de Programacao Orientada a Objetos,
desenvolvimento Android nativo, tratamento de dados locais e boas praticas de persistencia segura.

A proposta central e construir uma base simples, mas robusta, para cadastro e controle de estoque,
com foco em clareza de codigo, validacao de regras de negocio e seguranca no armazenamento local.

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

## Seguranca e Robustez

- Armazenamento em diretorio privado do aplicativo.
- Persistencia em JSON.
- Gravacao atomica dos dados.
- Validacao rigorosa de entrada.
- Protecao contra arquivos corrompidos.
- Limite de tamanho dos arquivos carregados.
- IDs unicos garantidos.
- Backup externo desabilitado.
- Nenhuma permissao sensivel utilizada.
- Nenhum acesso a internet.

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

## Licenca

Todos os direitos reservados.

Este projeto e disponibilizado apenas para fins de estudo, analise de codigo e demonstracao de portfolio.

Nao e permitida a copia, redistribuicao, modificacao ou utilizacao comercial sem autorizacao expressa do autor.
