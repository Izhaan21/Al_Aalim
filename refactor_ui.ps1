$base_pkg = "com.example.al_aalim"
$src_dir = "C:\Users\izhaa\AndroidStudioProjects\AlAalim\app\src\main\java\com\example\al_aalim"
$ui_dir = Join-Path $src_dir "ui"

$files = Get-ChildItem -Path $ui_dir -Recurse -Filter "*.kt"
$class_to_fqdn = @{}

foreach ($f in $files) {
    # Rel path
    $relPath = $f.FullName.Substring($ui_dir.Length + 1)
    $dirName = Split-Path $relPath -Parent
    $subpkg = $dirName -replace '\\', '.'
    $className = $f.BaseName
    $class_to_fqdn[$className] = "$base_pkg.ui.$subpkg.$className"
}

foreach ($f in $files) {
    $relPath = $f.FullName.Substring($ui_dir.Length + 1)
    $dirName = Split-Path $relPath -Parent
    $subpkg = $dirName -replace '\\', '.'
    $new_pkg = "$base_pkg.ui.$subpkg"
    
    $content = Get-Content $f.FullName -Raw -Encoding UTF8
    
    # 1. Update package
    $content = $content -replace "(?m)^package\s+com\.example\.al_aalim\s*$", "package $new_pkg"
    
    # 2. Add R import if not there
    if (!($content -match "import com\.example\.al_aalim\.R") -and ($content -match "\bR\.")) {
        $content = $content -replace "(?m)^(package\s+[^\r\n]+)", "`$1`r`n`r`nimport com.example.al_aalim.R"
    }
    
    # 3. Add necessary imports
    $needed_imports = @()
    foreach ($entry in $class_to_fqdn.GetEnumerator()) {
        $cls = $entry.Key
        $fqdn = $entry.Value
        
        if ($fqdn.StartsWith("$new_pkg.")) {
            continue
        }
        
        if ($content -match "\b$cls\b") {
            $needed_imports += "import $fqdn"
        }
    }
    
    if ($needed_imports.Count -gt 0) {
        $imports_str = $needed_imports -join "`r`n"
        if ($content -match "import com\.example\.al_aalim\.R") {
            $content = $content -replace "import com\.example\.al_aalim\.R", "import com.example.al_aalim.R`r`n$imports_str"
        } else {
            $content = $content -replace "(?m)^(package\s+[^\r\n]+)", "`$1`r`n`r`n$imports_str"
        }
    }
    
    [System.IO.File]::WriteAllText($f.FullName, $content, [System.Text.Encoding]::UTF8)
}

Write-Host "Kotlin files updated."
