param(
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]] $RankGAArgs = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$buildClasses = Join-Path $repoRoot "build\classes"
New-Item -ItemType Directory -Force -Path $buildClasses | Out-Null

$srcFiles = Get-ChildItem -Recurse -File -Path (Join-Path $repoRoot "src") -Filter *.java |
  Select-Object -ExpandProperty FullName
if( $srcFiles.Count -eq 0 ) {
  throw "No source files found under src"
}

& javac -encoding UTF-8 -cp "src" -d $buildClasses @srcFiles
if( $LASTEXITCODE -ne 0 ) {
  exit $LASTEXITCODE
}

& java -cp $buildClasses rankga.RankGA @RankGAArgs
exit $LASTEXITCODE
