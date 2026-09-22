# Compatibility

Install the **legacy** JAR on pre-1.13 servers and the **modern** JAR on 1.13+ servers. Never install both on one server. The modern descriptor declares `api-version: '1.13'`; the legacy descriptor intentionally has no API version.

| Minecraft version | Artifact | Reported live test |
|---|---|---|
| 1.7.10 | Legacy | Passed |
| 1.8.9 | Legacy | Passed |
| 1.12.2 | Legacy | Passed |
| 1.13.2 | Modern | Passed |
| 1.16.5 | Modern | Passed |
| 1.20.6 | Modern | Passed |
| 1.21.8 | Modern | Passed |
| 26.1+ | Modern candidate | Unverified |

These are the user's reported Build 8 live tests. They establish results for the listed versions, not every intermediate patch, every server distribution, or the final rebuilt JARs. Use the Java version required by the Minecraft server itself.
