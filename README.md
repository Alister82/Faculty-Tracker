<div align="center">

# 🎓 College Tracker Gateway

**A highly professional Spring Boot application for managing and tracking college activities, featuring intelligent document processing and Google Drive integration.**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg?style=for-the-badge&logo=java)](https://jdk.java.net/17/)
[![MySQL](https://img.shields.io/badge/MySQL-Database-blue.svg?style=for-the-badge&logo=mysql)](https://www.mysql.com/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Views-005C0F.svg?style=for-the-badge&logo=Thymeleaf)](https://www.thymeleaf.org/)
[![Google Drive](https://img.shields.io/badge/Drive-API-yellow.svg?style=for-the-badge&logo=googledrive)](https://developers.google.com/drive)

---

</div>

## 📌 Overview
The **College Tracker** is a robust web application built dynamically on the Java Spring Boot ecosystem. It aims to streamline operations around college activity management, student submissions, and administrative approval workflows. Designed with enterprise-grade security and scalability in mind, it handles varied data formats like Excel and PDF effortlessly, while utilizing integrated cloud capabilities for file storage.

---

## ✨ Key Features
- **🛡️ Robust Security System:** Fully secured web endpoints utilizing Spring Security 6. Uses custom authentication tailored for the `AppUser` domain, ensuring tight role-based access control (RBAC).
- **📋 Dynamic Approval Workflows:** Track states such as Pending, Approved, or Rejected seamlessly via the specialized `ApprovalStatus` module.
- **📄 Advanced Document Processing:** Generate, read, and manipulate reports and dynamic documents natively using **Apache POI** (Excel / OOXML) and **Apache PDFBox** (PDF).
- **☁️ Cloud Storage Integration:** Deeply integrated with **Google Drive API (v3)** for seamless document uploads and remote cloud storage management.
- **🖥️ Responsive Server-Side UI:** Clean, responsive UI rendered smoothly through **Thymeleaf** (with Spring Security Dialect), guaranteeing a rapid initial load without bloated client frameworks.

---

## 🏗️ Technology Stack

| Architecture Layer | Technology Used |
| :--- | :--- |
| **Backend Framework** | Java 17, Spring Boot 3.2.3 |
| **Database Structure** | MySQL, Spring Data JPA, Hibernate ORM |
| **Security Framework** | Spring Security 6, Google OAuth Client |
| **Templating Engine** | Thymeleaf & Thymeleaf Extras |
| **Document Processors** | Apache POI 5.2.5, Apache PDFBox 2.0.30 |
| **Build Automation** | Maven |

---

## 🚀 Getting Started

### 📋 Prerequisites
Before running the application locally, ensure you have the following provisioned:
- [Java 17 JDK](https://jdk.java.net/17/) (or higher)
- [Maven](https://maven.apache.org/) (or use the included default wrapper `./mvnw`)
- [MySQL Server](https://dev.mysql.com/downloads/mysql/) running natively or via Docker
- *Google Developer Project* (For Drive API / OAuth2 Credentials: `clientId`, `clientSecret`)

### 🛠️ Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/college-tracker.git
   cd tracker
   ```

2. **Configure Data Source and API keys:**
   Update your `src/main/resources/application.properties` (or your defined secrets manager/profile) with your target MySQL configuration.
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/tracker_db?useSSL=false
   spring.datasource.username=root
   spring.datasource.password=your_secure_password
   spring.jpa.hibernate.ddl-auto=update
   ```
   *Note: Ensure `.env`, `credentials.json`, and `application-secret.properties` are loaded with your Google API keys. Do not commit these files.*

3. **Build the project:**
   Using the Maven wrapper to ensure environment parity:
   ```bash
   ./mvnw clean install
   ```

4. **Run the application:**
   Boot up the Spring Server:
   ```bash
   ./mvnw spring-boot:run
   ```
   *The application will now safely listen for traffic on `http://localhost:8080`.*

---

## 📂 Project Architecture Pattern

A quick glimpse into the project's layout highlighting standard MVC implementation:

```text
tracker/
├── src/
│   ├── main/
│   │   ├── java/com/college/tracker/
│   │   │   ├── security/        # Security configurations, AppUser domain, Auth context
│   │   │   ├── controllers/     # Central ActivityController & Web Endpoints
│   │   │   ├── services/        # Business logic, Drive integration orchestrations
│   │   │   └── repositories/    # JPA Interfaces
│   │   └── resources/
│   │       ├── templates/       # Thymeleaf HTML views
│   │       ├── static/          # CSS, JS, and graphical assets
│   │       └── application.yml  # Base config profiles
│   └── test/                    # Unit & Integration Tests mapped against application architecture
├── .ignore                      # Local indexing exclusions
├── pom.xml                      # Maven Dependencies definition
└── README.md                    # Canonical Project Documentation
```

---

## 🤝 Contributing
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request for review.

---

<div align="center">
  <sub>Engineered with ❤️ for advanced collegiate administration tracking.</sub>
</div>
