$req = [System.Net.WebRequest]::Create("https://jsonblob.com/api/jsonBlob")
$req.Method = "POST"
$req.ContentType = "application/json"
$bytes = [System.Text.Encoding]::UTF8.GetBytes("{}")
$st = $req.GetRequestStream()
$st.Write($bytes, 0, $bytes.Length)
$st.Close()
$resp = $req.GetResponse()
Write-Output $resp.Headers["Location"]
