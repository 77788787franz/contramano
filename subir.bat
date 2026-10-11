@echo off
cd /d "%~dp0"
title Subir a GitHub
color 0A

:: ========================================
:: subir todo mi archivo gihub luego subir

:: cd /d "C:\xampp\htdocs\contramano" && "C:\Program Files\Git\bin\git.exe" remote set-url origin https://github.com/77788787franz/contramano.git && "C:\Program Files\Git\bin\git.exe" add . && "C:\Program Files\Git\bin\git.exe" commit -m "Subida final" --no-verify && "C:\Program Files\Git\bin\git.exe" push -u origin main --force


:: ========================================
set "GITHUB_USER=77788787franz"
set "REPO_NOMBRE=contramano"

:: Si tu repositorio existente tiene otro nombre, cámbialo aquí, por ejemplo:
:: set "REPO_NOMBRE=reproducto_mp3"

echo ========================================
echo   Subiendo a GitHub
echo   Usuario: %GITHUB_USER%
echo   Repositorio: %REPO_NOMBRE%
echo   contramano: %cd%
echo ========================================
echo.

:: Configurar credenciales (una sola vez)
git config --global credential.helper wincred
git config --global credential.helper store

:: Verificar si ya es un repositorio git
if exist ".git" (
    echo ✅ Repositorio git detectado
    echo 📦 Agregando archivos...
    git add .
    echo 📝 Creando commit...
    git commit -m "Actualizacion %date% %time%"
    echo 🚀 Subiendo a GitHub...
    git push origin main
    if %errorlevel% neq 0 (
        echo ❌ Error al subir. Verifica que el repositorio existe en GitHub.
        echo    Crea el repositorio %REPO_NOMBRE% en https://github.com/%GITHUB_USER%/%REPO_NOMBRE%
        pause
        exit /b %errorlevel%
    )
    echo ✅ Subida exitosa
) else (
    echo ❌ No se encontró repositorio git
    echo 🚀 Inicializando repositorio...
    git init
    git add .
    git commit -m "primer commit"
    git branch -M main
    git remote add origin https://github.com/%GITHUB_USER%/%REPO_NOMBRE%.git
    git push -u origin main
    if %errorlevel% neq 0 (
        echo ❌ Error al crear y subir. Verifica que el repositorio %REPO_NOMBRE% existe en GitHub.
        pause
        exit /b %errorlevel%
    )
    echo ✅ Repositorio creado y subido
)

echo.
echo 🌐 https://github.com/%GITHUB_USER%/%REPO_NOMBRE%
echo.
exit /b