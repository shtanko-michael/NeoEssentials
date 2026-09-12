## Как устроен аукцион

Отдельный модуль форка: игроки выставляют **весь стек из основной руки** командой `/ah sell <цена>`, остальные покупают через GUI `/ah`. Непроданное после таймера уходит в `/ah expired` и ждёт, пока продавец заберёт.

Денежный контур — тот же Vault/`EconomyAPI`, что у `/pay`. Конфиг — `config/auctionhouse.json` (не папка `neoessentials/`). Данные — коллекции DataStore `auction_listings` / `auction_expired`.

Модуль выключается `modules` (auction house) и `commands.ah`. Пока команда выключена, Brigadier-дерево не регистрируется.
