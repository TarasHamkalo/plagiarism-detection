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
    $submitter = Split-Path -Leaf $(Split-Path -Path $metadata.FilePath -Parent)

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
    
    foreach ($docxPath in $docxDocumentPaths) {

        if ([string]::IsNullOrWhiteSpace($docxPath)) {
            continue  
        }
        
        try {
            $metadata = ReadMetadata $docxPath
            AppendHtmlRow $metadata $OutputFilePath
        } catch {
            Write-Error "Error writing $($docxPath)"
        }
    }
}

$InputDir = "test_files"

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

# Get-ChildItem -Path $InputPath -Filter "*.docx" | ForEach-Object {
#     $SourceFile = $_.FullName
#     $TargetFile = Join-Path -Path $TmpPath -ChildPath ($_.BaseName + ".zip")
#     Copy-Item -Path $SourceFile -Destination $TargetFile
#     Write-Output "Copied and renamed: $SourceFile -> $TargetFile"
    
# }



# submission/[doc, docx] ->  {
#     doc -> docx -> meta to output
#     docx ->}


# | ForEach-Object {
#     $_.FullName
# }
# New-Item -ItemType Directory -Force -Path $OutputPath, $TmpPath | Out-Null
# Copy-Item $InputPath/*.docx to $TmpPath/*.zip
# Copy-Item "$InputPath/*.docx" $TmpPath
# Expand-Archive -Path $InputDir "*.docx" 
# Get-ChildItem -Path $InputDir -Filter *.docx 
# New-Item -Path $OutputDirName -ItemType Directory -Force | Out-Null
# New-Item -Path "$OutputDirName/$TmpDirName" -ItemType Directory -Force | Out-Null


# Expand-Archive "$INPUT_PATH/2013_2014_Angelovic_Kamil_296_11_dokumentacia_.zip" $OUTPUT_PATH/
# Copy-Item "$INPUT_PATH/*.zip"


# Copy-Item "$INPUT_PATH/*.docx" "$OUTPUT_PATH/*.zip"

# Expand-Archive -Path "C:\Downloads\Install.zip" -DestinationPath "C:\Downloads"


# $appProps = Get-Content "./docProps/app.xml"
# $coreProps = Get-Content "./docProps/core.xml"
# $xmlFileMinusFormatData = $xmlFile[1..($xmlFile.Length - 1)] #we need to remove the first line
# $usableXml = [xml]$xmlFileMinusFormatData # Convert to xml for easy handling
# $usableXml.Courses.CourseEntry.Count # just a count of rows
