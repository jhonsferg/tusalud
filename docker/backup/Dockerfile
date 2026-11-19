ARG POSTGRES_IMAGE=postgres:18-alpine
FROM ${POSTGRES_IMAGE}
LABEL org.opencontainers.image.title="tusalud-backup" \
      org.opencontainers.image.description="Respaldos cifrados de la base y del almacén de TuSalud con prueba de restauración" \
      org.opencontainers.image.source="https://github.com/jhonsferg/tusalud" \
      org.opencontainers.image.vendor="CodeSoftLabs"
ENV TZ=America/Lima \
    RESPALDOS_DIR=/respaldos
RUN apk add --no-cache rclone curl openssl jq tzdata coreutils \
    && mkdir -p /respaldos /opt/respaldos
COPY scripts/ /opt/respaldos/
RUN sed -i 's/\r$//' /opt/respaldos/*.sh && chmod 755 /opt/respaldos/*.sh
VOLUME ["/respaldos"]
HEALTHCHECK --interval=60s --timeout=5s --start-period=10s --retries=3 \
    CMD pgrep crond > /dev/null || exit 1
ENTRYPOINT ["/opt/respaldos/entrypoint.sh"]
