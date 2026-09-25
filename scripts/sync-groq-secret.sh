#!/usr/bin/env bash
# Sync .env keys into the cluster secret and force groq-local-embedding.
# Does not print secret values.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "Missing .env"
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

: "${GROQ_API_KEY:?GROQ_API_KEY missing in .env}"
if [[ "$GROQ_API_KEY" == REPLACE_ME || "$GROQ_API_KEY" == gsk_... || ${#GROQ_API_KEY} -lt 20 ]]; then
  echo "GROQ_API_KEY in .env looks invalid (too short / placeholder)"
  exit 1
fi

echo "==> Applying groq-local-embedding overlay"
kubectl apply -k k8s/overlays/groq-local-embedding

echo "==> Updating Secret from .env (values not printed)"
kubectl -n profilescreener create secret generic profilescreener-secrets \
  --from-literal=GROQ_API_KEY="$GROQ_API_KEY" \
  --from-literal=OPENAI_API_KEY="${OPENAI_API_KEY:-}" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "==> Restarting API"
kubectl -n profilescreener rollout restart deployment/profilescreener-api
kubectl -n profilescreener rollout status deployment/profilescreener-api --timeout=180s

echo "==> Pod env check (keys redacted)"
kubectl -n profilescreener exec deploy/profilescreener-api -- printenv \
  | grep -E '^(LLM_PROVIDER|GROQ_CHAT_MODEL|GROQ_BASE_URL|GROQ_API_KEY|OPENAI_API_KEY)=' \
  | sed -E 's/(API_KEY=).*/\1***redacted***/'

echo "==> Done. Re-try upload/ingest."
