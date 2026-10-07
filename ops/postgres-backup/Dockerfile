FROM postgres:18-alpine

RUN apk add --no-cache aws-cli

COPY backup.sh /usr/local/bin/nexa-postgres-backup
RUN chmod 0755 /usr/local/bin/nexa-postgres-backup

ENTRYPOINT ["/usr/local/bin/nexa-postgres-backup"]
