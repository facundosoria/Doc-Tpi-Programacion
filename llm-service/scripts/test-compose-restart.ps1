<#
.SYNOPSIS
  test-compose-restart.ps1 — Prueba automatizada de reinicio de Compose (H06 / T4)
  Traza: CA3 (persistencia tras reinicio) y CA6 (fallo bloqueante ante pérdida de datos)
#>
param(
  [string]$BaseUrl = $env:BASE_URL,
  [string]$HealthUrl = $env:HEALTH_URL,
  [string]$ProjectName = $env:PROJECT_NAME,
  [switch]$SkipComposeManage
)

# gateway-mock (Nginx, :8080) inyecta la identidad docente y enruta /api/courses al courses-mock; sin él, crear un golden set
# da 503 "Courses no está disponible" (validación de membresía contra courses-service desde la integración main→dev).
if (-not $BaseUrl) { $BaseUrl = "http://localhost:8080" }
if (-not $HealthUrl) { $HealthUrl = "http://localhost:8087" }
if (-not $ProjectName) { $ProjectName = "llm-s1-restart-test" }

$ComposeFiles = @("-f", "compose.yaml", "-f", "compose.workbench.yaml", "-f", "compose.debug.yaml", "-p", $ProjectName)
$Services = @("llm-service", "gateway-mock", "courses-mock")

# La clave AES de credenciales es obligatoria (compose.yaml); si no viene del entorno ni de .env, se genera una descartable.
if (-not $env:LLM_CREDENTIALS_MASTER_KEY) {
  $bytes = New-Object byte[] 32
  [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  $env:LLM_CREDENTIALS_MASTER_KEY = [Convert]::ToBase64String($bytes)
}

$CourseId = "22222222-2222-2222-2222-222222222222"
$TeacherId = "11111111-1111-1111-1111-111111111111"

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host " [H06*T4] Iniciando prueba automatizada de reinicio de Compose" -ForegroundColor Cyan
Write-Host " Traza: CA3 (persistencia de datos) / CA6 (deteccion de perdida)" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

function Wait-ForHealth {
  param([string]$Url)
  $maxRetries = 30
  $count = 0
  Write-Host -NoNewline "Esperando salud del servicio ($Url/actuator/health)... "
  while ($count -lt $maxRetries) {
    try {
      $resp = Invoke-RestMethod -Uri "$Url/actuator/health" -Method Get -TimeoutSec 3 -ErrorAction Stop
      if ($resp.status -eq "UP") {
        Write-Host " UP!" -ForegroundColor Green
        return $true
      }
    } catch {
      Start-Sleep -Seconds 2
      $count++
      Write-Host -NoNewline "."
    }
  }
  Write-Host " ERROR: El servicio no alcanzo estado UP a tiempo." -ForegroundColor Red
  return $false
}

$headers = @{
  "Content-Type" = "application/json"
  "X-Principal-Type" = "service"
  "X-Service-Id" = "admin-service"
  "X-Service-Scopes" = "llm.golden-set.manage"
  "X-Delegated-User" = $TeacherId
  "X-Actor-Id" = $TeacherId
  "X-User-Roles" = "TEACHER"
  "X-Teacher-Course-Ids" = $CourseId
  "X-Request-Id" = "h06-restart-test-ps1"
  "traceparent" = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
}

try {
  if (-not $SkipComposeManage) {
    Write-Host "1. Levantando stack con Docker Compose (proyecto: $ProjectName)..."
    docker compose @ComposeFiles up -d --build --wait @Services
  }

  $healthy = Wait-ForHealth -Url $HealthUrl
  if (-not $healthy) { exit 1 }

  Write-Host "2. Creando Golden Set de referencia para el curso $CourseId..."
  $createBody = @{ name = "Banco de referencia S1 - Prueba de reinicio" } | ConvertTo-Json
  $createResp = Invoke-RestMethod -Uri "$BaseUrl/api/llm/courses/$CourseId/golden-sets" -Method Post -Headers $headers -Body $createBody

  $versionId = $createResp.id
  if (-not $versionId) {
    Write-Host "ERROR: No se obtuvo el ID de la version del Golden Set creado." -ForegroundColor Red
    exit 1
  }
  Write-Host "   Golden Set creado exitosamente. VersionId: $versionId" -ForegroundColor Green

  Write-Host "3. Cargando caso de referencia en el Golden Set..."
  $casePayload = @{
    transcript = @(
      @{
        role = "STUDENT"
        content = "¿Cómo calculo la complejidad temporal de una búsqueda binaria?"
      }
    )
    challengeContext = @{
      statement = "Explicar complejidad logarítmica"
      expectedKeyPoints = @("división a la mitad", "O(log n)")
    }
    author = "Docente Titular S1"
    referenceScores = @{
      AUTONOMY = 85
      CLARITY = 90
      PROGRESSION = 88
      COMPLIANCE = 92
      EFFICIENCY = 85
    }
    scoreJustifications = @{
      AUTONOMY = "El estudiante resolvió de forma independiente"
    }
  } | ConvertTo-Json -Depth 5

  $null = Invoke-RestMethod -Uri "$BaseUrl/api/llm/courses/$CourseId/golden-sets/$versionId/cases" -Method Post -Headers $headers -Body $casePayload
  Write-Host "   Caso de referencia insertado correctamente." -ForegroundColor Green

  Write-Host "4. Consultando el Golden Set antes de reiniciar..."
  $listBefore = Invoke-RestMethod -Uri "$BaseUrl/api/llm/courses/$CourseId/golden-sets" -Method Get -Headers $headers
  $foundBefore = $listBefore.items | Where-Object { $_.id -eq $versionId }
  if (-not $foundBefore) {
    Write-Host "ERROR: Golden Set no encontrado en la lista previo al reinicio." -ForegroundColor Red
    exit 1
  }
  Write-Host "   Confirmado: Golden Set $versionId presente previo al reinicio." -ForegroundColor Green

  if (-not $SkipComposeManage) {
    Write-Host "5. Ejecutando reinicio de Compose (docker compose restart)..."
    docker compose @ComposeFiles restart
    Write-Host "   Reinicio completado. Verificando recuperacion del servicio..."
    $healthyAfter = Wait-ForHealth -Url $HealthUrl
    if (-not $healthyAfter) { exit 1 }
  } else {
    Write-Host "5. [Modo externo] Omitiendo restart de contenedores por parametro SkipComposeManage."
  }

  Write-Host "6. Consultando el Golden Set tras el reinicio del entorno..."
  $listAfter = Invoke-RestMethod -Uri "$BaseUrl/api/llm/courses/$CourseId/golden-sets" -Method Get -Headers $headers
  $foundAfter = $listAfter.items | Where-Object { $_.id -eq $versionId }

  if (-not $foundAfter -or $foundAfter.cases.Count -lt 1) {
    Write-Host "==================================================================" -ForegroundColor Red
    Write-Host " [FALLO CRITICO CA6] El Golden Set $versionId NO persiste tras el reinicio." -ForegroundColor Red
    Write-Host " Se detecto perdida de datos en la base. Bloqueando Review de S1." -ForegroundColor Red
    Write-Host "==================================================================" -ForegroundColor Red
    exit 1
  }

  Write-Host "==================================================================" -ForegroundColor Green
  Write-Host " [EXITO CA3 & CA6] El banco de casos de referencia persistio intacto." -ForegroundColor Green
  Write-Host " Prueba automatizada de reinicio superada en verde." -ForegroundColor Green
  Write-Host "==================================================================" -ForegroundColor Green
  exit 0

} finally {
  if (-not $SkipComposeManage) {
    Write-Host "Limpiando entorno Compose de prueba..."
    docker compose @ComposeFiles down --volumes --remove-orphans 2>$null
  }
}
