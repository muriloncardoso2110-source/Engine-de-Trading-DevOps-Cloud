$ErrorActionPreference='Stop'
Write-Host 'Health:'
Invoke-RestMethod 'http://localhost:8080/health' | ConvertTo-Json
Write-Host 'Open ask on account 2:'
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/orders' -Body @{account=2;side='SELL';price=100;quantity=5} | ConvertTo-Json
Write-Host 'Buy from account 1:'
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/orders' -Body @{account=1;side='BUY';price=110;quantity=5} | ConvertTo-Json
Invoke-RestMethod 'http://localhost:8080/trades' | ConvertTo-Json
