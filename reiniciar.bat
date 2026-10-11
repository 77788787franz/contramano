@echo off
:loop
npm run dev
echo El proceso terminó. Reiniciando en 5 segundos...
timeout /t 5 /nobreak
goto loop