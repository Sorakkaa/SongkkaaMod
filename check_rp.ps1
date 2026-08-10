$rpPath = 'D:\Songgka\run\resourcepacks\§7!        §dSora§9kkaa §7[v1.2] 1.21.x'
$allFiles = Get-ChildItem -LiteralPath $rpPath -File -Recurse

Write-Output "Total files: $($allFiles.Count)"

$hashes = @{}
foreach ($f in $allFiles) {
    $hash = (Get-FileHash -LiteralPath $f.FullName -Algorithm MD5).Hash
    if (-not $hashes.ContainsKey($hash)) {
        $hashes[$hash] = @()
    }
    $hashes[$hash] += $f.FullName
}

$duplicatesFound = $false
foreach ($hash in $hashes.Keys) {
    if ($hashes[$hash].Count -gt 1) {
        $duplicatesFound = $true
        Write-Output "Duplicate files (MD5: $hash):"
        foreach ($file in $hashes[$hash]) {
            $relPath = $file.Substring($rpPath.Length + 1)
            Write-Output "  - $relPath"
        }
    }
}
if (-not $duplicatesFound) { Write-Output "No duplicate files found." }

Write-Output ""
Write-Output "--- Files with identical names in different folders ---"
$names = @{}
foreach ($f in $allFiles) {
    $name = $f.Name.ToLower()
    if (-not $names.ContainsKey($name)) {
        $names[$name] = @()
    }
    $names[$name] += $f.FullName
}

$nameDuplicatesFound = $false
foreach ($name in $names.Keys) {
    if ($names[$name].Count -gt 1 -and $name -ne "pack.mcmeta" -and $name -ne "sounds.json" -and $name -notmatch "\.lang$" -and $name -ne "tick.json") {
        $nameDuplicatesFound = $true
        Write-Output "Files with name '$name':"
        foreach ($file in $names[$name]) {
            $relPath = $file.Substring($rpPath.Length + 1)
            Write-Output "  - $relPath"
        }
    }
}
if (-not $nameDuplicatesFound) { Write-Output "No identical names found." }
