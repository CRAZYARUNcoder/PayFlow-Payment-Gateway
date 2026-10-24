# PayFlow --- Payment Gateway Simulation

A full-stack payment gateway simulation built with **Spring Boot, Spring
Security, JWT, MySQL, and React**. PayFlow demonstrates common
payment-platform workflows such as user authentication, payment
creation, transaction history, refunds, and webhook signature
validation.

> **Note:** This is a learning/demo project. It simulates payment
> outcomes and does not process real money or connect to a real banking
> network.

## Features

-   User registration and login
-   Password hashing with BCrypt
-   JWT-based authentication and protected APIs
-   Create and view payments
-   Payment status tracking: `SUCCESS`, `PENDING`, and `FAILED`
-   Transaction history and payment details
-   Refund requests and refund history
-   Admin endpoints and dashboard-related APIs
-   Webhook endpoint with signature validation
-   Request validation and centralized exception handling
-   MySQL persistence
-   React frontend connected to the backend API

## Tech Stack

### Backend

-   Java
-   Spring Boot
-   Spring Security
-   Spring Data JPA
-   MySQL
-   JWT
-   Maven
-   REST APIs

### Frontend

-   React
-   JavaScript
-   CSS
-   Fetch/API request integration

## Project Structure

``` text
PayFlow-Payment-Gateway/
├── backend/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
└── frontend files/
    └── React application
```

> The exact frontend folder/file names may differ depending on the
> repository layout.

## Getting Started

### Prerequisites

Install the following before running the project:

-   Java version compatible with the backend's `pom.xml`
-   MySQL Server
-   Node.js and npm
-   Git

### 1. Clone the repository

``` bash
git clone https://github.com/CRAZYARUNcoder/PayFlow-Payment-Gateway.git
cd PayFlow-Payment-Gateway
```

### 2. Configure the database and backend secrets

Create a MySQL database for the application, then configure the
backend's local Spring Boot settings.

Do **not** commit real database passwords, JWT signing secrets, webhook
secrets, or other credentials. Keep them in a local configuration file
or environment variables that are excluded from Git.

The backend needs configuration values for: - MySQL JDBC URL - MySQL
username and password - JWT signing secret - Webhook signing secret

Use the property names expected by the backend source code. Do not use
production credentials in this demo.

### 3. Run the backend

From the repository root, open a terminal and run:

**Windows**

``` powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**macOS/Linux**

``` bash
cd backend
./mvnw spring-boot:run
```

If Maven Wrapper cannot run, install Maven and use
`mvn spring-boot:run`.

The backend's port depends on its local configuration. Check the backend
configuration before selecting the API base URL for the frontend.

### 4. Run the frontend

Open a second terminal in the frontend application directory (the
directory containing `package.json`), then run:

``` bash
npm install
npm run dev
```

Open the local URL printed by Vite in the terminal. If the frontend uses
a different start script, check `package.json`.

## Example Workflows

1.  Register a user and log in.
2.  Create a simulated payment.
3.  Review payment status and transaction history.
4.  Try a refund workflow.
5.  Test protected endpoints with a valid JWT.
6.  Test webhook signature validation using a configured signing secret.

## Security Notes

-   Never upload `application.properties`, `.env`, API keys, passwords,
    or private signing secrets.
-   Use strong, unique secrets in local development.
-   This project is for demonstration and learning, not production
    payment processing.
-   Before production use, a payment system would require a real payment
    provider, additional security reviews, monitoring, and compliance
    work.

## Screenshots

Add screenshots of the running application here. Suggested
screenshots: - Login / registration - Dashboard - Payments / transaction
history - Refund history or payment details

For example, save screenshots in a `screenshots/` directory and
reference them like this:

``` markdown
![PayFlow dashboard](screenshots/dashboard.png)
```

## Author

**GitHub:** [CRAZYARUNcoder](https://github.com/CRAZYARUNcoder)

------------------------------------------------------------------------

If you find a bug or have suggestions, feel free to open an issue in the
repository.
## Screenshots

### Dashboard
![PayFlow Dashboard](screenshots/Screenshot%202026-10-24%20184949.png)

### Payments
![PayFlow Payments](screenshots/Screenshot%202026-10-24%20185105.png)

### Payment and Transaction View
![PayFlow Transaction View](screenshots/Screenshot%202026-10-24%20185136.png)

### Refunds
![PayFlow Refunds](screenshots/Screenshot%202026-10-24%20185159.png)

### Payment Details
![PayFlow Payment Details](screenshots/Screenshot%202026-10-24%20185220.png)

### Additional Screen
![PayFlow Additional Screen](screenshots/Screenshot%202026-10-24%20185243.png)
