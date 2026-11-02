<#
.SYNOPSIS
    Checks the "definition of done" of the labs: every solution must be green and every
    starter must fail (otherwise the learner has nothing to do).

.EXAMPLE
    .\verify.ps1                  # all labs
    .\verify.ps1 -Lab lab-03-cdi  # one lab
#>
param([string]$Lab = '*')

$root = $PSScriptRoot
$failed = $false
$rows = @()

foreach ($dir in Get-ChildItem -Path $root -Directory -Filter "lab-*" | Where-Object { $_.Name -like $Lab }) {
    foreach ($variant in 'solution', 'starter') {
        $project = Join-Path $dir.FullName $variant
        if (-not (Test-Path (Join-Path $project 'pom.xml'))) { continue }

        Push-Location $project
        try {
            # Projects with integration tests (*IT.java) need the packaged application: `verify`, not `test`.
            # Multi-module projects keep their reports in every module, so look for them recursively.
            $hasIT = [bool](Get-ChildItem -Path $project -Recurse -Filter '*IT.java' -File -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notmatch '[/\\]target[/\\]' } | Select-Object -First 1)
            Get-ChildItem -Path $project -Recurse -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -in 'surefire-reports', 'failsafe-reports' } |
                ForEach-Object { Remove-Item $_.FullName -Recurse -Force }
            if ($hasIT) { & .\mvnw.cmd -q -fae verify '-DskipITs=false' *> $null } else { & .\mvnw.cmd -q -fae test *> $null }
            $tests = 0; $bad = 0
            $reports = Get-ChildItem -Path $project -Recurse -Filter '*.xml' -File -ErrorAction SilentlyContinue |
                Where-Object { $_.DirectoryName -match 'target[/\\](surefire|failsafe)-reports$' }
            foreach ($file in $reports) {
                $suite = ([xml](Get-Content $file.FullName)).testsuite
                if ($null -eq $suite) { continue }
                $tests += [int]$suite.tests
                $bad += [int]$suite.failures + [int]$suite.errors
            }
        } finally { Pop-Location }

        $ok = if ($variant -eq 'solution') { ($tests -gt 0 -and $bad -eq 0) } else { ($bad -gt 0) }
        if (-not $ok) { $failed = $true }
        $rows += [pscustomobject]@{
            Lab = $dir.Name; Variant = $variant; Tests = $tests; Failed = $bad
            Expected = $(if ($variant -eq 'solution') { 'all green' } else { 'some red' })
            Result = $(if ($ok) { 'OK' } else { 'PROBLEM' })
        }
    }
}

$rows | Format-Table -AutoSize
if ($failed) { Write-Host 'Some labs do not meet the definition of done.' -ForegroundColor Red; exit 1 }
Write-Host 'All labs meet the definition of done.' -ForegroundColor Green
exit 0
