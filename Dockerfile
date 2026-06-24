FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /retro-game-src
COPY . .
RUN \
  # Install packages needed to build the game.
  apt-get update && \
  apt-get install -y --no-install-recommends \
    build-essential \
    cmake && \
  rm -rf /var/lib/apt/lists/* && \
  # Build the battle engine.
  cmake -B build -DCMAKE_BUILD_TYPE=Release battle-engine && \
  cmake --build build && \
  # Build the game.
  mvn -B -DskipTests package && \
  rm -rf ~/.m2

FROM eclipse-temurin:21-jre
WORKDIR /retro-game
COPY --from=0 /retro-game-src/build/libBattleEngine.so .
COPY --from=0 /retro-game-src/target/retro-game-*.jar retro-game.jar
RUN \
  # Install packages needed to run the game.
  apt-get update && \
  apt-get install -y --no-install-recommends postgresql-client && \
  rm -rf /var/lib/apt/lists/* && \
  # Change the permissions of the artifacts.
  chmod 400 *
CMD ["java", "-Djava.library.path=.", "-jar", "retro-game.jar"]
