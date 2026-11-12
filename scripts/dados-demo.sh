#!/usr/bin/env bash
# Carrega dados de exemplo para a demonstração. Opcional: rode UMA vez, com o ambiente no ar.
#   export MOCKAPI_URL=https://xxxx.mockapi.io/api/v1
#   bash scripts/dados-demo.sh
# Faz: (1) importa users/exemplos/usuarios-exemplo.csv no MS1; (2) cria a conta da Maria no MockAPI;
#      (3) registra transações de exemplo no MS2.
set -u

MS1="${MS1_URL:-http://localhost:8081}"
MS2="${MS2_URL:-http://localhost:8082}"
EMAIL="maria.silva@example.com"
PASSWORD="Senha@123"
CSV="users/exemplos/usuarios-exemplo.csv"

if [ -z "${MOCKAPI_URL:-}" ]; then
  echo "Defina MOCKAPI_URL antes (ex.: export MOCKAPI_URL=https://xxxx.mockapi.io/api/v1)." >&2
  exit 1
fi
if [ ! -f "$CSV" ]; then
  echo "Rode o script na raiz do repositório (não achei $CSV)." >&2
  exit 1
fi

echo "1) Importando usuários do CSV no MS1..."
curl -s -X POST "$MS1/users/import" -F "file=@$CSV;type=text/csv"
echo

echo "2) Descobrindo o id da Maria..."
AUTH_JSON=$(curl -s -X POST "$MS1/users/authenticate" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
USER_ID=$(echo "$AUTH_JSON" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
if [ -z "$USER_ID" ]; then
  echo "Não consegui autenticar a Maria no MS1. Resposta: $AUTH_JSON" >&2
  exit 1
fi
echo "   id: $USER_ID"

echo "3) Criando a conta da Maria no MockAPI..."
curl -s -X POST "$MOCKAPI_URL/accounts" -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"balance\":2000,\"perTransactionLimit\":500,\"dailyLimit\":1000}"
echo

tx() {
  curl -s -u "$EMAIL:$PASSWORD" -X POST "$MS2/transactions" -H "Content-Type: application/json" -d "$1"
  echo
}

echo "4) Registrando transações de exemplo no MS2..."
tx '{"type":"DEPOSIT","amount":3000.00,"description":"Salario"}'
tx '{"type":"PURCHASE","amount":120.00,"category":"FOOD","description":"Mercado"}'
tx '{"type":"PURCHASE","amount":85.50,"category":"FOOD","description":"Restaurante"}'
tx '{"type":"PURCHASE","amount":230.00,"category":"TRANSPORT","description":"Combustivel"}'
tx '{"type":"PURCHASE","amount":60.00,"category":"LEISURE","description":"Cinema"}'
tx '{"type":"PURCHASE","amount":45.00,"category":"LEISURE","description":"Livro"}'
tx '{"type":"WITHDRAWAL","amount":5000.00,"description":"Saque acima do saldo"}'

echo "Pronto. Entre no Swagger do MS2 (http://localhost:8082/swagger-ui.html), clique em Authorize"
echo "com $EMAIL / $PASSWORD e veja /expenses/summary e /transactions/report."
