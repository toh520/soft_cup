# 仅供显式执行 setup-campus 使用；工具来自官方 HTTPS 地址，校验完成才解压。
function Get-CampusRelease {
    param([ValidateSet('Java', 'Node', 'Maven')][string]$Kind)
    switch ($Kind) {
        'Java' {
            $uri = 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
            $releases = Invoke-RestMethod -Uri $uri -TimeoutSec 60 -UseBasicParsing
            if (-not @($releases).Count) { throw '[RELEASE] Adoptium 没有返回 Windows x64 JDK 21。' }
            $package = @($releases)[0].binary.package
            return [pscustomobject]@{ Name = $package.name; Url = $package.link; Hash = $package.checksum; Algorithm = 'SHA256' }
        }
        'Node' {
            $releases = Invoke-RestMethod -Uri 'https://nodejs.org/dist/index.json' -TimeoutSec 60 -UseBasicParsing
            $release = $releases | Where-Object { $_.version -match '^v22\.' -and $_.lts } |
                Sort-Object { [version]$_.version.TrimStart('v') } -Descending | Select-Object -First 1
            if (-not $release) { throw '[RELEASE] Node.js 没有返回 Node 22 LTS。' }
            $name = "node-$($release.version)-win-x64.zip"
            $base = "https://nodejs.org/dist/$($release.version)"
            $checksums = (Invoke-WebRequest -Uri "$base/SHASUMS256.txt" -UseBasicParsing -TimeoutSec 60).Content
            $match = [regex]::Match($checksums, '(?m)^([a-fA-F0-9]{64})\s+' + [regex]::Escape($name) + '\s*$')
            if (-not $match.Success) { throw '[CHECKSUM] Node.js 官方校验列表缺少目标 ZIP。' }
            return [pscustomobject]@{ Name = $name; Url = "$base/$name"; Hash = $match.Groups[1].Value; Algorithm = 'SHA256' }
        }
        'Maven' {
            $name = 'apache-maven-3.9.9-bin.zip'
            $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/$name"
            $hash = (Invoke-WebRequest -Uri "$url.sha512" -UseBasicParsing -TimeoutSec 60).Content.Trim()
            return [pscustomobject]@{ Name = $name; Url = $url; Hash = $hash; Algorithm = 'SHA512' }
        }
    }
}

function Assert-CampusArchiveHash {
    param([string]$Path, [string]$Hash, [ValidateSet('SHA256', 'SHA512')][string]$Algorithm)
    $length = if ($Algorithm -eq 'SHA256') { 64 } else { 128 }
    if ($Hash -notmatch "^[a-fA-F0-9]{$length}$") { throw '[CHECKSUM] 官方校验值格式不正确，已停止安装。' }
    if ((Get-FileHash -LiteralPath $Path -Algorithm $Algorithm).Hash -ne $Hash) {
        throw '[CHECKSUM] 下载文件校验不一致，已停止安装。请重新运行配置脚本，不要运行这个文件。'
    }
}

function Expand-CampusToolArchive {
    param([string]$Path, [string]$Destination)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $root = [IO.Path]::GetFullPath($Destination).TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    $archive = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        # 解压前检查所有条目，拒绝越界路径；不能让 ZIP 写到临时目录之外。
        foreach ($entry in $archive.Entries) {
            $target = [IO.Path]::GetFullPath((Join-Path $Destination $entry.FullName))
            if (-not $target.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) {
                throw '[ARCHIVE] ZIP 含目录之外的路径，已拒绝解压。'
            }
        }
    } finally { $archive.Dispose() }
    [IO.Compression.ZipFile]::ExtractToDirectory($Path, $Destination)
}

function Install-CampusTool {
    param([ValidateSet('Java', 'Node', 'Maven')][string]$Kind, [string]$ToolsDirectory, [string]$JavaPath)
    $stage = $null
    $step = '获取官方版本信息'
    try {
        $release = Get-CampusRelease -Kind $Kind
        $uri = [uri]$release.Url
        $hosts = @('github.com', 'nodejs.org', 'repo.maven.apache.org')
        if ($uri.Scheme -ne 'https' -or $uri.Host -notin $hosts -or
            $release.Name -notmatch '^[A-Za-z0-9_.+%-]+\.zip$') {
            throw '[RELEASE] 工具下载地址或文件名不符合官方发行格式。'
        }
        $step = '创建用户工具目录'
        New-Item -ItemType Directory -Force -Path $ToolsDirectory | Out-Null
        $stage = Join-Path $ToolsDirectory ('.campus-download-' + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $stage | Out-Null
        $zip = Join-Path $stage $release.Name
        $step = '下载 ZIP'
        Write-Host "正在下载 $Kind：$($release.Name)"
        Invoke-WebRequest -Uri $uri.AbsoluteUri -OutFile $zip -UseBasicParsing -TimeoutSec 600
        $step = '校验下载文件'
        Assert-CampusArchiveHash -Path $zip -Hash $release.Hash -Algorithm $release.Algorithm
        $step = '解压工具'
        $extracted = Join-Path $stage 'unpacked'
        Expand-CampusToolArchive -Path $zip -Destination $extracted
        $roots = @(Get-ChildItem -LiteralPath $extracted -Directory)
        $pattern = @{ Java = '^jdk-'; Node = '^node-v22\.'; Maven = '^apache-maven-3\.9\.' }[$Kind]
        if ($roots.Count -ne 1 -or $roots[0].Name -notmatch $pattern) { throw '[ARCHIVE] ZIP 内工具目录结构不正确。' }
        $relative = @{ Java = 'bin/java.exe'; Node = 'node.exe'; Maven = 'bin/mvn.cmd' }[$Kind]
        $step = '验证工具版本'
        $tool = Find-CampusTool -Kind $Kind -Candidates @((Join-Path $roots[0].FullName $relative)) -JavaPath $JavaPath
        if (-not $tool) { throw '[VERSION] 下载后的工具无法运行或版本不兼容。' }
        # 只移动本次新建临时目录中的工具，绝不覆盖已有安装。
        $toolsRoot = [IO.Path]::GetFullPath($ToolsDirectory).TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
        $destination = [IO.Path]::GetFullPath((Join-Path $ToolsDirectory $roots[0].Name))
        $source = [IO.Path]::GetFullPath($roots[0].FullName)
        if (-not $destination.StartsWith($toolsRoot, [StringComparison]::OrdinalIgnoreCase) -or
            -not $source.StartsWith(([IO.Path]::GetFullPath($stage) + [IO.Path]::DirectorySeparatorChar), [StringComparison]::OrdinalIgnoreCase)) {
            throw '[PATH] 安装目标或来源超出工具目录。'
        }
        if (Test-Path -LiteralPath $destination) {
            throw "[EXISTING] $destination 已存在但不可用。请自行检查该目录；脚本不会覆盖或删除已有工具。"
        }
        $step = '保存工具'
        Move-Item -LiteralPath $source -Destination $destination
        $tool.Path = Join-Path $destination $relative
        return $tool
    } catch {
        # 不打印可能包含凭据的代理/供应方响应正文。
        $detail = if ($_.Exception.Message -match '^\[') { $_.Exception.Message } else { $_.Exception.GetType().Name }
        throw "[INSTALL_$($Kind.ToUpper())] ${step}失败：$detail。请检查网络、证书、磁盘空间及目录权限后重试；脚本不会改代理或关闭证书校验。"
    } finally {
        if ($stage -and (Test-Path -LiteralPath $stage)) {
            $resolvedStage = [IO.Path]::GetFullPath($stage)
            $resolvedTools = [IO.Path]::GetFullPath($ToolsDirectory).TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
            if ($resolvedStage.StartsWith($resolvedTools, [StringComparison]::OrdinalIgnoreCase) -and
                (Split-Path $resolvedStage -Leaf) -match '^\.campus-download-[a-f0-9]{32}$') {
                Remove-Item -LiteralPath $resolvedStage -Recurse -Force -ErrorAction SilentlyContinue
            }
        }
    }
}
