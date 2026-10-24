terraform {
  required_version = ">= 1.5.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.32"
    }
  }
}
provider "kubernetes" { config_path = pathexpand(var.kubeconfig_path) }
variable "kubeconfig_path" { type = string; default = "~/.kube/config" }
resource "kubernetes_namespace_v1" "aurora" {
  metadata { name = "aurora-tf" }
}
output "namespace" { value = kubernetes_namespace_v1.aurora.metadata[0].name }
