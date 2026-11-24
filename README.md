# Prison

ColdMC's prison-mines plugin for Paper 1.21.11 (FastAsyncWorldEdit required; WorldGuard, Nexo,
MythicMobs and MMOCore optional). It pastes each prison schematic, boxes it in barriers, refills
its mining cube with a weighted random mix (vanilla blocks or Nexo custom blocks), pays MMOCore
Mining experience for blocks MMOCore does not list, and runs each prison as a small dungeon with
mobs, elites and rare bosses.

Mines are defined in `plugins/Prison/mines.yml` (generated elsewhere); cubes re-marked in game
with `/prison setcube <id>` are kept in `overrides.yml`. Build with `./build.sh`.

Commands (`prison.admin`): `/prison list | info | tp | build <id|all> [nopaste] | reset | boss | setcube | cancel | reload`.
