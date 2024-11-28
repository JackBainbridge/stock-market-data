IMAGE_NAME="stock-market-app"
CONTAINER_NAME="stock-market-container"

# Step 1, build the application (data.sql file generated at this point). Use Default profile.
mvn clean package -Dspring.profiles.active=default

# Step 2, build the docker conatiner.
docker build -t ${IMAGE_NAME} .

# Step 3, check if container exists. If so, stop and remove it.

# Check if the container exists
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Container ${CONTAINER_NAME} exists."

    # Check if the container is running
    if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "Stopping container ${CONTAINER_NAME}..."
        docker stop ${CONTAINER_NAME}
    fi

    echo "Removing container ${CONTAINER_NAME}..."
    docker rm ${CONTAINER_NAME}
else
    echo "Container ${CONTAINER_NAME} does not exist."
fi

# Step 4, run new container.
docker run -p 8080:8080 -d --name ${CONTAINER_NAME} ${IMAGE_NAME}