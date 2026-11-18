# Queueboard — табло черги замовлень

Бекенд системи відображення черги замовлень для шаурмічної (лабораторна робота 1 з курсу архітектури ПЗ).

- **Кухонний екран** — повний склад активних замовлень із модифікаторами та коментарями, дії зі статусами.
- **Табло для відвідувачів** (публічне) — номери замовлень у групах «Готується» (з орієнтовним часом очікування) та «Готово».
- Замовлення надходять після оплати з POS-системи Poster. Вебхук `POST /api/webhooks/poster` — лише сигнал (номер чека і дія), даних замовлення в ньому немає; склад чека система забирає через Poster API (`PosterClient`). У навчальній версії API Poster замінює `MockPosterClient`, а вебхуки надсилає скрипт `scripts/poster-mock.sh`.

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

Скрипт надсилає N сигнальних вебхуків (`action: changed`, нові випадкові `object_id`) на `POST /api/webhooks/poster`. Кожен вебхук підписаний: `verify` = md5 від `account;object;object_id;action;time;secret` рахується через `md5sum`. Дані чеків (позиції, модифікатори, коментар) віддає `MockPosterClient`: для одного `object_id` завжди той самий чек. Логін і працівник не потрібні. Скрипт завантажує `.env` з кореня проєкту (`POSTER_APPLICATION_SECRET`, `API_URL`; значення з `.env` перекривають змінні оточення, `API_URL` за замовчуванням `http://localhost:8080`). Вебхук приймає лише `Content-Type: application/json`. Для кожного виклику виводиться HTTP-статус.

```bash
scripts/poster-mock.sh                       # 5 замовлень
scripts/poster-mock.sh 3                     # 3 замовлення
scripts/poster-mock.sh 3 --duplicate         # + повторна відправка останнього вебхука (дубля замовлення немає)
scripts/poster-mock.sh 3 --duplicate --removed   # + action "removed" для останнього чека (замовлення стає CANCELLED)
```

Усі відповіді мають бути `200`. Результат видно в `GET /api/orders/kitchen` (активні замовлення) і `GET /api/orders/{id}`. Пауза між вебхуками задається змінною `DELAY_SECONDS` (за замовчуванням 0).

## Тести

```bash
./mvnw test                                  # усі тести (потрібен Docker для Testcontainers)
./mvnw test -DexcludedGroups=integration     # лише швидкі юніт-тести, Docker не потрібен
./mvnw verify                                # Checkstyle + усі тести
```

Тести написано на JUnit 5, AssertJ, Mockito, MockMvc і Testcontainers (`postgres:17-alpine`). Тестові пакети повторюють пакети коду (`order/service/OrderServiceTest` тестує `order/service/OrderService`).

### Піраміда тестів

- **Юніт-тести (основа, 141 тест, ~3 с)** — без Spring-контексту, бази та HTTP. Репозиторії — Mockito-моки, час — фіксований `Clock` (`Europe/Kyiv`), конфігураційні рекорди створюються прямо в тесті. Тут розв'язується більшість випадків: таблиці переходів статусів, граничні значення інваріантів, алгоритм черги, підпис вебхука. Вони швидкі, точно показують, що зламалось, і дозволяють перебрати багато комбінацій (`@ParameterizedTest`).
- **Інтеграційні тести (вершина, 33 тести, ~15 с)** — позначені `@Tag("integration")`, піднімають увесь застосунок на справжньому PostgreSQL у контейнері і ходять через MockMvc: HTTP → фільтр JWT → контролер → сервіс → JPA → БД. Вони перевіряють те, чого юніт-тести не бачать: реальну схему Liquibase, JPQL-запити, транзакції, безпеку, формат помилок. Усі класи успадковують `AbstractIntegrationTest`, тож Spring-контекст і контейнер стартують один раз; перед кожним тестом таблиці очищаються (спершу `orders`, потім `employees`).

Підписані вебхуки Poster для тестів будує `SignedWebhooks` (JSON-тіло і `verify` = md5 із секретом із `PosterProperties`).

### Що покриває кожен клас

| Клас | Що перевіряє |
|---|---|
| `OrderStatusTest` | усі дозволені переходи; кожна заборонена дія в кожному статусі (повна таблиця статус × дія) кидає `IllegalStatusTransitionException`; ISSUED нічого не дозволяє |
| `PrepTimeCalculatorTest` | 1, 2, 5, 6, 10 позицій → 15, 18, 27, 32, 42 хв; кількості підсумовуються по рядках; інша місткість гриля змінює партії |
| `OrderServiceTest` | відмови `createFromPoster` (id чека, `paidAt` понад допуск, коментар, порожні позиції, назва, кількість 0 і 100, модифікатор) і межові прийняті значення; `displayNumber`; `estimatedReadyAt` (порожня черга, хвіст у минулому, хвіст у майбутньому); часові мітки статусних дій і `restore`; `NotFoundException`; `shiftPrepTime`; `estimatedReadyAt` = null для ISSUED і CANCELLED |
| `BoardServiceTest` | READY → `ready`, NEW/COOKING → `preparing`; `minutesLeft` округлюється вгору і не буває менше 0 |
| `PosterSignatureVerifierTest` | вірний підпис (з `data` і без), приклад із `docs/api.md`, регістр hex, невірний або відсутній підпис і підписані поля → `UnauthorizedException` |
| `PosterWebhookServiceTest` | інший `object`, `removed` (існуюче, видане, невідоме), уже існуюче замовлення, чек не закрито або не оплачено, розбір `date_close` / `num` / `modificator_name` / порожнього коментаря, помилки даних і `PosterClient` не виходять назовні |
| `AuthServiceTest` | дубль email без урахування регістру, нормалізація email, пароль зберігається хешем, неправильний пароль або невідомий email → `BadCredentialsException` |
| `JwtServiceTest` | токен повертає email, прострочений, підроблений і сміттєвий токени → порожньо |
| `AuthIntegrationTest` | реєстрація 201 / 409 / 400, вхід 200 / 401, захищений ендпоінт без токена і з хибним токеном → 401 із нашим JSON |
| `PosterWebhookIntegrationTest` | підписаний вебхук створює замовлення з позиціями; повтор не дублює; `removed` скасовує; хибний підпис 401; битий JSON 400; `object: product` → 200 без замовлення |
| `OrderIntegrationTest` | життєвий цикл start → ready → issue; заборонений перехід 409; `restore`; 404; `DELETE` прибирає замовлення з черги і табло; історія (фільтр, пагінація, `size=0` і `page=-1` → 400); `PATCH /api/orders/prep-time` (зсув рівно на 5 хв, 0 і 31 → 400); час другого замовлення пізніший; `PUT /api/orders/{id}` → 405 |
| `BoardIntegrationTest` | `GET /api/board` без токена, групування на `preparing` / `ready` |
| `QueueboardApplicationTests` | `contextLoads`: контекст стартує, схема Liquibase збігається із сутностями (`ddl-auto: validate`) |
