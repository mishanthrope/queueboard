# Queueboard — табло черги замовлень

Бекенд системи відображення черги замовлень для шаурмічної (навчальний проєкт з курсу архітектури ПЗ; поточний стан — лабораторна 5).

- **Кухонний екран** — повний склад активних замовлень із модифікаторами та коментарями, дії зі статусами.
- **Табло для відвідувачів** (публічне) — номери замовлень у групах «Готується» (з орієнтовним часом очікування) та «Готово».
- Замовлення надходять після оплати з POS-системи Poster. Вебхук `POST /api/webhooks/poster` — лише сигнал (номер чека і дія), даних замовлення в ньому немає; склад чека система забирає через Poster API (порт `PosterClient`, адаптер `PosterClientAdapter`). У навчальній версії API Poster замінює `MockPosterApi`, а вебхуки надсилає скрипт `scripts/poster-mock.sh`.

Специфікація: [`docs/use-cases.md`](docs/use-cases.md) (юзкейси) та [`docs/api.md`](docs/api.md) (REST API).

## Модулі

Застосунок — модульний моноліт на Spring Modulith з двома модулями; усередині кожного ті самі чотири шари (`domain`, `application`, `infrastructure`, `presentation`):

```
com.mishanthrope.queueboard
├── QueueboardApplication
├── core/                  модуль «Core»: замовлення, кухня, табло, автентифікація, Poster
│   ├── events/            ПУБЛІЧНИЙ КОНТРАКТ: OrderEvent і шість подій
│   └── domain/ application/ infrastructure/ presentation/
└── analytics/             модуль «Analytics»: статистика
    ├── api/               ПУБЛІЧНИЙ КОНТРАКТ: StatsApi, DailyStats
    ├── domain/ application/ presentation/
    └── infrastructure/    persistence/, acl/ (CoreEventListener, CoreEventTranslator), config/
```

Core не залежить від Analytics. Analytics бачить лише `core.events`, і тільки в шарі ACL (`analytics.infrastructure.acl`). Події йдуть асинхронно: команда Core записує публікацію в реєстр `event_publication` (Spring Modulith) у своїй транзакції, а `@ApplicationModuleListener` в Analytics обробляє її після коміту. Межі перевіряють `ModularityTest` (`ApplicationModules.verify()`) і `ArchitectureTest`; діаграма модулів — у [`docs/architecture/`](docs/architecture/), обґрунтування — в [`docs/adr/0002-module-boundaries.md`](docs/adr/0002-module-boundaries.md).

## Стек

Java 21, Spring Boot 4 (Web MVC, Data JPA, Validation, Security), Spring Modulith 2.1, PostgreSQL, Liquibase, JWT (jjwt), MapStruct, Lombok, Checkstyle.

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

Статистику збирає модуль `analytics` з подій модуля Core (див. «Модулі»). Статистика узгоджується зрештою (eventual consistency): відповідь команди не чекає на аналітику, а зміни у статистиці з'являються через десятки мілісекунд. Режиму синхронної доставки більше немає (він лишився під тегом `lab4`), властивості `app.analytics.mode` теж немає.

| Налаштування | За замовчуванням | Опис |
|---|---|---|
| `app.analytics.simulated-delay` | `PT0S` | Штучна затримка обробки події в Analytics (щоб побачити відставання статистики) |
| `app.analytics.fail` | `false` | Analytics кидає виняток перед записом: публікація лишається в реєстрі незавершеною |
| `spring.modulith.events.republish-outstanding-events-on-restart` | `true` | Незавершені публікації доставляються повторно під час старту |

```bash
java -jar target/queueboard-0.0.1-SNAPSHOT.jar --app.analytics.simulated-delay=PT0.5S
```

Стан доставки видно в таблиці `event_publication` (`status`: `PUBLISHED`, `PROCESSING`, `COMPLETED`, `FAILED`, `RESUBMITTED`; завершені публікації лишаються в таблиці). Повторна доставка однієї події нічого не змінює: Analytics ідемпотентна за `eventId`. Вимірювання — у [`docs/analysis/lab5.md`](docs/analysis/lab5.md).

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
./mvnw test -Dtest='com.mishanthrope.queueboard.core.domain.**'      # лише тести домену Core, Docker не потрібен
./mvnw test -Dtest='ArchitectureTest,ModularityTest'            # лише перевірка шарів і меж модулів
./mvnw verify                                                   # Checkstyle + усі тести
```

Тести написано на JUnit 5, AssertJ, Mockito, MockMvc, ArchUnit, Spring Modulith, Awaitility і Testcontainers (`postgres:17-alpine`). Тестові пакети повторюють структуру коду: `core.domain`, `core.application`, `core.infrastructure`, `core.presentation` і `analytics.*`; `ArchitectureTest`, `ModularityTest`, `AbstractIntegrationTest` і `QueueboardApplicationTests` лежать у кореневому пакеті.

Підхід до тестів відповідає CQS: **команди** (запис) перевіряються юніт-тестами на in-memory фейках репозиторіїв, **запити** (читання) — інтеграційними тестами через HTTP на справжній БД.

### Піраміда тестів

Усього 365 тестів (рахунок за звітами surefire):

| Модуль / шар | Тестів | Як тестується |
|---|---|---|
| `core.domain` | 122 | звичайні JUnit/AssertJ без Spring і Mockito; репозиторії замінюють in-memory фейки `InMemoryOrderRepository` і `InMemoryEmployeeRepository` |
| `core.application` | 100 | обробники команд на тих самих фейках (разом із перевіркою опублікованої події через `CollectingOrderEventPublisher`); Mockito лише для портів і обробників, які треба змусити кинути виняток; фіксований `Clock` |
| `core.infrastructure` | 44 | адаптери без Spring-контексту: JWT, md5-підпис, розбір даних Poster |
| `core.presentation` (юніт) | 4 | `GlobalExceptionHandler` через standalone MockMvc |
| `core` інтеграційні | 43 | `@Tag("integration")`, весь застосунок на PostgreSQL у контейнері, HTTP через MockMvc |
| `analytics.domain` | 11 | `OrderTiming` (найраніший старт, найпізніша готовність, тривалості) і `ModifierTally` без Spring |
| `analytics.infrastructure.acl` | 7 | `CoreEventTranslator`: кожна подія Core → правильний внутрішній факт |
| `analytics` інтеграційні | 19 | події в реєстрі Modulith, таблиці `analytics_*`, `GET /api/stats`, відставання, збій, повторна доставка (Awaitility) |
| `ModularityTest` | 2 | `ApplicationModules.verify()` і запис документації Modulith (PlantUML) |
| `ArchitectureTest` | 13 | ArchUnit: шари в кожному модулі і залежності між модулями |

Без інтеграційних тестів виконується 303 тести. Тести доменів (`core.domain` і `analytics.domain`, 133 тести) разом із запуском Maven тривають близько 4,3 с, усі юніт-тести — близько 9,3 с, `ArchitectureTest` з `ModularityTest` — близько 6,5 с, повний `./mvnw verify` — близько 33 с (по одному вимірюванню). Більша частина цього часу — старт Maven і JVM.

Інтеграційні тести успадковують `AbstractIntegrationTest`: Spring-контекст і контейнер стартують один раз, перед кожним тестом таблиці очищаються (спершу `orders`, потім `employees`) через Spring Data репозиторії `SpringDataOrderRepository` і `SpringDataEmployeeRepository`. Підписані вебхуки Poster для тестів будує `SignedWebhooks` (JSON-тіло і `verify` = md5 із секретом із `PosterProperties`).

### Що покриває кожен клас

**`core.domain`** (122)

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

**`core.application`** (100): команди

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `OrderStatusCommandsTest` | 26 | `StartCooking`, `MarkReady`, `IssueOrder`, `CancelOrder`, `RestoreOrder`: стан замовлення в репозиторії після команди (статус і мітка), заборонена дія → `IllegalStatusTransitionError` без змін, невідомий id → `OrderNotFoundError`, чужі замовлення не змінюються; кожна команда публікує свою подію з правильними полями (`CollectingOrderEventPublisher`), невдала команда подій не публікує |
| `ShiftPrepTimeHandlerTest` | 4 | зсув `estimatedReadyAt` для NEW і COOKING в обидва боки; READY, ISSUED, CANCELLED не змінюються; порожня черга |
| `CreateOrderFromPosterHandlerTest` | 29 | відмови (id чека, `paidAt`, коментар, порожні позиції, рядок команди з порушенням правил позиції) і межові значення; дубль `posterCheckId`; збережені позиції; `displayNumber`; `estimatedReadyAt` (порожня черга, хвіст у минулому, у майбутньому, друге замовлення після першого); подія `OrderAccepted` з позиціями й часом готовності, дубль і помилка подій не публікують |
| `RegisterEmployeeHandlerTest` | 6 | повертається id нового працівника; нормалізація email і імені; зберігається хеш із `PasswordHasher`; дубль email в іншому регістрі → `DuplicateEmailError`; некоректний email або порожнє ім'я |
| `ProcessPosterWebhookHandlerTest` | 19 | невірний підпис → `InvalidWebhookSignatureError`; інший `object`; `removed` (існуюче, видане, невідоме); уже існуюче замовлення; чек не оплачений; значення чека в команді `CreateOrderFromPoster`; стан репозиторію після вебхука; помилки домену, Poster і БД не ламають відповідь |

**`core.application`**: запити

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `LoginHandlerTest` | 4 | токен для нормалізованого email; неправильний пароль, невідомий і некоректний email → `InvalidCredentialsError` |
| `GetBoardHandlerTest` | 12 | групування рядків на `preparing` і `ready`; `minutesLeft` округлюється вгору і не буває менше 0 (фейк `BoardReadRepository`) |

**`core.infrastructure`** (44)

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `JwtTokenServiceTest` | 11 | токен повертає email, прострочений, підроблений і сміттєвий токени → порожньо |
| `Md5WebhookSignatureVerifierTest` | 14 | вірний підпис (з `data` і без), приклад із `docs/api.md`, регістр hex, невірний або відсутній підпис і підписані поля |
| `PosterClientAdapterTest` | 19 | чек не закрито або не оплачено → порожньо; розбір `date_close`, `num`, `modificator_name`, порожнього коментаря; помилкові дані → `PosterDataError` |

**`core.presentation`** (4 юніт + 43 інтеграційні)

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

**`analytics`** (11 доменних, 7 ACL, 19 інтеграційних; базовий клас `AbstractAnalyticsTest` очищає таблиці `analytics_*` і реєстр `event_publication` та чекає, поки всі публікації будуть доставлені)

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `OrderTimingTest` | 8 | найраніший `start` і найпізніший `complete` незалежно від порядку; `cookingTime`, `waitTime`, `estimateError` (знак), порожні тривалості без однієї з міток; `reconstitute` |
| `ModifierTallyTest` | 3 | порожній модифікатор і нестрога кількість відхиляються |
| `CoreEventTranslatorTest` | 7 | `OrderAccepted` → `OrderReceived` (обіцяний час, лічильники модифікаторів із урахуванням кількості), п'ять статусних подій → `ActionPerformed` з потрібною дією і часом |
| `OrderStatsIntegrationTest` | 13 | кожен тип події оновлює свої таблиці; найраніший старт, найпізніша готовність; події до прийняття замовлення; дубль події і запізнілий дубль нічого не псують; `GET /api/stats` за сценарієм доби, порожній день, межа доби, дата за замовчуванням, 400, 401 |
| `EventualConsistencyIntegrationTest` | 6 | статистика з'являється згодом; доставлена публікація завершена; затримка 1 с не гальмує `POST /ready`; `fail=true` лишає публікацію `FAILED`, замовлення не страждає; після `fail=false` і `resubmitIncompletePublications` статистика наздоганяє; невдала команда (409) нічого не публікує |

**Архітектура** (13 правил `ArchitectureTest` і 2 тести `ModularityTest`): `core.domain` залежить лише від `java..` і себе, `core.application` не залежить від `core.infrastructure` і `core.presentation`, `core.presentation` не залежить від `core.infrastructure` і доменних репозиторіїв, `core.infrastructure` не залежить від `core.presentation`; ті самі правила для `analytics.domain`, `analytics.application`, `analytics.presentation` (лише `api`), `analytics.infrastructure`; ніщо в `core` не залежить від `analytics`; `core.events` використовує лише `analytics.infrastructure.acl`, решта `core` для `analytics` закрита. `ModularityTest` додатково перевіряє публічні контракти (`@NamedInterface`) і відсутність циклів.
