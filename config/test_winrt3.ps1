try {
    $null = [Windows.Foundation.IAsyncInfo, Windows.Foundation, ContentType=WindowsRuntime]
    $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media, ContentType=WindowsRuntime]
    $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManagerSessionMediaProperties, Windows.Media, ContentType=WindowsRuntime]
    Add-Type -AssemblyName System.Runtime.WindowsRuntime

    $asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Length -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' }[0]

    $op = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()
    $task = $asTask.MakeGenericMethod([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]).Invoke($null, @($op))
    $task.Wait(2000)
    $mgr = $task.Result

    if ($mgr) {
        $sessions = @($mgr.GetCurrentSession()) + @($mgr.GetSessions())
        foreach ($session in $sessions) {
            if ($session) {
                try {
                    $op2 = $session.TryGetMediaPropertiesAsync()
                    $task2 = $asTask.MakeGenericMethod([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManagerSessionMediaProperties]).Invoke($null, @($op2))
                    $task2.Wait(2000)
                    $props = $task2.Result
                    if ($props -and $props.Title) {
                        [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
                        Write-Output ($props.Title + " - " + $props.Artist)
                        exit 0
                    }
                } catch {
                    Write-Output "ERR2: $_"
                }
            }
        }
        Write-Output "NO_ACTIVE_MEDIA_SESSION"
    } else {
        Write-Output "MGR_NULL"
    }
} catch {
    Write-Output "ERR1: $_"
}
