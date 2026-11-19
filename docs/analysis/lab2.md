# Самоаналіз лабораторної роботи 2

Числа нижче взято з коду і реальних запусків. Стан «Lab 1» — коміт `15073e7` (`lab 1`), стан «Lab 2» — поточне дерево.

## 1. Що змінилося в структурі проєкту порівняно з лабораторною 1?

У Lab 1 пакети ділили за фічами (`order`, `board`, `employee`, `poster`, `common`), а всередині — за типом компонента (`controller`, `service`, `repository`, `model`, `dto`). У Lab 2 верхній рівень — чотири шари з правилом «залежності спрямовані досередини»:

- `domain` (19 файлів): `Order`, `OrderItem`, `OrderStatus`, `OrderFactory`, `PrepTimeCalculator`, `KitchenSettings`, `Employee`, `Email`, `EmployeeFactory`, порти `OrderRepository` і `EmployeeRepository`, `PageResult`, помилки;
- `application` (18): `OrderService`, `BoardService`, `AuthService`, `PosterWebhookService`, порти `PasswordHasher`, `TokenService`, `PosterClient`, `WebhookSignatureVerifier`, записи результату табло;
- `infrastructure` (21): `persistence` (9 файлів), `security` (`JwtTokenService`, `BCryptPasswordHasher`), `poster` (`PosterClientAdapter`, `Md5WebhookSignatureVerifier`, `PosterApi` з `MockPosterApi`), конфігурація (`DomainConfig`, `OrdersProperties`, `ClockConfig`);
- `presentation` (21): контролери, DTO запитів і відповідей, MapStruct-мапери, `SecurityConfig` з JWT-фільтром, `GlobalExceptionHandler`.

Разом 80 виробничих файлів проти 49 (79 у шарах і `QueueboardApplication` поза ними), рядків Java у `src/main` — 2499 проти 1718. Тестових файлів 28 проти 17, тестів 278 проти 174.

Те, що в Lab 1 було одним класом, тепер розділене. `Order` — це доменний клас без сеттерів із методами `startCooking`, `markReady`, `issue`, `cancel`, `restore`, `shiftEstimate`, а таблиця `orders` описана окремою `OrderEntity` (так само `OrderItemEntity`, `EmployeeEntity`). Шлях до БД тепер триває три класи: порт `OrderRepository` (домен) → `OrderRepositoryAdapter` (реалізує порт, мапить через `OrderEntityMapper`) → `SpringDataOrderRepository` (інтерфейс Spring Data над `OrderEntity`). Для співробітників те саме: `EmployeeRepository` → `EmployeeRepositoryAdapter` → `SpringDataEmployeeRepository`.

Створення об'єктів, яким потрібен репозиторій, перейшло у фабрики: `OrderFactory.createFromPoster` (унікальність `posterCheckId`, `displayNumber`, `estimatedReadyAt`, допуск `paidAt`, непорожні позиції, довжина коментаря) і `EmployeeFactory.create` (унікальність email, довжина імені). Інваріанти позицій переїхали в конструктор `OrderItem`.

Винятки з іменами за HTTP (`NotFoundException`, `ConflictException`, `BadRequestException`, `UnauthorizedException` у `common.exception`) замінила ієрархія без знання про HTTP: `DomainError` (`OrderNotFoundError`, `IllegalStatusTransitionError`, `InvalidOrderError`, `DuplicatePosterCheckError`, `DuplicateEmailError`, `InvalidEmployeeError`) і `ApplicationError` (`InvalidCredentialsError`, `InvalidWebhookSignatureError`, `PosterDataError`). Статуси HTTP призначає лише `GlobalExceptionHandler`; для не призначених явно `DomainError` і `ApplicationError` є запасний обробник з відповіддю 400.

Правила залежностей тепер перевіряє `ArchitectureTest` (5 правил ArchUnit), а не домовленість у `CLAUDE.md`.

## 2. Які переваги дало розділення на шари?

**Тести домену без БД і Spring.** 120 тестів домену виконуються командою `./mvnw test -Dtest='com.mishanthrope.queueboard.domain.**'` за 4,3–4,7 с (два вимірювання), без Docker; більша частина цього часу — старт Maven. У Lab 1 найшвидші тести правил теж були юніт-тестами, але правила приймання замовлення можна було перевірити тільки через `OrderService` із замоканим репозиторієм (`OrderServiceTest`, 45 тестів, моки `findTopByCreatedAtGreaterThanEqualOrderByDisplayNumberDesc` тощо). Тепер `OrderFactoryTest` (22 тести) працює з `InMemoryOrderRepository` без Mockito, а `OrderServiceTest` скоротився до 36 тестів, бо перевірки інваріантів переїхали в `OrderItemTest` (14), `OrderFactoryTest` і `OrderTest` (12).

**Правила в одному місці.** З `OrderService` (226 рядків у Lab 1, тепер 96) пішло: `validateIntake` і `validateItem` → `OrderFactory` і конструктор `OrderItem`; `nextDisplayNumber` і `estimateReadyAt` → `OrderFactory`; пара «статус + часова мітка» → методи `Order`; множини `KITCHEN_STATUSES`, `QUEUE_STATUSES`, `FINISHED_STATUSES` → `OrderStatus.isActive()/isInQueue()/isFinished()`; мапінг у відповідь і пагінація Spring Data → `presentation` і `OrderRepositoryAdapter.findHistory`.

**Помилки не знають про HTTP.** `IllegalStatusTransitionError` більше не наслідує `ConflictException` із `common`; відповідність «помилка → статус» зібрана в одному `GlobalExceptionHandler`. Це дозволило покрити її окремим юніт-тестом `GlobalExceptionHandlerTest`.

**Залежності контролює тест.** Коли в доменному `Employee` з'явився Lombok-`@Getter`, `./mvnw verify` упав на правилі `domainDependsOnlyOnJavaAndItself` ще до перегляду коду.

**Поведінка не змінилась.** 33 інтеграційні тести (як і в Lab 1) пройшли без зміни HTTP-запитів і перевірок. У них змінилися тільки імпорти, пакети, тип `Order` → `OrderEntity` у хелперах і назви полів репозиторіїв (`orderEntities`, `employeeEntities`). Жива перевірка `scripts/poster-mock.sh 3 --duplicate --removed` дала лише відповіді 200.

## 3. Які недоліки або ускладнення з'явилися?

**Більше коду.** +31 виробничий файл (49 → 80) і +781 рядок Java у `src/main` (1718 → 2499). Дев'ять файлів лише обслуговують персистентність (`infrastructure/persistence`): п'ять для замовлення (`OrderEntity`, `OrderItemEntity`, `OrderEntityMapper`, адаптер, Spring Data інтерфейс) і чотири для співробітників.

**Відображення у двох місцях.** Сутність ↔ домен пишеться вручну (`OrderEntityMapper`, `EmployeeEntityMapper`: у домену немає сеттерів, тож MapStruct тут не годиться), а домен ↔ DTO — MapStruct (`OrderResponseMapper`, `EmployeeResponseMapper`, `PosterWebhookMapper`). Нове поле замовлення тепер треба додавати щонайменше в чотирьох місцях: `Order`, `OrderEntity`, `OrderEntityMapper` і DTO відповіді (плюс changeset, якщо поле зберігається).

**Складніша навігація.** Спочатку Spring Data інтерфейс і адаптер називались `OrderJpaRepository` і `JpaOrderRepository` і відрізнялись порядком слів, їх плутали; довелося перейменувати на `SpringDataOrderRepository` і `OrderRepositoryAdapter` та додати правило іменування в `CLAUDE.md`.

**Оновлення замовлення.** `OrderRepositoryAdapter.save` для наявного замовлення завантажує `OrderEntity` і копіює лише змінні поля (статус, мітки, `estimatedReadyAt`), бо позиції після створення не змінюються. За семантикою JPA цей `findById` у межах тієї ж транзакції бере сутність із контексту персистентності (додаткового SQL я не вимірював), але правило «яке поле змінне» тепер живе в `OrderEntityMapper.copyMutableFields`, окремо від `Order`, і його треба синхронізувати вручну.

**Табло завантажує зайве.** `BoardService` будує доменні `Order` через `findActive()`, тож мапер читає позиції і модифікатори кожного активного замовлення. Реальний запуск із `show-sql` показав 3 запити на один `GET /api/board` (замовлення, `order_items`, `order_item_modifiers`; завдяки `default_batch_fetch_size: 50` вони пакетні, а не по запиту на замовлення). У Lab 1 `BoardService` не торкався колекцій сутності, тож, судячи з коду, запит був один (цього я окремо не вимірював). Табло потребує лише номер, статус і `estimatedReadyAt`; прочитні моделі в Lab 3 це виправлять.

## 4. Наскільки простіше тепер змінити БД або фреймворк?

**Заміна PostgreSQL на MongoDB.** У Lab 1 довелося б змінити приблизно 15 із 49 виробничих файлів плюс тести, бо JPA-анотації були в моделі. Тепер зміни обмежені `infrastructure/persistence` (9 файлів: сутності стають документами, `SpringData...Repository` — репозиторіями Spring Data MongoDB, адаптери й мапери міняються слабо), а також: три changeset-и Liquibase і майстер-файл (Mongo без схеми, їх прибирають), `application.yaml` (секція `spring.datasource`/`jpa`/`liquibase`), `pom.xml` (стартери JPA, Liquibase, драйвер PostgreSQL), `docker-compose.yml`, `TestcontainersConfiguration` і типи в `AbstractIntegrationTest`. Не змінюються `domain` (19 файлів) і `presentation`. У `application` текст лишається тим самим, але чотири сервіси мають `@Transactional`, і для Mongo це вимагає `MongoTransactionManager` та реплікації; `PosterWebhookService` ловить `DataIntegrityViolationException`, а `DuplicateKeyException` Spring Data MongoDB є її підкласом, тому і це працює. Варто також перевірити `findHistory` (сортування, півінтервал дат) і гарантію унікальності `posterCheckId` через унікальний індекс.

**Заміна Spring на інший фреймворк.** `domain` не змінюється зовсім: жоден із його 19 файлів не імпортує `org.springframework` чи `jakarta`, це чиста Java. `application` залежить від Spring слабо: 4 із 18 файлів — чотири сервіси з `@Service` і `@Transactional`, а `PosterWebhookService` ще й імпортує `DataIntegrityViolationException`; логіку сервісів можна перенести, змінивши лише анотації, транзакції й цей виняток. `infrastructure` (18 із 21 файлу) і `presentation` (12 із 21) залежать від Spring сильно і переписуються майже повністю. Тобто фреймворк тепер лишається всередині двох зовнішніх шарів, а правила й сценарії від нього відділені.

## 5. Чому було обрано Rich Domain Model?

Правила цієї предметної області (переходи статусів із мітками, допуск `paidAt`, межі кількості, формула черги) безпосередньо про замовлення, тому вони мають лежати в `Order`, `OrderItem`, `OrderFactory`, а сервіси мають лише завантажувати, викликати домен і зберігати. Для `Employee` після створення правил немає, тож методів йому не вигадували. Повне обґрунтування, розглянуті варіанти й наслідки: [`docs/adr/0001-rich-domain-model.md`](../adr/0001-rich-domain-model.md).

## Свідомі компроміси

- **`infrastructure` залежить і від `application`, і від `domain`.** Технічні порти `PasswordHasher`, `TokenService`, `PosterClient`, `WebhookSignatureVerifier` лежать в `application`, бо їх викликають сценарії. Це відхилення від таблиці залежностей із завдання, але залежність усе одно спрямована досередини, а `ArchitectureTest` забороняє зовнішні напрямки (`application` → `infrastructure`/`presentation`, `presentation` → `infrastructure`, `infrastructure` → `presentation`).
- **`presentation` читає доменні типи**: відображення в DTO (`OrderResponseMapper`) і відповідність доменних помилок статусам HTTP в `GlobalExceptionHandler`; `OrderResponse` навіть містить доменний `OrderStatus`.
- **`PageResult` лежить у домені** лише тому, що порт `OrderRepository.findHistory` повертає сторінку. Пагінація не є бізнес-поняттям; у Lab 3 історія стане запитом (read-модель) і `PageResult` вийде з домену.
- **`PosterWebhookService` ловить `DataIntegrityViolationException`** зі Spring (деталь персистентності в `application`) для гонки одночасного імпорту того самого чека. Доменна перевірка `DuplicatePosterCheckError` ловить лише послідовні дублі, а при одночасних запитах ловить лише унікальне обмеження в БД.
- **Lombok заборонений у `domain` за домовленістю.** ArchUnit аналізує байткод, а анотації Lombok (`@Getter` тощо) мають source-retention, тож самі вони в байткод не потрапляють. Тест на Lombok-`@Getter` у `Employee` усе ж спрацював, але лише тому, що згенеровані методи отримали анотацію `lombok.Generated`; це залежить від налаштувань Lombok і не гарантоване, тому надійна перевірка — `grep -rn "lombok" src/main/java/com/mishanthrope/queueboard/domain` (порожній вивід).
