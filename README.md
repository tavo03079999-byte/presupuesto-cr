name: Compilar APK Android

on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build:
    name: Compilar APK en la Nube
    runs-on: ubuntu-latest

    steps:
      - name: Checkout del codigo
        uses: actions/checkout@v4

      - name: Configurar Java 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'

      - name: Configurar Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Asegurar Gradle Wrapper
        run: |
          gradle wrapper --gradle-version 8.6 || true
          chmod +x gradlew || true

      - name: Compilar APK Debug
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Subir APK como Descarga
        uses: actions/upload-artifact@v4
        with:
          name: PresupuestoCR-APK
          path: app/build/outputs/apk/debug/app-debug.apk

      - name: Publicar Release para Celular
        uses: softprops/action-gh-release@v2
        if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master'
        with:
          tag_name: v1.0.${{ github.run_number }}
          name: Presupuesto Inteligente CR v1.0.${{ github.run_number }}
          body: |
            Descarga el archivo adjunto app-debug.apk en tu celular Android para instalarlo.
          files: app/build/outputs/apk/debug/app-debug.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
