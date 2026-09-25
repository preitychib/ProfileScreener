#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OVERLAY="${1:-}"
IMAGE="${IMAGE:-profilescreener-api:local}"

usage() {
  echo "Usage: $0 <openai|groq-cloud-embedding|groq-local-embedding|local-slm>"
  exit 1
}

[[ -n "$OVERLAY" ]] || usage
case "$OVERLAY" in
  openai|groq-cloud-embedding|groq-local-embedding|local-slm) ;;
  *) usage ;;
esac

echo "==> Building image ${IMAGE}"
docker build -t "${IMAGE}" "${ROOT}"

# Minikube cannot see host Docker images unless we load them
if kubectl config current-context 2>/dev/null | grep -q minikube; then
  echo "==> Loading ${IMAGE} into minikube"
  minikube image load "${IMAGE}"
fi

echo "==> Applying overlay: ${OVERLAY}"
kubectl apply -k "${ROOT}/k8s/overlays/${OVERLAY}"

echo "==> Done. Namespace: profilescreener"
kubectl -n profilescreener get pods,svc,pvc
