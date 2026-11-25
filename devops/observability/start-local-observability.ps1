[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryDirectory = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$environmentFile = Join-Path $repositoryDirectory '.env'

function Import-LumiXEnvironment {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return }
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*([A-Z][A-Z0-9_]*)=(.*)\s*$') {
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
        }
    }
}

function Get-LumiXPort {
    param([string]$Name, [int]$Default)
    $raw = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if ([string]::IsNullOrWhiteSpace($raw)) { return $Default }
    $port = 0
    if (-not [int]::TryParse($raw, [ref]$port) -or $port -lt 1 -or $port -gt 65535) {
        throw "$Name 必須是 1 到 65535 的整數。"
    }
    return $port
}

Import-LumiXEnvironment -Path $environmentFile
$elasticsearchPort = Get-LumiXPort -Name 'LUMIX_ELASTICSEARCH_PORT' -Default 9200
$kibanaPort = Get-LumiXPort -Name 'LUMIX_KIBANA_PORT' -Default 5601
$retentionRaw = [Environment]::GetEnvironmentVariable('LUMIX_LOG_RETENTION_DAYS', 'Process')
$retentionDays = if ([string]::IsNullOrWhiteSpace($retentionRaw)) { 14 } else { [int]$retentionRaw }
if ($retentionDays -lt 1 -or $retentionDays -gt 365) {
    throw 'LUMIX_LOG_RETENTION_DAYS 必須介於 1 與 365。'
}

New-Item -ItemType Directory -Force -Path (Join-Path $repositoryDirectory 'runtime\logs') | Out-Null

Push-Location $repositoryDirectory
try {
    & docker compose --profile observability config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Observability Compose 設定驗證失敗。' }

    & docker compose --profile observability up -d elasticsearch
    if ($LASTEXITCODE -ne 0) { throw 'Elasticsearch 啟動失敗。' }

    $elasticsearch = "http://127.0.0.1:$elasticsearchPort"
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            $health = Invoke-RestMethod -Uri "$elasticsearch/_cluster/health" -TimeoutSec 2
            if ($health.status -in @('yellow', 'green')) { $ready = $true; break }
        } catch { }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'Elasticsearch 未在 60 秒內就緒。' }

    # 每日或 5 GB rollover；到期索引由 ILM 刪除，避免開發機磁碟無界成長。
    $policy = @{
        policy = @{
            phases = @{
                hot = @{ actions = @{ rollover = @{ max_age = '1d'; max_primary_shard_size = '5gb' } } }
                delete = @{ min_age = "${retentionDays}d"; actions = @{ delete = @{} } }
            }
        }
    } | ConvertTo-Json -Depth 10
    Invoke-RestMethod -Method Put -Uri "$elasticsearch/_ilm/policy/lumix-logs-policy" `
        -ContentType 'application/json' -Body $policy | Out-Null

    # 單節點本機索引不配置無法分配的 replica；正式叢集必須依節點與復原目標另行核定。
    $indexTemplate = @{
        index_patterns = @('lumix-logs-*')
        priority = 500
        template = @{
            settings = @{
                'index.number_of_shards' = 1
                'index.number_of_replicas' = 0
                'index.lifecycle.name' = 'lumix-logs-policy'
                'index.lifecycle.rollover_alias' = 'lumix-logs'
            }
        }
    } | ConvertTo-Json -Depth 10
    Invoke-RestMethod -Method Put -Uri "$elasticsearch/_index_template/lumix-logs" `
        -ContentType 'application/json' -Body $indexTemplate | Out-Null
    try {
        $existingIndexSettings = @{ index = @{ number_of_replicas = 0 } } | ConvertTo-Json -Depth 5
        Invoke-RestMethod -Method Put -Uri "$elasticsearch/lumix-logs-*/_settings" `
            -ContentType 'application/json' -Body $existingIndexSettings | Out-Null
    } catch {
        if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 404) { throw }
    }

    & docker compose --profile observability up -d logstash filebeat kibana
    if ($LASTEXITCODE -ne 0) { throw 'Logstash、Filebeat 或 Kibana 啟動失敗。' }

    $kibana = "http://127.0.0.1:$kibanaPort"
    $kibanaReady = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            $status = Invoke-RestMethod -Uri "$kibana/api/status" -TimeoutSec 2
            if ($status.status.overall.level -in @('available', 'degraded')) { $kibanaReady = $true; break }
        } catch { }
        Start-Sleep -Seconds 1
    }
    if (-not $kibanaReady) { throw 'Kibana 未在 60 秒內就緒。' }

    $dataViewExists = $false
    try {
        Invoke-RestMethod -Uri "$kibana/api/data_views/data_view/lumix-logs" `
            -Headers @{ 'kbn-xsrf' = 'lumix-local-observability' } | Out-Null
        $dataViewExists = $true
    } catch {
        if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 404) { throw }
    }
    if (-not $dataViewExists) {
        $dataView = @{ data_view = @{ id = 'lumix-logs'; title = 'lumix-logs-*'; timeFieldName = '@timestamp' } } |
            ConvertTo-Json -Depth 5
        Invoke-RestMethod -Method Post -Uri "$kibana/api/data_views/data_view" `
            -Headers @{ 'kbn-xsrf' = 'lumix-local-observability' } -ContentType 'application/json' -Body $dataView | Out-Null
    }

    Write-Host 'LumiX 集中日誌已啟動：'
    Write-Host "  Kibana：$kibana/app/discover#/?_a=(dataView:lumix-logs)"
    Write-Host "  Elasticsearch：$elasticsearch"
    Write-Host "  日誌目錄：$(Join-Path $repositoryDirectory 'runtime\logs')"
} finally {
    Pop-Location
}
