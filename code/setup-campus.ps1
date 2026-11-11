[CmdletBinding()]
param([switch]$CheckOnly)

$ErrorActionPreference = 'Stop'
$savedJava = $env:JAVA_HOME
$savedPath = $env:PATH
$savedTls = [Net.ServicePointManager]::SecurityProtocol
$savedProgress = $ProgressPreference
$exitCode = 0
try {
    . (Join-Path $PSScriptRoot 'scripts/campus-tools.ps1')
    . (Join-Path $PSScriptRoot 'scripts/campus-downloads.ps1')
    $toolsDirectory = Get-CampusToolsDirectory
    $backend = Join-Path $PSScriptRoot 'backend'
    $frontend = Join-Path $PSScriptRoot 'frontend'
    if (-not (Test-Path -LiteralPath (Join-Path $backend 'pom.xml')) -or
        -not (Test-Path -LiteralPath (Join-Path $frontend 'package-lock.json'))) {
        throw '[SOURCE] 项目文件不完整。请完整下载或拉取仓库后运行配置脚本。'
    }
    $architecture = if ($env:PROCESSOR_ARCHITEW6432) { $env:PROCESSOR_ARCHITEW6432 } else { $env:PROCESSOR_ARCHITECTURE }
    if ($architecture -ne 'AMD64') { throw '[PLATFORM] 当前自动配置仅支持 Windows x64；ARM64/32 位请按环境清单手动配置。' }
    Write-Host '检查 JDK 21、Node.js 22/24（含 npm）及 Maven 3.9.x。'
    if (-not $CheckOnly) {
        Write-Host "缺失工具将下载到：$toolsDirectory"
        Write-Host '仅解压用户目录，不修改系统/用户 PATH、注册表、代理或安全设置，不需要管理员权限。'
        # TLS 1.2 仅作用于当前 PowerShell 进程，不绕过证书校验。
        [Net.ServicePointManager]::SecurityProtocol = $savedTls -bor [Net.SecurityProtocolType]::Tls12
        $ProgressPreference = 'SilentlyContinue'
    }
    $java = Find-CampusTool -Kind Java
    if (-not $java -and -not $CheckOnly) { $java = Install-CampusTool -Kind Java -ToolsDirectory $toolsDirectory }
    Assert-CampusTool $java 'JDK 21（含 javac）'
    $node = Find-CampusTool -Kind Node
    if (-not $node -and -not $CheckOnly) { $node = Install-CampusTool -Kind Node -ToolsDirectory $toolsDirectory }
    Assert-CampusTool $node 'Node.js 22.12+ / 24 及 npm'
    $maven = Find-CampusTool -Kind Maven -JavaPath $java.Path
    if (-not $maven -and -not $CheckOnly) { $maven = Install-CampusTool -Kind Maven -ToolsDirectory $toolsDirectory -JavaPath $java.Path }
    Assert-CampusTool $maven 'Maven 3.9.x'
    if ($CheckOnly) {
        Write-Host '检查通过；未下载、安装、构建或修改配置。'
    } else {
        Enable-CampusTools -Java $java -Node $node -Maven $maven
        $npm = Join-Path (Split-Path $node.Path -Parent) 'npm.cmd'
        Push-Location $frontend
        try {
            Write-Host '安装前端项目依赖（npm ci）…'
            & $npm ci --no-fund --no-audit
            if ($LASTEXITCODE -ne 0) { throw "[NPM_INSTALL] 前端依赖安装失败，退出码 $LASTEXITCODE。请检查上方 npm 提示中的网络、证书或文件占用问题。" }
        } finally { Pop-Location }
        Push-Location $backend
        try {
            Write-Host '下载后端项目及构建插件依赖（Maven）…'
            & $maven.Path -B dependency:go-offline
            if ($LASTEXITCODE -ne 0) { throw "[MAVEN_DEPENDENCIES] 后端依赖下载失败，退出码 $LASTEXITCODE。请检查上方 Maven 提示及仓库网络连接。" }
        } finally { Pop-Location }
        $localConfig = Join-Path $backend 'config/application-local.properties'
        if (-not (Test-Path -LiteralPath $localConfig)) {
            # 只在不存在时复制空密钥模板；已有真实模型配置绝不覆盖。
            Copy-Item -LiteralPath (Join-Path $backend 'config/application-local.properties.example') -Destination $localConfig
            Write-Host '已创建空密钥配置模板：code/backend/config/application-local.properties'
        }
        Write-Host '依赖配置完成。请双击 code/start-campus.cmd，首次启动会构建前后端。'
        Write-Host 'AI 功能还需自行填写模型配置；新数据库的知识资料需管理员重建向量。'
    }
} catch {
    Write-Host "配置失败：$($_.Exception.Message)" -ForegroundColor Red
    $exitCode = 1
} finally {
    $env:JAVA_HOME = $savedJava
    $env:PATH = $savedPath
    [Net.ServicePointManager]::SecurityProtocol = $savedTls
    $ProgressPreference = $savedProgress
}
exit $exitCode
