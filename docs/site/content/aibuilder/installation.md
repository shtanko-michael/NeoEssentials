## Требования

- Сервер **NeoForge 1.21.1**, Java 21.
- **Тот же jar у игроков** в `mods/` клиента: клиентская половина рисует голограмму-прицел (`R` — поворот, ЛКМ/Enter — поставить, ПКМ/Backspace — отменить) и всю анимацию стройки.
- Аккаунт на [farmsteadminecraft.online](https://farmsteadminecraft.online) с AI-токенами на балансе.
- Исходящий HTTPS с сервера на `ai.farmsteadminecraft.online`.
- **LuckPerms — необязателен.** Нужен, только если хотите раздавать команды по группам, а не уровнями прав из конфига.

Игрок **без** мода на сервер зайдёт, но постройка просто появится перед ним — без прицела и без анимации. Это аварийный режим, а не способ играть.

## Установка

1. Скачайте [последний релиз](https://github.com/shtanko-michael/farmstead-aibuilder/releases/latest) и положите jar в `mods/` сервера. Тот же файл раздайте игрокам.
2. Запустите сервер один раз — появится `config/farmstead_aibuilder.toml`.
3. В кабинете [farmsteadminecraft.online/cabinet/ai-builder](https://farmsteadminecraft.online/cabinet/ai-builder) нажмите «Создать токен». Значение показывается **один раз** — скопируйте сразу.
4. Впишите его в конфиг:

    ```toml
    [api]
    api_base_url = "https://ai.farmsteadminecraft.online"
    bearer_token = "fsm_..."
    ```

5. Перезапустите сервер.

## Проверка по логу

После перезапуска в логе появятся две строки:

```text
[Farmstead] AI service OK: server='Основной сервер' mode=external apiVersion=1 …
[regionguard] API available (ClaimsApi) — claims are enforced for non-operators
```

Первая — токен принят. Если вместо неё `[Farmstead] the AI service rejected this server's token`, токен вписан неверно или отозван. Если `[Farmstead] /aibuild is not configured`, в конфиге пусты `api_base_url` или `bearer_token`.

Вторая — про приваты: найден ли Farmstead Claims или исходный RegionGuard. Подробнее — в разделе [«Защита территорий»](../land-protection/).

## Обновление

Постоянная ссылка на свежую сборку: [releases/latest/download/farmstead-aibuilder-neoforge-1.21.1.jar](https://github.com/shtanko-michael/farmstead-aibuilder/releases/latest/download/farmstead-aibuilder-neoforge-1.21.1.jar). Имя файла одно и то же во всех версиях, поэтому новый jar **заменяет** старый — два файла с одним mod id FML молча загрузил бы в случайном порядке. Версия указана в названии релиза и в манифесте jar. Лимиты хранятся на стороне сервиса, обновление мода их не сбрасывает.

## Безопасность токена

Токен — это ключ к вашему кошельку AI-токенов. Не публикуйте конфиг. Утёк — отзовите токен в кабинете и создайте новый.
