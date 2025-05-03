#!/bin/bash

# Script to swap in the new sequential polling implementation

# Set directory variables
SRC_DIR="app/src/main/java/com/example/cardash"

# Backup original files
echo "Creating backups of original files..."
cp "$SRC_DIR/ui/metrics/MetricViewModel.kt" "$SRC_DIR/ui/metrics/MetricViewModel.kt.bak"
cp "$SRC_DIR/ui/settings/SettingsDialog.kt" "$SRC_DIR/ui/settings/SettingsDialog.kt.bak"
cp "$SRC_DIR/ui/settings/AboutDialog.kt" "$SRC_DIR/ui/settings/AboutDialog.kt.bak"

# Copy new files to their proper locations
echo "Installing new implementation files..."
cp "$SRC_DIR/ui/metrics/MetricViewModel.kt.new" "$SRC_DIR/ui/metrics/MetricViewModel.kt"
cp "$SRC_DIR/ui/settings/SettingsDialog.kt.new" "$SRC_DIR/ui/settings/SettingsDialog.kt"
cp "$SRC_DIR/ui/settings/AboutDialog.kt.new" "$SRC_DIR/ui/settings/AboutDialog.kt"

# Remove temporary files
echo "Cleaning up temporary files..."
rm "$SRC_DIR/ui/metrics/MetricViewModel.kt.new"
rm "$SRC_DIR/ui/settings/SettingsDialog.kt.new"
rm "$SRC_DIR/ui/settings/AboutDialog.kt.new"

echo "Installation complete!"
echo "The sequential polling implementation has been installed."
echo "Make sure to test thoroughly before committing changes."
