# Gradle Wrapper

O NEXA usa Gradle Wrapper para tornar os builds reproduzíveis.

## Estado atual

O ambiente usado para preparar esta etapa não possui uma instalação do Gradle nem acesso de rede suficiente para gerar o `gradle-wrapper.jar`. Por isso, **o Wrapper é o único item pendente do 1.2**.

Isso é um bloqueio real, não uma falha escondida.

## Geração

Em uma máquina de desenvolvimento com Gradle instalado:

### Windows PowerShell

```powershell
.\scripts\bootstrap-gradle-wrapper.ps1
```

O script gera o Wrapper para `android/` e `backend/`, ambos fixados em Gradle `9.5.0`.

Depois, os arquivos abaixo devem ser commitados para cada projeto:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

O Gradle recomenda versionar esses arquivos, incluindo o JAR do Wrapper. A documentação oficial também recomenda usar o Wrapper em vez de depender do Gradle instalado na máquina. citeturn160169search0turn160169search2

## Validação esperada

```powershell
cd android
.\gradlew.bat --version

cd ..\backend
.\gradlew.bat --version
```

Ambos devem reportar Gradle `9.5.0`.
