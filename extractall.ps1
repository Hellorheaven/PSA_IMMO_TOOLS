# Répertoire courant
$currentDir = Get-Location

# Fichier de sortie unique
$outputFile = Join-Path $currentDir "project_content.txt"

# Liste des extensions de fichiers à exclure
$excludedExtensions = @(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".jar", ".apk",".dm",".webp")

# Supprimer le fichier de sortie s'il existe déjà
if (Test-Path $outputFile) {
    Remove-Item $outputFile -Force
}

# Récupérer tous les fichiers à inclure
$filesToInclude = Get-ChildItem -Recurse -File `
  | Where-Object {
    $_.FullName -notmatch '\\\.git\\' -and
            $_.FullName -notmatch '\\\.gradle\\' -and
            $_.FullName -notmatch '\\build\\' -and
            $_.FullName -notmatch '\\\.idea\\' -and
            $excludedExtensions -notcontains $_.Extension
}

# Écrire le contenu de chaque fichier dans le fichier de sortie
foreach ($file in $filesToInclude) {
    # Chemin relatif du fichier
    $relativePath = $file.FullName.Replace($currentDir.Path + "\", "")

    # Séparateur pour chaque fichier
    Add-Content -Path $outputFile -Value "`n--- Fichier : $relativePath ---`n" -Encoding UTF8

    # Contenu du fichier
    Get-Content -Path $file.FullName | Add-Content -Path $outputFile -Encoding UTF8
}

Write-Host "✅ Contenu du projet exporté dans : $outputFile"