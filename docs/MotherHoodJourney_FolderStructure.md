                           **MotherHood Journey**

**Project Folder Structure Guide**

*What every folder is, what every file does, and where new code goes*

| Organisation | IgireRwanda Organization |
| :---- | :---- |
| **Programme** | SheCanCode Bootcamp |
| **Domain** | Public Health & Digital Services |
| **Backend** | Java 21 \+ Spring Boot 3 \+ PostgreSQL |
| **Version** | v1.0 April 2026 |

## **1\. Overview How the Project is Organised**

The MotherHood Journey backend is a Spring Boot 3 application. Instead of grouping all controllers together or all repositories together, the code is organised by domain. Each business area (mothers, children, appointments, etc.) lives in its own self-contained package.

Inside each domain package, the code is then split into layers: controller, service, repository, entity, dto, and enums. This makes it very easy to find any file: you first go to the domain, then to the layer.

| KEY RULE | Domain first, then layer. If you are working on appointments, everything you need is inside the appointment/ package not scattered across the project. |
| :---: | :---- |

## **2\. Full Folder Structure**

Below is the complete folder tree of the project. Every folder and file is annotated so you know exactly what it contains.

| motherhood-journey/ |
| :---- |
| ├── src/ |
| │   ├── main/ |
| │   │   ├── java/com/motherhood/journey/      ← All application code lives here |
| │   │   │   │ |
| │   │   │   ├── maternal/                     ← Domain: Mothers & Pregnancies & Visits |
| │   │   │   │   ├── controller/ |
| │   │   │   │   │   ├── MotherController.java |
| │   │   │   │   │   ├── PregnancyController.java |
| │   │   │   │   │   └── HealthVisitController.java |
| │   │   │   │   ├── service/ |
| │   │   │   │   │   ├── MotherService.java |
| │   │   │   │   │   ├── MotherServiceImpl.java |
| │   │   │   │   │   ├── PregnancyService.java |
| │   │   │   │   │   ├── PregnancyServiceImpl.java |
| │   │   │   │   │   ├── HealthVisitService.java |
| │   │   │   │   │   └── HealthVisitServiceImpl.java |
| │   │   │   │   ├── repository/ |
| │   │   │   │   │   ├── MotherRepository.java |
| │   │   │   │   │   ├── PregnancyRepository.java |
| │   │   │   │   │   └── HealthVisitRepository.java |
| │   │   │   │   ├── entity/ |
| │   │   │   │   │   ├── Mother.java |
| │   │   │   │   │   ├── Pregnancy.java |
| │   │   │   │   │   ├── HealthVisit.java |
| │   │   │   │   │   ├── Diagnosis.java |
| │   │   │   │   │   └── Prescription.java |
| │   │   │   │   ├── dto/ |
| │   │   │   │   │   ├── request/ |
| │   │   │   │   │   │   ├── CreateMotherRequest.java |
| │   │   │   │   │   │   ├── UpdateMotherRequest.java |
| │   │   │   │   │   │   ├── CreatePregnancyRequest.java |
| │   │   │   │   │   │   ├── UpdatePregnancyRequest.java |
| │   │   │   │   │   │   ├── CreateHealthVisitRequest.java |
| │   │   │   │   │   │   └── UpdateHealthVisitRequest.java |
| │   │   │   │   │   └── response/ |
| │   │   │   │   │       ├── MotherResponse.java |
| │   │   │   │   │       ├── PregnancyResponse.java |
| │   │   │   │   │       └── HealthVisitResponse.java |
| │   │   │   │   └── enums/ |
| │   │   │   │       ├── PregnancyStatus.java |
| │   │   │   │       └── VisitType.java |
| │   │   │   │ |
| │   │   │   ├── child/                        ← Domain: Children & Vaccinations |
| │   │   │   │   ├── controller/ |
| │   │   │   │   │   └── ChildController.java |
| │   │   │   │   ├── service/ |
| │   │   │   │   │   ├── ChildService.java |
| │   │   │   │   │   └── ChildServiceImpl.java |
| │   │   │   │   ├── repository/ |
| │   │   │   │   │   └── ChildRepository.java |
| │   │   │   │   ├── entity/ |
| │   │   │   │   │   └── Child.java |
| │   │   │   │   ├── dto/ |
| │   │   │   │   │   ├── request/ |
| │   │   │   │   │   │   ├── CreateChildRequest.java |
| │   │   │   │   │   │   └── UpdateChildRequest.java |
| │   │   │   │   │   └── response/ |
| │   │   │   │   │       └── ChildResponse.java |
| │   │   │   │   └── enums/ |
| │   │   │   │       └── ChildStatus.java |
| │   │   │   │ |
| │   │   │   ├── appointment/                  ← Domain: Scheduling & Reminders |
| │   │   │   │   ├── controller/ |
| │   │   │   │   │   └── AppointmentController.java |
| │   │   │   │   ├── service/ |
| │   │   │   │   │   ├── AppointmentService.java |
| │   │   │   │   │   └── AppointmentServiceImpl.java |
| │   │   │   │   ├── repository/ |
| │   │   │   │   │   └── AppointmentRepository.java |
| │   │   │   │   ├── entity/ |
| │   │   │   │   │   └── Appointment.java |
| │   │   │   │   ├── dto/ |
| │   │   │   │   │   ├── request/ |
| │   │   │   │   │   │   ├── CreateAppointmentRequest.java |
| │   │   │   │   │   │   └── UpdateAppointmentRequest.java |
| │   │   │   │   │   └── response/ |
| │   │   │   │   │       └── AppointmentResponse.java |
| │   │   │   │   └── enums/ |
| │   │   │   │       └── AppointmentStatus.java |
| │   │   │   │ |
| │   │   │   ├── consent/                      ← Domain: Data Consent Records |
| │   │   │   │   ├── controller/ |
| │   │   │   │   │   └── ConsentController.java |
| │   │   │   │   ├── service/ |
| │   │   │   │   │   ├── ConsentService.java |
| │   │   │   │   │   └── ConsentServiceImpl.java |
| │   │   │   │   ├── repository/ |
| │   │   │   │   │   └── ConsentRepository.java |
| │   │   │   │   ├── entity/ |
| │   │   │   │   │   └── ConsentRecord.java |
| │   │   │   │   ├── dto/ |
| │   │   │   │   │   ├── request/ |
| │   │   │   │   │   │   ├── CreateConsentRequest.java |
| │   │   │   │   │   │   └── UpdateConsentRequest.java |
| │   │   │   │   │   └── response/ |
| │   │   │   │   │       └── ConsentResponse.java |
| │   │   │   │   └── enums/ |
| │   │   │   │       └── ConsentType.java |
| │   │   │   │ |
| │   │   │   ├── identity/                     ← Domain: Users, Auth & JWT |
| │   │   │   │   ├── controller/ |
| │   │   │   │   │   ├── AuthController.java |
| │   │   │   │   │   └── UserController.java |
| │   │   │   │   ├── service/ |
| │   │   │   │   │   ├── AuthService.java |
| │   │   │   │   │   ├── AuthServiceImpl.java |
| │   │   │   │   │   ├── UserService.java |
| │   │   │   │   │   └── UserServiceImpl.java |
| │   │   │   │   ├── repository/ |
| │   │   │   │   │   └── UserRepository.java |
| │   │   │   │   ├── entity/ |
| │   │   │   │   │   ├── User.java |
| │   │   │   │   │   └── Role.java |
| │   │   │   │   ├── dto/ |
| │   │   │   │   │   ├── request/ |
| │   │   │   │   │   │   ├── LoginRequest.java |
| │   │   │   │   │   │   ├── RegisterRequest.java |
| │   │   │   │   │   │   └── UpdateUserRequest.java |
| │   │   │   │   │   └── response/ |
| │   │   │   │   │       ├── AuthResponse.java |
| │   │   │   │   │       └── UserResponse.java |
| │   │   │   │   └── enums/ |
| │   │   │   │       └── RoleType.java |
| │   │   │   │ |
| │   │   │   ├── notification/                 ← Domain: SMS Notifications |
| │   │   │   │   ├── controller/ |
| │   │   │   │   │   └── NotificationController.java |
| │   │   │   │   ├── service/ |
| │   │   │   │   │   ├── NotificationService.java |
| │   │   │   │   │   └── NotificationServiceImpl.java |
| │   │   │   │   ├── repository/ |
| │   │   │   │   │   └── NotificationRepository.java |
| │   │   │   │   ├── entity/ |
| │   │   │   │   │   └── Notification.java |
| │   │   │   │   ├── dto/ |
| │   │   │   │   │   ├── request/ |
| │   │   │   │   │   │   └── SendNotificationRequest.java |
| │   │   │   │   │   └── response/ |
| │   │   │   │   │       └── NotificationResponse.java |
| │   │   │   │   └── enums/ |
| │   │   │   │       └── NotificationType.java |
| │   │   │   │ |
| │   │   │   ├── government/                   ← Domain: Gov Integration & Sync |
| │   │   │   │   ├── controller/ |
| │   │   │   │   │   └── GovernmentController.java |
| │   │   │   │   ├── service/ |
| │   │   │   │   │   ├── GovernmentService.java |
| │   │   │   │   │   └── GovernmentServiceImpl.java |
| │   │   │   │   ├── repository/ |
| │   │   │   │   │   └── GovernmentRepository.java |
| │   │   │   │   ├── entity/ |
| │   │   │   │   │   └── GovernmentRecord.java |
| │   │   │   │   ├── dto/ |
| │   │   │   │   │   ├── request/ |
| │   │   │   │   │   │   └── GovernmentRequest.java |
| │   │   │   │   │   └── response/ |
| │   │   │   │   │       └── GovernmentResponse.java |
| │   │   │   │   └── enums/ |
| │   │   │   │       └── GovernmentSource.java |
| │   │   │   │ |
| │   │   │   ├── geo/                          ← Domain: Rwanda Location Hierarchy |
| │   │   │   │   ├── controller/ |
| │   │   │   │   │   └── GeoController.java |
| │   │   │   │   ├── service/ |
| │   │   │   │   │   ├── GeoService.java |
| │   │   │   │   │   └── GeoServiceImpl.java |
| │   │   │   │   ├── repository/ |
| │   │   │   │   │   └── GeoRepository.java |
| │   │   │   │   ├── entity/ |
| │   │   │   │   │   └── GeoLocation.java |
| │   │   │   │   ├── dto/ |
| │   │   │   │   │   ├── request/ |
| │   │   │   │   │   │   └── CreateGeoRequest.java |
| │   │   │   │   │   └── response/ |
| │   │   │   │   │       └── GeoResponse.java |
| │   │   │   │   └── enums/ |
| │   │   │   │       └── LocationType.java |
| │   │   │   │ |
| │   │   │   ├── admin/                        ← Domain: Admin Dashboard |
| │   │   │   │   ├── controller/ |
| │   │   │   │   │   └── AdminController.java |
| │   │   │   │   ├── service/ |
| │   │   │   │   │   ├── AdminService.java |
| │   │   │   │   │   └── AdminServiceImpl.java |
| │   │   │   │   └── dto/ |
| │   │   │   │       └── response/ |
| │   │   │   │           └── AdminDashboardResponse.java |
| │   │   │   │ |
| │   │   │   ├── me/                           ← Domain: Current User's Profile |
| │   │   │   │   ├── controller/ |
| │   │   │   │   │   └── MeController.java |
| │   │   │   │   ├── service/ |
| │   │   │   │   │   ├── MeService.java |
| │   │   │   │   │   └── MeServiceImpl.java |
| │   │   │   │   └── dto/ |
| │   │   │   │       └── response/ |
| │   │   │   │           └── MeProfileResponse.java |
| │   │   │   │ |
| │   │   │   ├── common/                       ← Shared utilities (used by ALL domains) |
| │   │   │   │   ├── dto/ |
| │   │   │   │   │   └── ApiResponse.java |
| │   │   │   │   ├── exception/ |
| │   │   │   │   │   ├── GlobalExceptionHandler.java |
| │   │   │   │   │   └── CustomException.java |
| │   │   │   │   └── util/ |
| │   │   │   │       └── DateUtils.java |
| │   │   │   │ |
| │   │   │   ├── config/                       ← Spring configuration classes |
| │   │   │   │   ├── SecurityConfig.java |
| │   │   │   │   ├── CorsConfig.java |
| │   │   │   │   └── OpenApiConfig.java |
| │   │   │   │ |
| │   │   │   ├── security/                     ← JWT filter and token utilities |
| │   │   │   │   ├── JwtFilter.java |
| │   │   │   │   ├── JwtUtil.java |
| │   │   │   │   └── CustomUserDetailsService.java |
| │   │   │   │ |
| │   │   │   ├── scheduler/                    ← Automated background jobs (cron) |
| │   │   │   │   └── ReminderScheduler.java |
| │   │   │   │ |
| │   │   │   └── MotherhoodJourneyApplication.java   ← Entry point — starts the app |
| │   │   │ |
| │   │   └── resources/ |
| │   │       ├── application.yml               ← Database, JWT, SMS config |
| │   │       └── db/migration/                 ← Flyway SQL migration scripts |
| │   │           ├── V1\_\_seed\_geo\_locations.sql |
| │   │           ├── V2\_\_create\_users.sql |
| │   │           └── ...  (one script per schema change) |
| │   │ |
| │   └── test/java/com/motherhood/journey/     ← Test code mirrors main structure |
| │       ├── controller/ |
| │       ├── service/ |
| │       └── repository/ |
| │ |
| └── pom.xml                                   ← Maven: dependencies & build config |

## **3\. Domain Modules What Each One Is For**

Each top-level domain folder is a self-contained slice of the application. Below is what each one does.

| Package | What it handles |
| :---- | :---- |
| maternal/ | Everything about mothers: registration, pregnancy records, ANC/PNC health visits, diagnoses, and prescriptions. |
| child/ | Newborn registration, digital birth certificates, vaccination status tracking, and growth monitoring. |
| appointment/ | Booking appointments, sending 24-hour SMS reminders, and tracking no-shows for analytics. |
| consent/ | Recording and revoking a mother's consent before sharing her data with government systems. Required by Rwanda Law No. 058/2021. |
| identity/ | User accounts, login, JWT token issuance, and role assignment (PATIENT, HEALTH\_WORKER, FACILITY\_ADMIN, etc.). |
| notification/ | Sending and tracking outbound SMS messages via Africa's Talking API  vaccination reminders, health tips, status updates. |
| government/ | Syncing data with NIDA (identity verification), MoH HMIS (health statistics), and Irembo (citizen service requests). |
| geo/ | Rwanda's 5-level administrative hierarchy: Province → District → Sector → Cell → Village. Every user and record is pinned to a location. |
| admin/ | Facility administrator dashboard: appointment counts, top diagnoses, no-show rates, and capacity data. |
| me/ | The currently logged-in user's own profile, linked children, and upcoming appointments  accessed via /api/v1/me. |

## **4\. The Layers Inside Every Domain**

Every domain (except the simplified admin and me modules) contains the same set of sub-folders. Here is what each layer is, what it does, and the rule that governs it.

### **4.1  controller/**

Controllers are the entry point for HTTP requests. Each controller class handles one group of related API endpoints. A controller reads the incoming request, passes it to the service, and sends the response back to the client.

| RULE | A controller must never contain business logic. It only calls the service and returns a response. All decisions and calculations belong in the service layer. |
| :---: | :---- |

### **4.2  service/**

The service layer holds all business logic. Every domain has two files: a Service interface that defines the methods, and a ServiceImpl class that implements them. The interface and the implementation are always kept separate.

| // MotherService.java the contract (interface) example |
| :---- |
| public interface MotherService { |
|     MotherResponse createMother(CreateMotherRequest request); |
|     MotherResponse getMotherById(UUID id); |
|     void deactivateMother(UUID id); |
| } |
|  |
| // MotherServiceImpl.java the actual implementation example  |
| @Service |
| public class MotherServiceImpl implements MotherService { |
|     private final MotherRepository motherRepository; |
|     // ... all the real logic lives here |
| } |

| RULE | The interface makes the code testable in unit tests, you can swap in a fake (mock) implementation. ServiceImpl is annotated with @Service so Spring manages it automatically. |
| :---: | :---- |

### **4.3  repository/**

Repositories handle all communication with the PostgreSQL database. Each repository is a Java interface that extends JpaRepository. Spring Data JPA automatically generates the SQL for standard operations (save, find, delete). Custom queries are declared as methods using Spring's naming convention.

| // MotherRepository.java example  |
| :---- |
| public interface MotherRepository extends JpaRepository\<Mother, UUID\> { |
|  |
|     // Spring generates SQL from the method name automatically |
|     Optional\<Mother\> findByUserId(UUID userId); |
|     boolean existsByHealthId(String healthId); |
|  |
|     // Custom query for anything more complex |
|     @Query("SELECT m FROM Mother m WHERE m.geoLocation.sector \= :sector") |
|     List\<Mother\> findBySector(@Param("sector") String sector); |
| } |

| RULE | Repositories are only injected into ServiceImpl classes, never into controllers. Never write native SQL with string concatenation. Always use parameterised queries to prevent SQL injection. |
| :---: | :---- |

### **4.4  entity/**

Entities are Java classes that represent database tables. Each entity maps directly to one table in PostgreSQL. They are annotated with @Entity and use a UUID primary key. Relationships to other tables (foreign keys) are declared using JPA annotations.

| // Mother.java these is an example  |
| :---- |
| @Entity |
| @Table(name \= "mothers") |
| public class Mother { |
|  |
|     @Id |
|     @GeneratedValue(strategy \= GenerationType.UUID) |
|     private UUID id; |
|  |
|     @OneToOne(fetch \= FetchType.LAZY) |
|     @JoinColumn(name \= "user\_id", nullable \= false, unique \= true) |
|     private User user;                    // FK → users table |
|  |
|     @ManyToOne(fetch \= FetchType.LAZY) |
|     @JoinColumn(name \= "facility\_id", nullable \= false) |
|     private Facility facility;             // FK → facilities table |
|  |
|     @Column(name \= "health\_id", unique \= true, nullable \= false) |
|     private String healthId;               // e.g. MH-2026-04821 |
| } |

| RULE | Entities are never sent directly to the API client. They must always be converted to a Response DTO before leaving the service layer. This prevents exposing internal database fields or sensitive data. |
| :---: | :---- |

### **4.5  dto/  (Data Transfer Objects)**

DTOs are simple Java classes (or records) used to carry data across the API boundary. They have no JPA annotations; they are not database objects. The dto/ folder is split into two sub-folders:

* **request/** Objects that arrive FROM the client (e.g. form submissions). These have validation annotations like @NotBlank and @NotNull.

* **response/** Objects sent BACK to the client. These contain only the fields that are safe to expose.

| // dto/request/CreateMotherRequest.java incoming data these is an example  |
| :---- |
| public record CreateMotherRequest( |
|     @NotBlank String nationalId, |
|     @NotBlank String phoneNumber, |
|     @NotNull UUID facilityId |
| ) {} |
|  |
| // dto/response/MotherResponse.java — outgoing data (safe fields only) |
| public record MotherResponse( |
|     UUID id, |
|     String healthId, |
|     String firstName, |
|     String lastName, |
|     String facilityName |
| ) {} |

### **4.6  enums/**

Enums define the fixed set of allowed values for status and type fields. Using enums instead of plain strings prevents typos and makes the code self-documenting. They are stored as strings in the database so entries remain readable.

| Enum File | Allowed Values |
| :---- | :---- |
| PregnancyStatus.java | ACTIVE | DELIVERED | LOST | TRANSFERRED |
| VisitType.java | ANC | PNC | IMMUNIZATION | SICK\_CHILD | GROWTH\_MONITORING |
| ChildStatus.java | HEALTHY | AT\_RISK | CRITICAL |
| AppointmentStatus.java | SCHEDULED | COMPLETED | NO\_SHOW | CANCELLED |
| ConsentType.java | GOV\_DATA\_SHARE | SMS\_REMINDERS | RESEARCH | FACILITY\_TRANSFER |
| NotificationType.java | VACCINATION\_REMINDER | APPOINTMENT | HEALTH\_TIP | SERVICE\_STATUS | EMERGENCY |
| RoleType.java | PATIENT | HEALTH\_WORKER | FACILITY\_ADMIN | DISTRICT\_OFFICER | GOVERNMENT\_ANALYST | MOH\_ADMIN |
| GovernmentSource.java | NIDA | HMIS | IREMBO | RURA |
| LocationType.java | PROVINCE | DISTRICT | SECTOR | CELL | VILLAGE |

## **5\. Shared Packages**

These packages do not belong to any single domain. They provide infrastructure and utilities used by all domains.

### **5.1  common/**

Shared code that every domain needs. It contains the standard API response wrapper, the global error handler, and utility methods.

| File | What it does |
| :---- | :---- |
| ApiResponse.java | A generic wrapper returned by every endpoint. It contains: success (true/false), a message string, and the data payload. This ensures every API response has the same shape. |
| GlobalExceptionHandler.java | A single @ControllerAdvice class that catches all exceptions thrown anywhere in the app and converts them into a clean JSON error response. Prevents stack traces from reaching the client. |
| CustomException.java | An application-level exception class. Services throw this when a business rule is violated (e.g. mother not found, duplicate national ID). It carries an HTTP status code and a message. |
| DateUtils.java | Utility methods used across domains: calculating the Estimated Due Date from an LMP date, formatting dates for Rwanda locale, computing how many days overdue a vaccination is. |

### **5.2  config/**

Spring configuration classes that set up how the application behaves.

| File | What it does |
| :---- | :---- |
| SecurityConfig.java | Defines which endpoints are public (login, register) and which require authentication. Sets role-based access rules e.g. only FACILITY\_ADMIN can approve service requests. |
| CorsConfig.java | Allows the Next.js frontend and the Irembo callback URL to call the API. All other origins are blocked in production. |
| OpenApiConfig.java | Configures Swagger UI. Adds JWT bearer token support so developers can test secured endpoints directly from the Swagger interface. |

### **5.3  security/**

Classes that handle JWT token creation, validation, and authentication on every incoming request.

| File | What it does |
| :---- | :---- |
| JwtFilter.java | Runs before every request. Reads the Authorization header, extracts the JWT, validates it, and sets the authenticated user in Spring's security context so the rest of the app knows who is calling. |
| JwtUtil.java | Creates JWT tokens when a user logs in. Validates tokens on each request. Extracts the user ID and role from the token. Tokens expire after 15 minutes. |
| CustomUserDetailsService.java | Loads the user record from the database by phone number. Called by Spring Security during login to verify credentials against the stored bcrypt password hash. |

### **5.4  scheduler/**

Background jobs that run automatically on a schedule without any HTTP request triggering them.

| Job | When it runs / What it does |
| :---- | :---- |
| Vaccination OVERDUE scan | Daily at 06:00. Finds vaccination records that are PENDING and past their due date. Flips status to OVERDUE and queues an SMS reminder to the mother. |
| Appointment SMS reminder | Hourly. Finds appointments within 24 hours where reminder\_sent \= false. Sends the SMS and marks reminder\_sent \= true to prevent duplicates. |
| Government sync retry | Every 5 minutes. Finds failed government API calls in gov\_sync\_log and retries them with exponential backoff (waits longer after each failure). |

## **6\. Resources Folder**

The resources/ folder contains configuration and database migration files not Java code.

### **6.1  application.yml**

The main application configuration file. It tells Spring how to connect to the database, what the JWT secret is, and where to send SMS messages. Sensitive values (passwords, API keys) are always read from environment variables; they are never written directly into this file.

| spring: |
| :---- |
|   datasource: |
|     url: ${DATABASE\_URL}         ← read from environment variable |
|     username: ${DATABASE\_USERNAME} |
|     password: ${DATABASE\_PASSWORD} |
|   jpa: |
|     hibernate.ddl-auto: validate ← Flyway manages the schema, NOT Hibernate |
|  |
| jwt: |
|   secret: ${JWT\_SECRET} |
|   expiration: 900000             ← 15 minutes in milliseconds |
|  |
| africas-talking: |
|   api-key: ${AT\_API\_KEY} |
|   username: ${AT\_USERNAME} |

### **6.2  db/migration/ Flyway Scripts**

Every database schema change is written as a numbered SQL script in this folder. Flyway runs these scripts in order when the application starts. A script is never changed after it has been applied to a shared database; any new change goes in a new script.

| RULE | Script names follow the pattern V{number}\_\_{description}.sql. The number must increase with every new script. Flyway will refuse to start if a previously applied script has been modified. |
| :---: | :---- |

| Migration File | What it creates |
| :---- | :---- |
| V1\_\_seed\_geo\_locations.sql | Inserts all 14,000+ Rwanda location rows: provinces, districts, sectors, cells, and villages. |
| V2\_\_create\_users.sql | Creates the users table with all columns, unique indexes on national\_id and phone\_number. |
| V3\_\_create\_facilities.sql | Creates the facilities table with the official MoH facility code column. |
| V4\_\_create\_mothers.sql | Creates the mothers table with foreign keys to users, facilities, and geo\_locations. |
| V5\_\_create\_pregnancies.sql | Creates the pregnancies table linked to mothers, with the assigned CHW foreign key. |
| V6\_\_create\_children.sql | Creates the children table with a unique constraint on birth\_certificate\_no. |
| V7\_\_seed\_vaccination\_schedules.sql | Inserts Rwanda's EPI vaccination schedule: every vaccine, dose number, and due\_age\_days. |
| V8\_\_create\_vaccination\_records.sql | Creates vaccination\_records with a unique index on (child\_id, schedule\_id) each child gets each dose exactly once. |
| V9\_\_create\_health\_visits.sql | Creates health\_visits with the polymorphic patient\_ref\_id so one table serves both mothers and children. |
| V10\_\_create\_appointments.sql | Creates the appointments table with the reminder\_sent flag to prevent duplicate SMS messages. |
| V11\_\_create\_consent\_records.sql | Creates consent\_records with the legal\_basis column required by Rwanda Data Protection Law. |
| V12\_\_create\_gov\_sync\_log.sql | Creates the government outbox table with a unique idempotency\_key to prevent duplicate API calls on retry. |
| V13\_\_create\_notifications.sql | Creates sms\_notifications with the Africa's Talking message ID for delivery tracking. |
| V14\_\_create\_audit\_log.sql | Creates the audit log as an append-only table. No UPDATE or DELETE is permitted. 7-year retention per MoH policy. |

## **7\. Where Does New Code Go?**

Use this as a quick reference whenever you are adding something new to the project.

| I need to add... | It goes in... |
| :---- | :---- |
| A new API endpoint | controller/ for the route, service/ interface \+ ServiceImpl for the logic |
| A new database column | entity/ to add the field, then a new Flyway V{N}\_.sql migration script |
| A new status value (e.g. CANCELLED) | enums/  add the value to the relevant enum class |
| A new validation rule on incoming data | dto/request/  add @Valid annotation or logic in ServiceImpl |
| A new automated background task | scheduler/ReminderScheduler.java add a new @Scheduled method |
| A new user role or permission rule | identity/enums/RoleType.java and config/SecurityConfig.java |
| A shared helper method used by many domains | common/util/DateUtils.java or a new file in common/util/ |
| A new exception type | common/exception/ extend CustomException or add a handler in GlobalExceptionHandler.java |
| A new government API integration | government/ service always write to gov\_sync\_log FIRST before calling the external API |

