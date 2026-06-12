# Blueprint de Desenvolvimento Orientado por IA: Budget AI API

Você atuará como um Engenheiro de Software Sênior especialista em Java 17+, Spring Boot e Arquitetura Hexagonal (Ports & Adapters). Sua missão é construir o projeto `budget-ai-api` seguindo estritamente as especificações de pacotes, dependências e acoplamento descritas neste documento.

## ⚖️ Premissas Críticas de Execução
1. **Isolamento de Frameworks:** A camada `domain` e as interfaces em `application` NÃO devem conter nenhuma anotação do Spring Framework.
2. **Abstração de IA Externa:** Para evitar falhas de download de artefatos Alpha/Snapshot do Spring AI, as capacidades de IA serão simuladas nativamente em infraestrutura através de inversão de controle.
3. **Estabilidade de Compilação:** Cada arquivo gerado deve conter todos os imports necessários. Não use pseudocódigo ou marcadores de omissão como `// restante do código aqui`.

---

## 🏗️ 1. Estrutura de Diretórios e Pacotes

O projeto deve respeitar rigidamente a seguinte árvore sob a raiz `/home/userlnx/docker/script_docker/java-ia/budget-ai-api/`:

budget-ai-api/
├── pom.xml
├── sftp-config.json
└── src/
    └── main/
        ├── java/
        │   └── dio/
        │       ├── BudgetAiApiApplication.java
        │       ├── MainSimulacao.java
        │       ├── application/
        │       │   ├── input/
        │       │   │   └── TransactionService.java
        │       │   └── output/
        │       │       └── TransactionRepository.java
        │       ├── domain/
        │       │   └── Transaction.java
        │       └── infrastructure/
        │           ├── BudgetAiController.java
        │           ├── BudgetAiEngine.java
        │           ├── RunnerTesteIa.java
        │           └── TransactionInMemoryAdapter.java
        └── resources/

---

## 📦 2. Configurações de Ambiente (Build & Deploy)

### Arquivo: `pom.xml`
Gere um arquivo de configuração Maven estável utilizando as seguintes especificações:
- **Java Version:** 17 (ou 21).
- **Spring Boot Starter Parent:** `3.2.5`.
- **Dependências Core:** `spring-boot-starter-web` e `spring-boot-starter`.
- **Plugins:** `maven-compiler-plugin` (versão `3.11.0`) configurado com a release correta do Java, e o plugin `spring-boot-maven-plugin`.

### Arquivo: `sftp-config.json`
Configure um arquivo JSON de sincronização com o host `vmlinuxd`, porta `22`, usuário `userlnx`, senha `1234`. O caminho remoto deve ser `/home/userlnx/docker/script_docker/java-ia/budget-ai-api`. Configure a propriedade `upload_on_save` como falsa e ignore expressões regulares clássicas de IDEs como `.idea`, `target`, e `.git`.

---

## 💻 3. Especificação de Código Fonte (Arquivo por Arquivo)

### Passo 1: O Domínio Puro
**Caminho:** `src/main/java/dio/domain/Transaction.java`
- Crie um POJO (Plain Old Java Object) que encapsule uma transação financeira.
- Atributos privados: `id` (Long), `description` (String), `amount` (BigDecimal), `type` (String, aceitando 'INCOME' ou 'EXPENSE'), e `createdAt` (LocalDateTime).
- Forneça construtores (padrão e populado), métodos getters/setters e uma sobrescrita limpa do método `toString()`.

### Passo 2: A Porta de Saída
**Caminho:** `src/main/java/dio/application/output/TransactionRepository.java`
- Crie uma interface Java pura.
- Defina os métodos abstratos contratos de persistência:
  - `Transaction save(Transaction transaction);`
  - `List<Transaction> findAll();`

### Passo 3: O Caso de Uso (Core da Aplicação)
**Caminho:** `src/main/java/dio/application/input/TransactionService.java`
- Crie uma classe de serviço isolada de anotações do Spring.
- Ela deve possuir uma dependência final da interface `TransactionRepository` injetada obrigatoriamente via construtor.
- Implemente os métodos de negócio:
  - `criarTransacao(Transaction transaction)`: Deve logar o processamento da regra de negócio usando a descrição e acionar o repositório.
  - `listarTodas()`: Deve delegar a busca de dados para a porta de saída.

### Passo 4: O Adaptador de Infraestrutura de Banco
**Caminho:** `src/main/java/dio/infrastructure/TransactionInMemoryAdapter.java`
- Crie uma classe anotada com `@Repository` que implementa a interface `TransactionRepository`.
- Simule um banco de dados interno utilizando uma lista thread-safe (`Collections.synchronizedList`) e gerencie IDs incrementais automáticos através de um `AtomicLong` iniciando em 1.

### Passo 5: O Motor Simulador de IA (Speech-to-Text & Tool Calling)
**Caminho:** `src/main/java/dio/infrastructure/BudgetAiEngine.java`
- Crie uma classe anotada com `@Component`. Injete o `TransactionService` via construtor.
- Implemente o método `transcreverAudio(String nomeArquivoAudio)`: Deve interceptar arquivos de áudio fictícios. Se o nome contiver "gasto_cafe", retorne o texto correspondente a um gasto; se contiver "recebi_salario", retorne texto de ganho.
- Implemente o método `processarIntencaoEToolCalling(String textoTranscrevido)`: Deve realizar análise semântica básica por palavras-chave (ex: "cafe", "salario"), mapear os dados para um objeto `Transaction` e invocar de forma automatizada o método `transactionService.criarTransacao()`.

### Passo 6: O Inicializador do Container e Definição de Beans
**Caminho:** `src/main/java/dio/BudgetAiApiApplication.java`
- Classe de inicialização padrão do Spring Boot anotada com `@SpringBootApplication`.
- Forneça um método explícito anotado com `@Bean` para instanciar manualmente o `TransactionService`, injetando a implementação do repositório gerenciada pelo Spring.

### Passo 7: O Testador de Inicialização em Console
**Caminho:** `src/main/java/dio/infrastructure/RunnerTesteIa.java`
- Classe anotada com `@Component` que estende a interface `CommandLineRunner`.
- No método `run`, execute uma simulação imediata do fluxo passando o arquivo virtual `"gasto_cafe.mp3"` para o `aiEngine` validar o acoplamento das camadas antes de abrir o servidor HTTP.

### Passo 8: O Adaptador de Entrada Web (Controlador REST)
**Caminho:** `src/main/java/dio/infrastructure/BudgetAiController.java`
- Crie um controlador anotado com `@RestController` mapeando a rota base `@RequestMapping("/api/budget")`.
- Implemente o endpoint `@PostMapping("/voice")` recebendo uma string contendo o nome do arquivo de áudio através de um `@RequestParam("file")`. Este método deve acionar as rotas do `BudgetAiEngine`.
- Implemente o endpoint `@GetMapping("/transactions")` (garantindo a capitalização correta do Spring) mapeando o retorno JSON de todas as transações guardadas na memória.

### Passo 9: Classe de Simulação Alternativa
**Caminho:** `src/main/java/dio/MainSimulacao.java`
- Crie uma classe Java com um método `main` puro para possibilitar testes manuais via inversão de dependência clássica, instanciando o `TransactionInMemoryAdapter` e acoplando-o diretamente ao `TransactionService` sem subir o ecossistema Spring.

---

## 🛑 4. Protocolo de Validação e Testes

Ao finalizar a geração das classes, garanta que os seguintes comportamentos sejam validados:

1. **Compilação e Execução via Console:**
   Comando: `mvn spring-boot:run`
   *Log esperado:* Inicialização do Tomcat na porta `8080`, seguido pelo disparo automático do log estruturado da IA registrando a transação simulada do café com ID 1.

2. **Validação das Rotas HTTP:**
   Simule ou prepare chamadas usando o protocolo HTTP nativo:
   - **POST** `http://localhost:8080/api/budget/voice?file=recebi_salario.mp3`
     *Saída:* Mensagem de sucesso confirmando o processamento do salário por IA.
   - **GET** `http://localhost:8080/api/budget/transactions`
     *Saída:* Retorno em formato JSON válido contendo a lista indexada de objetos com IDs auto-incrementados.