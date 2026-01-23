# Deployment

This directory contains deployment configurations organized by environment.

## Environments

| Directory | Description |
|-----------|-------------|
| `dev/` | Local development with Docker Compose |
| `production/` | Production deployment (Helm charts - coming soon) |

## Quick Start (Dev)

```bash
cd dev
docker compose up -d
```

Then run each microservice from the repository root. See [dev/README.md](dev/README.md) for details.
