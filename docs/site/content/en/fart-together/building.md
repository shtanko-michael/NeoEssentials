## Building

JAVA_HOME=<JDK 25> ./gradlew build                         # every target
    JAVA_HOME=<JDK 25> ./gradlew :1.21.1-neoforge:build        # one target
    python -I tools/lang.py                                   # write and check the 12 lang files (and every key the code uses)
    python -I tools/gen_content.py                            # models, blockstates, loot, recipe, gas foods, advancements, 26.x trades
    python -I tools/art.py install                            # the texture variants chosen in art/textures/slots.json
    python -I tools/sounds.py install                         # the sound takes chosen in art/sounds/slots.json
    python -I tools/duo.py 1.21.1-neoforge [--record]         # two clients on one server: every feature, screenshots (and GIF frames)
    python -I tools/build_wiki.py                             # docs/wiki/{en,ru} from the READMEs

Data files are written once in the 1.21.2+ form; `buildSrc/src/main/kotlin/DataCompat.kt` rewrites folder names, recipe and advancement shapes for older targets while the jar is built.

## License

Code — MIT, assets (textures, sounds, icon) — CC BY-NC 4.0 (see [LICENSE](https://github.com/shtanko-michael/farmstead-fart-together/blob/main/LICENSE)). Sounds made with ElevenLabs, textures and icon with Codex, picked by hand.
