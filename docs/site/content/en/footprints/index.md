<p class="cta-row"><a class="cta" href="https://modrinth.com/mod/farmstead-footprints">⬇ Modrinth</a> <a class="cta cta-soft" href="https://www.curseforge.com/minecraft/mc-mods/farmstead-footprints">CurseForge</a> <a class="cta cta-soft" href="https://github.com/shtanko-michael/farmstead-footprints">GitHub</a></p>

**Realistic footprints for players and mobs.** Walk through snow, sand or mud and your steps stay behind you, pressed into the ground, lit like the blocks around them, fading over time. Wolves leave paw prints, horses leave hooves, chickens leave three-toed scratches. Client-side: install it and it works on any server, vanilla included.

## See it

| | |
| --- | --- |
| ![A trail toward the mountains](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/snow_hero.gif) | ![A wolf pack in fresh snow](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/wolves.gif) |
| Your trail grows behind you | Every mob has its own tracks |
| ![Prints crumbling away](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/crumble.gif) | ![Detailed and pixel style](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/styles.gif) |
| Old prints crumble from the edges in | Two looks: detailed and pixel |
| ![A boat's foam wake](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/wake.gif) | ![Rings where a villager wades](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/ripples.gif) |
| Boats leave a wake | Rings where you wade |
| ![Try it from the settings](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/tryit.gif) | ![Searching blocks in the settings](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gifs/picker.gif) |
| "Try it" stamps prints where you look | Searchable block and mob lists |

More pictures in [docs/media/gallery](https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/docs/media/gallery).

## Features

- **Two looks.** *Detailed* (default): a shadowed hollow and a lit lip, coloured from the ground: blue shadows in snow, darker sand, a wet sheen in dark mud. *Pixel*: the block's own texture pixels pressed in, the north-west wall in shadow like a vanilla texture, matching rotated grass tops and biome tints.
- **Players and 47 mobs**, each with its own foot: boots or bare feet, paws, cat paws, hooves, cloven hooves, bird feet, bear paws, camel feet, the giant feet of golems, ravagers and wardens. Babies and scaled mobs (the scale attribute, Pehkui, Random Mob Sizes) leave prints to their size.
- **Modded mobs** get feet read from their model: the cubes that touch the ground are the feet, two make a biped, more make four, six or eight legs at their real positions. Mobs drawn without vanilla model parts (GeckoLib) fall back to a guess from the hitbox.
- **A real gait.** Two-legged walkers alternate left and right; four-legged animals put the hind foot into the print of the front foot on the same side; rabbits and frogs land in groups of four.
- **Any soft ground.** Every block softer than `surfaces.hardnessGate` takes prints, modded ones too; lists by id or #tag add or exclude blocks. Snow, sand, mud, grass, gravel, soil, clay and soul blocks have their own colour and lifetime. Thin snow layers count.
- **Prints age.** Over the second half of their life they crumble from the edges in (`effects.decay = FADE` just fades them). Rain and snowfall wear exposed prints faster. A print goes when its block is broken or replaced.
- **Water.** Boats leave a foam wake and throw spray, rings spread where you wade, bubbles trail swimmers, wet feet leave damp prints on stone and planks and throw droplets. Fast minecarts spark on the rails.
- **Your own prints** from PNG files or resource packs, see below.
- **Fast.** One draw per frame from geometry cached when a print is placed, culled to the camera's view.
- **Shaders welcome.** Works with Sodium and Iris (tested with Complementary Unbound).
- **Settings for everyone:** tabs, searchable block and mob pickers, an About tab with the update status, "Try it" to see the current settings on the ground, presets in one line, a commented config file, `/footprints reload`.
- **12 languages:** English, Russian, Chinese (Simplified), Portuguese (Brazil), Spanish, German, French, Polish, Ukrainian, Japanese, Korean, Turkish.
