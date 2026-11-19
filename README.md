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

### Піраміда тестів

Усього 278 тестів (рахунок за звітами surefire):

| Шар | Тестів | Як тестується |
|---|---|---|
| `domain` | 120 | звичайні JUnit/AssertJ без Spring і Mockito; репозиторії замінюють in-memory фейки `InMemoryOrderRepository` і `InMemoryEmployeeRepository` |
| `application` | 72 | Mockito-моки портів (`OrderRepository`, `EmployeeRepository`, `PasswordHasher`, `TokenService`, `PosterClient`, `WebhookSignatureVerifier`), фіксований `Clock` |
| `infrastructure` | 44 | адаптери без Spring-контексту: JWT, md5-підпис, розбір даних Poster |
| `presentation` (юніт) | 4 | `GlobalExceptionHandler` через standalone MockMvc |
| інтеграційні | 33 | `@Tag("integration")`, весь застосунок на PostgreSQL у контейнері, HTTP через MockMvc |
| архітектурні | 5 | ArchUnit: правила залежностей між шарами |

Без інтеграційних тестів виконується 245 тестів. Тести домену (120) разом із запуском Maven тривають близько 4,5 с (два вимірювання: 4,3 і 4,7 с), усі юніт-тести — близько 8,5 с, `ArchitectureTest` окремо — близько 5,3 с, повний `./mvnw verify` — близько 23,5 с. Більша частина цього часу — старт Maven і JVM.

Інтеграційні тести успадковують `AbstractIntegrationTest`: Spring-контекст і контейнер стартують один раз, перед кожним тестом таблиці очищаються (спершу `orders`, потім `employees`) через Spring Data репозиторії `SpringDataOrderRepository` і `SpringDataEmployeeRepository`. Підписані вебхуки Poster для тестів будує `SignedWebhooks` (JSON-тіло і `verify` = md5 із секретом із `PosterProperties`).

### Що покриває кожен клас

**`domain`** (120)

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

**`application`** (72)

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `OrderServiceTest` | 36 | `createFromPoster` (відмови, межові значення, дубль, `displayNumber`, оцінка від хвоста черги); кожна статусна дія зберігає замовлення; `OrderNotFoundError` для невідомого id; `shiftPrepTime`; `getKitchenQueue`, `getHistory` |
| `BoardServiceTest` | 12 | READY → `ready`, NEW/COOKING → `preparing`; `minutesLeft` округлюється вгору і не буває менше 0 |
| `AuthServiceTest` | 9 | дубль email, нормалізація, пароль зберігається хешем із `PasswordHasher`, неправильний пароль, невідомий і некоректний email → `InvalidCredentialsError` |
| `PosterWebhookServiceTest` | 15 | невірний підпис → `InvalidWebhookSignatureError`; інший `object`; `removed` (існуюче, видане, невідоме); уже існуюче замовлення; чек не оплачений; передача значень чека в `OrderService`; помилки домену, Poster і БД не ламають відповідь |

**`infrastructure`** (44)

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `JwtTokenServiceTest` | 11 | токен повертає email, прострочений, підроблений і сміттєвий токени → порожньо |
| `Md5WebhookSignatureVerifierTest` | 14 | вірний підпис (з `data` і без), приклад із `docs/api.md`, регістр hex, невірний або відсутній підпис і підписані поля |
| `PosterClientAdapterTest` | 19 | чек не закрито або не оплачено → порожньо; розбір `date_close`, `num`, `modificator_name`, порожнього коментаря; помилкові дані → `PosterDataError` |

**`presentation`** (4 юніт + 33 інтеграційні)

| Клас | Тестів | Що перевіряє |
|---|---|---|
| `GlobalExceptionHandlerTest` | 4 | непризначена `DomainError` і `ApplicationError` → 400, `OrderNotFoundError` → 404, непередбачений виняток → 500 |
| `AuthIntegrationTest` | 8 | реєстрація 201 / 409 / 400, вхід 200 / 401; захищений ресурс без токена, з неправильним і з правильним токеном |
| `PosterWebhookIntegrationTest` | 8 | підписаний вебхук створює замовлення з позиціями; повтор не дублює замовлення; `removed` скасовує, невідомий чек → 200; невірний або відсутній підпис → 401; зламаний JSON → 400; інший `object` ігнорується |
| `OrderIntegrationTest` | 14 | життєвий цикл start → ready → issue, заборонена дія → 409, restore, невідомий id → 404, `DELETE` прибирає замовлення з кухні й табло; історія з фільтром, пагінацією і помилковими параметрами; `PATCH /api/orders/prep-time` (зсув, завершені не зсуваються, неправильне значення, без токена); `PUT` → не підтримується |
| `BoardIntegrationTest` | 2 | `GET /api/board` без токена (порожнє табло), групування на `preparing` і `ready` |
| `QueueboardApplicationTests` | 1 | `contextLoads`: контекст стартує, схема Liquibase проходить валідацію Hibernate |

**Архітектура** (5): `ArchitectureTest` перевіряє, що `domain` залежить лише від `java..` і `domain..`, `application` не залежить від `infrastructure` і `presentation`, `presentation` не залежить від `infrastructure`, `infrastructure` не залежить від `presentation`.
