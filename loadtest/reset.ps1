param([int]$Stock = 100,[int]$Users = 1000,[switch]$NoPreheat)

function New-Jwt([string]$Sub, [string]$Role, [string]$Secret) {
    function B64Url([byte[]]$b) { [Convert]::ToBase64String($b).TrimEnd('=').Replace('+','-').Replace('/','_') }
    $enc = [Text.Encoding]::UTF8
    $header = B64Url $enc.GetBytes('{"alg":"HS256","typ":"JWT"}')
    $exp = [DateTimeOffset]::UtcNow.AddMinutes(10).ToUnixTimeSeconds()
    $payload = B64Url $enc.GetBytes("{`"sub`":`"$Sub`",`"role`":`"$Role`",`"exp`":$exp}")
    $hmac = New-Object Security.Cryptography.HMACSHA256 (,$enc.GetBytes($Secret))
    $sig = B64Url $hmac.ComputeHash($enc.GetBytes("$header.$payload"))
    "$header.$payload.$sig"
}
$jwtSecret = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { "dev-only-secret-do-not-use-in-production-0123456789" }

$psql = "D:\usyd_resource\postgresSQL\bin\psql.exe"
$env:PGPASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }

do {
    $line = docker exec flash-sale-rabbitmq rabbitmqctl -q list_queues name messages | Select-String 'seckill.order.queue'
    $left = if ($line) { [int](($line -split '\s+')[1]) } else { 0 }
    if ($left -gt 0) { Write-Host "Waiting for order queue to drain: $left left"; Start-Sleep -Seconds 2 }
} while ($left -gt 0)

Write-Host "1/3 Resetting database (stock=$Stock)..."
& $psql -U postgres -d flash_sale -v ON_ERROR_STOP=1 -v stock=$Stock -v users=$Users -f "$PSScriptRoot\reset.sql"
if ($LASTEXITCODE -ne 0) { throw "Database reset failed" }

if ($NoPreheat) { Write-Host "Skipping preheat (phase 1)"; return }

Write-Host "2/3 Preheating Redis..."
$adminToken = New-Jwt "0" "ADMIN" $jwtSecret
curl.exe -s -f -X POST http://localhost:8081/admin/seckill/1/preheat -H "Authorization: Bearer $adminToken"
if ($LASTEXITCODE -ne 0) { throw "Preheat failed - is the app running on 8081, and does JWT_SECRET match?" }

Write-Host "3/3 Redis stock:" (docker exec flash-sale-redis redis-cli GET seckill:stock:1)
