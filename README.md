# 🚀 AppHub

**AppHub** — это Android-приложение-хаб для хранения, просмотра, скачивания и установки пользовательских Android-приложений. Проект разработан как клиент-серверное решение, состоящее из трёх частей: Android-клиента, REST-сервера и встроенной панели администратора.

![Android](https://img.shields.io/badge/Android-10%2B-34A853?logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)

---

## 📋 Содержание

- 📖 **[Документация](https://github.com/Arkash1/AppHub/wiki/%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82%D0%B0%D1%86%D0%B8%D1%8F)** — техническое описание, архитектура, установка и API.
- 👤 **[Руководство пользователя](https://github.com/Arkash1/AppHub/wiki/%D0%A0%D1%83%D0%BA%D0%BE%D0%B2%D0%BE%D0%B4%D1%81%D1%82%D0%B2%D0%BE-%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D1%82%D0%B5%D0%BB%D1%8F)** — как пользоваться приложением и админ-панелью.

---

## ✨ Основные возможности

### Для пользователя
- 📱 **Каталог приложений** — красивый интерфейс с тёмной неоновой темой.
- ⬇️ **Скачивание и установка** APK с визуальным прогрессом и SHA-256 верификацией.
- 🔄 **Автообновление** — приложение само знает, когда вышла новая версия.
- 🔍 **Поиск и фильтрация** по категориям.
- 📴 **Офлайн-режим** — каталог доступен без интернета (кэш Room).

### Для администратора
- 🛠 **Встроенная админ-панель** — открывается прямо в приложении.
- 📝 **CRUD приложений** — добавление, редактирование, удаление.
- 📦 **Загрузка APK** — сервер автоматически парсит packageName, versionCode, minSdk.
- 🔐 **Безопасность** — BCrypt-пароль, rate-limiting, EncryptedSharedPreferences.

---

## 🏗 Архитектура

```
┌──────────────────┐   HTTPS/HTTP   ┌─────────────────────────────┐
│  Android клиент   │ ◄─────────────► │  Сервер Spring Boot 3 (Java) │
│  AppHub (Java)    │                 │  - REST API                  │
│  minSdk 29 (A10)  │                 │  - Хранение APK/иконок       │
│                   │                 │  - PostgreSQL (Flyway)       │
│  - Каталог        │                 │  - Админ-эндпоинты           │
│  - Скачивание     │                 └─────────────────────────────┘
│  - Админ-панель   │
└──────────────────┘
```

**Подробно:** [Документация → Архитектура](https://github.com/snoochx/apphub/wiki/Документация#-архитектура)

---

## 🛠 Технологический стек

| Компонент | Технология |
|-----------|------------|
| **Сервер** | Spring Boot 3.2.5, Java 17, PostgreSQL, Flyway, BCrypt, springdoc-openapi |
| **Клиент** | AndroidX, Material Components, Retrofit 2, OkHttp 4, Room, Glide, WorkManager, Java 17 |
| **Доступ** | LAN (Wi-Fi) — бесплатно / Cloudflare Tunnel — постоянный домен |

---

## 🚀 Быстрый старт

### 1. Сервер (15 минут)
```powershell
# 1. Создать БД в PostgreSQL
CREATE USER apphub WITH PASSWORD 'AppHub_Strong_2026!';
CREATE DATABASE apphub OWNER apphub ENCODING 'UTF8';

# 2. Собрать и запустить (нужен JDK 17)
cd server\apphub-server
.\gradlew.bat bootJar
java -jar build\libs\apphub-server.jar --spring.profiles.active=dev
```
📖 Подробно: [Документация → Установка сервера](https://github.com/snoochx/apphub/wiki/Документация#-установка-сервера)

### 2. Android-клиент (10 минут)
```powershell
# Указать IP компьютера (узнать через ipconfig)
# В android/apphub-client/app/build.gradle → DEFAULT_SERVER_URL

cd android\apphub-client
.\gradlew.bat assembleDebug
# APK: app\build\outputs\apk\debug\app-debug.apk
```
📖 Подробно: [Документация → Сборка Android-клиента](https://github.com/snoochx/apphub/wiki/Документация#-сборка-android-клиента)

### 3. Доступ с телефона
- **Дома:** телефон и ПК в одной Wi-Fi сети → приложение подключается автоматически.
- **С мобильного интернета:** запустить [бесплатный туннель TryCloudflare](https://github.com/snoochx/apphub/wiki/Документация#-доступ-с-телефона).

---

## 📂 Структура проекта

```
AppHub/
├── 📁 server/apphub-server/     ← Spring Boot backend
├── 📁 android/apphub-client/    ← Android client
├── 📁 scripts/                  ← PowerShell (туннель, бэкап, службы)
└── 📁 docs/                     ← Спецификация + UML-диаграммы
```

---

## 📜 Лицензия

Проект разработан как квалификационная работа. © Arkash's Studio.
