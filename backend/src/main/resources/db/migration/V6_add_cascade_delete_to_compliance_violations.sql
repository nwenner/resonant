-- Add CASCADE DELETE to compliance_violations foreign key
-- This ensures violations are automatically deleted when their parent resource is deleted

ALTER TABLE compliance_violations
DROP CONSTRAINT IF EXISTS fk_violation_resource;

ALTER TABLE compliance_violations
    ADD CONSTRAINT fk_violation_resource
        FOREIGN KEY (aws_resource_id)
            REFERENCES aws_resources(id)
            ON DELETE CASCADE;