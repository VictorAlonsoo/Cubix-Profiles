# CubixProfiles

Plugin de perfiles para Paper. Muestra el equipamiento y los cosméticos equipados de cualquier jugador — online u offline — en una GUI configurable. Sin estadísticas, sin inventario completo.

**Requisitos:** Paper 1.21.4+ · Java 21

---

## Comandos

| Comando | Descripción | Permiso |
|---|---|---|
| `/profile` | Abre tu propio perfil | `cubixprofiles.profile` |
| `/profile <jugador>` | Abre el perfil de otro jugador | `cubixprofiles.profile` |
| `/profile settings view` | Activa / desactiva la visibilidad de tu perfil | `cubixprofiles.settings` |
| `/profile reload` | Recarga configuración y menús | `cubixprofiles.admin.reload` |

Los admins con `cubixprofiles.admin.bypass` pueden ver perfiles ocultos y usar `/profile` en mundos restringidos.

---

## Dos menús independientes

| Archivo | Se abre cuando… |
|---|---|
| `menu-self.yml` | usas `/profile` sin argumentos |
| `menu-other.yml` | usas `/profile <jugador>` |

Cada archivo tiene su propio título, número de filas, slots de equipamiento, ítems y acciones. Cambias uno sin afectar el otro.

---

## Qué muestra el perfil

- **Equipamiento** — casco, peto, grebas, botas, mano principal, mano secundaria
- **Cosméticos** — integración con HMCCosmetics (opcional)
- **Prefix de rango** — integración con LuckPerms o Vault (opcional)
- Funciona con jugadores **offline** — los datos se persisten en la base de datos

---

## Personalización del menú

### Número de filas
```yaml
rows: 4   # 1–6 filas (9–54 slots)
```

### Head configurable
```yaml
head:
  slot: 4
  name: "<white><prefix><player>"
  lore:
    - "<#A5ACB8>Viewing <#5AB0FF><player>"
  hide-tooltip: true
```

### Ítems personalizados
```yaml
items:
  filler:
    slots: "0-35"
    material: GRAY_STAINED_GLASS_PANE
    name: " "

  my_button:
    slot: 31
    material: COMPASS
    name: "<bold><#FFFFFF>Stats"
    lore:
      - "<#A5ACB8>Click to view stats."
    actions:
      - "[player] stats <player>"
      - "[close]"
```

### Texture heads (minecraft-heads.com)
```yaml
  my_head:
    slot: 4
    material: CUSTOM_HEAD
    texture: "eyJ0ZXh0dXJlcyI6..."   # campo "Value" de minecraft-heads.com
    name: "<#5AB0FF><player>'s Profile"
```

---

## Acciones en ítems

Las acciones se ejecutan cuando el jugador hace clic en el ítem.

| Tipo | Comportamiento |
|---|---|
| `[player] <comando>` | el viewer ejecuta el comando (sin `/`) |
| `[console] <comando>` | la consola ejecuta el comando |
| `[message] <texto>` | envía un mensaje MiniMessage al viewer |
| `[close]` | cierra el inventario |

**Placeholders disponibles en acciones y en name/lore:**

| Placeholder | Valor |
|---|---|
| `<player>` | nombre del dueño del perfil (el **target** en `menu-other.yml`) |
| `<viewer>` | nombre del jugador que abrió el menú |
| `<prefix>` | prefix del dueño del perfil |
| `%papi%` | cualquier placeholder de PlaceholderAPI |

Ejemplo — botón de teleport en `menu-other.yml`:
```yaml
  teleport:
    slot: 31
    material: ENDER_PEARL
    name: "<bold><#FFFFFF>Teleport"
    actions:
      - "[player] tp <player>"   # <player> = el target que estás viendo
      - "[close]"
```

---

## Sonidos

```yaml
# Sonido global (cualquier clic con ítem)
click-sound:
  enabled: true
  sound: "minecraft:ui.button.click"
  source: MASTER
  volume: 1.0
  pitch: 1.0

# Sonido por ítem (sobreescribe el global)
items:
  my_button:
    slot: 49
    material: PAPER
    name: "..."
    sound:
      sound: "minecraft:entity.experience_orb.pickup"
      source: MASTER
      volume: 1.0
      pitch: 1.2
```

---

## PlaceholderAPI

Registra la expansión `cubixprofiles` automáticamente si PAPI está instalado.

| Placeholder | Valor |
|---|---|
| `%cubixprofiles_helmet%` | material del casco (`DIAMOND_HELMET` / `none`) |
| `%cubixprofiles_chestplate%` | material del peto |
| `%cubixprofiles_leggings%` | material de las grebas |
| `%cubixprofiles_boots%` | material de las botas |
| `%cubixprofiles_mainhand%` | material en mano principal |
| `%cubixprofiles_offhand%` | material en mano secundaria |
| `%cubixprofiles_view_status%` | `true` si el perfil es visible, `false` si está oculto |

---

## Integraciones opcionales

| Plugin | Qué aporta |
|---|---|
| **LuckPerms** | Prefix del rango en `<prefix>`. Se actualiza automáticamente al cambiar el rango. |
| **Vault** | Prefix del rango si LuckPerms no está instalado. |
| **HMCCosmetics** | Muestra los cosméticos equipados. Los slots se configuran con el prefijo `hmcc_`. |
| **PlaceholderAPI** | Ver tabla anterior. |

---

## Storage

```yaml
storage:
  type: sqlite   # sqlite | mysql
  sqlite:
    file: data/profiles.db
  mysql:
    host: localhost
    port: 3306
    database: cubix
    username: root
    password: ""
```

SQLite crea el archivo automáticamente. MySQL requiere la base de datos creada previamente; las tablas se generan solas.

---

## Mundos restringidos

```yaml
disabled-worlds:
  - world_nether
  - world_the_end
```

En estos mundos `/profile` está desactivado para jugadores sin `cubixprofiles.admin.bypass`.
