Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$junitJar = Join-Path $repoRoot "lib\junit-4.7.jar"
if( -not ( Test-Path $junitJar ) ) {
  throw "Missing bundled dependency: $junitJar"
}

$buildClasses = Join-Path $repoRoot "build\classes"
$buildTestClasses = Join-Path $repoRoot "build\test\classes"

New-Item -ItemType Directory -Force -Path $buildClasses | Out-Null
New-Item -ItemType Directory -Force -Path $buildTestClasses | Out-Null

$srcFiles = Get-ChildItem -Recurse -File -Path ( Join-Path $repoRoot "src" ) -Filter *.java |
  Select-Object -ExpandProperty FullName
if( $srcFiles.Count -eq 0 ) {
  throw "No source files found under src"
}

& javac -encoding UTF-8 -cp "src;$junitJar" -d $buildClasses @srcFiles
if( $LASTEXITCODE -ne 0 ) {
  exit $LASTEXITCODE
}

$testFiles = Get-ChildItem -Recurse -File -Path ( Join-Path $repoRoot "test" ) -Filter *.java |
  Select-Object -ExpandProperty FullName
if( $testFiles.Count -eq 0 ) {
  throw "No test files found under test"
}

& javac -encoding UTF-8 -cp "$buildClasses;$junitJar" -d $buildTestClasses @testFiles
if( $LASTEXITCODE -ne 0 ) {
  exit $LASTEXITCODE
}

$testClasses = Get-ChildItem -Recurse -File -Path ( Join-Path $repoRoot "test" ) -Filter *Test.java |
  ForEach-Object {
    $packageLine = Select-String -Path $_.FullName -Pattern '^package\s+([A-Za-z0-9_.]+);' |
      Select-Object -First 1
    $baseName = $_.BaseName
    if( $packageLine ) {
      $packageName = $packageLine.Matches[0].Groups[1].Value
      "$packageName.$baseName"
    } else {
      $baseName
    }
  }

if( $testClasses.Count -eq 0 ) {
  throw "No *Test.java classes found under test"
}

& java -cp "$buildClasses;$buildTestClasses;$junitJar" org.junit.runner.JUnitCore @testClasses
exit $LASTEXITCODE
