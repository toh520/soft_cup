# 无需 Pester；仅在项目 target 下构造夹具，不联网、不安装工具、不改系统配置。
$ErrorActionPreference = 'Stop'
$codeDirectory = Split-Path $PSScriptRoot -Parent
. (Join-Path $codeDirectory 'scripts/campus-tools.ps1')
. (Join-Path $codeDirectory 'scripts/campus-downloads.ps1')
$testRoot = Join-Path $codeDirectory ('backend/target/script-tests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot | Out-Null
$script:checks = 0
function Assert-True {
    param([bool]$Condition, [string]$Description)
    if (-not $Condition) { throw "测试失败：$Description" }
    $script:checks++
    Write-Host "PASS $Description"
}
function Assert-Fails {
    param([scriptblock]$Action, [string]$Expected, [string]$Description)
    $message = ''
    try { & $Action | Out-Null } catch { $message = $_.Exception.Message }
    Assert-True ($message -like "*$Expected*") "$Description ($Expected)"
}
try {
    $psFiles = @(Get-ChildItem -LiteralPath $codeDirectory -Filter '*.ps1') +
        @(Get-ChildItem -LiteralPath (Join-Path $codeDirectory 'scripts') -Filter '*.ps1')
    foreach ($file in $psFiles) {
        $tokens = $null; $errors = $null
        $null = [Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$tokens, [ref]$errors)
        Assert-True ($errors.Count -eq 0) "PowerShell 语法：$($file.Name)"
    }
    Assert-True ($null -eq (Find-CampusTool -Kind Java -Candidates @())) '没有候选时不误报工具可用'
    Assert-Fails { Assert-CampusTool $null 'JDK 21' } '[DEPENDENCY]' '缺依赖给出配置入口'

    # 模拟不可用的 JDK 17、只有 JRE 的 21 和完整 JDK 21，验证不会停在第一个存在的文件。
    foreach ($name in @('jdk-17', 'jre-21', 'jdk-21.0.8', 'jdk-21.0.12')) {
        $bin = Join-Path $testRoot "$name/bin"
        New-Item -ItemType Directory -Path $bin | Out-Null
        Set-Content -LiteralPath (Join-Path $bin 'java.exe') -Value 'version fixture'
        if ($name -ne 'jre-21') { Set-Content -LiteralPath (Join-Path $bin 'javac.exe') -Value 'compiler fixture' }
    }
    $originalVersion = ${function:Invoke-CampusVersion}
    function Invoke-CampusVersion {
        param([string]$Path, [string]$Argument)
        $output = if ($Path -match 'jdk-17') { 'openjdk version "17.0.21"' }
            elseif ($Path -match 'javac.exe$') { 'javac 21.0.12' }
            else { 'openjdk version "21.0.12"' }
        return [pscustomobject]@{ ExitCode = 0; Output = $output }
    }
    try {
        $paths = @('jdk-17', 'jre-21', 'jdk-21.0.12') | ForEach-Object { Join-Path $testRoot "$_/bin/java.exe" }
        $chosen = Find-CampusTool -Kind Java -Candidates $paths
        Assert-True ($chosen.Path -like '*jdk-21.0.12*') '跳过 Java 17 与缺 javac 的 JRE，选择完整 JDK 21'
        $candidates = @(Get-CampusToolCandidates -Kind Java -ToolsDirectory $testRoot | Where-Object { $_.StartsWith($testRoot) })
        Assert-True ($candidates[0] -like '*jdk-21.0.12*') '用户工具目录按数值版本排序，21.0.12 优先于 21.0.8'
    } finally { Set-Item Function:Invoke-CampusVersion $originalVersion }

    $sample = Join-Path $testRoot 'sample.zip'
    Set-Content -LiteralPath $sample -Value 'checksum fixture'
    $hash = (Get-FileHash -LiteralPath $sample -Algorithm SHA256).Hash
    Assert-CampusArchiveHash $sample $hash SHA256
    Assert-True $true '正确校验值通过'
    Assert-Fails { Assert-CampusArchiveHash $sample ('0' * 64) SHA256 } '[CHECKSUM]' '篡改文件拒绝安装'
    Assert-Fails { Assert-CampusArchiveHash $sample 'invalid' SHA256 } '[CHECKSUM]' '错误官方校验格式拒绝安装'
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $badZip = Join-Path $testRoot 'traversal.zip'
    $zip = [IO.Compression.ZipFile]::Open($badZip, [IO.Compression.ZipArchiveMode]::Create)
    try { $null = $zip.CreateEntry('../outside.txt') } finally { $zip.Dispose() }
    Assert-Fails { Expand-CampusToolArchive $badZip (Join-Path $testRoot 'blocked') } '[ARCHIVE]' 'ZIP 越界路径拒绝解压'
    Assert-True (-not (Test-Path -LiteralPath (Join-Path $testRoot 'outside.txt'))) '越界 ZIP 未写出任何文件'
    $validZip = Join-Path $testRoot 'valid.zip'
    $zip = [IO.Compression.ZipFile]::Open($validZip, [IO.Compression.ZipArchiveMode]::Create)
    try {
        $entry = $zip.CreateEntry('jdk-fixture/bin/readme.txt')
        $writer = New-Object IO.StreamWriter($entry.Open())
        try { $writer.Write('verified package') } finally { $writer.Dispose() }
    } finally { $zip.Dispose() }
    Expand-CampusToolArchive $validZip (Join-Path $testRoot 'expanded')
    Assert-True (Test-Path -LiteralPath (Join-Path $testRoot 'expanded/jdk-fixture/bin/readme.txt')) '合法 ZIP 正常解压'

    # 安装流程使用本地 ZIP 与可控工具探测，不访问网络或执行下载的程序。
    $originalRelease = ${function:Get-CampusRelease}
    $originalFind = ${function:Find-CampusTool}
    $packageHash = (Get-FileHash -LiteralPath $validZip -Algorithm SHA256).Hash
    function Get-CampusRelease {
        param($Kind)
        return [pscustomobject]@{ Name = 'fixture.zip'; Url = 'https://github.com/fixture.zip'; Hash = $packageHash; Algorithm = 'SHA256' }
    }
    function Invoke-WebRequest {
        param($Uri, $OutFile, [switch]$UseBasicParsing, $TimeoutSec)
        Copy-Item -LiteralPath $validZip -Destination $OutFile
    }
    function Find-CampusTool {
        param($Kind, $Candidates, $JavaPath)
        return [pscustomobject]@{ Kind = $Kind; Path = $Candidates[0]; Version = '21.0.12' }
    }
    try {
        $installRoot = Join-Path $testRoot 'installed-tools'
        $installed = Install-CampusTool -Kind Java -ToolsDirectory $installRoot
        Assert-True (Test-Path -LiteralPath (Join-Path $installRoot 'jdk-fixture/bin/readme.txt')) '校验后工具目录保存到指定用户工具目录'
        Assert-True ($installed.Path.StartsWith($installRoot)) '返回安装后的路径而非临时路径'
        Assert-Fails { Install-CampusTool -Kind Java -ToolsDirectory $installRoot } '[EXISTING]' '拒绝覆盖已有工具'
        Assert-True (@(Get-ChildItem -LiteralPath $installRoot -Filter '.campus-download-*').Count -eq 0) '成功与失败都清理本次临时下载目录'
        $packageHash = '0' * 64
        $badInstall = Join-Path $testRoot 'bad-install'
        Assert-Fails { Install-CampusTool -Kind Java -ToolsDirectory $badInstall } '[CHECKSUM]' '安装流程遇校验失败立即停止'
        Assert-True (-not (Test-Path -LiteralPath (Join-Path $badInstall 'jdk-fixture'))) '校验失败不产生工具安装'
    } finally {
        Set-Item Function:Get-CampusRelease $originalRelease
        Set-Item Function:Find-CampusTool $originalFind
        Remove-Item Function:Invoke-WebRequest
    }

    # 在隔离的含空格目录中运行配置入口；npm/Maven 用本地命令夹具，不安装真实依赖。
    $setupRoot = Join-Path $testRoot 'setup fixture'
    foreach ($directory in @('scripts', 'frontend', 'backend/config', 'tools/java/bin', 'tools/node', 'tools/maven/bin')) {
        New-Item -ItemType Directory -Force -Path (Join-Path $setupRoot $directory) | Out-Null
    }
    Copy-Item -LiteralPath (Join-Path $codeDirectory 'setup-campus.ps1') -Destination $setupRoot
    Copy-Item -LiteralPath (Join-Path $codeDirectory 'scripts/campus-downloads.ps1') -Destination (Join-Path $setupRoot 'scripts')
    Set-Content -LiteralPath (Join-Path $setupRoot 'frontend/package-lock.json') -Value '{}'
    Set-Content -LiteralPath (Join-Path $setupRoot 'backend/pom.xml') -Value '<project />'
    Set-Content -LiteralPath (Join-Path $setupRoot 'backend/config/application-local.properties.example') -Value 'llm.api-key='
    $helperPath = (Join-Path $codeDirectory 'scripts/campus-tools.ps1').Replace("'", "''")
    $shim = @'
. '__HELPER__'
function Find-CampusTool {
    param($Kind, $JavaPath)
    $root = Split-Path $PSScriptRoot -Parent
    $relative = @{ Java='java/bin/java.exe'; Node='node/node.exe'; Maven='maven/bin/mvn.cmd' }[$Kind]
    return [pscustomobject]@{ Kind=$Kind; Path=(Join-Path $root "tools/$relative"); Version='fixture' }
}
'@
    [IO.File]::WriteAllText((Join-Path $setupRoot 'scripts/campus-tools.ps1'), $shim.Replace('__HELPER__', $helperPath), (New-Object Text.UTF8Encoding($true)))
    foreach ($relative in @('tools/node/npm.cmd', 'tools/maven/bin/mvn.cmd')) {
        Set-Content -LiteralPath (Join-Path $setupRoot $relative) -Encoding ASCII -Value "@echo off`r`necho called>> dependencies-called.txt`r`nexit /b 0"
    }
    $setupScript = Join-Path $setupRoot 'setup-campus.ps1'
    $output = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $setupScript -CheckOnly 2>&1
    Assert-True ($LASTEXITCODE -eq 0 -and -not (Test-Path (Join-Path $setupRoot 'frontend/dependencies-called.txt')) -and
        -not (Test-Path (Join-Path $setupRoot 'backend/config/application-local.properties'))) 'CheckOnly 不安装依赖或创建配置'
    $output = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $setupScript 2>&1
    Assert-True ($LASTEXITCODE -eq 0 -and (Test-Path (Join-Path $setupRoot 'backend/dependencies-called.txt'))) '配置入口调用 npm/Maven 并正确处理含空格目录'
    $privateConfig = Join-Path $setupRoot 'backend/config/application-local.properties'
    Assert-True ((Get-Content -LiteralPath $privateConfig -Raw).Trim() -eq 'llm.api-key=') '首次配置只复制空密钥模板'
    Set-Content -LiteralPath $privateConfig -Value 'preserve-existing-local-config'
    $output = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $setupScript 2>&1
    Assert-True ($LASTEXITCODE -eq 0 -and (Get-Content -LiteralPath $privateConfig -Raw).Trim() -eq 'preserve-existing-local-config') '重复配置保留已有私有文件'
    Set-Content -LiteralPath (Join-Path $setupRoot 'tools/node/npm.cmd') -Encoding ASCII -Value "@echo off`r`nexit /b 17"
    $mavenCalls = Get-Content -LiteralPath (Join-Path $setupRoot 'backend/dependencies-called.txt') -Raw
    $output = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $setupScript 2>&1
    Assert-True ($LASTEXITCODE -eq 1 -and ($output -join "`n") -match '\[NPM_INSTALL\]' -and
        (Get-Content -LiteralPath (Join-Path $setupRoot 'backend/dependencies-called.txt') -Raw) -eq $mavenCalls) 'npm 失败返回非零退出码并停止后续 Maven 操作'

    # 使用本机已有工具的只读检查，确认探测 Maven 时临时 JAVA_HOME/PATH 能恢复。
    $beforeJava = $env:JAVA_HOME; $beforePath = $env:PATH
    $java = Find-CampusTool -Kind Java
    if ($java) {
        $null = Find-CampusTool -Kind Maven -JavaPath $java.Path
        Assert-True ($env:JAVA_HOME -eq $beforeJava -and $env:PATH -eq $beforePath) '工具探测恢复进程环境'
    } else { Write-Host 'SKIP 本机无 JDK 21，跳过实际 Maven 环境恢复检查' }
    Write-Host "脚本测试通过：$script:checks 项。"
} finally {
    $allowedRoot = [IO.Path]::GetFullPath((Join-Path $codeDirectory 'backend/target')).TrimEnd('\') + '\'
    $resolved = [IO.Path]::GetFullPath($testRoot)
    if ($resolved.StartsWith($allowedRoot, [StringComparison]::OrdinalIgnoreCase) -and
        (Split-Path $resolved -Leaf) -match '^script-tests-[a-f0-9]{32}$') {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
