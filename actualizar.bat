@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d C:\xampp\htdocs\contramano

:: --- JAVA_HOME fijo para este build, no depende del entorno global ---
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" goto ERROR_JAVA_HOME

:: ------------------------------------------------
:: CONFIGURACION FIJA
:: ------------------------------------------------
set MODO=release
set SOLO_JAVA=0

:: ------------------------------------------------
:: UNICA PREGUNTA: generar tambien el .aab?
:: ------------------------------------------------
set HACER_AAB=0
if /I "%MODO%"=="release" (
  powershell -NoProfile -Command "Add-Type -AssemblyName PresentationFramework; $r=[System.Windows.MessageBox]::Show('Deseas generar tambien el .aab?','PANDA GO PRO','YesNo','Question'); if($r -eq 'Yes'){exit 0}else{exit 1}"
  if not errorlevel 1 set HACER_AAB=1
)

if /I "%MODO%"=="release" (
  set GRADLE_TASK=assembleRelease
  set APK=android\app\build\outputs\apk\release\app-release.apk
  set APK_BACKUP=android\app\build\outputs\apk\release\app-release-BACKUP.apk
) else (
  set GRADLE_TASK=assembleDebug
  set APK=android\app\build\outputs\apk\debug\app-debug.apk
  set APK_BACKUP=android\app\build\outputs\apk\debug\app-debug-BACKUP.apk
)
set AAB=android\app\build\outputs\bundle\release\app-release.aab
set JAVAC_DIR=android\app\build\intermediates\javac\%MODO%

echo ============================================
echo PANDA GO PRO - ACTUALIZACION RAPIDA
echo ============================================
echo   Modo: %MODO%
if "%SOLO_JAVA%"=="1" (echo   Solo Java: se salta vite y cap sync) else (echo   Se compila el frontend tambien)
if "%HACER_AAB%"=="1" (echo   Se generara tambien el AAB) else (echo   Sin AAB)
echo.

:: ------------------------------------------------
:: 0) VERIFICAR LOS ARCHIVOS JAVA
:: ------------------------------------------------
set JAVA_DIR=android\app\src\main\java\com\reproductor\mp3
set JAVA_SRC=%JAVA_DIR%\MusicPlayerPlugin.java

if not exist "%JAVA_SRC%" goto ERROR_JAVA_MISSING

findstr /C:"descartadas" "%JAVA_SRC%" >nul
if errorlevel 1 goto ERROR_JAVA_SRC
echo [0/5] MusicPlayerPlugin.java OK.
echo.

:: ------------------------------------------------
:: 1) LIMPIEZA
:: ------------------------------------------------
echo [1/5] Sin limpieza (Gradle recompila solo lo modificado).
echo.

:: ------------------------------------------------
:: 2) VERSION
:: ------------------------------------------------
set BUILD_FILE=android\app\build.gradle
if not exist "%BUILD_FILE%" goto ERROR_BUILD_FILE

set CURRENT_CODE=
set CURRENT_NAME=
set NEW_NAME=
set NEW_CODE=

if /I not "%MODO%"=="release" (
  echo [2/5] Modo debug: version sin cambios.
  echo.
  goto AFTER_VERSION
)

for /f "usebackq delims=" %%a in (`powershell -NoProfile -Command "$t=Get-Content '%BUILD_FILE%' -Raw; if($t -match 'versionCode\s+(\d+)'){Write-Output $matches[1]}"`) do set CURRENT_CODE=%%a
for /f "usebackq delims=" %%a in (`powershell -NoProfile -Command "$t=Get-Content '%BUILD_FILE%' -Raw; if($t -match 'versionName\s+[^0-9]*([0-9]+\.[0-9]+\.[0-9]+)'){Write-Output $matches[1]}"`) do set CURRENT_NAME=%%a

if "%CURRENT_CODE%"=="" set CURRENT_CODE=0
if "%CURRENT_NAME%"=="" set CURRENT_NAME=1.0.0

echo [2/5] Version actual: %CURRENT_NAME% - Codigo: %CURRENT_CODE%

for /f "usebackq delims=" %%a in (`powershell -NoProfile -Command "$v='%CURRENT_NAME%'; $p=$v.Split('.'); $ma=[int]$p[0]; $mi=[int]$p[1]; $pa=[int]$p[2]; $pa++; if($pa -ge 100){$pa=0; $mi++; if($mi -ge 100){$mi=0; $ma++}}; Write-Output ($ma.ToString()+'.'+$mi.ToString()+'.'+$pa.ToString())"`) do set NEW_NAME=%%a

set /a NEW_CODE=%CURRENT_CODE%+1
echo      Nueva version: %NEW_NAME% - Codigo: %NEW_CODE%

set EDIT_TRIES=0

:EDIT_LOOP
set /a EDIT_TRIES+=1
if %EDIT_TRIES% GTR 5 goto ERROR_EDIT

if exist "%BUILD_FILE%.backup" del /q "%BUILD_FILE%.backup"
copy /Y "%BUILD_FILE%" "%BUILD_FILE%.backup" >nul 2>&1

powershell -NoProfile -Command "$f='%BUILD_FILE%'; $t=Get-Content $f -Raw; $q=[char]34; $t=$t -replace 'versionCode\s+\d+',('versionCode %NEW_CODE%'); $t=$t -replace 'versionName\s+[^\r\n]+',('versionName '+$q+'%NEW_NAME%'+$q); Set-Content $f $t -NoNewline" >nul 2>&1

if not errorlevel 1 goto EDIT_OK

echo      Intento %EDIT_TRIES%: archivo bloqueado, esperando 3s...
timeout /t 3 /nobreak >nul
goto EDIT_LOOP

:EDIT_OK
echo build.gradle actualizado: %NEW_NAME% - Codigo: %NEW_CODE%
echo.

:AFTER_VERSION

:: ------------------------------------------------
:: 3) COMPILAR FRONTEND
:: ------------------------------------------------
if "%SOLO_JAVA%"=="1" (
  echo [3/5] Frontend omitido: solo Java.
  echo.
  goto AFTER_FRONTEND
)

echo [3/5] Compilando frontend...

set CI=true
set NODE_ENV=production
set CAP_BUILD=1

call npx vite build
if errorlevel 1 goto ERROR_VITE

echo.
echo Limpiando .gz/.br de dist antes de copiar a Android...
if exist dist\ (
  del /s /q dist\*.gz  >nul 2>&1
  del /s /q dist\*.br  >nul 2>&1
)

echo Limpiando carpeta public de Android antes de copiar...
if exist android\app\src\main\assets\public (
  rmdir /s /q android\app\src\main\assets\public
)

echo.
echo Sincronizando Capacitor...
call npx cap sync android
if errorlevel 1 goto ERROR_CAP
echo.

:AFTER_FRONTEND

:: ------------------------------------------------
:: 4) GENERAR APK
:: ------------------------------------------------
echo [4/5] Generando APK %MODO%...

set KEYSTORE=C:/keystores/contramano.jks
set KS_PASS=pandita
set KEY_ALIAS=contramano
set KEY_PASS=pandita

set SIGN_ARGS=
if /I "%MODO%"=="release" set SIGN_ARGS=-Pandroid.injected.signing.store.file=%KEYSTORE% -Pandroid.injected.signing.store.password=%KS_PASS% -Pandroid.injected.signing.key.alias=%KEY_ALIAS% -Pandroid.injected.signing.key.password=%KEY_PASS%

if not exist "%APK%" goto NO_BACKUP

echo      Haciendo backup del APK anterior...
if exist "%APK_BACKUP%" del /q "%APK_BACKUP%"
copy /Y "%APK%" "%APK_BACKUP%" >nul 2>&1
echo      Backup guardado en: %APK_BACKUP%
goto AFTER_BACKUP

:NO_BACKUP
echo      No habia APK anterior, no se hace backup.

:AFTER_BACKUP

for /f "usebackq delims=" %%a in (`powershell -NoProfile -Command "Get-Date -Format s"`) do set BUILD_START=%%a

cd android

set GRADLE_TRIES=0

:GRADLE_LOOP
set /a GRADLE_TRIES+=1
if %GRADLE_TRIES% GTR 3 goto GRADLE_FAIL

if %GRADLE_TRIES% LEQ 1 goto GRADLE_RUN

echo.
echo      Reintento %GRADLE_TRIES% de 3
echo      Deteniendo Gradle y limpiando dex...
call gradlew.bat --stop >nul 2>&1
taskkill /F /IM java.exe >nul 2>&1
taskkill /F /IM javaw.exe >nul 2>&1
timeout /t 3 /nobreak >nul
if exist "app\build\intermediates\dex" rmdir /s /q "app\build\intermediates\dex"

:GRADLE_RUN

call gradlew.bat %GRADLE_TASK% ^
    --build-cache ^
    --parallel ^
    --daemon ^
    --configuration-cache ^
    --configuration-cache-problems=warn ^
    -x lintVitalAnalyzeRelease ^
    -x lintVitalReportRelease ^
    -x lintVitalRelease ^
    -x lintRelease ^
    -x lint ^
    -Dorg.gradle.java.home="%JAVA_HOME%" ^
    -Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" ^
    %SIGN_ARGS% < nul

set GRADLE_EXIT=%ERRORLEVEL%

if "%GRADLE_EXIT%"=="0" goto GRADLE_OK

powershell -NoProfile -Command "$s=[datetime]::Parse('%BUILD_START%'); $f='..\%APK%'; if((Test-Path $f) -and ((Get-Item $f).LastWriteTime -gt $s)){exit 0}else{exit 1}"
if errorlevel 1 goto GRADLE_RETRY_MSG
echo.
echo      Gradle dio error de comunicacion con el daemon, pero el APK nuevo SI se genero. Continuo.
goto GRADLE_OK

:GRADLE_RETRY_MSG
echo.
echo      Gradle fallo con exit code %GRADLE_EXIT%. Reintentando...
goto GRADLE_LOOP

:GRADLE_FAIL
echo.
echo ============================================
echo   ERROR: GRADLE FALLO 3 VECES
echo   Sube en esta ventana y lee el primer error
echo   en rojo (suele decir "error:" con el archivo
echo   y la linea del Java que no compila).
echo ============================================
cd ..
if not exist "%APK_BACKUP%" goto SKIP_RESTORE
echo      Restaurando APK anterior desde backup...
copy /Y "%APK_BACKUP%" "%APK%" >nul 2>&1
echo      APK anterior restaurado. NO LO INSTALES: es el viejo.
:SKIP_RESTORE
pause
exit /b 1

:GRADLE_OK
cd ..
if exist "%APK_BACKUP%" del /q "%APK_BACKUP%"

if not exist "%APK%" goto NO_APK

:: ------------------------------------------------
:: VERIFICAR .class COMPILADO
:: ------------------------------------------------
powershell -NoProfile -Command "$f=Get-ChildItem '%JAVAC_DIR%' -Recurse -Filter MusicPlayerPlugin.class -ErrorAction SilentlyContinue | Select-Object -First 1; if($f -and (Select-String -Path $f.FullName -Pattern 'descartadas' -Quiet)){exit 0}else{exit 1}"
if errorlevel 1 goto ERROR_CLASS

goto APK_OK

:NO_APK
echo.
echo ============================================
echo   ERROR: NO SE GENERO EL APK
echo ============================================
pause
exit /b 1

:APK_OK

:: ------------------------------------------------
:: 5) GENERAR AAB
:: ------------------------------------------------
if not "%HACER_AAB%"=="1" goto SHOW_RESULT

echo.
echo [5/5] Generando AAB...
if exist "%AAB%" del /q "%AAB%"

cd android
call gradlew.bat bundleRelease ^
    --build-cache ^
    --parallel ^
    --daemon ^
    -x lintVitalAnalyzeRelease ^
    -x lintVitalReportRelease ^
    -x lintVitalRelease ^
    -x lintRelease ^
    -x lint ^
    -Dorg.gradle.java.home="%JAVA_HOME%" ^
    -Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" ^
    %SIGN_ARGS% < nul
set AAB_EXIT=%ERRORLEVEL%
cd ..

if not "%AAB_EXIT%"=="0" goto ERROR_AAB
if not exist "%AAB%" goto ERROR_AAB

:SHOW_RESULT
echo.
echo ============================================
echo   LISTO! APK %MODO% GENERADO
echo ============================================
if /I "%MODO%"=="release" (
  echo Version anterior: %CURRENT_NAME%
  echo Version nueva:    %NEW_NAME% - Codigo: %NEW_CODE%
)
for %%F in ("%APK%") do echo APK: %%~fF  [%%~zF bytes]  [%%~tF]
if "%HACER_AAB%"=="1" for %%F in ("%AAB%") do echo AAB: %%~fF  [%%~zF bytes]  [%%~tF]
echo ============================================
echo.
pause
exit /b 0

:ERROR_JAVA_HOME
echo.
echo ============================================
echo   ERROR: JAVA_HOME no es valido: %JAVA_HOME%
echo ============================================
pause
exit /b 1

:ERROR_JAVA_MISSING
echo [ERROR] No existe %JAVA_SRC%
pause
exit /b 1

:ERROR_JAVA_SRC
echo.
echo ============================================
echo   ERROR: MusicPlayerPlugin.java NO tiene el
echo   filtro nuevo (falta el texto "descartadas").
echo   Guarda el archivo (Ctrl+S) o pega doScan.
echo ============================================
pause
exit /b 1

:ERROR_CLASS
echo.
echo ============================================
echo   ERROR: el APK se genero pero el Java
echo   compilado NO tiene el filtro nuevo.
echo   No lo instales.
echo   Si persiste, borra una vez la carpeta
echo   android\app\build\intermediates\javac
echo ============================================
pause
exit /b 1

:ERROR_BUILD_FILE
echo [ERROR] No existe %BUILD_FILE%
pause
exit /b 1

:ERROR_EDIT
echo.
echo [ERROR] No se pudo editar build.gradle despues de 5 intentos.
pause
exit /b 1

:ERROR_AAB
echo.
echo ============================================
echo   ERROR: el APK se genero bien pero el AAB FALLO
echo ============================================
pause
exit /b 1

:ERROR_VITE
echo ERROR: vite build FALLO
pause
exit /b 1

:ERROR_CAP
echo ERROR: npx cap sync android FALLO
pause
exit /b 1