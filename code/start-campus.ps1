[CmdletBinding()]
param(
    [switch]$Rebuild,
    [switch]$Stop,
    [switch]$NoBrowser
)

$ErrorActionPreference = 'Stop'
$backendDirectory = Join-Path $PSScriptRoot 'backend'
$frontendDirectory = Join-Path $PSScriptRoot 'frontend'
$jarPath = Join-Path $backendDirectory 'target\campus-1.0.0.jar'
$siteUrl = 'http://127.0.0.1:8080'
$logsDirectory = Join-Path $backendDirectory 'logs'
$toolsDirectory = Join-Path $env:LOCALAPPDATA 'campus-build-tools'

function Find-Tool {
    param([string]$Name, [string[]]$Candidates)
    foreach ($candidate in $Candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate -PathType Leaf)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    $command = Get-Command $Name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { return $command.Source }
    throw "找不到 $Name。请按 docs/环境依赖清单.md 安装工具，设置 JAVA_HOME / PATH 后重试。"
}

function Get-CampusListener {
    # 只识别本项目的 JAR，不能停止其他项目或复用不明端口上的服务。
    $listeners = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
    if ($listeners.Count -eq 0) { return $null }
    $processIds = @($listeners | Select-Object -ExpandProperty OwningProcess -Unique)
    if ($processIds.Count -ne 1) { throw '8080 端口存在多个服务监听，请检查端口占用。' }
    $serverProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($processIds[0])"
    if (-not $serverProcess -or -not $serverProcess.CommandLine -or
        $serverProcess.CommandLine.IndexOf($jarPath, [StringComparison]::OrdinalIgnoreCase) -lt 0) {
        throw '8080 端口被其他服务或无法识别的进程占用；请先停止该服务再启动。'
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
    if ($Stop -and $Rebuild) { throw '-Stop 和 -Rebuild 不能同时使用。' }
    $existingProcess = Get-CampusListener
    if ($Stop) {
        if ($existingProcess) {
            Stop-Process -Id $existingProcess.ProcessId
            Write-Host '校园系统已停止；数据库文件保留。'
        } else { Write-Host '校园系统当前没有运行。' }
        exit 0
    }
    if ($existingProcess) {
        if ($Rebuild) { throw '系统正在运行。请先运行 start-campus.cmd -Stop，再用 -Rebuild 启动。' }
        if (-not (Test-CampusHealth)) { throw '本项目进程正在运行但尚未就绪，请稍后重试或检查日志。' }
        Write-Host "系统已运行：$siteUrl"
        if (-not $NoBrowser) { Start-Process $siteUrl }
        exit 0
    }

    # 优先使用显式 JAVA_HOME，其次使用 PATH；最后兼容本机已安装的构建工具目录。
    $jdkCandidates = @()
    if ($env:JAVA_HOME) { $jdkCandidates += Join-Path $env:JAVA_HOME 'bin\java.exe' }
    $javaCommand = Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($javaCommand) { $jdkCandidates += $javaCommand.Source }
    $jdkCandidates += @(Get-ChildItem -LiteralPath $toolsDirectory -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin\java.exe' })
    $javaPath = Find-Tool 'java.exe' $jdkCandidates
    # Java 将正常版本信息写入 stderr；Windows PowerShell 5.1 不应将其视为启动失败。
    $savedErrorAction = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $javaVersion = (& $javaPath -version 2>&1 | Out-String)
        $javaExitCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $savedErrorAction }
    if ($javaExitCode -ne 0) { throw '无法执行 Java，请检查 JDK 安装。' }
    if ($javaVersion -notmatch 'version "21\.') { throw '本项目已验证 JDK 21，请将 JAVA_HOME 指向 JDK 21。' }
    $env:JAVA_HOME = Split-Path (Split-Path $javaPath -Parent) -Parent
    $env:PATH = "$(Split-Path $javaPath -Parent);$env:PATH"

    if ($Rebuild -or -not (Test-Path -LiteralPath $jarPath)) {
        if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) { throw '构建需要完整 JDK 21，当前 Java 缺少 javac.exe。' }
        $npmCandidates = @()
        $npmCommand = Get-Command npm.cmd -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($npmCommand) { $npmCandidates += $npmCommand.Source }
        $nvmDirectories = @($env:NVM_HOME, (Join-Path $env:LOCALAPPDATA 'nvm')) | Where-Object { $_ } | Select-Object -Unique
        foreach ($nvmDirectory in $nvmDirectories) {
            $npmCandidates += @(Get-ChildItem -LiteralPath $nvmDirectory -Directory -Filter 'v22.*' -ErrorAction SilentlyContinue |
                Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'npm.cmd' })
        }
        $npmPath = Find-Tool 'npm.cmd' $npmCandidates
        $env:PATH = "$(Split-Path $npmPath -Parent);$env:PATH"
        $mavenCandidates = @()
        if ($env:MAVEN_HOME) { $mavenCandidates += Join-Path $env:MAVEN_HOME 'bin\mvn.cmd' }
        $mavenCommand = Get-Command mvn.cmd -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($mavenCommand) { $mavenCandidates += $mavenCommand.Source }
        $mavenCandidates += @(Get-ChildItem -LiteralPath $toolsDirectory -Directory -Filter 'apache-maven-*' -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin\mvn.cmd' })
        $mavenPath = Find-Tool 'mvn.cmd' $mavenCandidates

        Write-Host '正在安装前端依赖并构建，首次运行需要联网…'
        Push-Location $frontendDirectory
        try {
            & $npmPath ci --no-fund --no-audit
            if ($LASTEXITCODE -ne 0) { throw 'npm ci 失败，请检查网络和 Node.js 版本。' }
            & $npmPath run build
            if ($LASTEXITCODE -ne 0) { throw '前端构建失败，请查看上方输出。' }
        } finally { Pop-Location }
        Push-Location $backendDirectory
        try {
            # 一键启动只构建；完整业务测试仍由 mvn verify 显式执行。
            & $mavenPath -B -DskipTests package
            if ($LASTEXITCODE -ne 0) { throw '后端构建失败，请查看上方输出。' }
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
        if ($launchedProcess.HasExited) { throw "Java 进程已退出。请查看 $stdoutPath 和 $stderrPath" }
        if (Test-CampusHealth) { $ready = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw "等待启动超时，进程保留供排查。请查看 $stdoutPath；停止可运行 start-campus.cmd -Stop。" }
    Write-Host "启动成功：$siteUrl"
    Write-Host "默认数据库：$(Join-Path $backendDirectory 'data\campus.db')"
    Write-Host '关闭本窗口不会停止服务；停止请运行 start-campus.cmd -Stop。'
    if (-not $NoBrowser) { Start-Process $siteUrl }
    exit 0
} catch {
    Write-Host "启动操作失败：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
