#!/bin/sh
set -eu

if [ -z "${STORAGE_ACCESS_KEY:-}" ] || [ -z "${STORAGE_SECRET_KEY:-}" ]; then
    echo "storage: defina STORAGE_ACCESS_KEY y STORAGE_SECRET_KEY" >&2
    exit 1
fi

cat > /tmp/s3.json <<EOF
{
  "identities": [
    {
      "name": "tusalud",
      "credentials": [
        { "accessKey": "${STORAGE_ACCESS_KEY}", "secretKey": "${STORAGE_SECRET_KEY}" }
      ],
      "actions": ["Admin", "Read", "Write", "List", "Tagging"]
    }
  ]
}
EOF

exec weed server \
    -dir=/data \
    -ip.bind=0.0.0.0 \
    -master.volumeSizeLimitMB="${STORAGE_VOLUME_MB:-1024}" \
    -volume.max="${STORAGE_MAX_VOLUMES:-30}" \
    -filer \
    -s3 \
    -s3.config=/tmp/s3.json \
    -s3.port=8333
