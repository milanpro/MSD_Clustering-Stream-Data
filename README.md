# ClusteringStreamData-Kafka

Reimplementation of the paper "Clustering Stream Data by Exploring the Evolution of Density Mountain" in the project seminar Mining Streaming Data at HPI [Link](https://arxiv.org/abs/1710.00867)

## Prerequisites

- [Java JDK (v8+)](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
- [Docker and Docker-Compose](https://www.docker.com/get-started)
- [Node and NPM (v10+)](https://www.npmjs.com/get-npm)

## Getting Started

1. Clone the repository via `git clone git@github.com:milanpro/ClusteringStreamData-Kafka.git`
2. Start Docker deamon
3. Build the application docker images by running `./gradlew dockerBuildImage` inside the folder
4. Run `docker-compose up` to start the kafka cluster locally
5. Connect to `localhost:5656` with the browser of your choice

## Known Issues

### Frontend

- Input fields for changing etcd values are not sanity checked and might break the execution
- Sometimes all clustered ClusterCells disappear, which seems to be a SerDes problem
