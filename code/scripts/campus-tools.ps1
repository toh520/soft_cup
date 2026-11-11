# 启动与配置共用的工具发现逻辑；仅在当前进程使用工具，不写注册表或永久环境变量。
function Get-CampusToolsDirectory {
    if (-not $env:LOCALAPPDATA) { throw '[ENV] 无法确定当前用户的 LOCALAPPDATA 目录。' }
    return Join-Path $env:LOCALAPPDATA 'campus-build-tools'
}

function Invoke-CampusVersion {
    param([string]$Path, [string]$Argument = '--version')
    # Windows PowerShell 5.1 把 Java 正常的 stderr 版本信息包装成 ErrorRecord。
    $savedPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = (& $Path $Argument 2>&1 | Out-String)
        return [pscustomobject]@{ ExitCode = $LASTEXITCODE; Output = $output }
    } catch {
        return [pscustomobject]@{ ExitCode = -1; Output = '程序无法执行' }
    } finally { $ErrorActionPreference = $savedPreference }
}

function Get-CampusToolCandidates {
    param([ValidateSet('Java', 'Node', 'Maven')][string]$Kind, [string]$ToolsDirectory)
    $candidates = @()
    $commandName = @{ Java = 'java.exe'; Node = 'node.exe'; Maven = 'mvn.cmd' }[$Kind]
    if ($Kind -eq 'Java' -and $env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME 'bin/java.exe' }
    if ($Kind -eq 'Maven' -and $env:MAVEN_HOME) { $candidates += Join-Path $env:MAVEN_HOME 'bin/mvn.cmd' }
    $candidates += @(Get-Command $commandName -CommandType Application -All -ErrorAction SilentlyContinue | ForEach-Object { $_.Source })
    $pattern = @{ Java = 'jdk-*'; Node = 'node-*'; Maven = 'apache-maven-*' }[$Kind]
    $relative = @{ Java = 'bin/java.exe'; Node = 'node.exe'; Maven = 'bin/mvn.cmd' }[$Kind]
    $candidates += @(Get-ChildItem -LiteralPath $ToolsDirectory -Directory -Filter $pattern -ErrorAction SilentlyContinue |
        Sort-Object { if ($_.Name -match '(\d+\.\d+\.\d+(?:\.\d+)?)') { [version]$Matches[1] } else { [version]'0.0' } } -Descending |
        ForEach-Object { Join-Path $_.FullName $relative })
    if ($Kind -eq 'Java') {
        foreach ($vendor in @('Microsoft', 'Eclipse Adoptium', 'Java')) {
            if ($env:ProgramFiles) {
                $candidates += @(Get-ChildItem -LiteralPath (Join-Path $env:ProgramFiles $vendor) -Directory -ErrorAction SilentlyContinue |
                    ForEach-Object { Join-Path $_.FullName 'bin/java.exe' })
            }
        }
    }
    if ($Kind -eq 'Node') {
        if ($env:ProgramFiles) { $candidates += Join-Path $env:ProgramFiles 'nodejs/node.exe' }
        foreach ($directory in @($env:NVM_HOME, (Join-Path $env:LOCALAPPDATA 'nvm')) | Where-Object { $_ } | Select-Object -Unique) {
            $candidates += @(Get-ChildItem -LiteralPath $directory -Directory -Filter 'v*' -ErrorAction SilentlyContinue |
                Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'node.exe' })
        }
    }
    return @($candidates | Where-Object { $_ } | Select-Object -Unique)
}

function Find-CampusTool {
    param(
        [ValidateSet('Java', 'Node', 'Maven')][string]$Kind,
        [string]$ToolsDirectory = (Get-CampusToolsDirectory),
        [string]$JavaPath,
        [string[]]$Candidates
    )
    if (-not $PSBoundParameters.ContainsKey('Candidates')) {
        $Candidates = @(Get-CampusToolCandidates -Kind $Kind -ToolsDirectory $ToolsDirectory)
    }
    $savedJava = $env:JAVA_HOME
    $savedPath = $env:PATH
    try {
        if ($JavaPath) {
            $env:JAVA_HOME = Split-Path (Split-Path $JavaPath -Parent) -Parent
            $env:PATH = "$(Split-Path $JavaPath -Parent);$savedPath"
        }
        foreach ($candidate in $Candidates) {
            if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) { continue }
            $path = (Resolve-Path -LiteralPath $candidate).Path
            $version = Invoke-CampusVersion -Path $path -Argument $(if ($Kind -eq 'Java') { '-version' } else { '--version' })
            $valid = $false
            $label = ''
            switch ($Kind) {
                'Java' {
                    if ($version.Output -match 'version "(21\.[^"]+)"') {
                        $label = $Matches[1]
                        $compiler = Join-Path (Split-Path $path -Parent) 'javac.exe'
                        if (Test-Path -LiteralPath $compiler -PathType Leaf) {
                            $compilerVersion = Invoke-CampusVersion -Path $compiler -Argument '-version'
                            $valid = $compilerVersion.ExitCode -eq 0 -and $compilerVersion.Output -match 'javac 21\.'
                        }
                    }
                }
                'Node' {
                    if ($version.Output.Trim() -match '^v(\d+)\.(\d+)\.(\d+)$') {
                        $major = [int]$Matches[1]; $minor = [int]$Matches[2]
                        $label = $version.Output.Trim()
                        # Vite 7 需要 Node 22.12+；本项目接受已验证的 22/24 两个 LTS 大版本。
                        $valid = ($major -eq 22 -and $minor -ge 12) -or $major -eq 24
                        $npm = Join-Path (Split-Path $path -Parent) 'npm.cmd'
                        if ($valid -and (Test-Path -LiteralPath $npm -PathType Leaf)) {
                            $env:PATH = "$(Split-Path $path -Parent);$savedPath"
                            $npmVersion = Invoke-CampusVersion -Path $npm
                            $valid = $npmVersion.ExitCode -eq 0 -and $npmVersion.Output -match '\d+\.\d+\.\d+'
                        } else { $valid = $false }
                    }
                }
                'Maven' {
                    if ($version.Output -match 'Apache Maven (3\.9\.\d+)\b') { $label = $Matches[1]; $valid = $true }
                }
            }
            if ($version.ExitCode -eq 0 -and $valid) {
                return [pscustomobject]@{ Kind = $Kind; Path = $path; Version = $label }
            }
            Write-Host "跳过不兼容或不完整的 $Kind：$path"
        }
        return $null
    } finally { $env:JAVA_HOME = $savedJava; $env:PATH = $savedPath }
}

function Assert-CampusTool {
    param($Tool, [string]$Requirement)
    if (-not $Tool) { throw "[DEPENDENCY] 未找到可用的 $Requirement。请先双击 code/setup-campus.cmd，或手动安装后重试。" }
    Write-Host "$($Tool.Kind) $($Tool.Version)：$($Tool.Path)"
}

function Enable-CampusTools {
    param($Java, $Node, $Maven)
    $env:JAVA_HOME = Split-Path (Split-Path $Java.Path -Parent) -Parent
    $directories = @((Split-Path $Java.Path -Parent))
    if ($Node) { $directories += Split-Path $Node.Path -Parent }
    if ($Maven) { $directories += Split-Path $Maven.Path -Parent }
    $env:PATH = ($directories -join ';') + ';' + $env:PATH
}
