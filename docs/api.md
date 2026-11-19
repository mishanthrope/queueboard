# REST API (лабораторна 1)

Усі ендпоінти, крім позначених як публічні та вебхука, вимагають заголовок `Authorization: Bearer <JWT>`. Без нього вони повертають 401. Вебхук POS-системи автентифікується підписом `verify` у тілі запиту.

Помилки повертаються у форматі:

```json
{ "status": 409, "error": "Conflict", "message": "Cannot restore order in status ISSUED" }
```

## Автентифікація (публічні)

| Метод | Шлях | Юзкейс | Успіх | Помилки |
|---|---|---|---|---|
| POST | /api/auth/register | UC-01 | 201 | 400, 409 |
| POST | /api/auth/login | UC-02 | 200 `{ "token": "..." }` | 400, 401 |

## Замовлення

| Метод | Шлях | Юзкейс | Успіх | Помилки |
|---|---|---|---|---|
| GET | /api/orders/kitchen | UC-04 | 200 | 401 |
| GET | /api/orders/{id} | UC-05 | 200 | 401, 404 |
| POST | /api/orders/{id}/start | UC-06 (NEW → COOKING) | 200 | 401, 404, 409 |
| POST | /api/orders/{id}/ready | UC-06 (COOKING → READY) | 200 | 401, 404, 409 |
| POST | /api/orders/{id}/issue | UC-06 (READY → ISSUED) | 200 | 401, 404, 409 |
| PATCH | /api/orders/prep-time | UC-11 (глобальний зсув часу очікування) | 204 | 400, 401 |
| DELETE | /api/orders/{id} | UC-07 (скасування, soft delete) | 204 | 401, 404, 409 |
| POST | /api/orders/{id}/restore | UC-08 (READY / CANCELLED → COOKING) | 200 | 401, 404, 409 |
| GET | /api/orders?date=2026-11-17&status=READY&page=0&size=20 | UC-09 | 200 | 400, 401 |

Дії зі статусом (start, ready, issue, restore) не мають тіла запиту і повертають оновлене замовлення.

## Вебхук POS-системи

| Метод | Шлях | Юзкейс | Успіх | Помилки |
|---|---|---|---|---|
| POST | /api/webhooks/poster | UC-03 | 200 (завжди, коли підпис вірний; тіло порожнє) | 400, 401 |

Вебхук Poster — лише сигнал, даних замовлення в ньому немає. Заголовок `Authorization` не потрібен: автентифікація — це підпис `verify` = md5(`account;object;object_id;action;[data;]time;application_secret`), де `data` входить у рядок лише якщо присутнє. Невірний або відсутній підпис → 401, тіло не JSON → 400.

Для вірного підпису відповідь завжди 200 OK: Poster очікує саме 200, інакше повторює доставку до 15 разів. Це стосується і вебхуків про інші об'єкти, повторних вебхуків, нескасовуваних замовлень та чеків, які не вдалося прийняти (причина йде в лог).

## Табло (публічне)

| Метод | Шлях | Юзкейс | Успіх |
|---|---|---|---|
| GET | /api/board | UC-10 | 200 |

## Статистика

| Метод | Шлях | Юзкейс | Успіх | Помилки |
|---|---|---|---|---|
| GET | /api/stats?date=2026-11-17 | UC-12 | 200 | 400, 401 |

`date` необов'язковий (`YYYY-MM-DD`), за замовчуванням сьогодні в часовій зоні сервера. Доба — півінтервал `[00:00, 00:00 наступного дня)`.

## Приклади тіл

`POST /api/webhooks/poster` (формат реального Poster; `time` — рядок, `verify` — md5-підпис):

```json
{
  "account": "shawarma",
  "object": "transaction",
  "object_id": 48213,
  "action": "changed",
  "time": "1794919500",
  "verify": "a1a76179b60cdc30c584824ae98635db"
}
```

Підпис у прикладі пораховано з локальним секретом `local-dev-poster-secret`. `action`: `added` | `changed` | `removed`. Іноді приходить додатковий рядок `data`. Для `added` / `changed` система сама забирає чек через Poster API за `object_id` і створює замовлення (якщо воно закрите, оплачене і ще не існує); для `removed` — скасовує замовлення з таким posterCheckId.

`GET /api/orders?page=0&size=20` → 200:

```json
{
  "content": [ { "id": 15, "displayNumber": 15, "status": "ISSUED", "...": "..." } ],
  "page": 0,
  "size": 20,
  "totalElements": 134,
  "totalPages": 7
}
```

`PATCH /api/orders/prep-time` → 204 (зсуває `estimatedReadyAt` усіх замовлень у NEW і COOKING; `deltaMinutes` — ціле ненульове від −30 до 30, інакше 400):

```json
{ "deltaMinutes": 5 }
```

`GET /api/board` → 200:

```json
{
  "preparing": [ { "displayNumber": 14, "minutesLeft": 6 }, { "displayNumber": 15, "minutesLeft": 9 } ],
  "ready":     [ { "displayNumber": 12 }, { "displayNumber": 13 } ]
}
```

`GET /api/stats?date=2026-11-17` → 200 (середні — у хвилинах з одним знаком, `null`, якщо за день не було готових замовлень):

```json
{
  "ordersAccepted": 5,
  "ordersIssued": 2,
  "ordersCancelled": 2,
  "averageCookingMinutes": 0.1,
  "averageWaitMinutes": 0.2,
  "averageEstimateErrorMinutes": -66.9,
  "cookActions": { "COOKING_STARTED": 3, "READY": 3, "ISSUED": 2, "CANCELLED": 2, "RESTORED": 1 },
  "topModifiers": [ { "modifier": "без цибулі", "times": 5 }, { "modifier": "сир", "times": 3 } ]
}
```
