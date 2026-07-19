Write-Host "=== Health ==="
Invoke-RestMethod http://localhost:8080/api/health | ConvertTo-Json

Write-Host "=== Banners ==="
try {
    $r = Invoke-RestMethod http://localhost:8080/api/banners
    Write-Host "Code: $($r.code), Count: $($r.data.Count)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host "=== Products ==="
try {
    $r = Invoke-RestMethod http://localhost:8080/api/products?size=2
    Write-Host "Code: $($r.code), Count: $($r.data.Count)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }
