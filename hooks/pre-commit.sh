#!/bin/sh

echo "🔍 Running pre-commit checks..."

# 1. Code formatting
echo "  → Checking code format..."
./mvnw spotless:check -q
if [ $? -ne 0 ]; then
    echo "❌ Code formatting issues found!"
    echo "Fix with: ./mvnw spotless:apply"
    exit 1
fi

# 2. Linting
echo "  → Running Checkstyle..."
./mvnw checkstyle:check -q
if [ $? -ne 0 ]; then
    echo "❌ Checkstyle violations found!"
    exit 1
fi

# 3. Compilation
echo "  → Compiling..."
./mvnw compile -q
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

echo "✅ Pre-commit checks passed!"
exit 0
