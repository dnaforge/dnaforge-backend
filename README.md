# Backend for DNA Forge

This repository provides relaxation and simulation capabilities for
the [DNA Forge project](https://github.com/Ritkuli/dnaforge).  
Under the hood, [oxDNA](https://github.com/lorenzo-rovigatti/oxDNA) is used to provide these features.  
Communication with the DNA Forge frontend is via a REST-API and WebSockets.
If you want to know more about the communication protocol, you can have a look at [Protocol.md](Protocol.md).

## Usage

The provided [Dockerfile](Dockerfile) and [compose.yaml](compose.yaml) make it easy to deploy this project in a
container.  
Of course, it is also possible to run this project bare-metal.  
The application uses some environment variables for configuration.
See [compose.yaml](compose.yaml) for explanations.
The `NO_CUDA` build variable is not needed when running bare-metal.
Instead, `CUDA` can be set to `false` if needed.

### Containerized

#### Requirements

Containerization software such as [Docker](https://docs.docker.com/engine/install/#server) needs to be installed.
Docker Compose makes the deployment even easier.  
To use CUDA-enabled oxDNA in a Docker container, the
[NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html#setting-up-nvidia-container-toolkit)
is required.  
If you don't want to use Docker Compose, you will need to build the image manually:
`docker build --tag dnaforge/dnaforge-backend .`.

#### Usage

With Docker Compose, just run `docker compose up -d`.  
With pure Docker you need to run
`docker run --runtime=nvidia --name dnaforge-backend -e "DATADIR=/data/" -e "PORT=8080" -p 8080:8080 -d dnaforge/dnaforge-backend`.

### Bare-metal

#### Requirements

CUDA, JDK17 and [oxDNA](https://github.com/lorenzo-rovigatti/oxDNA) need to be installed.  
This project can then be build using `./gradlew installDist`.

#### Usage

To run the backend, execute `./build/install/dnaforge-backend/bin/dnaforge-backend`.
