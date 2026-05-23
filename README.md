# CubixProfiles

Visor de perfiles de jugador virtual para Paper. Muestra el equipamiento y los
cosméticos equipados de jugadores online y offline en una GUI configurable.
Minimalista por diseño: sin estadísticas, sin inventario completo, sin
sincronización agresiva.

---

## Stack

| | |
|---|---|
| Servidor objetivo | Paper 1.21.4 – 26.x |
| API de compilación | Paper 1.21.11 |
| Java | 21 |
| Build | Maven |
| groupId | `com.victoralonso` |
| artifactId | `cubixprofiles` |

Compatibilidad multiversión mediante `CapabilityDetector`: features nuevas
(`item-model`, `tooltip-style`, `hide-tooltip`) se detectan por reflection al
arranque. Fallback silencioso si no existen.

---

## Comandos y permisos

| Comando | Descripción | Permiso | Default |
|---|---|---|---|
| `/profile` | Abre tu propio perfil | `cubixprofiles.profile` | todos |
| `/profile <jugador>` | Abre el perfil de otro jugador (online u offline) | `cubixprofiles.profile` | todos |
| `/profile reload` | Recarga config, mensajes y layout del menú | `cubixprofiles.reload` | op |

---

## Ítems del menú — acciones y sonido

Cada ítem declarado en la sección `items:` de `menu.yml` puede tener acciones
ejecutables al hacer clic y un sonido personalizado.

### Acciones

```yaml
actions:
  - "[player]  <command>"   # el viewer ejecuta un comando (sin /)
  - "[console] <command>"   # la consola ejecuta un comando
  - "[message] <text>"      # envía texto MiniMessage al viewer
  - "[close]"               # cierra el inventario
```

**Placeholders disponibles en acciones:**

| Placeholder | Resuelve a |
|---|---|
| `<player>` | username del dueño del perfil |
| `<viewer>` | username del jugador que hace clic |
| `%papi%` | cualquier placeholder de PlaceholderAPI, evaluado para el viewer |

### Sonido de clic

Se configura a dos niveles:

- **Global** (`click-sound:` en `menu.yml`): se reproduce en cualquier slot que
  contenga un ítem. Si `enabled: false`, no se reproduce ningún sonido por defecto.
- **Por ítem** (`sound:` dentro de cada ítem): sobreescribe el sonido global para
  ese ítem concreto.

```yaml
# Global
click-sound:
  enabled: true
  sound: "minecraft:ui.button.click"  # vanilla o resource pack
  source: MASTER  # MASTER | MUSIC | RECORD | WEATHER | BLOCK | HOSTILE | NEUTRAL | PLAYER | AMBIENT | VOICE
  volume: 1.0
  pitch: 1.0

# Por ítem
items:
  info:
    slot: 49
    material: COMPASS
    name: "<#FFFFFF>Stats"
    sound:
      sound: "minecraft:entity.experience_orb.pickup"
      source: MASTER
      volume: 1.0
      pitch: 1.2
    actions:
      - "[player] stats <player>"
      - "[close]"
```

El campo `source` controla qué control de volumen del cliente afecta al sonido,
lo que lo hace compatible con cambios futuros de versión y con resource packs que
redefinan las categorías.

---

## Integraciones opcionales

### PlaceholderAPI
Detectado automáticamente al arranque. Registra la expansión `cubixprofiles`.

| Placeholder | Valor |
|---|---|
| `%cubixprofiles_helmet%` | Material del casco |
| `%cubixprofiles_chestplate%` | Material del peto |
| `%cubixprofiles_leggings%` | Material de las grebas |
| `%cubixprofiles_boots%` | Material de las botas |
| `%cubixprofiles_mainhand%` | Material en mano principal |
| `%cubixprofiles_offhand%` | Material en mano secundaria |

PlaceholderAPI también se resuelve en las acciones de los ítems del menú
(cualquier `%placeholder%` se evalúa para el jugador que hace clic).

### HMCCosmetics
Detectado automáticamente si está instalado y `cosmetics.providers.hmccosmetics.enabled: true`.
- Captura los cosméticos equipados al entrar, al equipar/desquitar y cada `update-interval`.
- Persiste los cosméticos en la tabla `profile_cosmetics` para mostrarlos en perfiles offline.
- Los slots se configuran en `cosmetic-slots:` con el prefijo `hmcc_`.

---

## Storage

| Backend | Driver | Activación |
|---|---|---|
| SQLite | `org.xerial:sqlite-jdbc` | `storage.type: sqlite` (default) |
| MySQL / MariaDB | `com.mysql:mysql-connector-j` + HikariCP | `storage.type: mysql` |

Los drivers los descarga Paper en runtime (`libraries:` en `paper-plugin.yml`).
No se incluyen en el jar.

**Tablas:**
- `profiles` — snapshot de equipamiento por UUID + username.
- `profile_cosmetics` — cosmético por UUID + slot (`PRIMARY KEY (uuid, slot)`).
  Extensible sin `ALTER TABLE` al añadir nuevos proveedores.

Serialización: `ItemStack.serializeAsBytes()` / `deserializeBytes()` almacenado como `BLOB`.


### Decisiones de diseño

- `ProfileSnapshot.of(Player)` no accede a cosméticos. Usar siempre
  `profileService.capture(player)` para snapshots completos.
- `PlayerProfile` (skin) no entra en el record. Se resuelve en render, async si es offline.
- El GUI es estático: se construye una vez al abrir. El refresh es manual
  (`/profile`) o por `update-interval`, nunca por tick.
- Los sonidos se reproducen solo si el slot contiene un ítem real (no slots vacíos).
- Acciones de tipo `[player]` y `[console]` se despachan en el siguiente tick para
  evitar conflictos con el estado del inventario dentro del evento cancelado.
- `CosmeticsProvider` permite añadir futuros plugins sin tocar el código base.
