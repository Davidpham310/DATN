FROM minio/minio:latest

# Create necessary directories
RUN mkdir -p /data

# Expose ports
EXPOSE 9000 9001

# Health check
HEALTHCHECK --interval=30s --timeout=20s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:9000/minio/health/live || exit 1

# Start MinIO server
CMD ["server", "/data", "--console-address", ":9001"]
