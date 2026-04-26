# Herd Instinct

Architectury multi-loader setup for Minecraft `1.21.1` with:

- `common` shared gameplay and config code
- `fabric` Fabric loader implementation
- `neoforge` NeoForge loader implementation
- Parchment mappings layered on Mojang mappings

## Build

```powershell
.\gradlew build
```

Loader-specific jars are produced in:

- `fabric/build/libs`
- `neoforge/build/libs`

## Notes

- Shared herd panic logic lives in `common`.
- Loader event registration is isolated to the platform modules.
- Fabric integrates the config screen through Mod Menu when it is present.
