FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04


ENV BABASHKA_VERSION=1.3.191

ENV JDK_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.4_7.tar.gz"
ENV JDK_NAME=temurin-jdk-21
ENV JAVA_HOME="/lib/jvm/${JDK_NAME}"
ENV PATH="$PATH:$JAVA_HOME/bin"


RUN \
    # update and install packages
    apt-get update && export DEBIAN_FRONTEND=noninteractive && apt-get install -y --no-install-recommends \
        curl \
        wget \
        tree \
        locate \
    
    # clean-up
    && apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false \
    && apt-get autoremove -y \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*


RUN \
    # install Babashka
    curl -sLO https://raw.githubusercontent.com/babashka/babashka/master/install \
    && chmod +x install \
    && ./install --version $BABASHKA_VERSION \
    && rm install \

    # install JDK
    && wget "${JDK_URL}" -O "${JDK_NAME}.tar.gz" \
    && mkdir -p "${JAVA_HOME}" \
    && tar -xf "${JDK_NAME}.tar.gz" -C "${JAVA_HOME}" --strip-components=1 \
    && rm "${JDK_NAME}.tar.gz" \
    && rm -rf "${JDK_NAME}"
