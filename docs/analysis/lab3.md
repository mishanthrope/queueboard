# Самоаналіз лабораторної роботи 3

Числа нижче взято з коду і реальних запусків. Стан «Lab 2» — тег `lab2`, стан «Lab 3» — поточне дерево.

## 1. Що змінилося в структурі проєкту порівняно з лабораторною 2?

Чотири шари лишилися, змінився шар `application`. Замість чотирьох сервісів (`OrderService`, `BoardService`, `AuthService`, `PosterWebhookService`) там тепер команди, запити й по одному обробнику на кожну:

- `order/command`: `StartCooking`, `MarkReady`, `IssueOrder`, `CancelOrder`, `RestoreOrder`, `ShiftPrepTime`, `CreateOrderFromPoster` і сім обробників (`@Transactional`, `void handle`). Команда `CreateOrderFromPoster` несе лише прості дані (`List<Line>` із назвою, кількістю й модифікаторами), а `OrderItem` будує і перевіряє `CreateOrderFromPosterHandler`, тож `InvalidOrderError` кидає обробник, а не код, що формує команду;
- `order/query`: `GetKitchenQueue`, `GetOrderDetails`, `GetOrderHistory`, їхні обробники, моделі читання `OrderView` і `OrderItemView`, порт читання `OrderReadRepository`;
- `board/query`: `GetBoard`, `GetBoardHandler`, `BoardView`, `PreparingEntry`, `ReadyEntry`, проєкція `BoardRow`, порт `BoardReadRepository`;
- `auth/command`: `RegisterEmployee` і `RegisterEmployeeHandler` (єдиний обробник, що повертає значення, id працівника); `auth/query`: `Login`, `GetEmployee`, обробники, `AuthToken`, `EmployeeView`, `EmployeeReadRepository`;
- `poster/command`: `ProcessPosterWebhook` (замінила `PosterWebhookNotice`) і `ProcessPosterWebhookHandler`;
- `common`: `CommandHandler<C>`, `QueryHandler<Q, R>`, `PageView<T>` (перейшов із домену, де був `PageResult`).

З домену прибрано `findActive` і `findHistory` (порт `OrderRepository` тепер лише для запису) та `PageResult`; додано `EmployeeNotFoundError`. В `infrastructure` з'явилися читальні адаптери `OrderReadRepositoryAdapter`, `BoardReadRepositoryAdapter`, `EmployeeReadRepositoryAdapter`, `OrderViewMapper` і службовий `OrderStatusSets`. У `presentation` зникли сім класів (`OrderResponse`, `OrderItemResponse`, `OrderResponseMapper`, `EmployeeResponse`, `EmployeeResponseMapper`, `TokenResponse`, `PageResponse`): контролери повертають моделі читання напряму.

Файлів Java у `src/main`: 114 проти 80, рядків 2916 проти 2499. По шарах (файлів / рядків): `application` 54 / 750 проти 18 / 389, `infrastructure` 26 / 1017 проти 21 / 898, `presentation` 14 / 529 проти 21 / 585, `domain` 19 / 605 проти 19 / 612.

## 2. Переваги CQS: чим допомогло розділення?

**Читання більше не проходить через домен.** У Lab 2 `GET /api/board` будував доменні `Order` через `findActive()`, і мапер читав позиції та модифікатори кожного активного замовлення: `show-sql` показував 3 запити (`orders`, `order_items`, `order_item_modifiers`). Тепер `GetBoardHandler` читає проєкцію `BoardRow`, і той самий `GET /api/board` виконує 1 запит: `select oe1_0.display_number,oe1_0.status,oe1_0.estimated_ready_at from orders oe1_0 where oe1_0.status in (?,?,?) order by oe1_0.paid_at`.

**Запис не знає про читання.** Порт `OrderRepository` має сім методів, потрібних лише для збереження і перевірок домену (`save`, `findById`, `findByPosterCheckId`, `existsByPosterCheckId`, `findInQueue`, `findLatestEstimatedReadyAtInQueue`, `findMaxDisplayNumberCreatedSince`), без пагінації й сторінок. Правило «`estimatedReadyAt` порожній для завершених замовлень» переїхало в `OrderViewMapper` (сторона читання), а `Order` зайвого не знає.

**Менша зона зміни.** Обробник статусної команди — 25–28 рядків (`StartCookingHandler`, `ShiftPrepTimeHandler`), у Lab 2 весь запис жив в одному `OrderService` (96 рядків) разом із читанням. Зміна запиту історії зачіпає лише `GetOrderHistoryHandler` і `OrderReadRepositoryAdapter.findHistory`.

**Правила зафіксовано тестом.** `ArchitectureTest` отримав шосте правило: `presentation` не залежить від `OrderRepository` і `EmployeeRepository`, тож контролер не може обійти обробники.

## 3. Недоліки CQS: що ускладнилося?

**Більше класів.** У `application` 54 файли проти 18 (+36), у цілому 114 проти 80. На кожен сценарій тепер два класи (запис команди і обробник) замість одного методу сервісу: 15 обробників (9 команд, 6 запитів).

**Композиція «команда + запит» у контролерах.** Команди нічого не повертають, а HTTP-контракт лишився колишнім, тому п'ять ендпоінтів складають дві дії: `POST /api/orders/{id}/start|ready|issue|restore` виконують команду, потім `GetOrderDetails`, а `POST /api/auth/register` виконує `RegisterEmployee` і `GetEmployee`. `OrderController` став з 85 до 113 рядків і має 9 залежностей-обробників замість одного `OrderService`. Ціна — додаткове читання після кожної такої дії (кількість SQL я не вимірював) і окрема транзакція для запиту: помилка між командою й запитом дала б успішну зміну з відповіддю 5xx.

**Дві моделі однієї сутності.** Для замовлення тепер три відображення: `OrderEntity` ↔ `Order` (запис, `OrderEntityMapper`), `OrderEntity` → `OrderView` (читання, `OrderViewMapper`) і DTO запитів. Нове поле замовлення треба додати в `OrderView` і `OrderViewMapper` окремо від `Order`.

**Обробник вебхука лишився найбільшим.** `ProcessPosterWebhookHandler` — 106 рядків, бо маршрутизація, обробка помилок і логування вебхука навмисно не змінені. Він викликає два інших обробники (`CancelOrderHandler`, `CreateOrderFromPosterHandler`), тобто команди тепер викликають команди.

## 4. Чим підхід із Command / Query Handler відрізняється від Service, який робить все сам?

`OrderService` у Lab 2 мав 10 публічних методів (створення, зсув часу, п'ять статусних дій, три читання) і залежав від `OrderRepository`, `OrderFactory` і `Clock`. Усі методи жили в одному класі й одній залежності, тож читальні методи (`getKitchenQueue`, `getOrder`, `getHistory`) були лише делегуванням до репозиторія, а зміни `OrderRepository` зачіпали і запис, і читання. Тепер кожен обробник залежить лише від того, що йому треба: `StartCookingHandler` — від `OrderRepository` і `Clock`, `GetKitchenQueueHandler` — тільки від `OrderReadRepository`. Метод сервісу приймав розрізнені аргументи (`createFromPoster(long, Instant, String, List)`), команда — це іменований незмінний пакет даних (`CreateOrderFromPoster`), який можна створити, передати й перевірити окремо. Те, що сервіс повертав доменний `Order` для маппінгу в контролері, тепер повертає модель читання, форма якої описана самим запитом.

## 5. Як CQS впливає на розширюваність?

Нову команду (наприклад, повернення виданого замовлення у стан «готується») тепер додають без зміни наявного коду: запис команди і обробник у `order/command`, метод у `Order` і рядок у контролері. У Lab 2 це був би ще один метод у `OrderService` на 96 рядків із усіма його залежностями, з ризиком зачепити сусідні методи. Новий запит (наприклад, «замовлення за номером») додається так само: запис, обробник, метод читального порту й SQL у читальному адаптері, без доторку до домену і до запису. Ціна розширення — шаблонний код: два нових файли (запис команди й обробник) плюс тест, метод у `Order` і рядок у контролері замість одного методу в сервісі.

## 6. Чи відрізняється структура даних, яку повертає Query, від доменної моделі? Чому це важливо?

Відрізняється, і це головна вигода. Для табло запиту потрібні три поля: `BoardRow(displayNumber, status, estimatedReadyAt)`. Доменний `Order` має 13 полів і список позицій з модифікаторами. Якщо читати табло через `Order`, база віддає всі колонки й ще два запити на колекції (3 запити в Lab 2); через `BoardRow` вона віддає три колонки одним запитом (1 запит, виміряно `show-sql` на одноразовій БД). `BoardView` (`preparing`, `ready`) і `minutesLeft` — форма, яку чекає клієнт, її рахує `GetBoardHandler`, а не `Order`. `OrderView` навпаки збирає все замовлення з позиціями, але напряму з `OrderEntity`, минаючи інваріанти домену: для читання вони не потрібні. Окрема форма читання дозволяє міняти JSON і SQL, не торкаючись `Order`, і навпаки.

## Що стало легшим і важчим для тестування порівняно з Lab 2

Легше: тести команд перевіряють стан `InMemoryOrderRepository` після команди (статус і часова мітка), а не виклики моків; у Lab 2 `OrderServiceTest` підміняв методи `OrderRepository` через Mockito. Мок лишився лише для портів, які без нього не змоделювати (`PasswordHasher`, `TokenService`, `PosterClient`, `WebhookSignatureVerifier`) і для обробників, які треба змусити кинути виняток. Тести-делегування читання (`getOrder`, `getKitchenQueue`, `getHistory`) видалено. Групування табло перевіряється чистим юніт-тестом `GetBoardHandlerTest` (12 тестів) із фейком `BoardReadRepository`.

Важче: поведінку читальних адаптерів (JPQL-проєкції, фільтри за `date` і `status`, пагінація, `estimatedReadyAt = null` для завершених) неможливо перевірити без БД, тому вона покрита інтеграційними тестами через HTTP: 43 тести проти 33 у Lab 2 (нові `OrderQueryIntegrationTest` — 7 тестів і `BoardQueryIntegrationTest` — 3). Усього тестів 304 проти 278; `application` — 85 проти 72, `ArchitectureTest` — 6 проти 5.

## Чому `Login` є запитом, і рішення за варіантом A

`Login` не змінює стан: токен JWT stateless, його не зберігають ні в БД, ні в пам'яті, тож обробник лише читає працівника через `EmployeeRepository`, перевіряє пароль і повертає `AuthToken`. Команда за визначенням CQS змінює стан, а тут нічого не змінюється.

Варіант A: команди нічого не повертають (виняток — `RegisterEmployeeHandler`, що повертає id), а HTTP-відповіді лишилися колишніми завдяки запиту, який контролер виконує після команди. Це зберігає чисте правило «команда не віддає дані» і не змінює API, але ціною додаткового читання (розділ 3).
