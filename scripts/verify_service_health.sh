#!/bin/bash

# This script is used by AWS CodeDeploy as an AfterAllowTraffic hook
# to verify that the newly deployed application is healthy.

# The service is expected to be available at medical-register-service on port 80
# (as defined in k8s/service.yaml) and expose /actuator/health.

SERVICE_ENDPOINT="http://medical-register-service/actuator/health"
MAX_ATTEMPTS=10
SLEEP_DURATION=10 # seconds

echo "Starting health check for $SERVICE_ENDPOINT..."

for i in $(seq 1 $MAX_ATTEMPTS); do
  echo "Attempt $i of $MAX_ATTEMPTS: Curling $SERVICE_ENDPOINT"
  # Use -f to fail fast if the HTTP status code indicates an error (>=400)
  # Use -s for silent mode, -L to follow redirects
  HTTP_RESPONSE=$(curl -s -L -w "%{http_code}" -o /dev/null "$SERVICE_ENDPOINT")

  if [ "$HTTP_RESPONSE" -eq 200 ]; then
    echo "Health check PASSED. Service is UP. HTTP Status: $HTTP_RESPONSE"
    exit 0
  fi
  echo "Health check FAILED. HTTP Status: $HTTP_RESPONSE. Retrying in $SLEEP_DURATION seconds..."
  sleep $SLEEP_DURATION
done

echo "Health check FAILED after $MAX_ATTEMPTS attempts."
exit 1