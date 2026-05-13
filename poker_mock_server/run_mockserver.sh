#!/bin/bash
# ============================================================
# ポーカーモックサーバー 起動スクリプト
# Gradleなしで直接ライブラリをダウンロードして実行します
# ============================================================

set -e

LIB_DIR="libs"
SRC="src/main/java/mock/MockPokerServer.java"
OUT="out"
MAIN="mock.MockPokerServer"

# ===== ダウンロードするライブラリ =====
declare -A JARS
JARS["Java-WebSocket-1.5.4.jar"]="https://repo1.maven.org/maven2/org/java-websocket/Java-WebSocket/1.5.4/Java-WebSocket-1.5.4.jar"
JARS["jackson-databind-2.16.1.jar"]="https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar"
JARS["jackson-core-2.16.1.jar"]="https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar"
JARS["jackson-annotations-2.16.1.jar"]="https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.16.1/jackson-annotations-2.16.1.jar"
JARS["slf4j-api-1.7.36.jar"]="https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
JARS["slf4j-simple-1.7.36.jar"]="https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar"

# ===== libsフォルダ作成 =====
mkdir -p "$LIB_DIR"
mkdir -p "$OUT"

# ===== ライブラリのダウンロード =====
echo "📦 ライブラリをダウンロード中..."
for JAR in "${!JARS[@]}"; do
    if [ ! -f "$LIB_DIR/$JAR" ]; then
        echo "  ↓ $JAR"
        curl -L -o "$LIB_DIR/$JAR" "${JARS[$JAR]}"
    else
        echo "  ✅ $JAR（既にあります）"
    fi
done

# ===== クラスパスを組み立てる =====
CLASSPATH=""
for JAR in "$LIB_DIR"/*.jar; do
    CLASSPATH="$CLASSPATH:$JAR"
done
# 先頭の : を除去
CLASSPATH="${CLASSPATH:1}"

# ===== コンパイル =====
echo ""
echo "🔨 コンパイル中..."
javac -cp "$CLASSPATH" -d "$OUT" "$SRC"
echo "  ✅ コンパイル完了"

# ===== 実行 =====
echo ""
echo "🚀 モックサーバーを起動します..."
java -cp "$OUT:$CLASSPATH" "$MAIN"
