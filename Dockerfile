FROM eclipse-temurin:21-jdk AS builder
WORKDIR /retro-game-src
COPY . .
RUN \
  # Install packages needed to build the game.
  apt-get update && \
  apt-get install -y --no-install-recommends \
    build-essential \
    cmake && \
  rm -rf /var/lib/apt/lists/* && \
  ./gradlew --no-daemon :battle-engine:buildBattleEngine :bootJar -x test && \
  rm -rf ~/.gradle

FROM eclipse-temurin:21-jre
WORKDIR /retro-game
COPY --from=0 /retro-game-src/battle-engine/build/native/libBattleEngine.so .
COPY --from=0 /retro-game-src/build/libs/retro-game-*.jar retro-game.jar
RUN \
  # Install packages needed to run the game.
  apt-get update && \
  apt-get install -y --no-install-recommends postgresql-client && \
  rm -rf /var/lib/apt/lists/* && \
  # Change the permissions of the artifacts.
  chmod 400 *
CMD ["java", "-Djava.library.path=.", "-jar", "retro-game.jar"]
