## How the auction house works

A separate module of the fork: players list **the whole stack in their main hand** with `/ah sell <price>`, and everyone else buys through the `/ah` GUI. Anything unsold when the timer runs out goes to `/ah expired` and waits there until the seller collects it.

The money flow is the same Vault/`EconomyAPI` that `/pay` uses. The config is `config/auctionhouse.json` (not the `neoessentials/` folder). Data lives in the DataStore collections `auction_listings` / `auction_expired`.

The module is turned off with `modules` (auction house) and `commands.ah`. While the command is disabled, its Brigadier tree is not registered.
