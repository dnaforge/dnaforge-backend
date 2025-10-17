# Simulation Backend for DNAforge

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.11045829.svg)](https://doi.org/10.5281/zenodo.11045829)

This repository provides relaxation and simulation capabilities for
the [DNAforge project](https://github.com/dnaforge/dnaforge).  
Under the hood, [oxDNA](https://github.com/lorenzo-rovigatti/oxDNA) is used to provide these features.  
Communication with the DNAforge frontend is via a REST-API and WebSockets.
If you want to know more about the communication protocol, you can have a look at [Protocol.md](Protocol.md).

## Usage

The provided [Dockerfile](Dockerfile) and [compose.yaml](compose.yaml) make it easy to deploy this project in a
container.  
Of course, it is also possible to run this project bare-metal.  
The application uses some environment variables for configuration.
See [compose.yaml](compose.yaml) for explanations.
The `CUDA` build variable can be set to `true` to enable CUDA support or `false` for CPU-only mode.

### Containerized

#### Requirements

Containerization software such as [Docker](https://docs.docker.com/engine/install/#server) needs to be installed.
Docker Compose makes the deployment even easier.  
To use CUDA-enabled oxDNA in a Docker container, the
[NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html#setting-up-nvidia-container-toolkit)
is required.  
If you don't want to use Docker Compose, you will need to build the image manually:

- For CPU-only: `docker build --build-arg CUDA=false --tag dnaforge/dnaforge-backend .`
- For CUDA support: `docker build --build-arg CUDA=true --tag dnaforge/dnaforge-backend .`

#### Usage

With Docker Compose, just run `docker compose up -d`.  
To enable CUDA support, first edit `compose.yaml` to set `CUDA: true` and uncomment the `runtime: nvidia` line.  
With pure Docker you need to run:

- CPU-only: `docker run --name dnaforge-backend -e "DATADIR=/data/" -e "PORT=8080" -p 8080:8080 -d dnaforge/dnaforge-backend`
- CUDA-enabled: `docker run --runtime=nvidia --name dnaforge-backend -e "DATADIR=/data/" -e "PORT=8080" -p 8080:8080 -d dnaforge/dnaforge-backend`

### Bare-metal

#### Requirements

CUDA, JDK17 and [oxDNA](https://github.com/lorenzo-rovigatti/oxDNA) need to be installed.  
This project can then be build using `./gradlew installDist`.

#### Usage

To run the backend, execute `./build/install/dnaforge-backend/bin/dnaforge-backend`.
