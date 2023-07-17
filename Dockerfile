# dnaforge-backend builder
FROM gradle:8.2-jdk17-alpine  as builder

## Copy necessary data (see .dockerignore)
COPY . /dnaforge-backend/

## Compile Kotlin app
RUN cd /dnaforge-backend/ \
    && gradle installDist



# Use NVIDIA CUDA base image
FROM nvidia/cuda:12.2.0-devel-ubuntu22.04

## Install necessary dependencies
### doxygen, graphviz and libgsl-dev are not necessarily required
RUN apt-get update && apt-get install -y \
    git \
    cmake \
    build-essential \
    openjdk-17-jre-headless

## Clone oxDNA
RUN git clone https://github.com/lorenzo-rovigatti/oxDNA.git --branch v3.5.2

# Build oxDNA using CMake with CUDA support
RUN mkdir /oxDNA/build/ && cd /oxDNA/build/ \
    && cmake -DCUDA=On .. \
    && make -j$(nproc)

## Copy binaries to correct location
RUN cp /oxDNA/build/bin/* /usr/bin/

## Copy DNA Forge backend files
COPY --from=builder /dnaforge-backend/build/install/dnaforge-backend/ /dnaforge-backend/


## Start server
ENTRYPOINT ["dnaforge-backend/bin/dnaforge-backend"]
