# Сборка и выпуск Xiaomi Buds 5 Overlay

[← Обзор](../README.md) · [Проверки на устройстве](../OBSERVATIONS.md)

Требуются JDK 17, Android SDK Platform 37.0 и Build Tools 36.0.0. Проект использует Android Gradle Plugin 9.4.1 и Gradle Wrapper 9.6.0. Минимальная версия приложения — Android 12 (API 31), целевая — Android 16 (API 36).

## Собрать

```sh
git clone https://github.com/cveloy102/buds-curtain.git
cd buds-curtain
./gradlew :app:assembleDebug :app:lintDebug
```

На Windows используйте `gradlew.bat` вместо `./gradlew`. Укажите SDK через `ANDROID_HOME` или локальный `local.properties` с `sdk.dir`. `local.properties`, SDK, кеши и ключи подписи не коммитятся.

APK появится в `app/build/outputs/apk/debug/app-debug.apk`. Можно открыть корень репозитория в Android Studio и выполнить те же задачи Gradle.

## Проверить разбор Bluetooth-пакетов без телефона

```sh
mkdir -p build/checks
javac -d build/checks app/src/main/java/com/budscurtain/FastPairBattery.java app/src/main/java/com/budscurtain/CurtainState.java app/src/main/java/com/budscurtain/BudsDockEvidence.java checks/FastPairBatteryCheck.java checks/CurtainStateCheck.java checks/BudsDockEvidenceCheck.java
java -cp build/checks com.budscurtain.FastPairBatteryCheck
java -cp build/checks com.budscurtain.CurtainStateCheck
java -cp build/checks com.budscurtain.BudsDockEvidenceCheck
```

На Windows перед `javac` создайте каталог командой `New-Item -ItemType Directory -Force build/checks`.

GitHub Actions повторяет эти Java-проверки и собирает debug APK. Проверки на реальных Xiaomi Buds 5 и POCO F7 описаны в [OBSERVATIONS.md](../OBSERVATIONS.md). Изображения карточки на сайте показывают пример заряда и не считывают Bluetooth-данные.

## Выпуск APK

1. Обновите `versionCode` и `versionName` в `app/build.gradle`, текст версии на экране приложения, README и CHANGELOG.
2. Выполните Java-проверки, сборку и проверку на устройстве. Сверьте package name, версию, подпись и SHA-256 готового APK.
3. Создайте тег и GitHub Release, приложите проверенный APK и его SHA-256.

Текущий релиз 0.6.7 — тестовый APK. Android позволяет установить обновление поверх прежней версии только при совпадении подписи. Debug APK, собранный на другой машине или в GitHub Actions, обычно использует другой debug-ключ и может потребовать удаления установленной версии. Для устойчивого канала обновлений нужен отдельный постоянный ключ выпуска; его нельзя хранить в репозитории.

## Публикация сайта

Статический сайт находится в `landing/`. Workflow `.github/workflows/pages.yml` переносит его содержимое в GitHub Pages после изменений в `main`. Для публикации репозиторий должен иметь доступный GitHub Pages и источник **GitHub Actions** в настройках Pages. Сайт не содержит данных пользователя; загрузка APK ведёт на GitHub Releases.

Рендер наушников для сайта совпадает с `app/src/main/res/drawable-nodpi/buds5_graphite.webp`. Скриншоты `landing/assets/stand-dark.jpg` и `stand-light.jpg` сняты непосредственно с исходных стендов `design/theme-stand-dark.html` и `theme-stand-light.html`. Для повторного снимка откройте нужный HTML в браузере и снимите область 370 × 515 пикселей от верхнего левого угла. Это дизайн-стенд с примерными значениями заряда.
