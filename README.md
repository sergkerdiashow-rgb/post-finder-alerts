# Post Finder Alerts

**Post Finder Alerts** — Android-компаньон для плагина **Post Finder WB**. Приложение получает структурированные сигналы от плагина, показывает отдельные push-уведомления и сохраняет историю находок независимо от разрешения уведомлений самого exteraGram.

> Текущая версия: **2.0.1** (`versionCode 8`)

## Возможности

- real-time уведомления от **Post Finder WB**;
- отдельные Android push-уведомления;
- Deal Score **0–100**;
- Watchlist и приоритетные сделки;
- цена предложения и эффективная цена с WB-пошлиной;
- история цен и редкость текущей цены;
- расчёт ожидаемой прибыли;
- локальная история находок;
- избранное и прочитано/непрочитано;
- статусы **Куплено** / **Пропущено**;
- личные заметки;
- поиск, фильтры и сортировка по score, прибыли, цене и задержке;
- аналитика;
- диагностика связи с Post Finder WB;
- экспорт/импорт истории;
- полная резервная копия настроек и истории;
- встроенная проверка обновлений.

## Связь с Post Finder WB

Плагин отправляет explicit Android broadcast:

`com.postfinder.alerts.SHOW_ALERT`

Приложение принимает его через `AlertReceiverV20`, сохраняет событие в локальную историю и затем показывает уведомление.

Диагностика показывает время последнего сигнала, количество сигналов, версию плагина, товар, источник, цену, эффективную цену и время обработки.

## Установка

Стабильные APK публикуются в разделе **Releases**.

1. Открой последний Release.
2. Скачай `PostFinderAlerts.apk`.
3. Установи APK поверх предыдущей версии.

Package ID остаётся неизменным:

`com.postfinder.alerts`

## Обновления

Канал обновлений хранится в `latest.json`. Готовый APK берётся из последнего GitHub Release:

`releases/latest/download/PostFinderAlerts.apk`

Начиная с **2.0.1**, приложение использует новое имя репозитория `sergkerdiashow-rgb/post-finder-alerts` напрямую.

## Android

- Package: `com.postfinder.alerts`
- minSdk: **23**
- targetSdk: **35**
- compileSdk: **35**
- Java: **17**
- UI: **Liquid Glass / Intelligence Center 2.0**

## Структура проекта

- `app/src/main/java/com/postfinder/alerts/` — код приложения;
- `app/src/main/res/` — ресурсы и иконки;
- `app/build.gradle` — Android-конфигурация;
- `latest.json` — канал обновлений;
- `.github/workflows/build-apk.yml` — CI и публикация Releases.

Ветка `post-finder-alerts` содержит только проект Post Finder Alerts. Старые исходники Retro Platformer из неё удалены.

## Post Finder WB

Плагин **Post Finder WB** хранится отдельно в репозитории `post-finder-wb`, чтобы версии плагина и Android-приложения не смешивались.

## Releases

Формат релизов:

- tag: `alerts-v2.0.1`
- title: `Post Finder Alerts 2.0.1`
- asset: `PostFinderAlerts.apk`

SHA-256 APK публикуется в описании Release.
