## Сборка из исходников

Gradle запускается на **JDK 25** (Stonecutter 0.10); тулчейны Java 17 и 21 скачиваются сами.

```
./gradlew build                          # все шесть целей -> versions/<цель>/build/libs
./gradlew :1.21.1-neoforge:runClient     # клиент для разработки одной цели
python -I tools/lang.py --check          # все 12 языковых файлов полные
```

## Лицензия

Код — [MIT](https://github.com/shtanko-michael/farmstead-footprints/blob/main/LICENSE). Ассеты (текстуры, картинки, видео) — [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/). Включать мод в сборки можно свободно.

Сделано [Farmstead](https://farmsteadminecraft.online). Ошибки и идеи — [GitHub Issues](https://github.com/shtanko-michael/farmstead-footprints/issues).
