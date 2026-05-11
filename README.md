# ✈️ FlySphere – Flight Booking System

FlySphere is a full-stack Flight Booking Web Application built using:

- ✅ **Spring Boot (Java)** – Backend
- ✅ **Angular** – Frontend
- ✅ **JWT Authentication**
- ✅ **REST APIs**
- ✅ **MySQL / Relational Database**

This project allows users to search flights, book tickets, manage bookings, and includes an admin panel for managing flights.

---

# 📌 Project Structure

```
flysphere-java_springboot/
│
├── flysphere-backend/      → Spring Boot Backend
├── flysphere-frontend/     → Angular Frontend
└── README.md
```

---

# 🖥️ Backend – Spring Boot

### 📂 Location:
`flysphere-backend/`

### 🚀 Technologies Used
- Java 17+
- Spring Boot
- Spring Security
- JWT Authentication
- Spring Data JPA
- Maven
- MySQL

### ✅ Features
- User Registration & Login
- JWT-based Authentication
- Flight CRUD (Admin)
- Search Flights
- Book Flights
- Booking Details
- Secure APIs using Spring Security

### ▶️ How To Run Backend

1. Navigate to backend folder:

```
cd flysphere-backend
```

2. Configure database in:

```
src/main/resources/application.properties
```

3. Run the application:

```
mvn spring-boot:run
```

Backend runs on:
```
http://localhost:8080
```

---

# 🌐 Frontend – Angular

### 📂 Location:
`flysphere-frontend/`

### 🚀 Technologies Used
- Angular
- TypeScript
- Angular Router
- HTTP Client
- Angular Guards
- JWT Interceptor
- Material UI (if used)

### ✅ Features
- User Login & Registration
- Flight Search
- Booking Flow
- Booking Confirmation
- Admin Dashboard
- Create / Edit / Delete Flights

### ▶️ How To Run Frontend

1. Navigate to frontend folder:

```
cd flysphere-frontend
```

2. Install dependencies:

```
npm install
```

3. Start Angular server:

```
ng serve
```

Frontend runs on:
```
http://localhost:4200
```

---

# 🔐 Authentication Flow

- User logs in
- Backend generates JWT token
- Token stored in frontend
- Token attached to API requests
- Secured endpoints validate JWT

---

# 📊 Database Entities

- User
- Flight
- Booking
- Passenger
- BookingSegment
## `flightmgtable` table (Columns: 20)

- `flightid` integer
- `airlinename` varchar(100)
- `flighttype` varchar(100)
- `flightno` varchar(20)
- `departureairport` varchar(10)
- `arrivalairport` varchar(10)
- `departuredate` date
- `arrivaldate` date
- `departuretime` time without time zone
- `arrivaltime` time without time zone
- `totaleconomyseats` integer
- `totalbusinessseats` integer
- `totalfirstclassseats` integer
- `economyadultfare` double precision
- `economychildfare` double precision
- `businessadultfare` double precision
- `businesschildfare` double precision
- `firstadultfare` double precision
- `firstchildfare` double precision
- `flightstatus` varchar(50)
## `users` table (Columns: 10)

- `userid` varchar(50)
- `firstname` varchar(50)
- `lastname` varchar(50)
- `phone` varchar(15)
- `email` varchar(100)
- `password` varchar(255)
- `role` varchar(20)
- `id` bigint
- `created_at` timestamp(6) without time zone
- `updated_at` timestamp(6) without time zone
## `passengers` table (Columns: 20)

- `id` bigint
- `booking_id` bigint
- `title` varchar(10)
- `first_name` varchar(50)
- `last_name` varchar(50)
- `age` integer
- `type` varchar(10)
- `created_at` timestamp(6) without time zone
- `updated_at` timestamp(6) without time zone
- `outbound_seat` varchar(50)
- `outbound_meal` varchar(50)
- `outbound_baggage` varchar(50)
- `return_seat` varchar(50)
- `return_meal` varchar(50)
- `return_baggage` varchar(50)
- `insurance_selected` boolean
- `email` varchar(255)
- `phone` varchar(20)
- `outbound_seat_no` varchar(20)
- `return_seat_no` varchar(20)
## `booking_segments` table (Columns: 6)

- `id` bigint
- `booking_id` varchar(20)
- `segment_no` integer
- `flight_id` integer
- `created_at` timestamp(6) without time zone
- `updated_at` timestamp(6) without time zone
## `bookings` table (Columns: 15)

- `id` bigint
- `booking_id` varchar(20)
- `user_id` varchar(255)
- `flight_id` integer
- `total_amount` double precision
- `status` varchar(20)
- `created_at` timestamp(6) without time zone
- `updated_at` timestamp(6) without time zone
- `cabin_class` varchar(255)
- `outbound_cabin_class` varchar(255)
- `return_cabin_class` varchar(255)
- `trip_type` varchar(255)
- `contact_phone` varchar(255)
- `contact_email` varchar(255)
- `insurance_selected` boolean

---

# 📡 Important API Endpoints (Sample)

| Method | Endpoint | Description |
|--------|----------|------------|
| POST | /auth/register | Register user |
| POST | /auth/login | Login user |
| GET | /flights | Get all flights |
| POST | /flights | Create flight (Admin) |
| POST | /bookings | Create booking |
| GET | /bookings/{id} | Get booking details |

---

# 🛠️ Admin Module

Admin can:
- Add new flights
- Edit flights
- Delete flights
- View flight list

---

# 👩‍💻 Developed By

**Katarey Pooja**

---

# 📌 Future Improvements

- Payment Integration
- Email Confirmation
- Role-based Dashboard
- Deployment (AWS / Render / Railway)
- Docker Support

---

# 🚀 How To Push Updates

```
git add .
git commit -m "Updated feature"
git push
```

---

# 📄 License

This project is developed for learning and academic purposes.

---

# ✅ Status

✔ Backend Completed  
✔ Frontend Completed  
✔ JWT Security Implemented  
✔ Admin Module Added  

---

# 📬 Contact

For collaboration or queries:
- GitHub: https://github.com/katareypooja

---

✨ FlySphere – Making Flight Booking Simple ✨
