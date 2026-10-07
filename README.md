# Kestrel

Client-side Fabric mod for Minecraft Java. While you draw a bow it shows the predicted arrow trajectory, an impact marker, boxes around lockable entities in range (turning green with `TARGET` when the arrow's path hits them) and a full-charge indicator. No server install needed.

| Minecraft | Java | Directory |
|-----------|------|-----------|
| 1.21.11   | 21   | `mc-1.21.11` |
| 26.3      | 25   | `mc-26.3` |

`shared/` holds the source used by both versions (physics in `core`, drawing in `sight`). Settings live in `KestrelSettings.java`.

Build (needs JDK 25 to run Gradle): `.\gradlew.bat build` inside a version directory. Jar: `build/libs/kestrel-1.0.0.jar`.

Status: Step 1 of 5 (see `brief/`). Builds on both versions; in-game testing pending.
