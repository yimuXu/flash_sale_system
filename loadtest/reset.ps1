param([int]$Stock = 100, [switch]$Preheat)

$psql = "D:\usyd_resource\postgresSQL\bin\psql.exe"
$env:PGPASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }

Write-Host "1/3 Resetting database (stock=$Stock)..."
& $psql -U postgres -d flash_sale -v ON_ERROR_STOP=1 -v stock=$Stock -f "$PSScriptRoot\reset.sql"
if ($LASTEXITCODE -ne 0) { throw "Database reset failed" }

if ($NoPreheat) { Write-Host "Skipping preheat (phase 1)"; return }

Write-Host "2/3 Preheating Redis..."
curl.exe -s -f -X POST http://localhost:8081/admin/seckill/1/preheat
if ($LASTEXITCODE -ne 0) { throw "Preheat failed - is the app running on 8081?" }

Write-Host "3/3 Redis stock:" (docker exec flash-sale-redis redis-cli GET seckill:stock:1)
