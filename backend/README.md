# Styled Somehow backend

Java 21 / Spring Boot backend, built with Gradle and backed by MySQL. Database changes are managed by Flyway.

## Reference documents

- [Database table reference](docs/DATABASE_REFERENCE.md) — what every table stores, relationships, indexes, deletion rules and read-only inspection queries.
- [Admin and local setup](../docs/ADMIN_SETUP.md)
- [Costing and profit estimates](../docs/COSTING_AND_PROFIT.md)
- [Deployment, backups and recovery](../deploy/README.md)
- [Audit remediation status](../docs/AUDIT_FIX_STATUS.md)

The database reference describes migrations through V20. The SQL files in [`src/main/resources/db/migration`](src/main/resources/db/migration) remain the source of truth; the migrations recorded in your database determine which changes are actually installed.
