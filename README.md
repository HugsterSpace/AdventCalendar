# AdventCalendar

High-performance, modern Advent Calendar plugin for Paper and Folia Minecraft servers.

## Features

- **Folia & Paper Native Support**: Fully threaded and safe for Folia region/entity scheduling as well as Paper servers.
- **Storage Backends**: Seamless support for **SQLite**, **MySQL** (HikariCP + Exposed DAO), and **MongoDB**.
- **Flexible Rewards**: Configurable per-day rewards and console commands dispatched upon claiming.
- **Timezone Aware**: Configurable target month, timezone, claim windows, and options for claiming missed days.
- **Join Reminder**: Customizable delayed notification for players when they log in with unclaimed rewards.

---

## Requirements

- **Java**: 21+
- **Server Software**: Paper or Folia 1.20+
- **Optional**: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)

---

## Commands & Permissions

| Command | Permission | Description                                      |
|---|---|--------------------------------------------------|
| `/calendar` (or `/advent`, `/adventcalendar`) | None | Opens the Advent Calendar menu.                  |
| `/calendar open` | None | Opens the Advent Calendar menu.                  |
| `/calendar claim <day>` | None | Claims the reward for a specific day.            |
| `/calendar reload` | `adventcalendar.admin` | Reloads `config.yml` and updates active menus.   |
| `/calendar reset <player>` | `adventcalendar.admin` | Resets calendar progress for a specific player.  |
| `/calendar resetall` | `adventcalendar.admin` | Resets calendar progress for all players.        |
| `/calendar setday <player> <day> <claimed>` | `adventcalendar.admin` | Manually toggles claim state for a player's day. |

---

## Placeholders

| Placeholder | Description |
|---|---|
| `%adventcalendar_current_day%` | Current active calendar day (0 if outside event). |
| `%adventcalendar_total_days%` | Configured total event days. |
| `%adventcalendar_claimed_count%` | Number of days claimed by the player. |
| `%adventcalendar_unclaimed_count%` | Number of unclaimed available days for the player. |
| `%adventcalendar_status_<day>%` | Status of a day (`available`, `claimed`, `locked`, `expired`). |
| `%adventcalendar_countdown_<day>%` | Time remaining until a locked day unlocks (e.g. `02h 15m 30s`). |

---

## Building from Source

Clone the repository and build using Gradle:

```bash
git clone https://github.com/HugsterSpace/AdventCalendar.git
cd AdventCalendar
./gradlew shadowJar
```

The compiled plugin jar will be generated in `build/libs/`.

---

## License

This project is licensed under the [MIT License](LICENSE) - see the [LICENSE](LICENSE) file for details.

Developed by **Hugster Space**.
