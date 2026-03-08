# InfiniteBlocks

A Paper plugin that gives players infinite-use placeable blocks. Blocks placed with an infinite item are automatically restored to the player's hand, so they never run out. Blocks are organized into color-themed schemes with custom gradient item names and lore.

**Requires:** Paper 1.21.1+

## Download

Head to the [Releases](../../releases) tab to download the latest version.

## Commands

| Command | Description |
|---|---|
| `/ib get` | Open the block selector GUI |
| `/ib get <player> <block> <amount>` | Give a player infinite blocks directly |
| `/ib scheme` | Open the scheme management GUI |
| `/ib scheme create <name>` | Create a new color scheme |
| `/ib scheme remove <scheme>` | Delete a scheme |
| `/ib scheme addblock <scheme> [material]` | Add a block to a scheme |
| `/ib scheme removeblock <scheme> [material]` | Remove a block from a scheme |
| `/ib reload` | Reload config |

Aliases: `/ib`, `/infiniteblock`

## Permissions

| Permission | Default | Description |
|---|---|---|
| `infiniteblocks.use` | OP | Use the `/ib` command |
| `infiniteblocks.admin` | OP | Full access |
| `infiniteblocks.reload` | OP | Reload config |

## Schemes

Schemes define the color gradient and lore applied to infinite block items. Each block is assigned to one scheme; when placed, the item auto-updates to match the currently assigned scheme. Schemes are configured in `plugins/InfiniteBlocks/schemes.yml`.

You can create schemes interactively via `/ib scheme create` using a [Birdflop](https://www.birdflop.com/resources/rgb/) gradient string, or manage them through the in-game GUI with `/ib scheme`.
