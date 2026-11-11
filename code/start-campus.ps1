[CmdletBinding()]
param(
    [switch]$Rebuild,
    [switch]$Stop,
    [switch]$NoBrowser,
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:PATH
$backendDirectory = Join-Path $PSScriptRoot 'backend'
$frontendDirectory = Join-Path $PSScriptRoot 'frontend'
$jarPath = Join-Path $backendDirectory 'target\campus-1.0.0.jar'
$siteUrl = 'http://127.0.0.1:8080'
$logsDirectory = Join-Path $backendDirectory 'logs'

function Get-CampusListener {
    # 只识别本项目的 JAR，不能停止其他项目或复用不明端口上的服务。
    $listeners = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
    if ($listeners.Count -eq 0) { return $null }
    $processIds = @($listeners | Select-Object -ExpandProperty OwningProcess -Unique)
    if ($processIds.Count -ne 1) { throw '[PORT] 8080 端口存在多个服务监听，请检查端口占用。' }
    $serverProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($processIds[0])"
    if (-not $serverProcess -or -not $serverProcess.CommandLine -or
        $serverProcess.CommandLine.IndexOf($jarPath, [StringComparison]::OrdinalIgnoreCase) -lt 0) {
        throw '[PORT] 8080 端口被其他服务或无法识别的进程占用；本脚本不会停止该进程，请确认端口用途后处理。'
    }
    return $serverProcess
}

function Test-CampusHealth {
    try {
        $health = Invoke-RestMethod "$siteUrl/api/health" -TimeoutSec 2
        return $health.status -eq 'ok'
    } catch { return $false }
}

try {
    . (Join-Path $PSScriptRoot 'scripts/campus-tools.ps1')
    if ($CheckOnly -and $Stop) { throw '[ARGUMENT] -CheckOnly 和 -Stop 不能同时使用。' }
    if ($Stop -and $Rebuild) { throw '-Stop 和 -Rebuild 不能同时使用。' }
    if (-not (Test-Path -LiteralPath (Join-Path $backendDirectory 'pom.xml')) -or
        -not (Test-Path -LiteralPath (Join-Path $frontendDirectory 'package-lock.json'))) {
        throw '[SOURCE] 项目文件不完整，请完整下载或拉取仓库，不要单独复制启动脚本。'
    }
    $existingProcess = Get-CampusListener
    if ($Stop) {
        if ($existingProcess) {
            Stop-Process -Id $existingProcess.ProcessId
            Write-Host '校园系统已停止；数据库文件保留。'
        } else { Write-Host '校园系统当前没有运行。' }
        exit 0
    }
    if ($existingProcess -and -not $CheckOnly) {
        if ($Rebuild) { throw '系统正在运行。请先运行 start-campus.cmd -Stop，再用 -Rebuild 启动。' }
        if (-not (Test-CampusHealth)) { throw '本项目进程正在运行但尚未就绪，请稍后重试或检查日志。' }
        Write-Host "系统已运行：$siteUrl"
        if (-not $NoBrowser) { Start-Process $siteUrl }
        exit 0
    }

    # 按版本逐一验证；JAVA_HOME/PATH 中的 Java 17 不阻止继续寻找已有 JDK 21。
    $java = Find-CampusTool -Kind Java
    Assert-CampusTool $java 'JDK 21（含 javac）'
    $javaPath = $java.Path
    $needsBuild = $Rebuild -or -not (Test-Path -LiteralPath $jarPath)
    $node = $null; $maven = $null
    if ($needsBuild -or $CheckOnly) {
        $node = Find-CampusTool -Kind Node
        Assert-CampusTool $node 'Node.js 22.12+ / 24 及 npm'
        $maven = Find-CampusTool -Kind Maven -JavaPath $javaPath
        Assert-CampusTool $maven 'Maven 3.9.x'
    }
    if ($CheckOnly) {
        Write-Host '环境检查通过；未安装依赖、未构建、未启动或停止服务。'
        exit 0
    }
    Enable-CampusTools -Java $java -Node $node -Maven $maven
    if ($needsBuild) {
        $npmPath = Join-Path (Split-Path $node.Path -Parent) 'npm.cmd'
        $mavenPath = $maven.Path
        Write-Host '正在安装前端依赖并构建，首次运行需要联网…'
        Push-Location $frontendDirectory
        try {
            & $npmPath ci --no-fund --no-audit
            if ($LASTEXITCODE -ne 0) { throw "[NPM_INSTALL] npm ci 失败，退出码 $LASTEXITCODE。请查看上方错误，检查网络、证书及目录权限；不要关闭 TLS 校验。" }
            & $npmPath run build
            if ($LASTEXITCODE -ne 0) { throw "[FRONTEND_BUILD] 前端构建失败，退出码 $LASTEXITCODE，请查看上方输出。" }
        } finally { Pop-Location }
        Push-Location $backendDirectory
        try {
            # 一键启动只构建；完整业务测试仍由 mvn verify 显式执行。
            & $mavenPath -B -DskipTests package
            if ($LASTEXITCODE -ne 0) { throw "[BACKEND_BUILD] 后端构建失败，退出码 $LASTEXITCODE。请检查上方编译错误、Maven 仓库网络和文件权限。" }
        } finally { Pop-Location }
    }

    New-Item -ItemType Directory -Force -Path $logsDirectory | Out-Null
    $stdoutPath = Join-Path $logsDirectory 'campus-output.log'
    $stderrPath = Join-Path $logsDirectory 'campus-error.log'
    # 固定后端工作目录，确保本地配置和默认数据库在正确位置加载。
    $launchedProcess = Start-Process -FilePath $javaPath -ArgumentList @('-jar', ('"' + $jarPath + '"')) -WorkingDirectory $backendDirectory -WindowStyle Hidden -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath -PassThru
    $deadline = (Get-Date).AddSeconds(60)
    $ready = $false
    while ((Get-Date) -lt $deadline) {
        $launchedProcess.Refresh()
        if ($launchedProcess.HasExited) { throw "[SERVER_EXIT] Java 进程已退出（退出码 $($launchedProcess.ExitCode)）。请查看 $stdoutPath 和 $stderrPath，检查端口、数据库权限与本地配置。" }
        if (Test-CampusHealth) { $ready = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw "[SERVER_TIMEOUT] 等待启动超过 60 秒。请查看 $stdoutPath；停止本项目可运行 start-campus.cmd -Stop。" }
    Write-Host "启动成功：$siteUrl"
    Write-Host "默认数据库：$(Join-Path $backendDirectory 'data\campus.db')"
    Write-Host '关闭本窗口不会停止服务；停止请运行 start-campus.cmd -Stop。'
    if (-not $NoBrowser) { Start-Process $siteUrl }
    exit 0
} catch {
    Write-Host "启动操作失败：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    $env:JAVA_HOME = $originalJavaHome
    $env:PATH = $originalPath
}
