# VetClinic - Session Notes
# Poslednje azuriranje: 2026-02-08

## Projekat
- **Lokacija**: C:\Users\ndraskovic\worksapceVetClinic\vetclinic
- **Tech stack**: Spring Boot 3.5.10, Java 17, PostgreSQL 18, Hibernate 6.6.41, HikariCP
- **Java path**: C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot
- **Build**: mvn clean package -DskipTests -q (sa JAVA_HOME setovanim na JDK 17)
- **Run**: java -jar target\vetclinic-0.0.1-SNAPSHOT.jar (koristiti pun path do JDK 17)
- **DB**: vetapp na localhost:5432, app user: vetapp_user/vetapp_pass, admin: postgres/admin
- **Clinic ID**: b5434818-265f-4386-8ed5-e568a238a451
- **Test user**: admin@test.com / admin123

## Sta je zavrseno

### 1. REST API (219 source fajlova)
- 24 kontrolera, 25 JPA repozitorijuma, service layer, MapStruct mapperi, DTOs
- Svi entiteti koriste BaseEntity sa clinic_id, soft delete, verzionisanje
- JSONB polja koriste @JdbcTypeCode(SqlTypes.JSON) na 5 entiteta

### 2. Spring Security + JWT autentifikacija
- JwtAuthenticationFilter - ekstraktuje clinicId iz JWT tokena
- CustomUserDetailsService - loaduje usera po clinicId + email
- Endpointi: POST /api/auth/login, /refresh, /logout
- SecurityConfig whitelist: /api/auth/**, /api/health, /swagger-ui/**, /v3/api-docs/**

### 3. PostgreSQL Row-Level Security (RLS)
- Dedicirani DB user vetapp_user (non-superuser, RLS se primenjuje)
- 23 tabele sa RLS policy-jima (tenant_isolation) - koriste NULLIF za PG 18
- SECURITY DEFINER funkcije: get_clinic_id_for_user(), count_all_users()
- ClinicContextHolder (ThreadLocal) + TenantAwareDataSource (Dynamic Proxy)
- TenantConnectionInterceptor (MVC interceptor) + DataSourceConfig
- Session.doWork() za SET na vec otvorenoj konekciji (refresh flow, DataSeeder)
- Kljucni fix: ClinicContextHolder.set() PRE @Transactional metode (u AuthController)
- NULLIF fix za PG 18: current_setting vraca '' umesto NULL

### 4. Swagger UI + Postman kolekcija
- SpringDoc OpenAPI (springdoc-openapi-starter-webmvc-ui 2.8.4)
- OpenApiConfig.java - JWT security scheme, Pageable customizer (page=0, size=20, sort=createdAt,desc)
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Postman kolekcija: VetClinic.postman_collection.json (60+ requestova, auto-save tokena)

### 5. @EntityGraph na 14 repozitorijuma
- Dodate za resavanje LazyInitializationException (open-in-view: false)
- User->role, Breed->species, Pet->species+breed, Appointment->location+pet, itd.

### 6. Migracija podataka
- Svi podaci prebaceni sa stare klinike (a0eebc99...) na novu (b5434818...)
- Stara klinika obrisana, ostaje samo jedna klinika
- Postojeci podaci: 8 species, 18 breeds, 17 services, 1 clinic_location, 1 user, 1 role

## Kljucni fajlovi (kreirani/modifikovani u sesijama)

### Novi fajlovi:
- config/tenant/ClinicContextHolder.java
- config/tenant/TenantAwareDataSource.java
- config/tenant/TenantConnectionInterceptor.java
- config/tenant/DataSourceConfig.java
- config/WebMvcConfig.java
- config/OpenApiConfig.java
- src/main/resources/rls-setup.sql
- VetClinic.postman_collection.json

### Modifikovani:
- pom.xml (springdoc, actuator dependency)
- application.yml (vetapp_user credentials, springdoc config)
- config/security/SecurityConfig.java (swagger whitelist)
- config/security/JwtAuthenticationFilter.java (ClinicContextHolder.set)
- config/DataSeeder.java (RLS kompatibilan, Session.doWork)
- controller/AuthController.java (ClinicContextHolder.set pre login)
- service/AuthService.java (Session.doWork za refresh, EntityManager)
- 14 repository interfejsa (@EntityGraph anotacije)

## Sledeci koraci (predlozeni, korisnik bira):
1. Flyway migracije
2. Testovi (JUnit 5 + Testcontainers)
3. Validacije i error handling
4. Audit log automatizacija (AOP)
5. CORS konfiguracija
6. Pagination limiti
7. Rate limiting
8. Docker/Docker Compose
9. CI/CD pipeline
10. Frontend
