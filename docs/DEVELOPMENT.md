# Сборка Buds Curtain

Откройте корень проекта в Android Studio и выполните `assembleDebug`. Проект использует AGP 9.4.1, SDK 37.0 и Java 17. APK создаётся в `app/build/outputs/apk/debug/app-debug.apk`.

Минимальная версия Android — 12 (API 31), целевая — 16 (API 36). Реальная совместимость проверена на POCO F7 и Xiaomi Buds 5.

Для проверки разбора сигналов без телефона:

```powershell
javac -d build/checks app/src/main/java/com/budscurtain/FastPairBattery.java app/src/main/java/com/budscurtain/CurtainState.java app/src/main/java/com/budscurtain/BudsDockEvidence.java checks/FastPairBatteryCheck.java checks/CurtainStateCheck.java checks/BudsDockEvidenceCheck.java
java -cp build/checks com.budscurtain.FastPairBatteryCheck
java -cp build/checks com.budscurtain.CurtainStateCheck
java -cp build/checks com.budscurtain.BudsDockEvidenceCheck
```

Сценарии на устройстве и наблюдения: [OBSERVATIONS](../OBSERVATIONS.md). Макеты интерфейса: [DESIGN](../DESIGN.md). Ключи подписи и локальные пути SDK храните отдельно от Git.
