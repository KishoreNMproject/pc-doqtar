#define MyAppName "PC DOQTAR"
#ifndef MyAppVersion
  #define MyAppVersion "1.1.4"
#endif

; BaseDir defaults to the repo root (one level up from packaging/)
; CI can override: ISCC.exe /DBaseDir=C:\path\to\repo packaging\PCDOQTAR-Setup.iss
#ifndef BaseDir
  #define BaseDir ".."
#endif

[Setup]
AppId={{7B1DBF30-22C9-4A56-83D6-1C92A5D142A1}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
DefaultDirName={autopf}\PC DOQTAR
DefaultGroupName=PC DOQTAR
OutputDir={#BaseDir}\dist
OutputBaseFilename=PCDOQTAR-Setup
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64compatible
WizardStyle=modern

; --- ICON CONFIGURATION ---
SetupIconFile={#BaseDir}\src\main\resources\org\pcdoqtar\app\pc-doqtar.ico
UninstallDisplayIcon={app}\GUI\PCDOQTAR.exe

[Files]
Source: "{#BaseDir}\src\main\resources\org\pcdoqtar\app\pc-doqtar.ico"; DestDir: "{app}\GUI"; Flags: ignoreversion
Source: "{#BaseDir}\dist\gui\PCDOQTAR\*"; DestDir: "{app}\GUI"; Flags: recursesubdirs createallsubdirs
Source: "{#BaseDir}\dist\PCDOQTAR-Service\*"; DestDir: "{app}\Service"; Flags: recursesubdirs createallsubdirs

[Icons]
; Start Menu Shortcut
Name: "{group}\PC DOQTAR"; Filename: "{app}\GUI\PCDOQTAR.exe"; IconFilename: "{app}\GUI\pc-doqtar.ico"
; Desktop Shortcut
Name: "{autodesktop}\PC DOQTAR"; Filename: "{app}\GUI\PCDOQTAR.exe"; IconFilename: "{app}\GUI\pc-doqtar.ico"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional icons:"

[Registry]
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "PCDOQTAR_GUI"; ValueData: """{app}\gui\PCDOQTAR.exe"""; Flags: uninsdeletevalue

[Run]
Filename: "{app}\Service\PCDOQTARService.exe"; Parameters: "install"; Flags: runhidden waituntilterminated; StatusMsg: "Registering PC DOQTAR diagnostic service..."
Filename: "{app}\Service\PCDOQTARService.exe"; Parameters: "start"; Flags: runhidden waituntilterminated; StatusMsg: "Starting PC DOQTAR diagnostic service..."
Filename: "{app}\GUI\PCDOQTAR.exe"; Description: "Launch PC DOQTAR"; Flags: postinstall nowait skipifsilent

[UninstallRun]
Filename: "{app}\Service\PCDOQTARService.exe"; Parameters: "stop"; Flags: runhidden waituntilterminated; RunOnceId: "StopPCDOQTARService"
Filename: "{app}\Service\PCDOQTARService.exe"; Parameters: "uninstall"; Flags: runhidden waituntilterminated; RunOnceId: "UninstallPCDOQTARService"
