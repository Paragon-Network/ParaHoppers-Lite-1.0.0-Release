# ParaHoppers Lite 1.0.0

ParaHoppers Lite provides collector hoppers with XP upgrades, item filters, hopper links, and a virtual diamond mining tool. Mining protects bedrock and storage blocks. Hopper state, filters, links, and mining settings persist across restarts. Lite has no licensing requirement or license check.

## Build

Open the root `pom.xml` in IntelliJ and run the Maven `package` goal. The plugin source targets Java 8 bytecode. Build output is:

- `legacy/target/Para-Hoppers-Lite-1.0.0-legacy.jar` for Minecraft 1.7.10 through 1.12.2.
- `modern/target/Para-Hoppers-Lite-1.0.0-modern.jar` for Minecraft 1.13.2 and later tested modern versions.

Install exactly one JAR in the server's `plugins` folder. Remove older ParaHoppers JARs first. Back up the `plugins/ParaHoppers` data folder before upgrading. The plugin name and persisted data formats are unchanged.

## Use

Use `/ph help` for commands. `/ph stats` requires `parahoppers.admin.stats` and shows runtime counters. Right-click a ParaHopper to manage upgrades, mining, and filters. See [commands and permissions](docs/COMMANDS-AND-PERMISSIONS.md) and [compatibility](docs/COMPATIBILITY.md).

## Release status

The user reported live passes on 1.7.10, 1.8.9, 1.12.2, 1.13.2, 1.16.5, 1.20.6, and 1.21.8 with the matching legacy or modern artifact. Version 26.1+ has not been verified. This source package restores `/ph stats` to the supplied Build 8 source; compile and test the final artifacts before distribution.
