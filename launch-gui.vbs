' Yerel Ajan - terminalsiz başlatıcı
' Önce derler (gizli), sonra javaw ile GUI'yi açar - hiç konsol görünmez.
Set ws = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

root = fso.GetParentFolderName(WScript.ScriptFullName)
ws.CurrentDirectory = root

' Derleme (pencere gizli)
ws.Run "cmd /c """ & root & "\build-gui.bat""", 0, True

' GUI'yi terminal olmadan başlat
jdk = "C:\Program Files\Android\openjdk\jdk-21.0.8\bin\javaw.exe"
If Not fso.FileExists(jdk) Then jdk = "javaw.exe"
ws.Run """" & jdk & """ -cp out-gui agent.ui.MainGUI", 0, False
