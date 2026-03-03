#!/usr/bin/env bash
set -e

echo "🔍 Running pre-commit checks..."

# Get staged files (Added, Copied, Modified)
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM)

# Filter to Java files
JAVA_FILES=$(echo "$STAGED_FILES" | grep -E '\.java$' || true)

if [ -z "$JAVA_FILES" ]; then
  echo "ℹ️  No Java files staged — skipping backend checks"
  exit 0
fi

echo "☕ Java files detected:"
echo "$JAVA_FILES"

echo "→ Running formatting + lint checks..."

# Single Maven invocation (much faster)
./mvnw -q spotless:apply

echo "✅ Pre-commit checks passed!"
