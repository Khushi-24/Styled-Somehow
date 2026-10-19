#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${MYSQL_OPTION_FILE:?Set path to a private MySQL client option file}"
: "${DB_NAME:?Set the database name}"
: "${MEDIA_DIRECTORY:?Set the persistent media directory}"
: "${BACKUP_DIRECTORY:?Set a private backup directory outside the web root}"
: "${BACKUP_GPG_RECIPIENT:?Set the public-key recipient for encrypted backups}"
: "${BACKUP_REMOTE:?Set an off-server rclone remote/path}"
[[ "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]] || { echo "Invalid database name" >&2; exit 1; }
[[ -f "$MYSQL_OPTION_FILE" && -d "$MEDIA_DIRECTORY" ]] || { echo "MySQL options or media directory missing" >&2; exit 1; }
for binary in mysqldump gpg tar rclone sha256sum; do command -v "$binary" >/dev/null || { echo "Required command missing: $binary" >&2; exit 1; }; done
mkdir -p "$BACKUP_DIRECTORY"
backup_stage=$(mktemp -d "$BACKUP_DIRECTORY/.backup-XXXXXXXX")
trap 'rm -rf -- "$backup_stage"' EXIT
mysqldump --defaults-extra-file="$MYSQL_OPTION_FILE" --single-transaction --routines --triggers --hex-blob "$DB_NAME" | gpg --batch --yes --encrypt --recipient "$BACKUP_GPG_RECIPIENT" --output "$backup_stage/database.sql.gpg"
tar -C "$MEDIA_DIRECTORY" -cf - . | gpg --batch --yes --encrypt --recipient "$BACKUP_GPG_RECIPIENT" --output "$backup_stage/media.tar.gpg"
(cd "$backup_stage" && sha256sum database.sql.gpg media.tar.gpg > SHA256SUMS)
backup_name="$(date -u +%Y%m%dT%H%M%SZ)-${RANDOM}"
# Remote copy must succeed before this run is reported successful. No retention deletion is automatic.
rclone copy "$backup_stage" "${BACKUP_REMOTE%/}/$backup_name"
mv "$backup_stage" "$BACKUP_DIRECTORY/$backup_name"
trap - EXIT
echo "Encrypted database and media backup completed: $backup_name"
