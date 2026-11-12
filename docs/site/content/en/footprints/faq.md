## Compatibility

- **Sodium, Iris**: supported. Prints are drawn after the world, so shader packs light them; `footprints.lift` raises them if a shader pack's deep ground hides them.
- **Other footprint mods** (FootprintParticle, Soft Imprints): both will draw their prints; keep one.
- **Modded blocks and mobs**: soft modded blocks take prints by hardness, modded mobs get feet from their model; both can be tuned by id or #tag.

## FAQ

**Do other players see my prints?** Everyone who has the mod sees everyone's prints, players and mobs alike, computed on their own client. Players without the mod see nothing.

**Is it heavy?** Prints are drawn in one batch per frame and capped by `maxPrints`. On a weak machine pick the `performance` preset.

**Prints on grass?** Yes: grass, moss and other soft blocks take prints. Exclude any block in the Ground tab.

## Planned

Trails worn into grass that stay (server module with LuckPerms permissions), Minecraft 1.16.5–1.19.
