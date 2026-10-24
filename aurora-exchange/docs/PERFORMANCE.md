# Plano de performance

## Hipótese

Sustentar 100 mil submissões internas de ordens/segundo, em uma carga fixa de 10 segundos, mantendo p99 abaixo de 10 ms e sem erros de processamento. Não equivale a 100 mil RPS de HTTP.

## Procedimento

1. Execute `mvn clean verify` antes de medir.
2. Feche processos de alto consumo e mantenha a máquina conectada à energia.
3. Registre hardware, SO, versão do Java, heap e flags JVM.
4. Execute `scripts/benchmark-windows.bat` ou o script Linux; repita pelo menos 3 vezes.
5. Guarde cada linha de resultado integralmente em um arquivo próprio; relate mediana e pior p99, não apenas a melhor rodada.
6. Para RPS HTTP, rode um gerador de carga separado contra `/orders` em ambiente isolado e registre códigos de status e latência fim a fim.

## O que a métrica representa

A duração é medida do envio para a engine até o término do processamento. O teste controla o ritmo de chegada. O `achieved_rps` usa tempo de emissão + conclusão de todo o lote, de modo que uma fila acumulada reduz a vazão efetiva. Os percentis são computados sobre as ordens do lote.

## Registro de experimento

| Campo | Valor |
|---|---|
| Data | preencher após executar |
| Máquina / CPU / RAM | preencher |
| JDK e flags | preencher |
| Alvo | 100000/s |
| Duração | 10 s |
| Vazão obtida | **não medida neste pacote** |
| p99 | **não medida neste pacote** |
| Exceções | **não medidas neste pacote** |

Um PASS no benchmark interno não certifica comportamento sob HTTP, contingência, tráfego de produção ou durabilidade de liquidação.
