# Répertoire courant et parent
$currentDir = Get-Location
$parentDir  = Split-Path $currentDir -Parent

# Fichiers de sortie
$listFile = Join-Path $currentDir "file_list.txt"
$zipPath  = Join-Path $parentDir "project_sources.zip"

# Récupérer tous les fichiers sauf ceux exclus
$files = Get-ChildItem -Recurse -File `
  | Where-Object {
      $_.FullName -notmatch '\\\.git\\' -and
      $_.FullName -notmatch '\\\.gradle\\' -and
      $_.FullName -notmatch '\\build\\' -and
      $_.FullName -notmatch '\\\.idea\\'
    }

# Sauvegarder la liste relative des fichiers
$files | ForEach-Object {
    $_.FullName.Replace($currentDir.Path + "\", "")
} | Sort-Object | Out-File -FilePath $listFile -Encoding UTF8

# Supprimer l’archive si elle existe déjà
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

# Créer l’archive ZIP avec la structure relative
Compress-Archive -Path $files.FullName -DestinationPath $zipPath

Write-Host "✅ Archive créée : $zipPath"
Write-Host "✅ Liste des fichiers sauvegardée dans : $listFile"
