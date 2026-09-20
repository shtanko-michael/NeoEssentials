## Требования

- Minecraft `1.21.1`;
- NeoForge `21.1.230` или новее в ветке `21.1.x`;
- Java 21;
- WorldEdit для создания и переопределения регионов;
- необязательно: NeoEssentials и LuckPerms для выдачи прав без OP.

Поместите JAR RegionGuard в каталог `mods` сервера и перезапустите сервер. При первом запуске мод создаст конфигурацию, языковые файлы и глобальный регион для каждого загруженного измерения.

## Права для обычного игрока

Минимальный набор для создания и управления собственным приватом. Ниже перечислены безопасные флаги дома; `regionguard.command.flag.*` вместо них выдаст также опасные балансные флаги, включая `invincibility`.

```text
regionguard.command
regionguard.command.define
regionguard.command.redefine
regionguard.command.remove
regionguard.command.list
regionguard.command.info
regionguard.command.tp
regionguard.command.members
regionguard.command.flag
regionguard.command.flag.greeting
regionguard.command.flag.farewell
regionguard.command.flag.deny-message
regionguard.command.flag.weather
regionguard.command.flag.entry
regionguard.command.flag.interact
regionguard.command.flag.use
regionguard.command.flag.chest-access
regionguard.command.flag.pvp
regionguard.command.flag.passive-entity-damage
regionguard.command.flag.fall-damage
regionguard.command.flag.item-drop
regionguard.command.flag.potion-splash
```

OP уровня 2 и выше проходит командные проверки без этих нод. Подробности и безопасные наборы прав приведены в разделе [«Права»](../permissions/).

## Первый приват

Выделите две противоположные точки деревянным топором WorldEdit или командами:

```text
//pos1
//pos2
```

Создайте регион:

```text
/rg define home
```

`/rg claim home` делает то же самое. Создавший игрок становится владельцем, а стандартные флаги защиты записываются явно из `defaultDeniedFlagsOnClaim` и `defaultAllowedFlagsOnClaim`.

Проверьте результат:

```text
/rg info home
/rg flag home list
```

## Участники и владельцы

Добавьте игрока, который должен строить и пользоваться территорией:

```text
/rg addmember home PlayerName
```

Новый владелец сможет также менять флаги и состав региона:

```text
/rg addowner home PlayerName
```

## Типовые настройки защиты

Полностью запретить PvP внутри региона:

```text
/rg flag home pvp deny
```

Новые приваты уже создаются с запретом для гостей на взаимодействия с дверями, кнопками и сущностями. Для старого региона включите его вручную:

```text
/rg flag home interact deny
```

Запретить вход посторонним и взрывы:

```text
/rg flag home entry deny
/rg flag home tnt deny
/rg flag home creeper-explosion deny
/rg flag home other-explosion deny
```

При `pvp deny` статус владельца или участника не даёт обхода. Ударить игрока в защищённом регионе сможет только администратор с `regionguard.bypass.pvp`, `regionguard.admin.access` или OP уровня 2.

Безопасная зона, где игроки не теряют предметы и не получают урон от мобов:

```text
/rg flag home mob-damage deny
/rg flag home item-drop deny
/rg flag home potion-splash deny
```

## Изменение границ и удаление

Сделайте новое выделение WorldEdit и выполните:

```text
/rg redefine home
```

Владельцы, участники и флаги сохранятся. Для полного удаления используйте `/rg remove home`.
