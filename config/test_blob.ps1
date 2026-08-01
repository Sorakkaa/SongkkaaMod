$url = "https://jsonblob.com/api/jsonBlob/019fb298-b5f9-7ed8-ada8-a9afb0dceaf9"
$body = '{"player1":"#FF55FF"}'
Invoke-RestMethod -Uri $url -Method Put -Body $body -ContentType "application/json"
$res = Invoke-RestMethod -Uri $url -Method Get
Write-Output $res
