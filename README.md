Desafio Beca Java 2026

Sobre o projeto:
Sistema de gestão de finanças pessoais desenvolvido como parte do Desafio Beca Java 2026.
O projeto tem como objetivo aplicar conhecimentos de desenvolvimento Java, construção de APIs REST, persistência de dados e arquitetura de microsserviços.

Tecnologias:
Java 25
Maven
Spring Boot
Spring Data JPA
PostgreSQL
Docker e Docker Compose
Springdoc OpenAPI
Git e GitHub

Arquitetura:
O sistema é organizado em microsserviços com responsabilidades independentes:
MS1 - Usuários: responsável pelo cadastro, consulta, atualização, exclusão e importação de usuários por CSV.
MS2 - Transações: responsável pelo processamento das operações financeiras.

Status do projeto:
Em desenvolvimento.

Estrutura do projeto:
O projeto utiliza uma estrutura monorepo, mantendo os microsserviços organizados em um único repositório.
DesafioBecaJava2026/
├── users/
│   ├── src/
│   ├── exemplos/
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
Microsserviço de Usuários (MS1)
Responsável pelo gerenciamento de usuários, oferecendo:
Cadastro de usuários.
Consulta e listagem de usuários.
Atualização e exclusão de usuários.
Validação de dados e CPF.
Criptografia de senhas com BCrypt.
Importação de usuários em massa por arquivo CSV.

Pré-requisitos:
Para executar o projeto, é necessário ter instalado:
Java 25
Docker
Docker Compose
Git

Configuração do ambiente
Clone o repositório:
git clone https://github.com/gabsbackess/desafio-beca-java-2026.git

Entre na pasta do projeto:
cd desafio-beca-java-2026

Crie o arquivo .env a partir do exemplo:
cp .env.example .env

Configure no arquivo .env as credenciais dos bancos PostgreSQL.
Importante: o arquivo .env contém credenciais e configurações locais dos bancos de dados. Ele não deve ser versionado nem enviado ao GitHub.
O arquivo .env.example deve ser utilizado como modelo para configurar o ambiente. Ele contém apenas os nomes das variáveis, sem credenciais reais.

Executando o projeto
Na raiz do repositório, execute:
docker compose up -d --build
Esse comando constrói a imagem do microsserviço de usuários e inicia os serviços configurados no Docker Compose.
Para verificar os containers:
docker compose ps
Para acompanhar os logs do microsserviço de usuários:
docker compose logs -f users
Para parar os serviços:
docker compose down

Portas utilizadas:
Serviço: Microsserviço de Usuários
Porta local: 8081
Serviço: PostgreSQL Users DB
Porta local: 5433
Serviço: PostgreSQL Transactions DB
Porta local: 5434


Microsserviço de Usuários (MS1)
O MS1 disponibiliza uma API REST para o gerenciamento de usuários.
URL base: http://localhost:8081
Endpoints disponíveis
POST /users: cadastrar um novo usuário.
GET /users: listar todos os usuários.
GET /users/{id}: buscar um usuário pelo ID.
PUT /users/{id}: atualizar os dados de um usuário.
DELETE /users/{id}: excluir um usuário.
POST /users/import: importar usuários em massa por meio de um arquivo CSV.
Documentação Swagger
Com o projeto em execução, acesse:
http://localhost:8081/swagger-ui.html
O Swagger permite visualizar a documentação da API e testar seus endpoints diretamente pelo navegador.

Importação de usuários por CSV
O MS1 permite realizar o cadastro em massa de usuários por meio do envio de um arquivo CSV.
Formato esperado do arquivo:
name,email,cpf,password
Maria Silva,maria.silva@example.com,12345678901,Senha@123
Joao Santos,joao.santos@example.com,23456789012,Senha@456
Regras da importação:
O arquivo deve possuir as colunas name, email, cpf e password.
O limite máximo é de 1.000 registros por arquivo.
O tamanho máximo permitido para upload é de 5 MB.
Registros inválidos são identificados individualmente, sem interromper o processamento das demais linhas.
O resultado informa a quantidade de registros processados, cadastrados e com erro.
Endpoint: POST /users/import
Um arquivo de exemplo está disponível em users/exemplos/usuarios-exemplo.csv.

Testes automatizados
O microsserviço de usuários possui testes automatizados para validar suas funcionalidades e regras de negócio.
Os testes utilizam:
JUnit: framework para execução dos testes automatizados.
Mockito: biblioteca para criação de objetos simulados (mocks) e isolamento de dependências.
Executando os testes
Na pasta users, execute:
./mvnw test
Para executar os testes com limpeza e compilação completa:
./mvnw clean test

Observações:
Os comandos devem ser executados dentro da pasta users.
As variáveis de ambiente do arquivo .env devem estar carregadas quando os testes precisarem acessar a configuração do banco de dados.
No Git Bash, a partir da pasta users, carregue as variáveis com:
set -a
source ../.env
set +a

Respostas HTTP
A API utiliza códigos de status HTTP para indicar o resultado das operações:
200 OK: operação realizada com sucesso.
201 Created: usuário cadastrado com sucesso.
400 Bad Request: dados de entrada inválidos.
404 Not Found: usuário não encontrado.
409 Conflict: conflito no cadastro, como e-mail ou CPF já existente.