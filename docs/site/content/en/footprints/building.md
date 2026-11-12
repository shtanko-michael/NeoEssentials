## Building from source

Gradle runs on **JDK 25** (Stonecutter 0.10); the Java 17 and 21 toolchains are downloaded automatically.

```
./gradlew build                          # all six targets -> versions/<target>/build/libs
./gradlew :1.21.1-neoforge:runClient     # dev client of one target
python -I tools/lang.py --check          # all 12 language files are complete
```

## License

Code: [MIT](https://github.com/shtanko-michael/farmstead-footprints/blob/main/LICENSE). Assets (textures, images, video): [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/). Modpacks may include the mod freely.

Made by [Farmstead](https://farmsteadminecraft.online). Bugs and ideas: [GitHub Issues](https://github.com/shtanko-michael/farmstead-footprints/issues).
