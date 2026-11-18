#!/bin/sh
set -eu

maestro="${STORAGE_MASTER:-storage:9333}"
buckets="${STORAGE_BUCKETS:-avatares imagenes documentos reportes plantillas}"

for intento in $(seq 1 30); do
    if wget -q -O /dev/null "http://${maestro}/cluster/status"; then
        break
    fi
    sleep 2
done

for bucket in $buckets; do
    salida=$(echo "s3.bucket.create -name ${bucket}" | weed shell -master="${maestro}" 2>&1 || true)
    if echo "$salida" | grep -qiE "created bucket|already exist|exists"; then
        echo "bucket ${bucket}: listo"
    else
        echo "bucket ${bucket}: ${salida}" >&2
        exit 1
    fi
done
