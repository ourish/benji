# BENJI Wallet Management API

BENJI - Blockchain-Enabled Network Journal Interface is the new latest tech in managing your Crypto Investments!
A RESTful API for crypto wallet management with real-time price tracking and performance simulation.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-blue)
![Java](https://img.shields.io/badge/Java-21-red)
![H2 Database](https://img.shields.io/badge/H2-Database-green)

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Configuration](#configuration)
- [Running Tests](#running-tests)

## Features
- 🪪 **Wallet Management**  
  - Create wallets with unique email validation
  - Add/update crypto assets with real-time price validation
- 🔄 **Scheduled Updates**  
  - Auto-refresh prices every 30s (configurable)
  - Concurrent updates for 3 assets simultaneously
- 📈 **Performance Simulation**  
  - Historical/future portfolio simulations
  - Best/worst asset performance analysis
- 🛡️ **Error Handling**  
  - Custom exceptions for API errors
  - Global exception handling with JSON responses

## Tech Stack
- **Java 21** + **Spring Boot 3.2**
- **H2 Database** (in-memory)
- **Spring WebFlux** (Reactive endpoints)
- **Spring Data JPA** (Database operations)
- **Swagger/OpenAPI** (API documentation)
- **Project Reactor** (Concurrent processing)
- **Lombok** (Boilerplate reduction)

## Quick Start

### Prerequisites
- Java 21 JDK
- Maven
- CoinCap API key ([Get free key](https://pro.coincap.io/signup)) and update application.yml with it

```bash
git clone https://github.com/yourusername/benji-wallet.git
cd benji-wallet

# Run with default H2 database
mvn spring-boot:run
