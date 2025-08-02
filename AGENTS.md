# Agents as Maintainers

This document provides instructions for AI agents to maintain this repository.

## Project Structure

The project is a database gateway that proxies connections between clients and databases. It is composed of the following modules:

- `app`: The main application that starts the gateway.
- `common`: Common data structures and utility functions.
- `core`: The core components of the gateway, including the proxy server, state machine, and policy engine.
- `policy`: The policy API that allows for custom policies to be implemented.
- `policy-builtin`: Built-in policies that can be used out of the box.
- `protocols`: Implementations of the MySQL and PostgreSQL protocols.
- `test`: Integration and protocol tests.

## Building the Project

The project is built with Gradle. To build the project, run the following command:

```bash
./gradlew build
```

## Running Tests

To run the tests, run the following command:

```bash
./gradlew test
```

## Contributing

When contributing to this repository, please follow these guidelines:

- Write clean, concise, and well-documented code.
- Write unit tests for all new code.
- Ensure that all tests pass before submitting a pull request.
- Follow the existing coding style.
