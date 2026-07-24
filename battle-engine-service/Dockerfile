FROM eclipse-temurin:21-jdk AS builder
WORKDIR /retro-game-src
COPY . .
RUN \
  apt-get update && \
  apt-get install -y --no-install-recommends \
    build-essential \
    cmake && \
  rm -rf /var/lib/apt/lists/* && \
  ./gradlew --no-daemon :battle-engine:buildBattleEngine :battle-engine-service:installDist && \
  rm -rf ~/.gradle

FROM eclipse-temurin:21-jre
WORKDIR /retro-game
COPY --from=builder /retro-game-src/battle-engine/build/native/libBattleEngine.so .
COPY --from=builder /retro-game-src/battle-engine-service/build/install/battle-engine-service .
ENV JAVA_OPTS="-Djava.library.path=/retro-game"
EXPOSE 8080
ENTRYPOINT ["/retro-game/bin/battle-engine-service"]
