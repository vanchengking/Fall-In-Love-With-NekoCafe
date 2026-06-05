# RDS migration workflow

The backend now uses Flyway to apply SQL files in `db/migrations` to the database configured by `SPRING_DATASOURCE_URL`.

## What happens on startup

When `docker compose up -d --build` starts the `backend` service:

1. Spring Boot connects to the database in `.env`.
2. Flyway reads migration files from the mounted `db/migrations` directory.
3. Flyway creates or updates `flyway_schema_history`.
4. Missing migrations, such as `V001__init.sql` and `V002__d01_upgrade.sql`, are executed once in filename order.

This means schema changes are synchronized to RDS by application startup. Rebuilding the Docker image alone does not modify the database; the migration runs when the backend container starts.

## Team setup

Each developer should create a local `.env` from `.env.example` and fill in their own RDS account:

```env
SPRING_DATASOURCE_URL=jdbc:mysql://<rds-host>:3306/nekocafe?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&useUnicode=true
SPRING_DATASOURCE_USERNAME=<rds-user>
SPRING_DATASOURCE_PASSWORD=<rds-password>
SPRING_FLYWAY_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
```

Do not commit `.env`.

## Existing RDS databases

`SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` is intended for an RDS database that already has earlier tables but does not yet have a Flyway history table. Flyway will mark the existing schema as the baseline and then apply later migrations.

If the RDS database was manually changed and is only partially upgraded, Flyway can still fail on duplicate columns or indexes. In that case either:

- reset the database and let Flyway apply all migrations from an empty schema, or
- manually repair the partial schema, then rerun the backend.

## Local MySQL

The local `mysql` and `adminer` services are now behind the `local-db` Compose profile:

```powershell
docker compose --profile local-db up -d --build
```

RDS usage does not need that profile.
