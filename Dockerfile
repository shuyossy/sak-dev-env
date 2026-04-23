# syntax=docker/dockerfile:1.7

ARG JDK_IMAGE=eclipse-temurin:21-jdk-jammy
ARG JRE_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${JDK_IMAGE} AS builder

WORKDIR /workspace

# Maven Wrapper / ビルド定義を先に配置し、依存解決キャッシュを効かせる
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
COPY wrapper/ wrapper/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B dependency:go-offline

# frontend-maven-plugin が Node/npm を自動セットアップするため、
# package.json / package-lock.json はビルドコンテキストに含めておけば十分
COPY package.json package-lock.json ./

# Docker ビルドではローカル lint tool（Checkstyle/PMD 等）は不要のため
# postinstall スクリプトをスキップしてビルド時間とイメージサイズを削減
ENV NPM_CONFIG_IGNORE_SCRIPTS=true

# アプリケーションソースとフロントエンドビルド設定をコピー
COPY src/ src/
COPY vite.config.js ./

# Spring Boot の fat JAR を生成。frontend-maven-plugin が Node のダウンロードと
# npm ci / npm run build までを担うので、ここで一括してパッケージングする
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -DskipTests package

# Layered JAR を抽出し、runtime ステージでレイヤー分割して COPY する
RUN cp target/*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---------------------------------------------------------

FROM ${JRE_IMAGE} AS runtime

# 非 root 実行ユーザーを用意
RUN groupadd --system --gid 1001 app \
 && useradd --system --uid 1001 --gid app --home /app --shell /usr/sbin/nologin app

WORKDIR /app

COPY --from=builder --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /workspace/extracted/application/ ./

USER app:app
EXPOSE 8080

ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE="prod" \
    TZ="Asia/Tokyo"

# Actuator 未導入なら以下の HEALTHCHECK ブロックを削除してください（ID:2 で Actuator を追加予定）
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]
