$base_pkg = "com.example.al_aalim"
$src_dir = "C:\Users\izhaa\AndroidStudioProjects\AlAalim\app\src\main\java\com\example\al_aalim"
$ui_dir = Join-Path $src_dir "ui"
$layout_dir = "C:\Users\izhaa\AndroidStudioProjects\AlAalim\app\src\main\res\layout"

$files = Get-ChildItem -Path $ui_dir -Recurse -Filter "*.kt"
$class_to_fqdn = @{}

foreach ($f in $files) {
    $relPath = $f.FullName.Substring($ui_dir.Length + 1)
    $dirName = Split-Path $relPath -Parent
    $subpkg = $dirName -replace '\\', '.'
    $className = $f.BaseName
    $class_to_fqdn[$className] = ".ui.$subpkg.$className"
}

$xml_files = Get-ChildItem -Path $layout_dir -Filter "*.xml"
foreach ($xml in $xml_files) {
    $content = Get-Content $xml.FullName -Raw -Encoding UTF8
    $modified = $false
    
    foreach ($entry in $class_to_fqdn.GetEnumerator()) {
        $cls = $entry.Key
        $new_context = $entry.Value
        
        $search = 'tools:context=".' + $cls + '"'
        $replace = 'tools:context="' + $new_context + '"'
        
        if ($content -match "\b" + [regex]::Escape($search) + "\b") {
            $content = $content -replace [regex]::Escape($search), $replace
            $modified = $true
        }
    }
    
    if ($modified) {
        [System.IO.File]::WriteAllText($xml.FullName, $content, [System.Text.Encoding]::UTF8)
        Write-Host "Updated $($xml.Name)"
    }
}
