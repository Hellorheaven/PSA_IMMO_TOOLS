# Liste uniquement les fichiers pertinents (Kotlin, Java, XML)
Get-ChildItem -Recurse -Include *.kt, *.java, *.xml `
  | Where-Object {
      $_.FullName -notmatch '\\\.git\\' -and
      $_.FullName -notmatch '\\\.gradle\\' -and
      $_.FullName -notmatch '\\build\\' -and
      $_.FullName -notmatch '\\\.idea\\'
    } `
  | ForEach-Object { $_.FullName } `
  | Out-File -FilePath file_list.txt -Encoding UTF8

Write-Output "Liste sauvegardée dans file_list.txt"