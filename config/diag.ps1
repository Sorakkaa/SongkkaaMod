Get-Process | Where-Object { $_.MainWindowTitle } | ForEach-Object { "$($_.ProcessName) | $($_.MainWindowTitle)" } | Out-File -FilePath "config/diag.txt" -Encoding utf8
