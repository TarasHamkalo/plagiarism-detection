param(
    [Parameter(Mandatory=$true)]
    [string]$InputDir
)
# $File = New-TemporaryFile
# Remove-Item $File -Force
# $TMP_DIR = New-Item -Itemtype Directory -Path "$($ENV:Temp)\$($File.Name)" 


$HTML_HEAD = @"
<html lang="sk">
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
    <title>Metadata Report</title>
</head>
<body>
    <h1>Metadata Report</h1>
    <table border="1">
    <tr>
        <th>Submitter</th>
        <th>Creator</th>
        <th>Last Modified By</th>
        <th>Total Edit Time (MIN)</th>
        <th>Date Created</th>
        <th>Date Modified</th>
        <th>Last Printed</th>
        <th>Template</th>
        <th>Pages</th>
    </tr>
"@

$HTML_END = @"
    </table>
</body>
</html>
"@

function AppendHtmlRow {
    param($metadata, $OutputFilePath)
    # $submitter = Split-Path -Path $metadata.FilePath -Parent
    # $submitter = Split-Path -Leaf $(Split-Path -Path $metadata.FilePath -Parent)
    $pattern = '\d{4}_\d{4}_[A-Za-z]+_[A-Za-z]+_\d+_\d+_dokumentacia_'
    $submitter = ($metadata.FilePath | Select-String -Pattern $pattern).Matches.Value

    $htmlRow = @"
<tr>
    <td>$($submitter)</td>
    <td>$($metadata.creator)</td>
    <td>$($metadata.lastModifiedBy)</td>
    <td>$($metadata.totalTime)</td>
    <td>$($metadata.created)</td>
    <td>$($metadata.modified)</td>
    <td>$($metadata.lastPrinted)</td>
    <td>$($metadata.template)</td>
    <td>$($metadata.pages)</td>
</tr>
"@
    $htmlRow | Add-Content $OutputFilePath
}

function ConvertDocToDocx {
    param ($submissionPath, $outputPath )

    $wordApp = New-Object -ComObject Word.Application
    $convertedFiles = Get-ChildItem -Recurse -Path $submissionPath -Filter *.doc | ForEach-Object {
        $document = $wordApp.Documents.Open($_.FullName)
        $document.Convert()
        $document.Close()

        Write-Output Join-Path $_.Directory "$($_.BaseName).docx" | Out-Null
    }

    $wordApp.Quit()
    Write-Output $convertedFiles
}

function GetDocxPaths {
    param ($submissionPath, $TmpDocxPath)

    $docConvertedDocuments = ConvertDocToDocx $submissionPath $TmpDocxPath
    $docxDocuments = Get-ChildItem -Recurse -Path $submissionPath -Filter *.docx | ForEach-Object {
        Convert-Path $_.FullName
    }
       
    Write-Output $($docxDocuments; $docConvertedDocuments)
}

function ReadMetadata {
    param($docxPath)

    try {
        $zipFile = [IO.Compression.ZipFile]::OpenRead($docxPath)
        $appXmlEntry = $zipFile.Entries | Where-Object { $_.FullName -eq "docProps/app.xml" }
        $coreXmlEntry = $zipFile.Entries | Where-Object { $_.FullName -eq "docProps/core.xml" }

        if ($appXmlEntry) {
            $appXmlContent = [xml](New-Object IO.StreamReader($appXmlEntry.Open())).ReadToEnd()
            $pages = $appXmlContent.Properties.Pages
            $totalTime = $appXmlContent.Properties.TotalTime
            $template = $appXmlContent.Properties.Template
        } else {
            Write-Output "docProps/app.xml not found in $docxPath"
        }

        if ($coreXmlEntry) {
            $coreXmlContent = [xml](New-Object IO.StreamReader($coreXmlEntry.Open())).ReadToEnd()
            # Write-Output $coreXmlContent.coreProperties
            $creator = $coreXmlContent.coreProperties.creator
            $lastModifiedBy = $coreXmlContent.coreProperties.lastModifiedBy
            $revision = $coreXmlContent.coreProperties.revision
            $created = $coreXmlContent.coreProperties.created.InnerText
            $modified = $coreXmlContent.coreProperties.modified.InnerText
            $lastPrinted = $coreXmlContent.coreProperties.lastPrinted
        } else {
            Write-Output "docProps/core.xml not found in $docxPath"
        }

        [PSCustomObject]@{
            FilePath       = $docxPath
            Pages          = $pages
            TotalTime      = $totalTime
            Template       = $template
            Creator        = $creator
            LastModifiedBy = $lastModifiedBy
            Revision       = $revision
            Created        = $created
            Modified       = $modified
            LastPrinted    = $lastPrinted
        }

    } catch {
        Write-Error "Error processing $($docxPath): $($_)"
    } finally {
        $zipFile.Dispose()
    }
}

function WriteSingleSubmission {
    param (
        $docxDocumentPaths, $OutputFilePath
    )

    $filteredDocxPaths = $docxDocumentPaths | Where-Object { 
        -not [string]::IsNullOrWhiteSpace($_) -and -not (Split-Path -Leaf $_).StartsWith('.') 
    }
    
    foreach ($docxPath in $filteredDocxPaths) {      
        try {
            $metadata = ReadMetadata $docxPath
            AppendHtmlRow $metadata $OutputFilePath
        } catch {
            Write-Error "Error writing $($docxPath)"
        }
    }
}

# $InputDir = "test_files"

$InputPath = Join-path $(pwd) $InputDir
$OutputPath = Join-path $(pwd) "out"
$TmpPath = Join-path $OutputPath "tmp"

$TmpSubmssionsPath = Join-path $TmpPath "submissions"
$TmpDocxPath = Join-path $TmpPath "docx"

New-Item -ItemType Directory -Force -Path $OutputPath, $TmpPath, $TmpSubmssionsPath | Out-Null

$OutputFilePath = Join-Path $OutputPath "result.html"

$HTML_HEAD | Set-Content $OutputFilePath

$zippedSubmssions = Get-ChildItem -Path $InputPath -Filter *.zip

foreach ($submission in $zippedSubmssions) {
    $unzippedSubmissionPath = $(Join-Path $TmpSubmssionsPath $submission.BaseName)
    
    Expand-Archive -Force $submission $unzippedSubmissionPath | Out-Null
    $docxDocumentPaths = GetDocxPaths $unzippedSubmissionPath $TmpDocxPath
    WriteSingleSubmission $docxDocumentPaths $OutputFilePath
}

$HTML_END | Add-Content $OutputFilePath

Remove-Item $TmpPath -Recurse
