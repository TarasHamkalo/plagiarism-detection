param(
    [Parameter(Mandatory=$true)]
    [string]$InputDir
)

$InputDir = Join-path $(pwd) $InputDir
$OutputDir = Join-path $(pwd) "processed"
$MetaDir = Join-Path $OutputDir "meta"
$TextDir = Join-Path $OutputDir "text"
$DocxDir = Join-Path $OutputDir "docx"

New-Item -ItemType Directory -Force -Path $DocxDir, $MetaDir, $TextDir

Get-ChildItem -Path $InputDir -Filter *.docx | Copy-Item -Destination $DocxDir -Force

$docFiles = Get-ChildItem -Path $InputDir -Filter *.doc | ForEach-Object {
    $_.FullName
}

Start-Process -NoNewWindow -Wait -FilePath "soffice.exe" -ArgumentList "--headless", "--convert-to", "docx", $($docFiles -join ' '), "--outdir", $DocxDir


$docx = Get-ChildItem -Path $DocxDir -Filter *.docx 

$docxPaths = $docx | ForEach-Object { $_.FullName }

Start-Process -NoNewWindow -Wait -FilePath "exiftool" -ArgumentList $($docxPaths -join ' '), "-XMP:ALL", "-XML:ALL", "-j", "-n", "-w", "$MetaDir/%f.json"
 

foreach ($file in $docx) {
  $document = $file.BaseName  # Get the file name without extension
  $textFile = $(Join-Path $TextDir "$document.txt")
  echo $file.FullName 
  echo $textFile

  pandoc $file.FullName -t plain -f docx+smart -o $textFile
}
