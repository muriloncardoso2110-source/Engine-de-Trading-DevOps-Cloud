# Runbook local

## Java/Maven no Windows

Verifique `java -version` (21) e `mvn -version` (3.9+), execute `mvn clean verify` e depois `scripts\start-windows.bat`. Se a porta 8080 estiver ocupada, defina a variável `PORT` antes de iniciar.

## Observabilidade com Compose

Crie `.env` na raiz com `GRAFANA_PASSWORD=uma-senha-local-forte`. Execute `docker compose up --build`. Verifique `/health`, Prometheus em `localhost:9090/targets` e o datasource provisionado no Grafana `localhost:3000`. Encerre com `docker compose down`.

## Kubernetes com kind

```bash
kind create cluster --name aurora
docker build -t aurora-exchange:local .
kind load docker-image aurora-exchange:local --name aurora
kubectl apply -f infra/k8s/deployment.yaml
kubectl -n aurora rollout status deployment/aurora-exchange
kubectl -n aurora port-forward svc/aurora-exchange 8080:8080
```

Se não subir: `kubectl -n aurora get pods`, `kubectl -n aurora describe pod <pod>` e `kubectl -n aurora logs deployment/aurora-exchange`. O manifesto fixa uma réplica porque o estado é somente local.

## Terraform

O exemplo em `infra/terraform` espera acesso a um cluster Kubernetes via kubeconfig. Execute `terraform init`, `terraform plan` e `terraform apply` **somente** em laboratório. O exemplo cria um namespace isolado `aurora-tf` e não publica a aplicação nem cria recursos em provedores externos.

## Recuperação

Não existe restore de ordens: ao reiniciar o processo, o livro e as contas são reinicializados. Isso é aceitável somente para simulação. Em incidentes reais, antes de reiniciar uma engine financeira seria obrigatório ter durabilidade e processo auditável de recuperação.
