# Blueprint de Desenvolvimento Orientado por IA: Budget AI API (Versão Produção Real)

Você atuará como um Engenheiro de Software Sênior especialista em Java 17+, Spring Boot e Arquitetura Hexagonal (Ports & Adapters). Sua missão é construir e manter o projeto `budget-ai-api` seguindo estritamente as especificações de pacotes, dependências e acoplamento descritas neste documento.

## ⚖️ Premissas Críticas de Execução
1. **Isolamento de Frameworks:** A camada `domain` e as interfaces em `application` NÃO devem conter nenhuma anotação do Spring Framework ou Jakarta Persistence (`@Entity`, `@Id`, etc.).
2. **Arquitetura Hexagonal Estrita:** A persistência de dados real deve usar o padrão de mapeamento de infraestrutura (*TransactionEntity*), convertendo os dados de forma bidirecional para manter o Domínio 100% puro e isolado.
3. **Integração Multimodal Real:** As capacidades de IA devem ser executadas através de chamadas HTTP/REST reais para a API do Google AI Studio, enviando o binário do áudio em formato Base64.
4. **Estabilidade de Compilação:** Cada arquivo gerado deve conter todos os imports necessários. Não use pseudocódigo ou marcadores de omissão como `// restante do código aqui`.

---

## 🏗️ 1. Estrutura de Diretórios e Pacotes
```text
O projeto deve respeitar rigidamente a seguinte árvore sob a raiz `/home/userlnx/docker/script_docker/java-ia/budget-ai-api/`:

    budget-ai-api/
    ├── .env
    ├── .gitignore
    ├── ai-blueprint.md
    ├── docker-compose.yml
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
            │       │   ├── DashboardReport.java
            │       │   └── Transaction.java
            │       └── infrastructure/
            │           ├── BudgetAiController.java
            │           ├── BudgetAiEngine.java
            │           ├── RunnerTesteIa.java
            │           ├── SpringPostgresRepository.java
            │           ├── TransactionEntity.java
            │           ├── TransactionInMemoryAdapter.java
            │           └── TransactionPostgresAdapter.java
            └── resources/
                └── application.properties
```

---

## 📦 2. Configurações de Ambiente (Build & Deploy)

### Arquivo: `pom.xml`
Gere um arquivo de configuração Maven estável utilizando as seguintes especificações:
- **Java Version:** 17 (compatível com execuções em Java 21).
- **Spring Boot Starter Parent:** `3.2.5`.
- **Dependências Core:** `spring-boot-starter-web`, `spring-boot-starter-data-jpa` e `spring-boot-starter-test` (escopo test).
- **Dependência de Banco de Dados:** `org.postgresql:postgresql` (escopo runtime).
- **Dependência de Ambiente:** `io.github.cdimascio:dotenv-java:3.0.0` para gerenciar o arquivo `.env`.
- **Plugins:** `maven-compiler-plugin` (versão `3.11.0`) e o plugin `spring-boot-maven-plugin`.

### Arquivo: `docker-compose.yml`
Provisionamento automatizado do banco de dados relacional oficial. Deve subir um container baseado em `postgres:15-alpine`, nomeado como `budget-postgres`, expondo a porta `5432:5432`, mapeando as credenciais de ambiente locais (`POSTGRES_USER: userlnx`, `POSTGRES_PASSWORD: super_senha_123`, `POSTGRES_DB: budget_ai_db`) e isolando os dados através de um volume nomeado (`pgdata`).

### Arquivo: `sftp-config.json`
Configure um arquivo JSON de sincronização com o host `vmlinuxd`, porta `22`, usuário `userlnx`, senha `1234`. O caminho remoto deve ser `/home/userlnx/docker/script_docker/java-ia/budget-ai-api`. Configure a propriedade `upload_on_save` como falsa e ignore expressões regulares clássicas de IDEs como `.idea`, `target`, `uploads` e `.git`.

### Arquivo: `src/main/resources/application.properties`
Mapeie os parâmetros operacionais do Spring Boot e do ecossistema de banco de dados:
- Definição da porta servidora (`server.port=8080`).
- Captura de propriedade de sistema dinâmica para a chave da IA (`google.ai.studio.api.key=${GOOGLE_AI_KEY}`).
- Limites de tamanho de requisição multipart fixados em `10MB` para suportar arquivos de áudio binários.
- Driver de conexão apontando para `org.postgresql.Driver` utilizando a URL de conexão do container Docker: `jdbc:postgresql://localhost:5432/budget_ai_db`.
- Estratégia de DDL configurada como `update` para geração automática de tabelas a partir das entidades de infraestrutura, com logs de SQL ativos (`spring.jpa.show-sql=true`).

---

## 💻 3. Especificação de Código Fonte (Evoluídos por Casos de Uso)

### Passo 1: O Domínio Puro e Objetos de Valor
**Caminhos:** `src/main/java/dio/domain/Transaction.java` e `src/main/java/dio/domain/DashboardReport.java`
- **Transaction:** POJO encapsulando uma transação financeira. Contém `id` (Long), `description` (String), `amount` (BigDecimal), `type` (String: 'INCOME' ou 'EXPENSE'), e `createdAt` (LocalDateTime). Forneça construtores, getters/setters e `toString()`.
- **DashboardReport:** Um componente nativo do Java do tipo `record`. Responsável por transportar de forma imutável as métricas de agregação financeira: `totalIncome` (BigDecimal), `totalExpense` (BigDecimal) e `balance` (BigDecimal).

### Passo 2: A Porta de Saída
**Caminho:** `src/main/java/dio/application/output/TransactionRepository.java`
- Interface Java pura que assina o contrato abstrato de persistência para as transações:
  - `Transaction save(Transaction transaction);`
  - `List<Transaction> findAll();`

### Passo 3: O Caso de Uso (Core com Agregações via Stream API)
**Caminho:** `src/main/java/dio/application/input/TransactionService.java`
- Classe de serviço pura isolada do Spring. Recebe `TransactionRepository` via injeção por construtor.
- **criarTransacao(Transaction)**: Registra, loga a descrição e retorna a transação salva.
- **listarTodas()**: Recupera o histórico completo chamando a porta de saída.
- **obterRelatorioDashboard()**: Executa a lógica de negócios analítica. Consome todos os dados da porta de saída e utiliza a Stream API do Java com *Method References* (`Transaction::getAmount`) para filtrar, mapear e reduzir (`reduce`) os somatórios segregados por tipo, computando dinamicamente o saldo consolidado (`balance = totalIncome - totalExpense`).

### Passo 4: O Mapeamento de Banco e Interfaces JPA (Infraestrutura)
**Caminhos:** `src/main/java/dio/infrastructure/TransactionEntity.java` e `src/main/java/dio/infrastructure/SpringPostgresRepository.java`
- **TransactionEntity:** Modelo mapeado com anotações relacionais Jakarta (`@Entity`, `@Table(name = "tb_transactions")`, `@Id`, `@GeneratedValue`). Contém métodos estáticos de conversão bidirecional (`fromDomain` e `toDomain`) para manter a barreira arquitetural imposta pelo Domínio Puro.
- **SpringPostgresRepository:** Interface de infraestrutura que estende `JpaRepository<TransactionEntity, Long>`, concedendo acesso às transações nativas de persistência no PostgreSQL.

### Passo 5: O Adaptador do Banco de Dados Real (PostgreSQL)
**Caminho:** `src/main/java/dio/infrastructure/TransactionPostgresAdapter.java`
- Classe anotada com `@Component` e demarcada com `@Primary` para assume a precedência de injeção sobre adaptadores temporários em memória. Implementa a porta de saída `TransactionRepository`.
- Orquestra as conversões de tipo mapeando as operações recebidas para `TransactionEntity`, persistindo-as através do `SpringPostgresRepository` e devolvendo objetos puros de domínio nas saídas das operações de persistência e listagem.

### Passo 6: O Motor de Conexão com Google AI Studio (IA Real Multimodal)
**Caminho:** `src/main/java/dio/infrastructure/BudgetAiEngine.java`
- Classe anotada com `@Component` que envia o arquivo de áudio local convertido em **Base64** em um payload REST estruturado (`contents -> parts`) para a API do Gemini via `RestTemplate`. Exige o retorno de um JSON puro contendo metadados financeiros, limpa marcações em bloco de markdown e aciona o core da aplicação para persistir o resultado.

### Passo 7: O Inicializador do Container e Injeção de Ambiente (.env)
**Caminho:** `src/main/java/dio/BudgetAiApiApplication.java`
- Classe de inicialização padrão do Spring Boot anotada com `@SpringBootApplication`.
- No método `main`, antes do bootstrap do Spring, inicialize o leitor `Dotenv.configure().ignoreIfMissing().load()`. Transfira todas as variáveis carregadas do arquivo `.env` para as propriedades de sistema do Java (`System.setProperty`) de forma iterativa.
- Forneça um método explícito anotado com `@Bean` para instanciar manualmente o `TransactionService`, injetando a implementação do repositório gerenciada pelo Spring para manter o desacoplamento de frameworks no Core.

### Passo 8: O Adaptador de Entrada Web (Controlador REST com Painel)
**Caminho:** `src/main/java/dio/infrastructure/BudgetAiController.java`
- Controlador `@RestController` mapeando a rota base `/api/budget`.
- **POST `/voice`**: Processa uploads via `MultipartFile`, salvando os arquivos físicos na pasta local `/uploads/` e disparando o motor de IA.
- **GET `/transactions`**: Expõe a listagem pura das transações salvas.
- **GET `/dashboard`**: Expõe os dados consolidados analíticos obtidos através da chamada do caso de uso `transactionService.obterRelatorioDashboard()`.

### Passo 9: Classe de Simulação Alternativa
**Caminho:** `src/main/java/dio/MainSimulacao.java`
- Crie uma classe Java com um método `main` puro para permitir testes manuais via inversão de dependência clássica, instanciando o `TransactionInMemoryAdapter` e acoplando-o diretamente ao `TransactionService` sem subir o ecossistema Spring.

---

## 🛑 4. Protocolo de Validação e Testes em Produção

Ao finalizar a execução das classes, garanta que os seguintes comportamentos sejam validados:

1. **Subida do Ambiente de Dados (Docker):**
   Comando: `docker compose up -d`. O container do PostgreSQL deve constar como ativo e mapear o volume de armazenamento permanente com sucesso.

2. **Compilação e Bootstrap da API:**
   Comando: `mvn clean spring-boot:run`. O console deve registrar o binding do pool Hikari com o PostgreSQL e exibir a instrução SQL DDL executada pelo Hibernate: `create table tb_transactions (...)`.

3. **Validação do Painel Consolidado via cURL:**
   Execute a chamada HTTP para verificar a agregação dinâmica em tempo real:
   
```bash
    curl -X GET http://localhost:8080/api/budget/dashboard
      *Retorno esperado:* Um payload JSON contendo as chaves numéricas estruturadas e computadas pela Stream API:
       {"totalIncome":500.00,"totalExpense":0,"balance":500.00}

   curl -X POST -F "file=@audio_real.mp3" "http://localhost:8080/api/budget/voice"
    *Retorno esperado:* Uma string contendo respostas computadas pela Stream API:
        Sucesso! Áudio interpretado pelo Google AI Studio e registrado. ID: 4 | Tipo: INCOMEuserlnx@vmlinuxd:~/docker/script_docker/java-ia/budget-ai-api$ 

   export GOOGLE_AI_KEY="sua key" mvn spring-boot:run   
```