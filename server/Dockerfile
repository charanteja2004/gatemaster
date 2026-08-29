# Two stages: the build needs a JDK and the whole Gradle cache, the runtime
# needs neither. Shipping only the second keeps the deployed image around a
# tenth the size of the one that produced it.

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src

# The wrapper and the dependency declarations first, on their own layer. They
# change far less often than the source does, so an edit to a route reuses the
# downloaded dependencies instead of fetching them again.
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
# :server depends on :protocol, so both build files are needed before Gradle
# can even configure. Leaving :protocol out is what broke this image the first
# time it ran in CI: settings.gradle.kts includes it, so the build fails at
# configuration with no such project.
COPY server/build.gradle.kts ./server/
COPY protocol/build.gradle.kts ./protocol/
# :app is in settings.gradle.kts and needs the Android SDK, which this image
# does not have. Excluding it here keeps the server image free of a 1 GB SDK
# download for a module it never builds.
RUN sed -i '/include(":app")/d' settings.gradle.kts
RUN ./gradlew --no-daemon :server:dependencies > /dev/null 2>&1 || true

COPY protocol/src ./protocol/src
COPY server/src ./server/src
RUN ./gradlew --no-daemon :server:installDist

FROM eclipse-temurin:17-jre
WORKDIR /app

# curl is here only for HEALTHCHECK below; the base image does not carry it.
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*

# An unprivileged user: nothing this process does needs root, and a container
# escape starting from uid 0 is a much shorter path than one starting here.
RUN useradd --system --uid 10001 --create-home gatemaster
USER gatemaster

COPY --from=build --chown=gatemaster:gatemaster /src/server/build/install/server ./

ENV PORT=8080
EXPOSE 8080

# -XX:MaxRAMPercentage rather than a fixed -Xmx: the free tiers this is meant
# for hand out between 256 MB and 1 GB, and a fixed heap is either wasteful on
# the large one or killed by the OOM reaper on the small one.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
    CMD ["/bin/sh", "-c", "curl -fsS http://localhost:${PORT}/health || exit 1"]

ENTRYPOINT ["./bin/server"]
