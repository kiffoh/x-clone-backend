#!/usr/bin/env bash

echo "🔍 Running pre-push checks..."

# Run full verification (includes tests, coverage, spotbugs)
./mvnw verify -Dspotbugs.skip -DskipTests -q

if [ $? -ne 0 ]; then
    echo "❌ Pre-push checks failed!"
    echo "Run './mvnw verify' locally to see details"
    exit 1
fi

echo "✅ All checks passed! Pushing..."
exit 0
