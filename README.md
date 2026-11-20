# Hotel Management System

A hotel management web app. Staff can take a customer's details, book them a room (picked by hand or allocated automatically), check guests in and out, and manage rooms and customers. Everything is stored in MySQL.

| Layer     | Technology                                            |
|-----------|-------------------------------------------------------|
| Front end | HTML, CSS, JavaScript (no frameworks, `fetch` for the API) |
| Back end  | Spring Boot 3 (Java 17+): REST API, Spring Data JPA, Bean Validation |
| Database  | MySQL 8 (schema script for MySQL Workbench)            |

## Project structure

```
Hotelmanagement_System/
├── database/
│   ├── schema.sql          # creates hotel_db, tables and sample rooms (run in MySQL Workbench)
│   └── queries.sql         # useful queries to inspect the data
└── backend/                # Spring Boot application
    ├── pom.xml
    └── src/main/
        ├── java/com/hotel/management/
        │   ├── entity/       # Room, Customer, Booking + enums
        │   ├── repository/   # Spring Data JPA repositories
        │   ├── dto/          # request/response objects with validation
        │   ├── service/      # business logic (room allocation, check-in/out)
        │   ├── controller/   # REST controllers (/api/...)
        │   ├── exception/    # error types + global JSON error handler
        │   └── config/       # CORS, sample-data seeder
        └── resources/
            ├── application.properties
            └── static/       # FRONT END: HTML pages, css/, js/
```

The front end lives in `backend/src/main/resources/static`, so Spring Boot serves it. One command runs the whole app.

## How to run

### 1. Prerequisites
- JDK 17 or newer
- Maven 3.8+ (or open the `backend` folder in IntelliJ IDEA / Eclipse / VS Code)
- MySQL Server 8 and MySQL Workbench

### 2. Create the database (MySQL Workbench)
1. Open MySQL Workbench and connect to your local server.
2. **File → Open SQL Script…** → choose `database/schema.sql`.
3. Click the ⚡ **Execute** button. This creates `hotel_db` with the `rooms`, `customers` and `bookings` tables and 9 sample rooms.

> You can skip this step. On first start the app creates `hotel_db` and the tables itself and adds the same sample rooms.

### 3. Set your MySQL username and password
The defaults in `backend/src/main/resources/application.properties` are `root` / `root`. Change them there, or set environment variables:

```bash
export DB_USERNAME=root
```

```bash
export DB_PASSWORD=your_mysql_password
```

### 4. Start the application
```bash
cd backend
```

```bash
mvn spring-boot:run
```

Then open **http://localhost:8080** in your browser.

## Using the app

| Page | What it does |
|------|--------------|
| **Dashboard** (`index.html`) | Room occupancy, upcoming bookings, guests in house, revenue, today's arrivals and departures with one-click check-in/check-out |
| **New Booking** (`book.html`) | 1) choose dates, number of guests and room type → free rooms appear; 2) pick a room or keep **Auto-allocate**; 3) enter the guest's details → the booking is saved and the allocated room is shown. Returning guests are recognised by email and their details are filled in. |
| **Bookings** (`bookings.html`) | Search/filter bookings. Check in, check out, edit (dates, guests, room), cancel or delete them |
| **Rooms** (`rooms.html`) | Add, edit and delete rooms. Put a room under maintenance to stop it being allocated |
| **Customers** (`customers.html`) | Search, add, edit and delete customers, and see each customer's booking history |

### How room allocation works
- A room is free for a stay when it is not under **maintenance**, its capacity fits the guests, and no `BOOKED` or `CHECKED_IN` booking overlaps the dates.
- Stays may be back to back: a new guest can check in on the day the previous guest checks out.
- **Auto-allocate** picks the cheapest free room of the requested type (or any type) that fits the guests.
- The chosen room is locked in the database (`SELECT … FOR UPDATE`) while the booking is saved, so two people booking at the same moment can never get the same room.
- Total amount = price per night × number of nights.

### Booking lifecycle
```
BOOKED ──check-in──► CHECKED_IN ──check-out──► CHECKED_OUT
   │                 (room → OCCUPIED)          (room → AVAILABLE)
   └──cancel──► CANCELLED
```
- Check-in is allowed from the check-in date until the day before check-out.
- Only `BOOKED` bookings can be edited or cancelled.
- A room or customer that has bookings cannot be deleted. Set a room to maintenance instead.

## REST API

Base URL: `http://localhost:8080/api`. All bodies are JSON and dates use `yyyy-MM-dd`.

### Rooms
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/rooms` | List all rooms |
| GET    | `/rooms/{id}` | Get one room |
| GET    | `/rooms/available?checkIn=2026-12-01&checkOut=2026-12-03&guests=2&type=DOUBLE` | Free rooms for a stay (`type` optional) |
| POST   | `/rooms` | Create a room |
| PUT    | `/rooms/{id}` | Update a room |
| DELETE | `/rooms/{id}` | Delete a room (only if it has no bookings) |

```json
{ "roomNumber": "401", "roomType": "SUITE", "pricePerNight": 12000, "capacity": 4, "floor": 4,
  "status": "AVAILABLE", "description": "Penthouse suite" }
```
`roomType`: `SINGLE`, `DOUBLE`, `DELUXE` or `SUITE`. `status`: `AVAILABLE` or `MAINTENANCE` (`OCCUPIED` is set by check-in).

### Customers
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/customers?search=asha` | List customers (`search` optional: name, email or phone) |
| GET    | `/customers/{id}` | Get one customer |
| GET    | `/customers/{id}/bookings` | A customer's booking history |
| POST   | `/customers` | Create a customer |
| PUT    | `/customers/{id}` | Update a customer |
| DELETE | `/customers/{id}` | Delete a customer (only if they have no bookings) |

### Bookings
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/bookings?status=BOOKED` | List bookings (`status` optional) |
| GET    | `/bookings/{id}` | Get one booking |
| POST   | `/bookings` | **Book a room**: saves the customer's details and allocates a room |
| PUT    | `/bookings/{id}` | Change dates, guests, room or special requests |
| PATCH  | `/bookings/{id}/check-in` | Check the guest in |
| PATCH  | `/bookings/{id}/check-out` | Check the guest out |
| PATCH  | `/bookings/{id}/cancel` | Cancel the booking |
| DELETE | `/bookings/{id}` | Delete the booking |

Create a booking (omit `roomId` to auto-allocate, omit `roomType` to accept any type):
```json
{
  "customer": {
    "firstName": "Asha", "lastName": "Rao", "email": "asha@example.com", "phone": "+91 98765 43210",
    "address": "MG Road, Bengaluru", "idProofType": "Aadhaar", "idProofNumber": "1234 5678 9012"
  },
  "roomType": "DOUBLE",
  "roomId": null,
  "checkInDate": "2026-12-01",
  "checkOutDate": "2026-12-04",
  "numberOfGuests": 2,
  "specialRequests": "Late check-in"
}
```
Response `201 Created`:
```json
{ "id": 1, "customerName": "Asha Rao", "roomNumber": "103", "roomType": "DOUBLE",
  "checkInDate": "2026-12-01", "checkOutDate": "2026-12-04", "nights": 3,
  "totalAmount": 7500.00, "status": "BOOKED", "...": "..." }
```

### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard/stats` | Room, customer, booking and revenue counts |

### Errors
Errors come back as JSON with a matching HTTP status: `400` validation, `404` not found, `409` conflict (for example, no room free or room already booked).
```json
{ "status": 409, "error": "Conflict",
  "message": "Room 103 is already booked between 2026-12-02 and 2026-12-05", "fieldErrors": null }
```

## Database tables

- **rooms**: `id`, `room_number` (unique), `room_type`, `price_per_night`, `capacity`, `floor`, `status`, `description`
- **customers**: `id`, `first_name`, `last_name`, `email` (unique), `phone`, `address`, `id_proof_type`, `id_proof_number`, `created_at`
- **bookings**: `id`, `customer_id` → customers, `room_id` → rooms, `check_in_date`, `check_out_date`, `number_of_guests`, `total_amount`, `status`, `special_requests`, `created_at`
