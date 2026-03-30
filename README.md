# Ant Colony Optimization for TSP

A modern Clojure implementation of Ant Colony Optimization (ACO) for solving the Traveling Salesman Problem (TSP).

## Overview

This project implements the Ant Colony Optimization metaheuristic to find near-optimal solutions to TSP instances. The algorithm simulates the behavior of ants finding paths between food sources and their colony, using pheromone trails to guide the search.

### Features

- **Modern Idiomatic Clojure**: Written using Clojure 1.12 with modern best practices
- **Parallel Processing**: Uses `pmap` for concurrent ant tour generation
- **Real-time Visualization**: Optional GUI using Quil showing the optimization progress with animated tour updates
- **Configurable Parameters**: Easily adjust algorithm parameters
- **Comprehensive Tests**: Full test suite with clojure.test

## Algorithm Parameters

- **alpha (α)**: Pheromone importance factor (default: 1.0)
- **beta (β)**: Distance importance factor (default: 2.0)
- **rho (ρ)**: Pheromone evaporation rate (default: 0.25)
- **num-ants**: Number of ants per generation (default: 500)
- **num-generations**: Number of iterations (default: 125)

## Installation

### Prerequisites

- Java 11 or higher
- Clojure CLI tools (1.11+)

### Setup

```bash
# Clone the repository
git clone <repository-url>
cd antOptClosureNew

# Install dependencies (automatic with Clojure CLI)
clj -P
```

## Usage

### Command Line Interface

Run optimization without GUI:

```bash
# Use default dataset and parameters (500 ants, 125 generations)
clj -M -m antopt.core

# Specify dataset with named parameter
clj -M -m antopt.core -f resources/eil51.tsm

# Customize with short flags
clj -M -m antopt.core -f resources/eil51.tsm -a 300 -g 100

# Customize with long flags
clj -M -m antopt.core --file resources/eil51.tsm --ants 200 --generations 50

# Mix flags (order doesn't matter)
clj -M -m antopt.core -a 300 -f resources/eil51.tsm -g 100

# Backward compatible: positional filepath still works
clj -M -m antopt.core resources/eil51.tsm -a 300
```

**Options:**
- `-f, --file PATH` - Path to TSM file (default: `resources/bier127.tsm`)
- `-a, --ants N` - Number of ants per generation (default: `500`)
- `-g, --generations N` - Number of generations to run (default: `125`)

### Graphical User Interface

Run with real-time visualization:

```bash
# Use default dataset and parameters (xqf131.tsm, 500 ants, 125 generations)
clj -M:run

# Specify dataset with named parameter
clj -M:run -f resources/eil51.tsm

# Customize with short flags
clj -M:run -f resources/eil51.tsm -a 300 -g 100

# Customize with long flags
clj -M:run --file resources/belgiumtour.tsm --ants 200 --generations 50

# Mix flags (order doesn't matter)
clj -M:run -a 300 -f resources/eil51.tsm -g 100

# Backward compatible: positional filepath still works
clj -M:run resources/belgiumtour.tsm -a 300
```

**Options:**
- `-f, --file PATH` - Path to TSM file (default: `resources/xqf131.tsm`)
- `-a, --ants N` - Number of ants per generation (default: `500`)
- `-g, --generations N` - Number of generations to run (default: `125`)

### REPL Usage

```clojure
(require '[antopt.core :as aco])

;; Load TSM data
(def nodes (aco/read-tsm-file "resources/eil51.tsm"))

;; Run optimization with default config
(def result (aco/optimize nodes))

;; Run with custom config
(def custom-config {:alpha 1.5
                    :beta 2.5
                    :rho 0.3
                    :num-ants 300
                    :num-generations 100})
(def result (aco/optimize nodes custom-config))

;; Access results
(:length result)  ; Tour length
(:path result)    ; Node sequence
```

## Testing

Run the test suite:

```bash
# Run all tests
clj -M:test

# Run with verbose output
clj -M:test -v
```

## Project Structure

```
antOptClosureNew/
├── deps.edn              # Project dependencies and configuration
├── README.md             # This file
├── src/
│   └── antopt/
│       ├── core.clj      # Core ACO algorithm
│       └── ui.clj        # GUI visualization
├── test/
│   └── antopt/
│       └── core_test.clj # Test suite
└── resources/
    ├── belgiumtour.tsm   # Belgium tour dataset
    ├── bier127.tsm       # 127-city dataset
    ├── eil51.tsm         # 51-city dataset
    └── xqf131.tsm        # 131-city dataset
```

## TSM Data Format

TSM files are EDN (Extensible Data Notation) files containing vectors of [x y] coordinates:

```clojure
[[37 52] [49 49] [52 64] ...]
```

## Algorithm Details

### Ant Colony Optimization Process

1. **Initialization**: Create connections between all nodes with random pheromone values
2. **Tour Construction**: Each ant builds a complete tour probabilistically based on:
   - Pheromone levels (τ^α)
   - Heuristic information (1/distance^β)
3. **Pheromone Update**:
   - Deposit pheromone on edges used in tours (proportional to tour quality)
   - Evaporate pheromone on all edges (by factor ρ)
4. **Iteration**: Repeat for specified number of generations
5. **Result**: Return the best tour found

### Key Improvements in This Implementation

- **Functional Design**: Pure functions with explicit state management
- **Transducers**: Efficient sequence processing where applicable
- **Memoization**: Caching of tour length calculations
- **Parallel Processing**: Concurrent ant tour generation
- **Separation of Concerns**: Clear separation between algorithm, UI, and I/O
- **Comprehensive Documentation**: Docstrings and comments throughout

## Performance

Typical performance on included datasets:

- **eil51.tsm** (51 cities): ~30 seconds, tour length ~430-450
- **bier127.tsm** (127 cities): ~2-3 minutes, tour length ~120,000-125,000
- **xqf131.tsm** (131 cities): ~2-3 minutes, tour length ~580-620

*Performance varies based on hardware and random initialization*

## Development

### REPL Development

```bash
# Start REPL with all dependencies
clj -M:repl

# Or with nREPL for editor integration
clj -M:repl -m nrepl.cmdline
```

### Adding New Datasets

1. Create an EDN file with node coordinates
2. Place in `resources/` directory
3. Run with: `clj -M -m antopt.core resources/your-dataset.tsm`

## License

Copyright © 2026 Martin Henke

Distributed under the Eclipse Public License, the same as Clojure.

## References

- Dorigo, M., & Stützle, T. (2004). *Ant Colony Optimization*. MIT Press.
- Original implementation: https://github.com/mhenke1/antOptClojure

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.