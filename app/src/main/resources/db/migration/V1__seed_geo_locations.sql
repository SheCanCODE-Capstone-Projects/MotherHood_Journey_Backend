-- TABLE: geo_locations
CREATE TABLE IF NOT EXISTS geo_locations (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      province VARCHAR(64) NOT NULL,
      district VARCHAR(64) NOT NULL,
      sector VARCHAR(64) NOT NULL,
      cell VARCHAR(64) NOT NULL,
      village VARCHAR(64) NOT NULL,
      postal_code VARCHAR(16),
      latitude FLOAT,
      longitude FLOAT,
     active BOOLEAN NOT NULL DEFAULT TRUE,
      created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
-- Indexes
CREATE INDEX IF NOT EXISTS idx_geo_pds
    ON geo_locations (province, district, sector);

CREATE INDEX IF NOT EXISTS idx_geo_sector
    ON geo_locations (sector);