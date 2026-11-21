# Affiche un access token Keycloak pour un compte de démo (DÉVELOPPEMENT UNIQUEMENT).
#   $TOKEN = .\scripts\get-token.ps1 coordinateur@rescueops.test
#   Invoke-RestMethod http://localhost:8080/api/users/me -Headers @{ Authorization = "Bearer $TOKEN" }
param(
    [Parameter(Mandatory = $true)][string]$Username,
    [string]$Password = 'Demo1234!',
    [string]$KeycloakUrl = 'http://localhost:8180'
)
$body = @{
    client_id  = 'rescueops-dev-cli'
    grant_type = 'password'
    username   = $Username
    password   = $Password
}
$response = Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/rescueops/protocol/openid-connect/token" -Body $body
$response.access_token
