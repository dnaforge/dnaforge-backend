# Select the image based on the NO_CUDA variable and store it in IMAGE
ARG NO_CUDA
ARG IMAGE=${NO_CUDA:+ubuntu:22.04}
ARG IMAGE=${IMAGE:-nvidia/cuda:12.3.1-devel-ubuntu22.04}



# dnaforge-backend builder
FROM gradle:9.1.0-jdk17-alpine AS builder

## Copy necessary data (see .dockerignore)
COPY .. /dnaforge-backend/

## Compile Kotlin app
RUN cd /dnaforge-backend/ \
    && gradle installDist



# Use image based on the NO_CUDA variable
FROM $IMAGE
ARG NO_CUDA

# Indicate if CUDA is available
ENV CUDA=${NO_CUDA:+false}
ENV CUDA=${CUDA:-true}

## Install necessary dependencies
### doxygen, graphviz and libgsl-dev are not necessarily required
RUN apt-get update && apt-get install -y \
    git \
    cmake \
    build-essential \
    openjdk-17-jre-headless

## Clone oxDNA
RUN git clone https://github.com/lorenzo-rovigatti/oxDNA.git --branch v3.6.0

# Build oxDNA using CMake
RUN mkdir /oxDNA/build/ && cd /oxDNA/build/ && \
    if [ "$CUDA" = "true" ]; then \
        cmake -DCUDA=On .. ; \
    else \
        cmake .. ; \
    fi && \
    make -j$(nproc)

## Copy binaries to correct location
RUN cp /oxDNA/build/bin/* /usr/bin/

## Copy DNAforge backend files
COPY --from=builder /dnaforge-backend/build/install/dnaforge-backend/ /dnaforge-backend/


## Start server
ENTRYPOINT ["dnaforge-backend/bin/dnaforge-backend"]
