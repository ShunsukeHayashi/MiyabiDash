#!/bin/bash
# Run this on Mac mini to generate the Xcode project
# Prerequisites: Xcode 16+, xcodegen (brew install xcodegen)

set -e

echo "🌸 Setting up MiyabiDash Xcode project..."

# Install xcodegen if needed
if ! command -v xcodegen &> /dev/null; then
    echo "Installing xcodegen..."
    brew install xcodegen
fi

# Generate project
xcodegen generate

echo "✅ Xcode project generated!"
echo "Open MiyabiDash.xcodeproj in Xcode"
echo "Set your Team in Signing & Capabilities"
echo "Build & Run!"
