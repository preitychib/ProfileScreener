# ProfileScreener Kubernetes

Chat and embeddings are **separate concerns**. Overlays pick the provider pair and only attach the Ollama component when local models are required.

## Layout

```
k8s/
├── base/                  # namespace, API, Redis(+PVC), ConfigMap, Secret
├── components/ollama/     # Ollama Deployment, Service, PVC (optional)
└── overlays/
    ├── openai/                  # OpenAI chat + OpenAI embeddings
    ├── groq-cloud-embedding/    # Groq chat + OpenAI embeddings
    ├── groq-local-embedding/    # Groq chat + Ollama embeddings
    └── local-slm/               # Ollama chat + Ollama embeddings
```

## Overlay → cluster footprint

| Overlay | Pods | Chat | Embeddings | Redis dim |
|---------|------|------|------------|-----------|
| `openai` | API + Redis | OpenAI | OpenAI | 1536 |
| `groq-cloud-embedding` | API + Redis | Groq | OpenAI | 1536 |
| `groq-local-embedding` | API + Redis + Ollama | Groq | Ollama `nomic-embed-text` | 768 |
| `local-slm` | API + Redis + Ollama | Ollama `qwen3:1.7b` | Ollama `nomic-embed-text` | 768 |

## Prerequisites

1. Docker + kubectl + a cluster (kind/minikube/k3d/cloud)
2. Replace secret placeholders:

```bash
kubectl -n profilescreener create secret generic profilescreener-secrets \
  --from-literal=OPENAI_API_KEY='sk-...' \
  --from-literal=GROQ_API_KEY='gsk-...' \
  --dry-run=client -o yaml | kubectl apply -f -
```

(For `local-slm` only, secrets can stay placeholders.)

## Deploy

```bash
# OpenAI (lowest resources — no Ollama)
./scripts/deploy-openai.sh

# Groq chat + OpenAI embeddings (no Ollama)
./scripts/deploy-groq-cloud-embedding.sh

# Groq chat + local embeddings
./scripts/deploy-groq-local-embedding.sh

# Fully local SLM
./scripts/deploy-local.sh

# Tear down
./scripts/cleanup.sh
```

Or: `./scripts/deploy.sh <overlay-name>`

Image default: `profilescreener-api:local` (built by the script). Override with `IMAGE=myregistry/api:tag ./scripts/deploy.sh openai`.

## Port-forward (demo)

```bash
kubectl -n profilescreener port-forward svc/profilescreener-api 8083:8083
```

## Notes

- Switching overlays with different embedding dims requires re-ingest (flush Redis index).
- Ollama initContainer pulls `qwen3:1.7b` + `nomic-embed-text` once into the PVC (demo-sized).
- H2 is in-memory: API pod restart loses candidate/interview rows (demo OK).
- Bare-minimum resource requests are set for a laptop/demo cluster.
