# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- New `antopt.cli` namespace with shared CLI utilities
- `handle-cli` function for common CLI parsing and error handling
- Proper error handling with try-catch for file operations
- Better integer parsing with NumberFormatException handling
- Comprehensive docstrings for all CLI functions
- Help flag (`-h`, `--help`) for both CLI and GUI modes

### Changed
- **BREAKING**: Removed support for positional filepath arguments
  - Old: `clj -M -m antopt.core resources/eil51.tsm -a 300`
  - New: `clj -M -m antopt.core -f resources/eil51.tsm -a 300`
  - Migration: Always use the `-f` or `--file` flag to specify file paths
- Refactored CLI argument parsing to use `tools.cli` library
- Improved error messages for invalid arguments
- System.exit now only called on actual errors (better REPL compatibility)
- Consolidated duplicate error handling code between core.clj and ui.clj

### Fixed
- Integer parsing now handles non-numeric input gracefully
- File validation provides clearer error messages
- Better exception handling prevents abrupt JVM exits in REPL

### Migration Guide

If you have existing scripts that use positional arguments:

**Before:**
```bash
clj -M -m antopt.core resources/eil51.tsm
clj -M -m antopt.core resources/eil51.tsm -a 300
clj -M:run resources/belgiumtour.tsm -a 200
```

**After:**
```bash
clj -M -m antopt.core -f resources/eil51.tsm
clj -M -m antopt.core -f resources/eil51.tsm -a 300
clj -M:run -f resources/belgiumtour.tsm -a 200
```

Or use the long form:
```bash
clj -M -m antopt.core --file resources/eil51.tsm --ants 300
```

## [1.0.0] - 2026-03-31

### Added
- Initial modernized release
- Clojure 1.12.0 support
- Quil 4.3.1563 for visualizations
- Comprehensive test suite
- Modern project structure with deps.edn

### Changed
- Migrated from Leiningen to Clojure CLI tools
- Updated all dependencies to latest versions
- Improved code organization and documentation
