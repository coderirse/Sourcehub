Write-Host "=== 1. Register ===" -ForegroundColor Cyan
$register = @{name="TestUser"; email="test@test.com"; password="123456"} | ConvertTo-Json
$r1 = Invoke-RestMethod -Uri http://localhost:8080/api/auth/register -Method POST -Body $register -ContentType "application/json"
$r1 | ConvertTo-Json -Depth 5
Write-Host ""

Write-Host "=== 2. Login ===" -ForegroundColor Cyan
$login = @{email="test@test.com"; password="123456"} | ConvertTo-Json
$r2 = Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method POST -Body $login -ContentType "application/json"
$r2 | ConvertTo-Json -Depth 5
$token = $r2.data.accessToken
Write-Host "Token: $($token.Substring(0, [Math]::Min(30,$token.Length)))..."

Write-Host ""
Write-Host "=== 3. Products ===" -ForegroundColor Cyan
$r3 = Invoke-RestMethod -Uri http://localhost:8080/api/products?size=2 -Headers @{Authorization="Bearer $token"}
Write-Host "Code: $($r3.code), Count: $($r3.data.Count)"
$r3.data[0] | Select-Object id,title,price | Format-List

Write-Host ""
Write-Host "=== 4. Banners ===" -ForegroundColor Cyan
$r4 = Invoke-RestMethod -Uri http://localhost:8080/api/banners
Write-Host "Code: $($r4.code), Count: $($r4.data.Count)"

Write-Host ""
Write-Host "=== 5. Categories ===" -ForegroundColor Cyan
$r5 = Invoke-RestMethod -Uri http://localhost:8080/api/categories
Write-Host "Code: $($r5.code), Count: $($r5.data.Count)"

Write-Host ""
Write-Host "=== 6. Create Order ===" -ForegroundColor Cyan
$orderBody = @{
    items = @(@{productId="p1"; quantity=1})
    couponCode = "SAVE10"
    paymentMethod = "WECHAT"
} | ConvertTo-Json -Depth 3
$r6 = Invoke-RestMethod -Uri http://localhost:8080/api/orders -Method POST -Body $orderBody -ContentType "application/json" -Headers @{Authorization="Bearer $token"}
Write-Host "OrderId: $($r6.data.id), Total: $($r6.data.finalAmount), Status: $($r6.data.status)"

Write-Host ""
Write-Host "=== 7. Pay ===" -ForegroundColor Cyan
$payBody = @{orderId=$r6.data.id; amount=$r6.data.finalAmount; method="WECHAT"} | ConvertTo-Json
$r7 = Invoke-RestMethod -Uri http://localhost:8080/api/payment/pay -Method POST -Body $payBody -ContentType "application/json" -Headers @{Authorization="Bearer $token"}
Write-Host "Status: $($r7.data.status), TxnId: $($r7.data.transactionId)"

Write-Host ""
Write-Host "=== 8. Forgot Password ===" -ForegroundColor Cyan
$fpBody = @{email="test@test.com"} | ConvertTo-Json
$r8 = Invoke-RestMethod -Uri http://localhost:8080/api/auth/forgot-password -Method POST -Body $fpBody -ContentType "application/json"
Write-Host "Code: $($r8.code), Message: $($r8.message)"

Write-Host ""
Write-Host "=== ALL TESTS DONE ===" -ForegroundColor Green
