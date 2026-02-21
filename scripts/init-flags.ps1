$ErrorActionPreference = "Stop"

$UNLEASH_URL = $env:UNLEASH_URL
if ([string]::IsNullOrWhiteSpace($UNLEASH_URL)) { $UNLEASH_URL = "http://localhost:4242" }

$UNLEASH_TOKEN = $env:UNLEASH_TOKEN
if ([string]::IsNullOrWhiteSpace($UNLEASH_TOKEN)) { $UNLEASH_TOKEN = "development.unleash-insecure-api-token" }

$PROJECT = $env:UNLEASH_PROJECT
if ([string]::IsNullOrWhiteSpace($PROJECT)) { $PROJECT = "default" }

Write-Host "Unleash URL: $UNLEASH_URL"
Write-Host "Project: $PROJECT"

# Wait for Unleash /health
Write-Host "Waiting for Unleash to be ready..."
$ready = $false
for ($i=1; $i -le 60; $i++) {
  try {
    $resp = Invoke-WebRequest -Uri "$UNLEASH_URL/health" -UseBasicParsing -TimeoutSec 3
    if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 300) {
      $ready = $true
      break
    }
  } catch {
    Start-Sleep -Seconds 2
  }
}
if (-not $ready) { throw "ERROR: Unleash not ready after waiting." }

function Create-Flag($name, $description) {
  $uri = "$UNLEASH_URL/api/admin/projects/$PROJECT/features"
  $headers = @{
    "Authorization" = $UNLEASH_TOKEN
    "Content-Type"  = "application/json"
  }
  $body = @{
    name        = $name
    description = $description
  } | ConvertTo-Json

  try {
    Invoke-WebRequest -Method Post -Uri $uri -Headers $headers -Body $body -UseBasicParsing | Out-Null
    Write-Host "Created flag: $name"
  } catch {
    # If exists, Unleash typically returns 409 Conflict
    $status = $_.Exception.Response.StatusCode.value__
    if ($status -eq 409) {
      Write-Host "Flag already exists: $name (ok)"
    } else {
      $msg = $_.Exception.Message
      throw "ERROR: Failed creating flag $name. HTTP $status. $msg"
    }
  }
}

Create-Flag "premium-pricing" "Enable premium products endpoint in Product Service"
Create-Flag "order-notifications" "Enable notification logging when an order is created"
Create-Flag "bulk-order-discount" "Enable 15% discount when order quantity > 5"

Write-Host "Done."