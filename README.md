# Retro Game
An OGame clone in retro style

[![CI](https://github.com/retro-game/retro-game/workflows/CI/badge.svg)](https://github.com/retro-game/retro-game/actions)

## Running
You can run retro-game on Docker Compose:
```shell script
docker-compose up
```
This will setup a development environment, where you can test the game.
The server should be running on [http://127.0.0.1:8080](http://127.0.0.1:8080).

Note that this setup is not ready for production!
This setup may expose ports to database and redis.
Make sure you configure it properly before running on production.
Check [etc](etc) directory if you are looking for systemd service and nginx configs.

## Building

Use the Gradle wrapper to build and test the application:

```shell
./gradlew test
./gradlew bootJar
```

The native battle engine can be built with:

```shell
./gradlew buildBattleEngine
```

## Database backup and restore

Administrators can download and restore PostgreSQL backups from the Database
page in the admin panel. The application runtime must have `pg_dump`,
`pg_restore`, and `psql` installed. Both provided Docker images include these
tools.

Restores are atomic and overwrite the database objects contained in the backup.
The uploaded file size is limited to 1 GiB by default; this can be changed with
`spring.servlet.multipart.max-file-size`,
`spring.servlet.multipart.max-request-size`, and
`retro-game.database-backup.max-size`.

### Upgrading an existing Docker Compose installation

The Compose configuration uses PostgreSQL 17 and Redis 8. PostgreSQL data files
cannot be reused directly across major versions. Before upgrading from the old
PostgreSQL 12 configuration:

1. Download a database backup from the admin panel.
2. Stop the old stack with `docker compose down` without the `--volumes` option.
3. Start the upgraded stack with `docker compose up -d --build`.
4. Copy the backup into PostgreSQL and restore it:

   ```shell
   docker compose cp retro-game-backup.dump postgres:/tmp/retro-game-backup.dump
   docker compose exec postgres pg_restore \
     --clean --if-exists --no-owner --no-privileges --single-transaction \
     --username=postgres --dbname=retro-game /tmp/retro-game-backup.dump
   ```

Redis contains cache and session data and does not require migration. Existing
sessions will be lost when its data is reset.

## Community & server
There is a testing server: [https://retro-game.org](https://retro-game.org).

If you want to chat about the game or development, see: [https://discord.gg/FJ6rK4V](https://discord.gg/FJ6rK4V).
