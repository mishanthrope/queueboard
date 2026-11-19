# Queueboard — табло черги замовлень

Бекенд системи відображення черги замовлень для шаурмічної (лабораторна робота 1 з курсу архітектури ПЗ).

- **Кухонний екран** — повний склад активних замовлень із модифікаторами та коментарями, дії зі статусами.
- **Табло для відвідувачів** (публічне) — номери замовлень у групах «Готується» (з орієнтовним часом очікування) та «Готово».
- Замовлення надходять після оплати з POS-системи Poster. Вебхук `POST /api/webhooks/poster` — лише сигнал (номер чека і дія), даних замовлення в ньому немає; склад чека система забирає через Poster API (порт `PosterClient`, адаптер `PosterClientAdapter`). У навчальній версії API Poster замінює `MockPosterApi`, а вебхуки надсилає скрипт `scripts/poster-mock.sh`.

Специфікація: [`docs/use-cases.md`](docs/use-cases.md) (юзкейси) та [`docs/api.md`](docs/api.md) (REST API).

## Стек

Java 21, Spring Boot 4 (Web MVC, Data JPA, Validation, Security), PostgreSQL, Liquibase, JWT (jjwt), MapStruct, Lombok, Checkstyle.

## Вимоги

- Java 21
- Docker (для PostgreSQL через docker-compose)
- `curl` (для швидкої перевірки і мок-скрипта Poster)

## Запуск

1. Створити файл налаштувань (він у `.gitignore`; значення за замовчуванням підходять для локального запуску):

   ```bash
   cp .env.example .env
   ```

2. Запустити базу даних (docker-compose читає `.env` автоматично):

   ```bash
   docker compose up -d
   ```

3. Запустити застосунок (він також читає `.env`):

   ```bash
   ./mvnw spring-boot:run
   ```

   Застосунок стартує на `http://localhost:8080`, Liquibase створює схему автоматично. Якщо порт 8080 зайнятий, запустіть з іншим портом: `SERVER_PORT=8081 ./mvnw spring-boot:run` (і вкажіть `API_URL` для мок-скрипта).

### Змінні оточення

Змінні беруться зі звичайного оточення або з `.env` (оточення має пріоритет). Усі мають значення за замовчуванням, перелічені в [`.env.example`](.env.example).

| Змінна | За замовчуванням | Опис |
|---|---|---|
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL (його ж публікує docker-compose) |
| `DB_NAME` | `queueboard` | Назва бази (її ж створює docker-compose) |
| `DB_USERNAME` | `queueboard` | Користувач БД |
| `DB_PASSWORD` | `queueboard` | Пароль БД |
| `JWT_SECRET` | dev-значення | Секрет для підпису JWT (щонайменше 32 байти) |
| `JWT_EXPIRATION` | `PT8H` | Час життя токена (ISO-8601 duration) |
| `POSTER_APPLICATION_SECRET` | `local-dev-poster-secret` | Секрет застосунку Poster для перевірки підпису вебхуків |
| `PAID_AT_MAX_SKEW` | `PT2M` | Наскільки `paidAt` може випереджати годинник сервера |
| `ANALYTICS_MODE` | `async` | Спосіб передачі подій аналітиці: `sync` або `async` (див. «Статистика») |
| `API_URL` | `http://localhost:8080` | Адреса застосунку (лише для `scripts/poster-mock.sh`) |

Пароль БД у `docker-compose` застосовується під час першого створення тому; щоб змінити його пізніше, видаліть том: `docker compose down -v`.

### Збірка і перевірка стилю

```bash
./mvnw verify -DskipTests
```

Checkstyle (`config/checkstyle/checkstyle.xml`) запускається у фазі `validate`; будь-яке порушення ламає збірку.

## Швидка перевірка

Усі ендпоінти, коди відповідей і приклади тіл — у [`docs/api.md`](docs/api.md). Мінімальний сценарій:

```bash
API=http://localhost:8080

# реєстрація (201) і вхід (200 {"token":"..."})
curl -i -X POST $API/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"cook@example.com","password":"secret123","fullName":"Іван Кухар"}'
TOKEN=$(curl -s -X POST $API/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"cook@example.com","password":"secret123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# табло — публічне, токен не потрібен; решта ендпоінтів вимагають -H "Authorization: Bearer $TOKEN"
curl -s $API/api/board
```

Усі помилки повертаються у форматі:

```json
{ "status": 409, "error": "Conflict", "message": "Cannot restore order in status ISSUED" }
```

## Час очікування

Кожне замовлення отримує `estimatedReadyAt` за моделлю черги з одним кухарем (опис і алгоритм — у [`docs/use-cases.md`](docs/use-cases.md)). Кнопки «+» / «−» зсувають час усіх активних замовлень: `PATCH /api/orders/prep-time` з тілом `{"deltaMinutes": 5}` (ціле ненульове від −30 до 30) → 204. Параметри моделі задаються в `application.yaml` під `app.orders.prep-time`: `overhead` (7 хв), `wrap-per-item` (3 хв), `grill` (5 хв), `grill-capacity` (5).

## Статистика

`GET /api/stats?date=2026-11-17` (токен потрібен, `date` необов'язковий, за замовчуванням сьогодні) повертає за добу: кількість прийнятих, виданих і скасованих замовлень, середній час приготування, очікування і похибку оцінки (хвилини), кількість дій кухаря (`COOKING_STARTED`, `READY`, `ISSUED`, `CANCELLED`, `RESTORED`) і до 10 найпопулярніших модифікаторів. Детально — у [`docs/api.md`](docs/api.md) (UC-12).

Статистику збирає окремий компонент `analytics`, який отримує події від обробників команд (`application/order/event`). Спосіб доставки перемикається в `application.yaml` або змінною `ANALYTICS_MODE`:

| Налаштування | За замовчуванням | Опис |
|---|---|---|
| `app.analytics.mode` | `async` | `async` — подія йде після коміту команди в окремому потоці, відповідь API не чекає на аналітику; `sync` — аналітика викликається прямо в потоці команди, збій аналітики лише логується |
| `app.analytics.simulated-delay` | `PT0S` | Штучна затримка аналітики (для порівняння режимів) |
| `app.analytics.fail` | `false` | Аналітика кидає виняток перед записом |

```bash
java -jar target/queueboard-0.0.1-SNAPSHOT.jar --app.analytics.mode=sync --app.analytics.simulated-delay=PT0.5S
```

Порівняння режимів з вимірюваннями — у [`docs/analysis/lab4.md`](docs/analysis/lab4.md).

## Мок Poster

Скрипт надсилає N сигнальних вебхуків (`action: changed`, нові випадкові `object_id`) на `POST /api/webhooks/poster`. Кожен вебхук підписаний: `verify` = md5 від `account;object;object_id;action;time;secret` рахується через `md5sum`. Дані чеків (позиції, модифікатори, коментар) віддає `MockPosterApi`: для одного `object_id` завжди той самий чек. Логін і працівник не потрібні. Скрипт завантажує `.env` з кореня проєкту (`POSTER_APPLICATION_SECRET`, `API_URL`; значення з `.env` перекривають змінні оточення, `API_URL` за замовчуванням `http://localhost:8080`). Вебхук приймає лише `Content-Type: application/json`. Для кожного виклику виводиться HTTP-статус.

```bash
scripts/poster-mock.sh                       # 5 замовлень
scripts/poster-mock.sh 3                     # 3 замовлення
scripts/poster-mock.sh 3 --duplicate         # + повторна відправка останнього вебхука (дубля замовлення немає)
scripts/poster-mock.sh 3 --duplicate --removed   # + action "removed" для останнього чека (замовлення стає CANCELLED)
```

Усі відповіді мають бути `200`. Результат видно в `GET /api/orders/kitchen` (активні замовлення) і `GET /api/orders/{id}`. Пауза між вебхуками задається змінною `DELAY_SECONDS` (за замовчуванням 0).

## Тести

```bash
./mvnw test                                                     # усі тести (потрібен Docker для Testcontainers)
./mvnw test -DexcludedGroups=integration                        # без інтеграційних, Docker не потрібен
./mvnw test -Dtest='com.mishanthrope.queueboard.domain.**'      # лише тести домену, Docker не потрібен
./mvnw test -Dtest=ArchitectureTest                             # лише перевірка залежностей між шарами
./mvnw verify                                                   # Checkstyle + усі тести
```

Тести написано на JUnit 5, AssertJ, Mockito, MockMvc, ArchUnit і Testcontainers (`postgres:17-alpine`). Тестові пакети повторюють чотири шари коду: `domain`, `application`, `infrastructure`, `presentation`; `ArchitectureTest`, `AbstractIntegrationTest` і `QueueboardApplicationTests` лежать у кореневому пакеті.

Підхід до тестів відповідає CQS: **команди** (запис) перевіряються юніт-тестами на in-memory фейках репозиторіїв, **запити** (читання) — інтеграційними тестами через HTTP на справжній БД.

### Піраміда тестів

Усього 344 тести (рахунок за звітами surefire):

| Шар | Тестів | Як тестується |
|---|---|---|
| `domain` | 122 | звичайні JUnit/AssertJ без Spring і Mockito; репозиторії замінюють in-memory фейки `InMemoryOrderRepository` і `InMemoryEmployeeRepository` |
| `application` | 100 | обробники команд на тих самих фейках; Mockito лише для `PasswordHasher`, `TokenService`, `PosterClient`, `WebhookSignatureVerifier` і обробників, які треба змусити кинути виняток; фіксований `Clock` |
| `infrastructure` | 44 | адаптери без Spring-контексту: JWT, md5-підпис, розбір даних Poster |
| `presentation` (юніт) | 4 | `GlobalExceptionHandler` через standalone MockMvc |
| інтеграційні | 65 | `@Tag("integration")`, весь застосунок на PostgreSQL у контейнері, HTTP через MockMvc; тут перевіряються запити читання і компонент аналітики в обох режимах (Awaitility для асинхронного) |
| архітектурні | 9 | ArchUnit: правила залежностей між шарами і правила для `analytics` |

Без інтеграційних тестів виконується 279 тестів. Тести домену (122) разом із запуском Maven тривають близько 4,6 с, усі юніт-тести — близько 11 с, `ArchitectureTest` окремо — близько 5,3 с, повний `./mvnw verify` — близько 39 с (по одному вимірюванню). Більша частина цього часу — старт Maven і JVM.

Інтеграційні тести успадковують `AbstractIntegrationTest`: Spring-контекст і контейнер стартують один раз, перед кожним тестом таблиці очищаються (спершу `orders`, потім `employees`) через Spring Data репозиторії `SpringDataOrderRepository` і `SpringDataEmployeeRepository`. Підписані вебхуки Poster для тестів будує `SignedWebhooks` (JSON-тіло і `verify` = md5 із секретом із `PosterProperties`).

### Що покриває кожен клас

**`domain`** (122)

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `OrderStatusTest` | 29 | усі дозволені переходи; кожна заборонена дія в кожному статусі (таблиця статус × дія) кидає `IllegalStatusTransitionError`; `isActive`, `isInQueue`, `isFinished` |
| `OrderTest` | 12 | кожна дія змінює статус і свою часову мітку; `restore` очищає `readyAt`/`cancelledAt` і ставить `startedAt` лише якщо його не було; заборонена дія нічого не змінює; `shiftEstimate` в обидва боки; незмінний список позицій |
| `OrderItemTest` | 14 | назва 1–100 символів, кількість 1–99, модифікатор до 100 (межові значення і тексти помилок); список модифікаторів копіюється незмінно |
| `KitchenSettingsTest` | 10 | порожні, нульові й від'ємні тривалості та місткість < 1 відхиляються |
| `PrepTimeCalculatorTest` | 11 | 1, 2, 5, 6, 10 позицій → 15, 18, 27, 32, 42 хв; інша місткість гриля змінює партії; без позицій — лише `overhead` |
| `OrderFactoryTest` | 22 | відмови `createFromPoster` (id чека, `paidAt` понад допуск, коментар, порожні позиції) і межові прийняті значення; дубль `posterCheckId`; `displayNumber` (порожній день, максимум + 1, межа місцевої півночі); `estimatedReadyAt` (порожня черга, хвіст у минулому, у майбутньому, найпізніший із черги, READY/ISSUED/CANCELLED не враховуються, кількості підсумовуються) |
| `EmailTest` | 13 | нормалізація (пробіли, регістр), порожнє значення, неправильний формат, надто довге значення |
| `EmployeeFactoryTest` | 9 | обрізання імені, межа 100 символів, порожнє ім'я, дубль email у іншому регістрі |
| `OrderRepositoryTest` | 2 | `getById` повертає збережене замовлення, невідомий id → `OrderNotFoundError` |

**`application`** (100): команди

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `OrderStatusCommandsTest` | 26 | `StartCooking`, `MarkReady`, `IssueOrder`, `CancelOrder`, `RestoreOrder`: стан замовлення в репозиторії після команди (статус і мітка), заборонена дія → `IllegalStatusTransitionError` без змін, невідомий id → `OrderNotFoundError`, чужі замовлення не змінюються; кожна команда публікує свою подію з правильними полями (`CollectingOrderEventPublisher`), невдала команда подій не публікує |
| `ShiftPrepTimeHandlerTest` | 4 | зсув `estimatedReadyAt` для NEW і COOKING в обидва боки; READY, ISSUED, CANCELLED не змінюються; порожня черга |
| `CreateOrderFromPosterHandlerTest` | 29 | відмови (id чека, `paidAt`, коментар, порожні позиції, рядок команди з порушенням правил позиції) і межові значення; дубль `posterCheckId`; збережені позиції; `displayNumber`; `estimatedReadyAt` (порожня черга, хвіст у минулому, у майбутньому, друге замовлення після першого); подія `OrderAccepted` з позиціями й часом готовності, дубль і помилка подій не публікують |
| `RegisterEmployeeHandlerTest` | 6 | повертається id нового працівника; нормалізація email і імені; зберігається хеш із `PasswordHasher`; дубль email в іншому регістрі → `DuplicateEmailError`; некоректний email або порожнє ім'я |
| `ProcessPosterWebhookHandlerTest` | 19 | невірний підпис → `InvalidWebhookSignatureError`; інший `object`; `removed` (існуюче, видане, невідоме); уже існуюче замовлення; чек не оплачений; значення чека в команді `CreateOrderFromPoster`; стан репозиторію після вебхука; помилки домену, Poster і БД не ламають відповідь |

**`application`**: запити

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `LoginHandlerTest` | 4 | токен для нормалізованого email; неправильний пароль, невідомий і некоректний email → `InvalidCredentialsError` |
| `GetBoardHandlerTest` | 12 | групування рядків на `preparing` і `ready`; `minutesLeft` округлюється вгору і не буває менше 0 (фейк `BoardReadRepository`) |

**`infrastructure`** (44)

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `JwtTokenServiceTest` | 11 | токен повертає email, прострочений, підроблений і сміттєвий токени → порожньо |
| `Md5WebhookSignatureVerifierTest` | 14 | вірний підпис (з `data` і без), приклад із `docs/api.md`, регістр hex, невірний або відсутній підпис і підписані поля |
| `PosterClientAdapterTest` | 19 | чек не закрито або не оплачено → порожньо; розбір `date_close`, `num`, `modificator_name`, порожнього коментаря; помилкові дані → `PosterDataError` |

**`presentation`** (4 юніт + 43 інтеграційні)

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `GlobalExceptionHandlerTest` | 4 | непризначена `DomainError` і `ApplicationError` → 400, `OrderNotFoundError` → 404, непередбачений виняток → 500 |
| `AuthIntegrationTest` | 8 | реєстрація 201 / 409 / 400, вхід 200 / 401; захищений ресурс без токена, з неправильним і з правильним токеном |
| `PosterWebhookIntegrationTest` | 8 | підписаний вебхук створює замовлення з позиціями; повтор не дублює замовлення; `removed` скасовує, невідомий чек → 200; невірний або відсутній підпис → 401; зламаний JSON → 400; інший `object` ігнорується |
| `OrderIntegrationTest` | 14 | життєвий цикл start → ready → issue, заборонена дія → 409, restore, невідомий id → 404, `DELETE` прибирає замовлення з кухні й табло; історія з фільтром за статусом, пагінацією і помилковими параметрами; `PATCH /api/orders/prep-time`; `PUT` → не підтримується |
| `OrderQueryIntegrationTest` | 7 | `GET /api/orders/kitchen` (усі поля, позиції, склад: NEW/COOKING/READY без ISSUED і CANCELLED); `GET /api/orders/{id}` (`estimatedReadyAt` є в активного і `null` у завершеного); історія з `date` (місцева доба), з `date` + `status`, порядок «нові першими» |
| `BoardIntegrationTest` | 2 | `GET /api/board` без токена (порожнє табло), групування на `preparing` і `ready` |
| `BoardQueryIntegrationTest` | 3 | `minutesLeft` відповідає `estimatedReadyAt`; скасоване не показується; після видачі замовлення зникає з обох груп |
| `QueueboardApplicationTests` | 1 | `contextLoads`: контекст стартує, схема Liquibase проходить валідацію Hibernate |

**`analytics`** (22 інтеграційні, базовий клас `AbstractAnalyticsTest` очищає таблиці `analytics_*` і чекає, поки спорожніє виконавець аналітики)

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `OrderAnalyticsIntegrationTest` | 12 | кожен тип події оновлює свої таблиці; перший `startedAt` зберігається, останній `readyAt` перемагає; повторна подія нічого не змінює (і не перезаписує новіший стан); `GET /api/stats` за сценарієм доби (середні, дії, топ модифікаторів), порожній день, межа доби, дата за замовчуванням, 400 для поганої дати, 401 без токена |
| `AnalyticsSyncModeIntegrationTest` | 5 | `app.analytics.mode=sync`: статистика видна одразу, затримка 300 мс подовжує `POST /ready`, `fail=true` не ламає команду (200, READY, статистика без змін), невдала команда нічого не пише |
| `AnalyticsAsyncModeIntegrationTest` | 5 | типовий режим: статистика з'являється згодом (Awaitility), затримка 1 с не гальмує `POST /ready` (< 500 мс), `fail=true` не впливає на замовлення, невдала команда (409) нічого не пише |

**Архітектура** (9): `ArchitectureTest` перевіряє, що `domain` залежить лише від `java..` і `domain..`, `application` не залежить від `infrastructure` і `presentation`, `presentation` не залежить від `infrastructure` і від доменних репозиторіїв (`OrderRepository`, `EmployeeRepository`), `infrastructure` не залежить від `presentation`; `analytics` залежить лише від Java, Spring, Jakarta, Lombok, slf4j, себе і `application.order.event`; `domain`, `application` і `presentation` не залежать від `analytics`; `analytics.api` використовує лише `infrastructure.event`.
