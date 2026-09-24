@echo off
REM Personal Apps - cadastra a chave de debug deste PC como segredo do GitHub Actions,
REM para os APKs gerados no GitHub instalarem por cima dos que ja estao no celular.
REM Precisa do GitHub CLI (gh) logado - o CONFIGURAR-PC.bat do ENEL-Apps ja fez isso.
set "KS=%USERPROFILE%\.android\debug.keystore"
if not exist "%KS%" (
  echo [ERRO] Nao achei %KS%
  pause
  exit /b 1
)
powershell -NoProfile -Command "gh secret set DEBUG_KEYSTORE_BASE64 --repo MihawkRJ/Personal-Apps --body ([Convert]::ToBase64String([IO.File]::ReadAllBytes($env:KS)))"
if errorlevel 1 ( echo [ERRO] Falhou. Rode "gh auth login" e tente de novo. ) else ( echo Pronto! Segredo cadastrado. )
pause
