$REGISTRY = "us-central1-docker.pkg.dev/storied-shore-473817-v5/college-services"
$LOG = "C:\Users\sande\IdeaProjects\push-gcp-ps.log"
$DONE_FILE = "C:\Users\sande\IdeaProjects\push-gcp-done.txt"

Remove-Item -Path $DONE_FILE -ErrorAction SilentlyContinue
"" | Out-File -FilePath $LOG

function Push-Image {
    param([string]$Name)
    $full = "$REGISTRY/$Name"
    Write-Host "[$([DateTime]::Now.ToString('HH:mm:ss'))] Pushing $Name..."
    Add-Content $LOG "[$([DateTime]::Now.ToString('HH:mm:ss'))] Pushing $Name..."
    docker push "$full`:latest" 2>&1 | Tee-Object -Append -FilePath $LOG
    docker push "$full`:v1.0.0"  2>&1 | Tee-Object -Append -FilePath $LOG
    if ($LASTEXITCODE -eq 0) {
        Add-Content $LOG "[$([DateTime]::Now.ToString('HH:mm:ss'))] $Name PUSHED OK"
        Write-Host "[$([DateTime]::Now.ToString('HH:mm:ss'))] $Name PUSHED OK"
    } else {
        Add-Content $LOG "[$([DateTime]::Now.ToString('HH:mm:ss'))] $Name PUSH FAILED (exit $LASTEXITCODE)"
        Write-Host "ERROR: $Name PUSH FAILED"
    }
}

$services = @(
    "college-notifications-frontend",
    "college-portal-frontend",
    "college-updates-frontend",
    "college-admin",
    "college-notifications-backend",
    "college-updates-backend"
)

foreach ($svc in $services) {
    Push-Image $svc
}

Add-Content $LOG "ALL_DONE"
"ALL_DONE" | Out-File -FilePath $DONE_FILE
Write-Host "All pushes complete. Results in $LOG"
