$assetsPath = "c:\Users\A. Charan Teja\AndroidStudioProjects\GateMaster\app\src\main\assets"
$courses = @()

# Folder mapping for nice titles
$titleMap = @{
    "algo" = "Algorithms"
    "aptitude" = "Aptitude"
    "cao" = "Computer Architecture"
    "cd" = "Compiler Design"
    "dbms" = "Database Management"
    "dl" = "Digital Logic"
    "ds" = "Data Structures"
    "os" = "Operating Systems"
    "previousPapers" = "Previous Papers"
    "shortnotes" = "Short Notes"
    "toc" = "Theory of Computation"
}

$folders = Get-ChildItem -Path $assetsPath -Directory | Where-Object { $_.Name -notmatch "pdfs|testseries" }

foreach ($folder in $folders) {
    $courseTitle = $titleMap[$folder.Name]
    if (-not $courseTitle) {
        $courseTitle = (Get-Culture).TextInfo.ToTitleCase($folder.Name.ToLower())
    }
    
    $topics = @()
    
    $files = Get-ChildItem -Path $folder.FullName -Filter "*.html"
    foreach ($file in $files) {
        $topicName = $file.BaseName
        $topicName = (Get-Culture).TextInfo.ToTitleCase($topicName.ToLower())
        
        $topics += @{
            id = $file.BaseName
            title = $topicName
            contentPath = ($folder.Name + "/" + $file.Name)
            pdfPath = "" 
        }
    }
    
    $courses += @{
        id = $folder.Name
        title = $courseTitle
        topics = $topics
    }
}

$wrapper = @{
    courses = $courses
}

$wrapper | ConvertTo-Json -Depth 4 | Out-File "$assetsPath\courses.json" -Encoding UTF8
Write-Host "JSON generated successfully."
