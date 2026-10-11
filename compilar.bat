@echo off
cd /d C:\xampp\htdocs\contramano

echo ============================================
echo Contramano
echo ============================================
echo.

:: Limpiar carpetas anteriores
echo 🧹 Limpiando carpetas anteriores...
rd /s /q android 2>nul
rd /s /q dist 2>nul

:: Crear keystore si no existe
if not exist C:\keystores\contramano.jks (
    echo 🔑 Creando keystore por primera vez...
    keytool -genkeypair -keystore C:\keystores\contramano.jks -storepass pandita -keypass pandita -alias contramano -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Panda Go Pro, OU=Desarrollo, O=Panda Go, L=Tarija, ST=Bermejo, C=BO"
    echo ✅ Keystore creado exitosamente!
) else (
    echo ✅ Keystore ya existe
)

:: Instalar Capacitor Android
echo.
echo 📦 Instalando Capacitor Android...
call npm install @capacitor/android

:: Agregar Android
echo.
echo 📱 Agregando plataforma Android...
call npx cap add android

:: Compilar frontend
echo.
echo 🚀 Compilando frontend...
call npm run build

:: Sincronizar Capacitor
echo.
echo 🔄 Sincronizando Capacitor...
call npx cap sync android

:: Copiar a Android
echo.
echo 📂 Copiando archivos a Android...
call npx cap copy android

:: Configurar archivos de propiedades
echo.
echo ⚙️ Configurando propiedades de compilación...
echo sdk.dir=C:\\Users\\qaz\\AppData\\Local\\Android\\Sdk> android\local.properties
echo storeFile=C:/keystores/contramano.jks> android\keystore.properties
echo storePassword=pandita>> android\keystore.properties
echo keyAlias=contramano>> android\keystore.properties
echo keyPassword=pandita>> android\keystore.properties

:: Compilar APK y AAB firmados
echo.
echo 🏗️ Compilando APK y AAB firmados...
echo Esto puede tomar varios minutos...
cd android
call gradlew.bat assembleRelease bundleRelease -Pandroid.injected.signing.store.file=C:/keystores/contramano.jks -Pandroid.injected.signing.store.password=pandita -Pandroid.injected.signing.key.alias=contramano -Pandroid.injected.signing.key.password=pandita
cd ..

:: Mostrar ubicación de los archivos
echo.
echo ============================================
echo ✅ ¡COMPILACIÓN COMPLETA!
echo ============================================
echo.
echo 📂 APK generado en:
echo    android\app\build\outputs\apk\release\app-release.apk
echo.
echo 📦 AAB generado en:
echo    android\app\build\outputs\bundle\release\app-release.aab
echo.
echo 📋 TAMAÑOS:
for %%i in ("android\app\build\outputs\apk\release\app-release.apk") do echo    APK: %%~zi bytes
for %%i in ("android\app\build\outputs\bundle\release\app-release.aab") do echo    AAB: %%~zi bytes
echo.
echo ============================================
echo 🚀 ¡LISTO PARA SUBIR A GOOGLE PLAY!
echo 📱 Nombre: Panda Go Pro  Reproductor MP3
echo 🆔 Paquete: com.pandagopro.reproductor
echo ============================================
pause