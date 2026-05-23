# CubixProfiles

Visor de perfiles de jugador completamente virtual para Paper. Muestra el
equipamiento (casco, peto, grebas, botas, mano principal, mano secundaria) de
jugadores online y offline mediante una GUI configurable. Sin estadísticas, sin
inventario completo, sin sincronización agresiva.

## Stack

- Paper API 1.21.11 (base de compilación)
- Java 21
- Maven
- groupId `com.victoralonso` · artifactId `cubixprofiles`
- Compatibilidad runtime: 1.21.4 hasta 26.x mediante detección de capacidades

## Principios

- Snapshot minimalista: solo lo que la GUI muestra.
- Capacidades, no versiones: una detección al arranque, cacheada, inmutable.
- Sin adapters NMS, sin paquetes por versión, sin reflection constante.
- `ItemFactory` como núcleo único de construcción de ítems.
- Storage abstracto e intercambiable.
- Mensajes por tokens semánticos (MiniMessage + `styles`).

## Arquitectura (base)

```
com.victoralonso.cubixprofiles
├── CubixProfiles                main
├── command/
│   └── ProfileCommand
├── profile/
│   ├── ProfileService           cache + orquestación storage
│   ├── ProfileSnapshot          record
│   ├── ProfileListener          join/quit/kick -> capturar snapshot
│   └── storage/
│       ├── StorageManager       interfaz
│       └── YamlStorage          primer backend
├── menu/
│   ├── ProfileMenu              holder + render, virtual inventory
│   ├── ItemFactory              núcleo: snapshot + config -> ItemStack
│   └── MenuLayout               lee menu.yml: title, size, slots
├── compatibility/
│   └── CapabilityDetector       item-model, tooltip-style, hide-tooltip
├── message/
│   └── MessageService           MiniMessage + styles TagResolver
└── config/
    └── ConfigManager
```

## PlayerProfile vs ProfileSnapshot

- `PlayerProfile` (`org.bukkit.profile.PlayerProfile`) = identidad + skin. Es la
  cabeza del GUI. Resoluble async para offline vía `update()`.
- `ProfileSnapshot` = equipamiento persistido en storage.
- Complementarios: la skin se resuelve en render (cacheable); el equipamiento
  sale del snapshot guardado. `PlayerProfile` no entra en el record.

```java
public record ProfileSnapshot(
        UUID uniqueId,
        String username,      // cache denormalizada para lectura offline sin Mojang
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        ItemStack mainHand,
        ItemStack offHand
) {}
```

## Compatibilidad multiversión

- Compila contra 1.21.11 con `<maven.compiler.release>21</maven.compiler.release>`.
- El jar corre sin recompilar en servidores 26.x: Java es retrocompatible y Paper
  mantiene estabilidad de API. El requisito de Java 25 es del servidor en runtime,
  no de tu compilación.
- Features nuevas (item-model, tooltip-style, hide-tooltip) se detectan en
  `CapabilityDetector` por reflection una sola vez al arranque. Fallback silencioso
  si no existen.
- No compilar contra la API 26.1 salvo necesidad real de símbolos exclusivos de esa
  versión; en ese caso, detectarlos por reflection en vez de fijar el target.

## Serialización de ítems

- `ItemStack.serializeAsBytes()` / `ItemStack.deserializeBytes(byte[])`.
- Maneja data components correctamente entre versiones. Mejor que `serialize()` de
  mapa para ítems modernos.
- En `YamlStorage`, los bytes se guardan en Base64.

## Roadmap

### Fase 0 — Scaffolding
`pom.xml`, `paper-plugin.yml`, `CubixProfiles`, `MessageService`,
`messages_en.yml`, `config.yml`. El plugin carga y registra `/profile`.

### Fase 1 — Snapshot + Storage
`ProfileSnapshot`, `StorageManager`, `YamlStorage`, serialización de `ItemStack`,
`ProfileListener` (join/quit/kick), `ProfileService` con `ConcurrentHashMap`.

### Fase 2 — GUI
`menu.yml` (title/size/slots), `MenuLayout`, `ProfileMenu` (virtual inventory
holder), `ItemFactory`, `ProfileCommand` `/profile [player]`. `CapabilityDetector`
aplicado en `ItemFactory`.

### Fase 3 — Offline
Resolución de UUID/username y skin vía `PlayerProfile.update()` async. Lectura de
snapshot desde storage para jugadores no conectados.

### Fase 4+ — Extensiones (no ahora)
`ItemDecorator` pipeline · `CustomItemProvider` registry (ItemsAdder/Nexo/
CraftEngine) · PlaceholderAPI · backends H2/MySQL/MariaDB · multi-idioma · refresh
configurable.

## Build

```
mvn clean install
```

Salida: `target/cubixprofiles-<version>.jar`

## Estado actual

Fase 0 pendiente.
