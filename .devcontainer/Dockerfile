# Build arguments for base image selection
## false (default) = CPU base, true = CUDA base
ARG CUDA=false

## Base image tags
ARG CPU_IMAGE=ubuntu:24.04
ARG CUDA_IMAGE=nvidia/cuda:12.9.1-devel-ubuntu24.04

# Base image selector stages
FROM ${CPU_IMAGE} AS base-false
FROM ${CUDA_IMAGE} AS base-true



# oxDNA builder stage
FROM base-${CUDA} AS oxdna-builder
ARG CUDA
ENV CUDA=${CUDA}

## Install build dependencies
### Note: doxygen, graphviz and libgsl-dev are not necessarily required
RUN apt-get update && \
    apt-get install -y \
    git \
    cmake \
    build-essential && \
    rm -rf /var/lib/apt/lists/*

## Clone oxDNA repository
RUN git clone https://github.com/lorenzo-rovigatti/oxDNA.git && \
    cd /oxDNA && \
    git switch --detach v3.7.0

## Build oxDNA with conditional CUDA support
RUN mkdir /oxDNA/build/ && cd /oxDNA/build/ && \
    if [ "$CUDA" = "true" ]; then \
        cmake -DCUDA=On .. ; \
    else \
        cmake .. ; \
    fi && \
    make -j$(nproc)



# Final runtime image
FROM base-${CUDA}
ARG CUDA
ENV CUDA=${CUDA}

## Install dependencies for sharing GPG keys
RUN apt-get update && \
    apt-get install -y \
    gnupg2 && \
    rm -rf /var/lib/apt/lists/*

## Copy oxDNA binaries from builder stage
COPY --from=oxdna-builder /oxDNA/build/bin/* /usr/bin/
COPY --from=oxdna-builder /oxDNA/build/src/liboxdna_common.* /usr/lib/
