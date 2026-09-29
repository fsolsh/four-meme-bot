# FourMeme Bot Service

## 📖 Project Overview

FourMeme Bot Service is a web-based interactive application designed to automate, manage, and interact with bots within the "FourMeme" ecosystem. It provides a user-friendly web interface for bot control, real-time interaction, and session management, backed by a robust Java Spring Boot backend.

The service acts as a bridge between users and automated bot processes, allowing for seamless command execution, state management, and secure access control.

---

## ✨ Core Features

### 1. User Authentication & Authorization
- **Secure Login System**: Users must authenticate before accessing the bot control panel. The system leverages a standard login flow to verify user credentials.
- **Session Management**: Maintains secure user sessions to ensure that only authenticated users can interact with the bots.

### 2. Bot Control & Management (Backend)
- **RESTful API**: Exposes endpoints to handle various bot operations such as starting, stopping, sending commands, and retrieving bot status.
- **State Management**: Tracks the lifecycle and current state of the bots, ensuring commands are executed reliably.

### 3. Interactive Web Dashboard (Frontend)
- **Control Panel**: A dynamic dashboard where authenticated users can view bot statuses, send commands, and monitor outputs in real-time.
- **Asynchronous Communication**: Utilizes AJAX/Fetch API to communicate with the backend without reloading the page, providing a smooth, single-page application (SPA) experience.

---

## 🛡️ Security Architecture

Security is a critical component of the FourMeme Bot Service. The application implements multiple layers of security to protect both the backend API and the frontend interface.

### Frontend Security
- **Route Guarding**: The application strictly separates public routes from protected routes. Users are automatically redirected to the login page if they attempt to access the dashboard without a valid session.
- **Token/Session Handling**: API requests from the frontend include necessary authentication tokens or session cookies. The application handles unauthorized API responses (e.g., HTTP 401) gracefully by redirecting the user to the login page.
- **Input Sanitization**: User inputs in the control panel are sanitized before being sent to the backend to prevent Cross-Site Scripting (XSS) and injection attacks.

### Backend Security
- **Controller-Level Protection**: The API endpoints are protected and require authentication. Requests lacking valid credentials are rejected at the controller or filter level before reaching the business logic.
- **CSRF Protection**: As a state-modifying application, Spring Security's CSRF protection is enabled by default for non-safe HTTP methods (POST, PUT, DELETE), preventing Cross-Site Request Forgery attacks.
- **CORS Policy**: Cross-Origin Resource Sharing is configured to only allow requests from trusted domains, preventing unauthorized external websites from interacting with the Bot API.

### Session & Credential Security
- **Secure Cookies**: Session cookies are configured with `HttpOnly` (preventing client-side JavaScript access) and `Secure` (only transmitted over HTTPS) flags to mitigate session hijacking and XSS attacks.
- **Password Hashing**: User passwords are never stored in plaintext. The system uses strong cryptographic hashing algorithms (e.g., BCrypt) to verify credentials.

---

## 🛠️ Tech Stack

- **Backend**: Java, Spring Boot, Spring Web, Spring Security
- **Frontend**: HTML5, CSS3, Vanilla JavaScript (ES6+)
- **Architecture**: RESTful API, Single Page Application (SPA) pattern

---

## 🚀 Getting Started

### Prerequisites
- Java JDK 11 or higher
- Maven or Gradle

### Installation & Running

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd fourmeme-bot
   
   ```
2. Build the project:
   ```bash
   mvn clean package
   ```
3. Run the application:
   ```bash
   java -jar target/fourmeme-bot-<version>.jar
   ```
4. Access the application via your web browser:
   ```bash
   http://localhost:8080
   ```
### UserName & Password(default username and password)

- **Username**: `admin`
- **Password**: `admin`



### Technical Exchange
![fsolsh@gmail.com 二维码](https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=fsolsh%40gmail.com)

