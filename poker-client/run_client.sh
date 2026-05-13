#!/bin/bash
# ============================================================
# ポーカークライアント 起動スクリプト（Ubuntu版）
# ============================================================

set -e

LIB_DIR="libs"
OUT="out"
MAIN="poker.PokerApp"
SRC_DIR="."

mkdir -p "$LIB_DIR"
mkdir -p "$OUT"

# ===== JavaFX を apt でインストール =====
if ! dpkg -l | grep -q openjfx 2>/dev/null; then
    echo "📦 JavaFX をインストール中..."
    sudo apt update -q
    sudo apt install -y openjfx fonts-noto-cjk
    echo "  ✅ JavaFX インストール完了"
else
    echo "  ✅ JavaFX（既にインストール済み）"
fi

# JavaFXのlibパスを自動検索
JAVAFX_LIB=$(find /usr -name "javafx.controls.jar" 2>/dev/null | head -1 | xargs dirname)
if [ -z "$JAVAFX_LIB" ]; then
    echo "❌ JavaFXのパスが見つかりません"
    exit 1
fi
echo "  JavaFX パス: $JAVAFX_LIB"

# ===== ライブラリのダウンロード =====
echo "📦 ライブラリをダウンロード中..."

download_jar() {
    local filename=$1
    local url=$2
    if [ ! -f "$LIB_DIR/$filename" ]; then
        echo "  ↓ $filename"
        curl -L -o "$LIB_DIR/$filename" "$url"
    else
        echo "  ✅ $filename（既にあります）"
    fi
}

download_jar "Java-WebSocket-1.5.4.jar" \
    "https://repo1.maven.org/maven2/org/java-websocket/Java-WebSocket/1.5.4/Java-WebSocket-1.5.4.jar"
download_jar "jackson-databind-2.16.1.jar" \
    "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar"
download_jar "jackson-core-2.16.1.jar" \
    "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar"
download_jar "jackson-annotations-2.16.1.jar" \
    "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.16.1/jackson-annotations-2.16.1.jar"
download_jar "slf4j-api-1.7.36.jar" \
    "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
download_jar "slf4j-simple-1.7.36.jar" \
    "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar"

# ===== クラスパス組み立て =====
CLASSPATH="$OUT"
for JAR in "$LIB_DIR"/*.jar; do
    CLASSPATH="$CLASSPATH:$JAR"
done

JAVAFX_MODULES="javafx.controls,javafx.fxml"

# ===== ソースファイルの列挙 =====
SOURCES=$(find "$SRC_DIR" -name "*.java" | tr '\n' ' ')

# ===== コンパイル =====
echo ""
echo "🔨 コンパイル中..."
javac \
    --module-path "$JAVAFX_LIB" \
    --add-modules "$JAVAFX_MODULES" \
    -cp "$CLASSPATH" \
    -d "$OUT" \
    $SOURCES
echo "  ✅ コンパイル完了"

# ===== 実行 =====
echo ""
echo "🚀 ポーカークライアントを起動します..."
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 \
    --module-path "$JAVAFX_LIB" \
    --add-modules "$JAVAFX_MODULES" \
    -cp "$CLASSPATH" \
    "$MAIN"
