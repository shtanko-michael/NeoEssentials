## Сборка

JAVA_HOME=<JDK 25> ./gradlew build                         # все цели
    JAVA_HOME=<JDK 25> ./gradlew :1.21.1-neoforge:build        # одна цель
    python -I tools/lang.py                                   # 12 языковых файлов и проверка всех ключей из кода
    python -I tools/gen_content.py                            # модели, blockstates, лут, рецепт, газ, ачивки, торги 26.x
    python -I tools/art.py install                            # варианты текстур из art/textures/slots.json
    python -I tools/sounds.py install                         # дубли звуков из art/sounds/slots.json
    python -I tools/duo.py 1.21.1-neoforge [--record]         # два клиента на одном сервере: все функции, скриншоты (и кадры GIF)
    python -I tools/build_wiki.py                             # docs/wiki/{en,ru} из README

Файлы данных пишутся один раз в форме 1.21.2+; `buildSrc/src/main/kotlin/DataCompat.kt` при сборке переделывает папки, рецепты и ачивки для старых целей.

## Лицензия

Код — MIT, ассеты (текстуры, звуки, иконка) — CC BY-NC 4.0 (см. [LICENSE](https://github.com/shtanko-michael/farmstead-fart-together/blob/main/LICENSE)). Звуки сделаны в ElevenLabs, текстуры и иконка — в Codex, отобраны вручную.
