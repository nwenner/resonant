-- Modify compliance_violations table to match ComplianceViolation entity
-- This migration updates the existing table structure from V2

-- Drop old columns we don't need
ALTER TABLE compliance_violations DROP COLUMN IF EXISTS violation_type;
ALTER TABLE compliance_violations DROP COLUMN IF EXISTS missing_tags;
ALTER TABLE compliance_violations DROP COLUMN IF EXISTS severity;
ALTER TABLE compliance_violations DROP COLUMN IF EXISTS remediation_action;
ALTER TABLE compliance_violations DROP COLUMN IF EXISTS resolved_by;

-- Add new columns
ALTER TABLE compliance_violations ADD COLUMN scan_job_id UUID REFERENCES scan_jobs(id) ON DELETE SET NULL;
ALTER TABLE compliance_violations ADD COLUMN violation_details JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE compliance_violations ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Update status constraint to only include: OPEN, RESOLVED, IGNORED
ALTER TABLE compliance_violations DROP CONSTRAINT IF EXISTS compliance_violations_status_check;
ALTER TABLE compliance_violations ADD CONSTRAINT compliance_violations_status_check
    CHECK (status IN ('OPEN', 'RESOLVED', 'IGNORED'));

-- Create index for scan_job_id
CREATE INDEX idx_violations_scan_job ON compliance_violations(scan_job_id);

-- Create unique constraint to prevent duplicate violations for same resource-policy
CREATE UNIQUE INDEX idx_violations_resource_policy ON compliance_violations(aws_resource_id, tag_policy_id);

-- Add trigger for updated_at
CREATE TRIGGER update_compliance_violations_updated_at
    BEFORE UPDATE ON compliance_violations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();