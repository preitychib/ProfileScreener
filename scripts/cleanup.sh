#!/usr/bin/env bash
set -euo pipefail
echo "==> Deleting namespace profilescreener (API, Redis, Ollama, PVCs)"
kubectl delete namespace profilescreener --ignore-not-found
echo "==> Cleanup complete"
