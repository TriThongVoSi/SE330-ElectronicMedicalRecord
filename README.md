# 🏥 Hệ Thống Quản Lý Bệnh Án Điện Tử (EMR)

> **SE330 – Software Architecture & Design**
>
> Hệ thống quản lý bệnh án điện tử toàn diện, được xây dựng với kiến trúc Modular Monolith, hỗ trợ quản lý bệnh nhân, lịch hẹn, đơn thuốc, dịch vụ y tế và cổng bệnh nhân trực tuyến.

---

## 📋 Mục Lục

- [Tổng Quan](#-tổng-quan)
- [Kiến Trúc Hệ Thống](#-kiến-trúc-hệ-thống)
- [Công Nghệ Sử Dụng](#-công-nghệ-sử-dụng)
- [Tính Năng](#-tính-năng)
- [Yêu Cầu Hệ Thống](#-yêu-cầu-hệ-thống)
- [Hướng Dẫn Cài Đặt](#-hướng-dẫn-cài-đặt)
- [Cấu Trúc Thư Mục](#-cấu-trúc-thư-mục)
- [API Documentation](#-api-documentation)
- [Testing](#-testing)
- [Đóng Góp](#-đóng-góp)
- [Giấy Phép](#-giấy-phép)

---

## 🔍 Tổng Quan

Hệ thống EMR (Electronic Medical Record) là một ứng dụng web full-stack giúp số hóa quy trình quản lý bệnh án tại các cơ sở y tế. Hệ thống hỗ trợ **3 vai trò người dùng** với các quyền truy cập khác nhau:

| Vai trò      | Mô tả                                                   |
| ------------ | -------------------------------------------------------- |
| **ADMIN**    | Quản trị viên – Toàn quyền quản lý hệ thống             |
| **DOCTOR**   | Bác sĩ – Quản lý bệnh nhân, lịch hẹn, đơn thuốc        |
| **PATIENT**  | Bệnh nhân – Xem lịch sử khám, lịch hẹn, hồ sơ cá nhân |

---

## 🏗 Kiến Trúc Hệ Thống

Dự án được xây dựng theo kiến trúc **Modular Monolith** với sự phân tách rõ ràng giữa các module nghiệp vụ. Mỗi module tuân theo cấu trúc phân lớp (Layered Architecture):

```
┌──────────────────────────────────────────────────┐
│                  Frontend (React)                │
│          Vite + TypeScript + React Router         │
└──────────────────────┬───────────────────────────┘
                       │ REST API (HTTP/JSON)
┌──────────────────────▼───────────────────────────┐
│               Backend (Spring Boot)              │
│                                                  │
│  ┌────────────┐ ┌──────────┐ ┌───────────────┐   │
│  │  Identity   │ │ Patient  │ │ Appointment   │   │
│  │  Module     │ │ Module   │ │ Module        │   │
│  └────────────┘ └──────────┘ └───────────────┘   │
│  ┌────────────┐ ┌──────────┐ ┌───────────────┐   │
│  │ Catalog    │ │Prescri-  │ │ Notification  │   │
│  │ Module     │ │ption     │ │ Module        │   │
│  └────────────┘ └──────────┘ └───────────────┘   │
│  ┌────────────┐ ┌──────────┐                     │
│  │ Reporting  │ │  Staff   │                     │
│  │ Module     │ │ Module   │                     │
│  └────────────┘ └──────────┘                     │
└──────────────────────┬───────────────────────────┘
                       │ JPA / Flyway
┌──────────────────────▼───────────────────────────┐
│                 MySQL Database                   │
│               (se330db - UTF8MB4)                │
└──────────────────────────────────────────────────┘
```

### Backend Modules

| Module           | Chức năng                                           |
| ---------------- | --------------------------------------------------- |
| `identity`       | Xác thực (JWT/OAuth2), phân quyền, OTP, reset mật khẩu |
| `patient`        | Quản lý hồ sơ bệnh nhân                            |
| `appointment`    | Quản lý lịch hẹn khám bệnh                         |
| `prescription`   | Quản lý đơn thuốc                                   |
| `catalog`        | Quản lý danh mục thuốc và dịch vụ y tế              |
| `staff`          | Quản lý thông tin bác sĩ, nhân viên                 |
| `notification`   | Gửi email thông báo (SMTP)                          |
| `reporting`      | Thống kê, báo cáo, dashboard                        |
| `shared`         | Các thành phần dùng chung (utilities, base classes)  |
| `config`         | Cấu hình CORS, security, ...                        |

Mỗi module tuân theo cấu trúc:
```
module/
├── api/              # REST Controllers
├── application/      # Use Cases / Services
├── domain/           # Entities, Repositories (interfaces)
└── infrastructure/   # Implementations, JPA Repositories
```

---

## 🛠 Công Nghệ Sử Dụng

### Backend

| Công nghệ                   | Phiên bản | Mục đích                         |
| ---------------------------- | --------- | -------------------------------- |
| **Java**                     | 23        | Ngôn ngữ lập trình              |
| **Spring Boot**              | 3.5.3     | Framework chính                  |
| **Spring Security + OAuth2** | —         | Xác thực & phân quyền (JWT)     |
| **Spring Data JPA**          | —         | Truy cập cơ sở dữ liệu         |
| **Spring Modulith**          | 1.2.9     | Hỗ trợ kiến trúc modular        |
| **Flyway**                   | —         | Quản lý migration database       |
| **MySQL**                    | —         | Cơ sở dữ liệu                   |
| **Spring Boot Mail**         | —         | Gửi email (SMTP)                |
| **Spring Boot Actuator**     | —         | Monitoring & health checks       |
| **Spotless (Google Format)** | 2.43.0    | Code formatting                 |
| **ArchUnit**                 | 1.3.0     | Kiểm tra kiến trúc (testing)    |

### Frontend

| Công nghệ              | Phiên bản | Mục đích                       |
| ----------------------- | --------- | ------------------------------ |
| **React**               | 19        | UI library                     |
| **TypeScript**          | ~5.9      | Type safety                    |
| **Vite**                | 8         | Build tool & dev server        |
| **React Router DOM**    | 7         | Client-side routing            |
| **TanStack React Query**| 5         | Server state management        |
| **Zustand**             | 5         | Client state management        |
| **React Hook Form**     | 7         | Form management                |
| **Zod**                 | 4         | Schema validation              |
| **Axios**               | 1         | HTTP client                    |
| **Playwright**          | —         | E2E testing                    |
| **ESLint + Prettier**   | —         | Linting & formatting           |

---

## ✨ Tính Năng

### 🔐 Xác Thực & Bảo Mật
- Đăng nhập / Đăng xuất với JWT (Access Token + Refresh Token)
- Phân quyền theo vai trò (RBAC): Admin, Doctor, Patient
- Quên mật khẩu với email reset link
- Đổi mật khẩu bắt buộc khi đăng nhập lần đầu
- Xác thực OTP
- Rate limiting cho chức năng reset mật khẩu

### 👨‍⚕️ Quản Lý (Admin & Bác Sĩ)
- **Dashboard** – Tổng quan thống kê hệ thống
- **Bệnh nhân** – CRUD hồ sơ bệnh nhân
- **Bác sĩ** – Quản lý danh sách bác sĩ
- **Lịch hẹn** – Đặt, sửa, hủy lịch hẹn khám
- **Thuốc** – Quản lý danh mục thuốc
- **Đơn thuốc** – Tạo, xem, quản lý đơn thuốc
- **Dịch vụ y tế** – Quản lý danh mục dịch vụ *(chỉ Admin)*

### 🧑‍💻 Cổng Bệnh Nhân
- **Dashboard cá nhân** – Tổng quan thông tin sức khỏe
- **Lịch sử khám bệnh** – Xem lại toàn bộ lịch sử khám
- **Lịch hẹn** – Xem lịch hẹn sắp tới
- **Hồ sơ cá nhân** – Cập nhật thông tin cá nhân

### 📧 Thông Báo
- Gửi email thông báo qua SMTP (Gmail)
- Hỗ trợ fallback khi chạy ở môi trường development

---

## 📦 Yêu Cầu Hệ Thống

Trước khi bắt đầu, hãy đảm bảo đã cài đặt:

| Phần mềm       | Phiên bản tối thiểu | Ghi chú                      |
| --------------- | -------------------- | ---------------------------- |
| **Java JDK**    | 23+                  | Khuyến nghị Oracle/OpenJDK   |
| **Maven**       | 3.9+                 | Hoặc dùng `./mvnw` wrapper   |
| **Node.js**     | 20+                  | Bao gồm npm                  |
| **MySQL**       | 8.0+                 | Server database               |
| **Git**         | 2.x                  | Version control               |

---

## 🚀 Hướng Dẫn Cài Đặt

### 1. Clone Repository

```bash
git clone https://github.com/TriThongVoSi/SE330-ElectronicMedicalRecord.git
cd SE330-ElectronicMedicalRecord
```

### 2. Cài Đặt Database

Đăng nhập vào MySQL và chạy script khởi tạo:

```bash
mysql -u root -p < init-mysql.sql
```

Script sẽ tự động:
- Tạo database `se330db`
- Tạo user `springuser` với password `springpass`
- Cấp quyền cho user trên database

> **💡 Tip:** Nếu cần reset database hoàn toàn, chạy:
> ```bash
> mysql -u root -p < reset-database.sql
> ```

### 3. Cấu Hình Backend

```bash
cd backend
cp .env.example .env
```

Chỉnh sửa file `.env` với các giá trị phù hợp:

```properties
# JWT – Thay bằng chuỗi ngẫu nhiên ít nhất 64 bytes
JWT_SIGNER_KEY=your-random-secret-key-at-least-64-bytes-long

# Database (giữ nguyên nếu dùng init-mysql.sql)
DB_URL=jdbc:mysql://localhost:3306/se330db
DB_USER=springuser
DB_PASS=springpass

# SMTP – Cấu hình email (tùy chọn cho development)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# OTP – Thay bằng chuỗi ngẫu nhiên ít nhất 32 ký tự
OTP_HASH_SECRET=your-random-secret-at-least-32-characters
```

### 4. Chạy Backend

```bash
# Từ thư mục backend/
./mvnw spring-boot:run
```

Backend sẽ chạy tại: **http://localhost:8080**

> Flyway sẽ tự động chạy các migration để tạo schema và seed data.

### 5. Cấu Hình & Chạy Frontend

```bash
cd frontend
cp .env.example .env.development
npm install
npm run dev
```

Frontend sẽ chạy tại: **http://localhost:5173**

### 6. Truy Cập Ứng Dụng

Mở trình duyệt và truy cập: **http://localhost:5173**

> **📝 Lưu ý:** Tài khoản mặc định được tạo bởi migration seed data. Kiểm tra file `V2__seed.sql` và `V8__identity_seed_roles_and_accounts.sql` để xem thông tin đăng nhập.

---

## 📁 Cấu Trúc Thư Mục

```
SE330-ElectronicMedicalRecord/
│
├── backend/                          # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/org/example/BenhAnDienTu/
│   │   │   │   ├── appointment/      # Module lịch hẹn
│   │   │   │   ├── catalog/          # Module danh mục (thuốc, dịch vụ)
│   │   │   │   ├── config/           # Cấu hình ứng dụng
│   │   │   │   ├── identity/         # Module xác thực & phân quyền
│   │   │   │   ├── notification/     # Module thông báo email
│   │   │   │   ├── patient/          # Module bệnh nhân
│   │   │   │   ├── prescription/     # Module đơn thuốc
│   │   │   │   ├── reporting/        # Module báo cáo
│   │   │   │   ├── shared/           # Thành phần dùng chung
│   │   │   │   └── staff/            # Module nhân viên
│   │   │   └── resources/
│   │   │       ├── application.yml   # Cấu hình Spring Boot
│   │   │       └── db/migration/     # Flyway migration scripts
│   │   └── test/                     # Unit & integration tests
│   ├── .env.example                  # Mẫu biến môi trường
│   └── pom.xml                       # Maven dependencies
│
├── frontend/                         # React Frontend
│   ├── src/
│   │   ├── app/                      # App shell, router, layout, providers
│   │   ├── assets/                   # Static assets
│   │   ├── components/               # Shared UI components
│   │   ├── core/                     # Core utilities, types, API client
│   │   │   ├── api/                  # Axios configuration
│   │   │   ├── auth/                 # Auth utilities
│   │   │   ├── config/               # App configuration
│   │   │   ├── i18n/                 # Internationalization
│   │   │   ├── types/                # TypeScript type definitions
│   │   │   └── utils/                # Helper functions
│   │   ├── features/                 # Feature modules
│   │   │   ├── appointments/         # Quản lý lịch hẹn
│   │   │   ├── auth/                 # Đăng nhập, quên mật khẩu
│   │   │   ├── dashboard/            # Dashboard thống kê
│   │   │   ├── drugs/                # Quản lý thuốc
│   │   │   ├── patient-portal/       # Cổng bệnh nhân
│   │   │   ├── patients/             # Quản lý bệnh nhân
│   │   │   ├── prescriptions/        # Quản lý đơn thuốc
│   │   │   ├── services/             # Quản lý dịch vụ y tế
│   │   │   └── staff/                # Quản lý bác sĩ
│   │   └── mocks/                    # Mock data cho testing
│   ├── e2e/                          # Playwright E2E tests
│   ├── .env.example                  # Mẫu biến môi trường
│   ├── package.json                  # NPM dependencies
│   └── vite.config.ts                # Vite configuration
│
├── .github/                          # GitHub Actions & workflows
├── docs/                             # Tài liệu dự án
├── init-mysql.sql                    # Script khởi tạo database
├── reset-database.sql                # Script reset database
├── .editorconfig                     # Quy ước code style
└── .gitignore                        # Git ignore rules
```

---

## 📖 API Documentation

Sau khi chạy backend, truy cập Swagger UI để xem tài liệu API:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

---

## 🧪 Testing

### Backend

```bash
cd backend

# Chạy toàn bộ tests
./mvnw test

# Kiểm tra code formatting
./mvnw spotless:check

# Tự động format code
./mvnw spotless:apply
```

### Frontend

```bash
cd frontend

# Kiểm tra TypeScript types
npm run typecheck

# Chạy ESLint
npm run lint

# Kiểm tra code formatting
npm run format:check

# Tự động format code
npm run format

# Chạy E2E tests (cần cài Playwright trước)
npm run e2e:install
npm run e2e

# Chạy E2E tests với UI
npm run e2e:ui
```

---

## 🤝 Đóng Góp

### Quy Trình

1. **Fork** repository
2. Tạo **branch** mới: `git checkout -b feature/ten-tinh-nang`
3. **Commit** thay đổi: `git commit -m "feat: mô tả thay đổi"`
4. **Push** lên branch: `git push origin feature/ten-tinh-nang`
5. Tạo **Pull Request**

### Quy Ước Code

- **Backend:** Tuân theo [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) (tự động format với Spotless)
- **Frontend:** Tuân theo ESLint + Prettier config có sẵn
- **Editor:** Sử dụng `.editorconfig` – indent 2 spaces (4 cho Java), UTF-8, LF line endings
- **Commit:** Sử dụng [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `refactor:`, ...)

### Trước Khi Tạo PR

```bash
# Backend
cd backend && ./mvnw spotless:apply && ./mvnw test

# Frontend
cd frontend && npm run format && npm run lint && npm run typecheck
```

---

## 📄 Giấy Phép

Dự án này được phát triển cho mục đích học tập trong khuôn khổ môn học **SE330 – Kiến trúc và Thiết kế Phần mềm**.

---

<p align="center">
  Made with ❤️ by <strong>SE330 Team</strong>
</p>
