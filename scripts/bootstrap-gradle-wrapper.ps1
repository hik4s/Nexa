$ErrorActionPreference = 'Stop'

$Root = Resolve-Path (Join-Path $PSScriptRoot '..')
$Projects = @(
    (Join-Path $Root 'android'),
    (Join-Path $Root 'backend')
)

$GradleVersion = '9.5.0'

if (-not (Get-Command gradle -ErrorAction SilentlyContinue)) {
    Write-Error "Gradle não está disponível no PATH. Instale o Gradle ou use uma máquina com Gradle configurado e execute novamente este script."
}

foreach ($Project in $Projects) {
    Push-Location $Project
    try {
        Write-Host "Generating Gradle Wrapper in $Project"
        & gradle :wrapper --gradle-version $GradleVersion --distribution-type bin
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle Wrapper generation failed in $Project"
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host "Wrapper generation completed. Commit gradlew, gradlew.bat and gradle/wrapper/ files."
