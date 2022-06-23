if /i "%*" EQU "--help" (
  echo ARCHIVE           Packs source files into a .jar archive.
  exit /b 0
)
"%JDKBin%\jar.exe" -c -M -f "%workspace%\!name!-!projectVersion!-sources.jar" -C "%src%" .
"%JDKBin%\jar.exe" -c -M -f "%workspace%\!name!-!projectVersion!.jar" -C "%classes%" .
exit /b %ERRORLEVEL%