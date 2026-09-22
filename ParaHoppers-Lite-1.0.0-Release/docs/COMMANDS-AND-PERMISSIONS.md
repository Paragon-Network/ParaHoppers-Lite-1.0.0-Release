# Commands and permissions

`/parahoppers` and `/ph` are equivalent. Help and first argument tab completion show administrative commands to senders with the relevant permission.

| Command | Purpose | Permission |
|---|---|---|
| `/ph help` | Show available commands | None |
| `/ph inspect` | Toggle hopper inspection | `parahoppers.inspect` |
| `/ph link` | Begin linking the targeted hopper | Hopper owner, or `parahoppers.admin` when owner-only linking is enabled |
| `/ph unlink` | Remove the targeted hopper link | In-game player |
| `/ph give <player> <collector> [amount]` | Give a collector hopper | `parahoppers.admin.give` |
| `/ph list` | List registered hoppers | `parahoppers.admin.list` |
| `/ph reload` | Reload configuration and engines | `parahoppers.admin.reload` |
| `/ph stats` | Show TPS and runtime counters | `parahoppers.admin.stats` |

`parahoppers.use` and `parahoppers.place` govern normal hopper use and placement. `parahoppers.admin` is an operator default parent for the administrative commands and an owner override. The command specific admin permissions default to operator. TPS falls back to 20.00 on server APIs without a TPS method; that value is a fallback, not a measurement.
