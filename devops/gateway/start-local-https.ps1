[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryDirectory = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$environmentFile = Join-Path $repositoryDirectory '.env'
$runtimeDirectory = Join-Path $PSScriptRoot 'runtime'
$certificateDirectory = Join-Path $PSScriptRoot 'certs'

function Import-LumiXEnvironment {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) { return }
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*([A-Z][A-Z0-9_]*)=(.*)\s*$') {
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
        }
    }
}

function Get-ServicePort {
    param([string]$Name, [int]$Default)

    $raw = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if ([string]::IsNullOrWhiteSpace($raw)) { return $Default }
    $parsed = 0
    if (-not [int]::TryParse($raw, [ref]$parsed) -or $parsed -lt 1 -or $parsed -gt 65535) {
        throw "$Name 必須是 1 到 65535 的整數。"
    }
    return $parsed
}

Import-LumiXEnvironment -Path $environmentFile
$apiPort = Get-ServicePort -Name 'LUMIX_API_PORT' -Default 8080
$webPort = Get-ServicePort -Name 'LUMIX_WEB_PORT' -Default 8090
$adminWebPort = Get-ServicePort -Name 'LUMIX_ADMIN_WEB_PORT' -Default 8100
$httpsPort = Get-ServicePort -Name 'LUMIX_HTTPS_PORT' -Default 443
$publicHost = [Environment]::GetEnvironmentVariable('LUMIX_PUBLIC_HOST', 'Process')
$publicCertificateFile = [Environment]::GetEnvironmentVariable('LUMIX_PUBLIC_TLS_CERT', 'Process')
$publicPrivateKeyFile = [Environment]::GetEnvironmentVariable('LUMIX_PUBLIC_TLS_KEY', 'Process')
if (-not [string]::IsNullOrWhiteSpace($publicHost) -and $publicHost -notmatch '^(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$') {
    throw 'LUMIX_PUBLIC_HOST 必須是有效的 DNS hostname。'
}
$hasPublicCertificate = -not [string]::IsNullOrWhiteSpace($publicCertificateFile)
$hasPublicPrivateKey = -not [string]::IsNullOrWhiteSpace($publicPrivateKeyFile)
if ($hasPublicCertificate -ne $hasPublicPrivateKey) {
    throw 'LUMIX_PUBLIC_TLS_CERT 與 LUMIX_PUBLIC_TLS_KEY 必須同時設定。'
}
if ($hasPublicCertificate -and (-not (Test-Path -LiteralPath $publicCertificateFile) -or -not (Test-Path -LiteralPath $publicPrivateKeyFile))) {
    throw '公開 TLS 憑證或私鑰檔案不存在。'
}
$publicTlsReady = -not [string]::IsNullOrWhiteSpace($publicHost) -and $hasPublicCertificate

# winget 安裝後，已開啟的 PowerShell 不會自動取得更新過的使用者 PATH；追加而不覆蓋現有工具路徑。
$env:Path = @(
    $env:Path
    [Environment]::GetEnvironmentVariable('Path', 'Machine')
    [Environment]::GetEnvironmentVariable('Path', 'User')
) -join ';'
$nginx = Get-Command nginx -ErrorAction Stop
$mkcert = Get-Command mkcert -ErrorAction Stop
$mimeTypesFile = Join-Path (Split-Path -Parent $nginx.Source) 'conf\mime.types'
$certificateFile = Join-Path $certificateDirectory 'lumix-local.pem'
$privateKeyFile = Join-Path $certificateDirectory 'lumix-local-key.pem'
$acmeWebRoot = Join-Path $runtimeDirectory 'acme-webroot'
$acmeChallengeDirectory = Join-Path $acmeWebRoot '.well-known\acme-challenge'

New-Item -ItemType Directory -Force -Path $certificateDirectory, $acmeChallengeDirectory, (Join-Path $runtimeDirectory 'conf'), (Join-Path $runtimeDirectory 'logs'), (Join-Path $runtimeDirectory 'temp') | Out-Null

if (-not (Test-Path -LiteralPath $certificateFile) -or -not (Test-Path -LiteralPath $privateKeyFile)) {
    # mkcert 建立本機開發 CA；站台私鑰只保留在被 git 忽略的本機目錄。
    & $mkcert.Source -cert-file $certificateFile -key-file $privateKeyFile localhost 127.0.0.1 '::1' admin.localhost
}

# 只信任目前 Windows 使用者，Chrome 會使用此憑證庫，且不要求系統管理員權限。
$certificateAuthorityDirectory = (& $mkcert.Source -CAROOT).Trim()
$certificateAuthorityFile = Join-Path $certificateAuthorityDirectory 'rootCA.pem'
$certificateAuthority = [System.Security.Cryptography.X509Certificates.X509Certificate2]::new($certificateAuthorityFile)
$trusted = Get-ChildItem Cert:\CurrentUser\Root | Where-Object Thumbprint -eq $certificateAuthority.Thumbprint
if (-not $trusted) {
    Import-Certificate -FilePath $certificateAuthorityFile -CertStoreLocation Cert:\CurrentUser\Root | Out-Null
}

$normalizePath = { param([string]$Path) $Path.Replace('\', '/') }
$template = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'nginx.conf.template') -Raw
$publicHttpServer = ''
if (-not [string]::IsNullOrWhiteSpace($publicHost)) {
    $publicHttpFallback = if ($publicTlsReady) { 'return 308 https://$host$request_uri;' } else { 'return 503;' }
    $publicHttpServer = @"
    server {
        listen 0.0.0.0:80 default_server;
        server_name $publicHost;

        location ^~ /.well-known/acme-challenge/ {
            root "$(& $normalizePath $acmeWebRoot)";
            default_type text/plain;
            try_files `$uri =404;
        }

        location / { $publicHttpFallback }
    }
"@
}

$publicHttpsServer = ''
if ($publicTlsReady) {
    $publicHttpsServer = @"
    server {
        listen 0.0.0.0:${httpsPort} ssl default_server;
        server_name $publicHost;

        ssl_certificate     "$(& $normalizePath $publicCertificateFile)";
        ssl_certificate_key "$(& $normalizePath $publicPrivateKeyFile)";
        ssl_protocols       TLSv1.2 TLSv1.3;

        location = /api/admin { return 404; }
        location ^~ /api/admin/ { return 404; }
        location = /admin { return 404; }
        location ^~ /admin/ { return 404; }

        location /api/ {
            proxy_pass http://lumix_api;
            proxy_http_version 1.1;
            proxy_set_header Host `$host;
            proxy_set_header X-Forwarded-Host `$host;
            proxy_set_header X-Forwarded-Port `$server_port;
            proxy_set_header X-Forwarded-Proto https;
            proxy_set_header X-Forwarded-For `$remote_addr;
            proxy_set_header Upgrade `$http_upgrade;
            proxy_set_header Connection `$connection_upgrade;
        }

        location / {
            proxy_pass http://lumix_web;
            proxy_http_version 1.1;
            proxy_set_header Host `$host;
            proxy_set_header Upgrade `$http_upgrade;
            proxy_set_header Connection `$connection_upgrade;
        }
    }
"@
}

$localBindAddress = if ($publicTlsReady) { '0.0.0.0' } else { '127.0.0.1' }
$configuration = $template.Replace('__API_PORT__', [string]$apiPort).
    Replace('__WEB_PORT__', [string]$webPort).
    Replace('__ADMIN_WEB_PORT__', [string]$adminWebPort).
    Replace('__HTTPS_PORT__', [string]$httpsPort).
    Replace('__LOCAL_BIND_ADDRESS__', $localBindAddress).
    Replace('__PUBLIC_HTTP_SERVER__', $publicHttpServer).
    Replace('__PUBLIC_HTTPS_SERVER__', $publicHttpsServer).
    Replace('__MIME_TYPES__', (& $normalizePath $mimeTypesFile)).
    Replace('__TLS_CERT__', (& $normalizePath $certificateFile)).
    Replace('__TLS_KEY__', (& $normalizePath $privateKeyFile))
$configurationFile = Join-Path $runtimeDirectory 'conf\nginx.conf'
Set-Content -LiteralPath $configurationFile -Value $configuration -Encoding utf8

$prefix = (& $normalizePath $runtimeDirectory) + '/'
$pidFile = Join-Path $runtimeDirectory 'logs\nginx.pid'
if (Test-Path -LiteralPath $pidFile) {
    $runningNginx = Get-Process -Id ([int](Get-Content -LiteralPath $pidFile -Raw).Trim()) -ErrorAction SilentlyContinue
    if ($runningNginx -and $runningNginx.ProcessName -eq 'nginx') {
        & $nginx.Source -p $prefix -c 'conf/nginx.conf' -s stop
        Start-Sleep -Milliseconds 300
    } else {
        # 非正常中止只會遺留 runtime 內的 pid 檔；不得依 stale pid 操作其他程序。
        Remove-Item -LiteralPath $pidFile
    }
}

& $nginx.Source -p $prefix -c 'conf/nginx.conf' -t
if ($LASTEXITCODE -ne 0) { throw 'Nginx 設定驗證失敗。' }
$nginxProcess = Start-Process -FilePath $nginx.Source -ArgumentList @('-p', $prefix, '-c', 'conf/nginx.conf') -WindowStyle Hidden -PassThru
Start-Sleep -Milliseconds 500
if ($nginxProcess.HasExited -and $nginxProcess.ExitCode -ne 0) { throw 'Nginx 啟動失敗。' }

Write-Host "LumiX HTTPS gateway 已啟動："
$portSuffix = if ($httpsPort -eq 443) { '' } else { ":$httpsPort" }
Write-Host "  前台：https://localhost$portSuffix"
Write-Host "  後台：https://admin.localhost$portSuffix/admin/login"
if (-not [string]::IsNullOrWhiteSpace($publicHost)) {
    if ($publicTlsReady) {
        Write-Host "  公開前台：https://$publicHost$portSuffix"
    } else {
        Write-Host "  ACME 驗證入口：http://$publicHost/.well-known/acme-challenge/"
    }
}
