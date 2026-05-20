# GeoLocation Module

## What this module does

This module manages Rwanda's full 5-level administrative hierarchy.
It powers the cascading dropdown menus on the mother registration form
and resolves village-level locations to UUIDs for storing in other records.

## Rwanda administrative levels

Province (5)
└── District (30)
└── Sector (416)
└── Cell (2,148)
└── Village (~14,000)

## API Endpoints

All endpoints are public — no JWT token required.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/geo/provinces` | Get all province names |
| GET | `/api/v1/geo/districts?province=` | Get districts in a province |
| GET | `/api/v1/geo/sectors?province=&district=` | Get sectors in a district |
| GET | `/api/v1/geo/cells?province=&district=&sector=` | Get cells in a sector |
| GET | `/api/v1/geo/villages?province=&district=&sector=&cell=` | Get villages in a cell |
| GET | `/api/v1/geo/resolve?province=&district=&sector=&cell=&village=` | Resolve full path to UUID |
| GET | `/api/v1/geo/{id}/summary` | Get location summary by UUID |

## Example usage

### Get all provinces
GET /api/v1/geo/provinces

Response:
["Kigali City", "Northern Province", "Southern Province",
"Eastern Province", "Western Province"]

### Get districts in Kigali City
GET /api/v1/geo/districts?province=Kigali City

Response:
["Gasabo", "Kicukiro", "Nyarugenge"]

### Resolve a full location to get its UUID
GET /api/v1/geo/resolve?province=Kigali City&district=Gasabo
&sector=Kacyiru&cell=Kagugu&village=Agatare

Response:
{
"id": "550e8400-e29b-41d4-a716-446655440000",
"province": "Kigali City",
"district": "Gasabo",
"sector": "Kacyiru",
"cell": "Kagugu",
"village": "Agatare",
"latitude": null,
"longitude": null
}

## Module structure

geo/
├── application/
│   ├── dto/
│   │   ├── GeoLocationRequest.java     # incoming request data
│   │   ├── GeoLocationResponse.java    # full location response
│   │   └── GeoLocationSummary.java     # lightweight response
│   └── service/
│       └── GeoLocationService.java     # main business logic
├── domain/
│   ├── model/
│   │   └── GeoLocation.java            # database entity
│   ├── repository/
│   │   └── GeoLocationRepository.java  # repository contract
│   └── service/
│       └── GeoLocationDomainService.java # domain rules
├── infrastructure/
│   ├── mapper/
│   │   └── GeoLocationMapper.java      # MapStruct DTO converter
│   └── persistence/
│       ├── JpaGeoLocationRepository.java   # repository implementation
│       └── SpringDataGeoRepository.java    # Spring Data JPA interface
└── presentation/
└── GeoLocationController.java  # REST endpoints

## How the layers connect

HTTP Request
↓
GeoLocationController (presentation)
↓ calls
GeoLocationService (application)
↓ calls
GeoLocationRepository (domain interface)
↓ implemented by
JpaGeoLocationRepository (infrastructure)
↓ uses
SpringDataGeoRepository (Spring Data JPA)
↓ queries
PostgreSQL — geo_locations table

## How cascading dropdowns work

1. Registration form loads
   → calls GET /api/v1/geo/provinces
   → province dropdown is filled

2. Health worker selects a province
   → calls GET /api/v1/geo/districts?province=Kigali City
   → district dropdown is filled

3. Health worker selects a district
   → calls GET /api/v1/geo/sectors?province=Kigali City&district=Gasabo
   → sector dropdown is filled

4. Health worker selects a sector
   → calls GET /api/v1/geo/cells?province=...&district=...&sector=Kacyiru
   → cell dropdown is filled

5. Health worker selects a cell
   → calls GET /api/v1/geo/villages?province=...&district=...&sector=...&cell=Kagugu
   → village dropdown is filled

6. Health worker selects a village
   → calls GET /api/v1/geo/resolve?province=...&district=...&sector=...&cell=...&village=Agatare
   → returns the UUID of that location
   → UUID is saved as geo_location_id on the mother record

## Developer notes

- All dropdown endpoints filter by active = true
  so deactivated locations never appear in forms
- The /resolve endpoint is used by the mother registration
  flow to get the UUID after the health worker selects a village
- The /{id}/summary endpoint is used by other modules
  when embedding location inside a response
- Location data is seeded via Flyway migration
  V1__seed_geo_locations.sql — never inserted manually
- The composite index on (province, district, sector) makes
  dropdown queries fast even with 14,000 village rows

## Technologies used

- Java 21
- Spring Boot 3.2.5
- Spring Data JPA
- PostgreSQL (native UUID type)
- MapStruct (DTO conversion)
- Lombok (boilerplate reduction)
- Flyway (database migrations)