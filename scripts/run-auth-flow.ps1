<#
PowerShell script to programmatically verify authentication flows against the local backend.
Usage: In PowerShell, from workspace root:
    .\scripts\run-auth-flow.ps1
Optionally provide base URL:
    .\scripts\run-auth-flow.ps1 -BaseUrl "http://localhost:8080"
#>
param(
    [string]$BaseUrl = "http://localhost:8080"
)

function Send-Request {
    param(
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [string]$Token = $null
    )
    $uri = "$BaseUrl$Path"
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }

    try {
        if ($Method -eq 'GET') {
            $resp = Invoke-WebRequest -Uri $uri -Method Get -Headers $headers -ErrorAction Stop
        } else {
            $json = if ($Body -ne $null) { $Body | ConvertTo-Json -Depth 5 } else { '' }
            $resp = Invoke-WebRequest -Uri $uri -Method $Method -Body $json -ContentType 'application/json' -Headers $headers -ErrorAction Stop
        }
        return @{ status = $resp.StatusCode; content = $resp.Content }
    } catch {
        $err = $_.Exception
        if ($err.Response -ne $null) {
            $stream = $err.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $text = $reader.ReadToEnd()
            $status = $err.Response.StatusCode.value__
            return @{ status = $status; content = $text }
        }
        throw
    }
}

Write-Host "Target backend:" $BaseUrl

$timestamp = (Get-Date).ToString('yyyyMMddHHmmss')
$email = "test.user.$timestamp@example.com"
$password = "Password123!"
$fullName = "Test User $timestamp"
$phone = "999000$($timestamp.Substring($timestamp.Length - 6))"

Write-Host "\n1) Registering user: $email"
$regBody = @{ email = $email; password = $password; fullName = $fullName; phoneNumber = $phone }
$reg = Send-Request -Method 'POST' -Path '/api/v1/auth/register' -Body $regBody
Write-Host "Status:" $reg.status
Write-Host $reg.content | ConvertFrom-Json | ConvertTo-Json -Depth 5

Write-Host "\n2) Sending Email OTP"
$sendOtpBody = @{ email = $email }
$otpResp = Send-Request -Method 'POST' -Path '/api/v1/auth/send-email-otp' -Body $sendOtpBody
Write-Host "Status:" $otpResp.status
try { $otpData = ($otpResp.content | ConvertFrom-Json).data } catch { $otpData = $null }
if ($otpData -ne $null -and $otpData.simulatedOtp) {
    $simOtp = $otpData.simulatedOtp
    Write-Host "Simulated OTP:" $simOtp
} else {
    Write-Host "No simulated OTP in response; you'll need to read the OTP from the delivery channel."
    $simOtp = Read-Host "Enter OTP to use for verification"
}

Write-Host "\n3) Verifying OTP"
$verifyBody = @{ email = $email; otp = $simOtp; verificationMethod = 'EMAIL' }
$verifyResp = Send-Request -Method 'POST' -Path '/api/v1/auth/verify-otp' -Body $verifyBody
Write-Host "Status:" $verifyResp.status
Write-Host $verifyResp.content | ConvertFrom-Json | ConvertTo-Json -Depth 5

Write-Host "\n4) Logging in"
$loginBody = @{ email = $email; password = $password }
$loginResp = Send-Request -Method 'POST' -Path '/api/v1/auth/login' -Body $loginBody
Write-Host "Status:" $loginResp.status
try { $loginJson = $loginResp.content | ConvertFrom-Json } catch { $loginJson = $null }
if ($loginJson -ne $null -and $loginJson.data -ne $null -and $loginJson.data.accessToken) {
    $accessToken = $loginJson.data.accessToken
    Write-Host "Access token received (truncated):" ($accessToken.Substring(0,40) + '...')
} else {
    Write-Host "Login did not return an access token. Response:\n" $loginResp.content
    exit 1
}

Write-Host "\n5) Calling protected endpoint /api/v1/dashboard/summary with valid token"
$okResp = Send-Request -Method 'GET' -Path '/api/v1/dashboard/summary' -Token $accessToken
Write-Host "Status:" $okResp.status
try { $okResp.content | ConvertFrom-Json | ConvertTo-Json -Depth 5 } catch { Write-Host $okResp.content }

Write-Host "\n6) Calling protected endpoint with invalid/expired token (should get 401)"
$badToken = $accessToken + 'corrupt'
$badResp = Send-Request -Method 'GET' -Path '/api/v1/dashboard/summary' -Token $badToken
Write-Host "Status:" $badResp.status
try { $badResp.content | ConvertFrom-Json | ConvertTo-Json -Depth 5 } catch { Write-Host $badResp.content }

Write-Host "\n7) Attempting refresh token endpoint POST /api/v1/auth/refresh (likely absent)"
$refreshResp = Send-Request -Method 'POST' -Path '/api/v1/auth/refresh' -Body @{ refreshToken = 'dummy' }
Write-Host "Status:" $refreshResp.status
try { $refreshResp.content | ConvertFrom-Json | ConvertTo-Json -Depth 5 } catch { Write-Host $refreshResp.content }

Write-Host "\n8) Logging out"
$logoutResp = Send-Request -Method 'POST' -Path '/api/v1/auth/logout' -Token $accessToken
Write-Host "Status:" $logoutResp.status
try { $logoutResp.content | ConvertFrom-Json | ConvertTo-Json -Depth 5 } catch { Write-Host $logoutResp.content }

Write-Host "\n-- Script complete. Review the status codes above."

# Exit with 0
exit 0
