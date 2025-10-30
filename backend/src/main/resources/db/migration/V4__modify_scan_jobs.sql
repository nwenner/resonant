-- Modify scan_jobs table to match ScanJob entity
-- This migration updates the existing table structure from V2

-- Drop columns we don't need
ALTER TABLE scan_jobs DROP COLUMN IF EXISTS regions;
ALTER TABLE scan_jobs DROP COLUMN IF EXISTS resource_types;
ALTER TABLE scan_jobs DROP COLUMN IF EXISTS error_message;
ALTER TABLE scan_jobs DROP COLUMN IF EXISTS started_at;

-- Add new columns to match entity
ALTER TABLE scan_jobs ADD COLUMN violations_resolved INTEGER NOT NULL DEFAULT 0;
ALTER TABLE scan_jobs ADD COLUMN started_at TIMESTAMP;
ALTER TABLE scan_jobs ADD COLUMN error_message VARCHAR(2000);
ALTER TABLE scan_jobs ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE scan_jobs ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Update status constraint to: PENDING, RUNNING, SUCCESS, FAILED
ALTER TABLE scan_jobs DROP CONSTRAINT IF EXISTS scan_jobs_status_check;
ALTER TABLE scan_jobs ADD CONSTRAINT scan_jobs_status_check
    CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED'));

-- Create additional indexes
CREATE INDEX idx_scan_jobs_created_at ON scan_jobs(created_at DESC);

-- Add trigger for updated_at
CREATE TRIGGER update_scan_jobs_updated_at
    BEFORE UPDATE ON scan_jobs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();