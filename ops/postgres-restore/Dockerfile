FROM postgres:18-alpine

RUN apk add --no-cache aws-cli

COPY restore.sh /usr/local/bin/nexa-postgres-restore
RUN chmod 0755 /usr/local/bin/nexa-postgres-restore

ENTRYPOINT ["/usr/local/bin/nexa-postgres-restore"]
