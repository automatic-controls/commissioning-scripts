if /i "%*" EQU "--help" (
  echo PACK              Packages all relevant files into newly created .addon and .jar archives.
  exit /b 0
)
setlocal
  set "override=0"
  call "%callback%" --goto pack
  set "err=%ErrorLevel%"
endlocal & set "err=%err%"
echo Archiving...
set "sourceJar=%workspace%\!name!-!projectVersion!-sources.jar"
set "classJar=%workspace%\!name!-!projectVersion!.jar"
del /F "%workspace%\*.jar" >nul 2>nul
set "err2=0"
"%JDKBin%\jar.exe" -c -M -f "%sourceJar%" -C "%src%" .
if %ErrorLevel% NEQ 0 set "err2=1"
"%JDKBin%\jar.exe" -c -M -f "%classJar%" -C "%classes%" .
if %ErrorLevel% NEQ 0 set "err2=1"
if "%err2%" EQU "0" (
  echo Archive successful.
) else (
  echo Archive unsuccessful.
  exit /b %err2%
)
exit /b %err%