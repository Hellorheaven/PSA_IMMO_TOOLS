Get-ChildItem -Recurse -Include *.kt, *.java, *.xml `
  | Where-Object {
      $_.FullName -notmatch '\\\.git\\' -and
      $_.FullName -notmatch '\\\.gradle\\' -and
      $_.FullName -notmatch '\\build\\' -and
      $_.FullName -notmatch '\\\.idea\\'
    } `
  | ForEach-Object { $_.FullName.Replace((Get-Location).Path + "\", "") } `
  | Sort-Object `
  | Out-File -FilePath file_list.txt -Encoding UTF8
