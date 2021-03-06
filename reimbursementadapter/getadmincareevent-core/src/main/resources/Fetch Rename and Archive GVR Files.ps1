## Script for fetching renaming and archiving GVR files.
# Lists all the XML files in the first parameter directory
#
## Arguments
# 1: Full path to the source directory (ending backslash optional)
# 2: Full path to the target directory (ending backslash optional)
# 3: Full path to the archive directory (ending backslash optional)

# Count the parameters
if ($args.Length -ne 3) {
    Write-Host "Invalid number of parameters!"
    Write-Host "Usage: powershell.exe <file>.ps1 c:\source c:\target c:\archive"
    exit 1
}

# Setup the source path and check that it exists
$sourcePath = $args[0].TrimEnd("\")
if (!(Test-Path $sourcePath)) {
    Write-Host "Error: the source path '$($sourcePath)' does not exist."
    exit 1
}

# Setup the target path and check that it exists
$targetPath = $args[1].TrimEnd("\")
if (!(Test-Path $targetPath)) {
    Write-Host "Error: the target path '$($targetPath)' does not exist."
    exit 1
}

# Setup the archive path and check that it exists
$archivePath = $args[2].TrimEnd("\")
if (!(Test-Path $targetPath)) {
    Write-Host "Error: the archive path '$($archivePath)' does not exist."
    exit 1
}

# Renames the incoming file and adds the last modified timestamp to the name.
# The resulting filename will be "ERSMO_<timestamp>.xml".
# Adapted from http://powershell.com/cs/media/p/23848.aspx
function Add-TimeStamp ($Path) { 
	$Vars = Get-ItemProperty $Path | select Directory,Name,LastWriteTime 
	$Directory = $([string]($vars | select -ExpandProperty Directory)+'\') 
	$BaseName = ($vars | select -ExpandProperty Name) 
	$Time = ($vars | select -ExpandProperty LastWriteTime) 
	$TimeStamp = (($Time | Get-Date -Format yyy-MM-ddTHHmmsszzz).Replace(":", ""))
	$NewName = "ERSMO_$($TimeStamp).xml"
    $Path1 = $([string]($Directory+$BaseName))
	Rename-Item -Path $Path1 -NewName $NewName
    return $NewName
}

# Iterate over all the XML files in the source path, timestamps them, copy the file
# to the archive directory and finally move it to the target tirectory.
$count = 0
Get-ChildItem "$($sourcePath)\\*.xml" |
   ForEach-Object {
      # Rename the file and add a timestamp to the filename.
      $NewFileName = Add-Timestamp($_)
      $NewFilePath = $([string]::Concat($_.DirectoryName, "\", $NewFileName))
      Copy-Item $NewFilePath "$($archivePath)\\$($NewFileName)";
      Move-Item $NewFilePath "$($targetPath)\\$($NewFileName)";
      $count++
   }


Write-Host "Finished with no errors. Successfully processed $($count) files."