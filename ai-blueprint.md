# Blueprint de Desenvolvimento Orientado por IA: Budget AI API (Versão Produção Real)

Você atuará como um Engenheiro de Software Sênior especialista em Java 17+, Spring Boot e Arquitetura Hexagonal (Ports & Adapters). Sua missão é construir o projeto `budget-ai-api` seguindo estritamente as especificações de pacotes, dependências e acoplamento descritas neste documento.

## ⚖️ Premissas Críticas de Execução
1. **Isolamento de Frameworks:** A camada `domain` e as interfaces em `application` NÃO devem conter nenhuma anotação do Spring Framework.
2. **Integração Multimodal Real:** As capacidades de IA devem ser executadas através de chamadas HTTP HTTP/REST reais para a API do Google AI Studio, enviando o binário do áudio em formato Base64.
3. **Estabilidade de Compilação:** Cada arquivo gerado deve conter todos os imports necessários. Não use pseudocódigo ou marcadores de omissão como `// restante do código aqui`.

---

## 🏗️ 1. Estrutura de Diretórios e Pacotes

O projeto deve respeitar rigidamente a seguinte árvore sob a raiz `/home/userlnx/docker/script_docker/java-ia/budget-ai-api/`:
```text
budget-ai-api/
├── .env
├── .gitignore
├── ai-blueprint.md
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
            └── application.properties
```

---

## 📦 2. Configurações de Ambiente (Build & Deploy)

### Arquivo: `pom.xml`
Gere um arquivo de configuração Maven estável utilizando as seguintes especificações:
- **Java Version:** 17.
- **Spring Boot Starter Parent:** `3.2.5`.
- **Dependências Core:** `spring-boot-starter-web` e `spring-boot-starter-test` (escopo test).
- **Dependência de Ambiente:** `io.github.cdimascio:dotenv-java:3.0.0` para gerenciar o arquivo `.env`.
- **Plugins:** `maven-compiler-plugin` (versão `3.11.0`) configurado com a release correta do Java, e o plugin `spring-boot-maven-plugin`.

### Arquivo: `sftp-config.json`
Configure um arquivo JSON de sincronização com o host `vmlinuxd`, porta `22`, usuário `userlnx`, senha `1234`. O caminho remoto deve ser `/home/userlnx/docker/script_docker/java-ia/budget-ai-api`. Configure a propriedade `upload_on_save` como falsa e ignore expressões regulares clássicas de IDEs como `.idea`, `target`, `uploads` e `.git`.

### Arquivo: `src/main/resources/application.properties`
Mapeie os parâmetros operacionais do Spring Boot:
- Definição da porta servidora (`server.port=8080`).
- Captura de propriedade de sistema dinâmica para a chave da IA (`google.ai.studio.api.key=${GOOGLE_AI_KEY}`).
- Limites de tamanho de requisição multipart fixados em `10MB` para suportar arquivos de áudio binários.

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

### Passo 5: O Motor de Conexão com Google AI Studio (IA Real Multimodal)
**Caminho:** `src/main/java/dio/infrastructure/BudgetAiEngine.java`
- Crie uma classe anotada com `@Component`. Injete o `TransactionService` via construtor e instancie um `RestTemplate`.
- Capture a chave de autenticação externa do Google usando a anotação `@Value("${google.ai.studio.api.key}")` em um atributo privado `apiKey`.
- Defina uma constante estática para a URL de chamada REST do Gemini: `https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent`.
- Implemente o método `processarAudioEIntencaoReal(File arquivoAudio)`:
  - Deve ler os bytes do arquivo físico e convertê-los em uma String **Base64**.
  - Monte o mapa de requisição estruturado (`contents -> parts`) enviando os dados em formato de payload multimodal inline (`mimeType: "audio/mpeg"`) e o prompt de comando textual em português exigindo o retorno de um JSON puro (contendo `description`, `amount`, e `type`).
  - Configure o cabeçalho HTTP obrigatório `X-goog-api-key` contendo o valor da sua variável.
  - Submeta a requisição POST via `RestTemplate`, faça o parse estruturado dos nós de resposta (`candidates -> content -> parts -> text`) e repasse o JSON resultante para a função de *Tool Calling* interna.
  - Trate o JSON de retorno limpando marcações de markdown e invoque o `transactionService.criarTransacao()` para salvar os dados finais extraídos no core do sistema.

### Passo 6: O Inicializador do Container e Injeção de Ambiente (.env)
**Caminho:** `src/main/java/dio/BudgetAiApiApplication.java`
- Classe de inicialização padrão do Spring Boot anotada com `@SpringBootApplication`.
- No método `main`, antes do bootstrap do Spring, inicialize o leitor `Dotenv.configure().ignoreIfMissing().load()`. Transfira todas as variáveis carregadas do arquivo `.env` para as propriedades de sistema do Java (`System.setProperty`) de forma iterativa.
- Forneça um método explícito anotado com `@Bean` para instanciar manualmente o `TransactionService`, injetando a implementação do repositório gerenciada pelo Spring para manter o desacoplamento de frameworks no Core.

### Passo 7: O Inibidor de Testes Automáticos via Console
**Caminho:** `src/main/java/dio/infrastructure/RunnerTesteIa.java`
- Classe anotada com `@Component` que implementa a interface `CommandLineRunner`.
- O método `run` deve ter sua lógica interna inteiramente comentada. Isso evita disparos de testes automatizados com arquivos de simulação inexistentes na subida da aplicação, deixando o ecossistema livre para receber requisições dinâmicas via API Web.

### Passo 8: O Adaptador de Entrada Web (Controlador REST Multipart)
**Caminho:** `src/main/java/dio/infrastructure/BudgetAiController.java`
- Crie um controlador anotado com `@RestController` mapeando a rota base `@RequestMapping("/api/budget")`.
- Defina uma constante estática apontando para o diretório físico absoluto de armazenamento local no servidor Linux: `/home/userlnx/docker/script_docker/java-ia/budget-ai-api/uploads/`.
- Implemente o endpoint `@PostMapping("/voice")` recebendo um arquivo binário real enviado via formulário através do parâmetro `@RequestParam("file") MultipartFile file`.
  - O método deve validar se o arquivo não está vazio, garantir a criação física da pasta de uploads no disco utilizando `pastaDestino.mkdirs()` caso ela não exista, e transferir o arquivo binário enviado para o armazenamento de arquivos usando `file.transferTo()`.
  - Chame o método `aiEngine.processarAudioEIntencaoReal()` passando o ponteiro do novo arquivo criado e retorne a string de resposta consolidada em um `ResponseEntity.ok()`.
- Implemente o endpoint `@GetMapping("/transactions")` mapeando o retorno do extrato financeiro em JSON com base em `transactionService.listarTodas()`.

### Passo 9: Classe de Simulação Alternativa
**Caminho:** `src/main/java/dio/MainSimulacao.java`
- Crie uma classe Java com um método `main` puro para possibilitar testes manuais via inversão de dependência clássica, instanciando o `TransactionInMemoryAdapter` e acoplando-o diretamente ao `TransactionService` sem subir o ecossistema Spring.

---

## 🛑 4. Protocolo de Validação e Testes

Ao finalizar a geração das classes, garanta que os seguintes comportamentos sejam validados:

1. **Compilação e Inicialização:**
   Comando: `mvn spring-boot:run` após garantir que a variável `GOOGLE_AI_KEY` esteja devidamente preenchida no arquivo `.env`. O servidor deve expor a porta `8080` sem travar logs por falta de arquivos locais.

2. **Validação do Endpoint de Voz via cURL (Upload de Áudio Físico):**
   Execute o comando enviando um arquivo real do seu terminal Linux:
```bash
   curl -X POST http://localhost:8080/api/budget/voice \
     -F "file=@/home/userlnx/docker/script_docker/java-ia/budget-ai-api/audio_real.mp3"
```