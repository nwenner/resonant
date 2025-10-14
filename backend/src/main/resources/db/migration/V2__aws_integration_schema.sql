-- AWS Accounts (user owns multiple AWS accounts)
CREATE TABLE aws_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id VARCHAR(12) NOT NULL,
    account_alias VARCHAR(100),
    role_arn VARCHAR(512),
    external_id VARCHAR(64),
    access_key_encrypted TEXT,
    secret_key_encrypted TEXT,
    credential_type VARCHAR(20) NOT NULL DEFAULT 'ROLE' CHECK (credential_type IN ('ROLE', 'ACCESS_KEY')),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INVALID', 'EXPIRED', 'TESTING')),
    last_scan_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, account_id)
);

CREATE INDEX idx_aws_accounts_user ON aws_accounts(user_id);
CREATE INDEX idx_aws_accounts_status ON aws_accounts(status);

-- AWS Regions enabled per account
CREATE TABLE aws_account_regions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aws_account_id UUID NOT NULL REFERENCES aws_accounts(id) ON DELETE CASCADE,
    region_code VARCHAR(20) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    last_scan_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(aws_account_id, region_code)
);

CREATE INDEX idx_aws_account_regions_enabled ON aws_account_regions(aws_account_id, enabled);

-- Tag Compliance Policies
CREATE TABLE tag_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    required_tags JSONB NOT NULL,
    resource_types JSONB NOT NULL,
    severity VARCHAR(20) DEFAULT 'MEDIUM' CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tag_policies_user ON tag_policies(user_id);
CREATE INDEX idx_tag_policies_enabled ON tag_policies(user_id, enabled);

-- Discovered AWS Resources
CREATE TABLE aws_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aws_account_id UUID NOT NULL REFERENCES aws_accounts(id) ON DELETE CASCADE,
    resource_id VARCHAR(255) NOT NULL,
    resource_arn VARCHAR(512) NOT NULL UNIQUE,
    resource_type VARCHAR(50) NOT NULL,
    region VARCHAR(20) NOT NULL,
    name VARCHAR(255),
    tags JSONB,
    metadata JSONB,
    discovered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_aws_resources_account ON aws_resources(aws_account_id);
CREATE INDEX idx_aws_resources_type ON aws_resources(resource_type);
CREATE INDEX idx_aws_resources_account_region ON aws_resources(aws_account_id, region);
CREATE INDEX idx_aws_resources_last_seen ON aws_resources(last_seen_at DESC);

-- Compliance Violations
CREATE TABLE compliance_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aws_resource_id UUID NOT NULL REFERENCES aws_resources(id) ON DELETE CASCADE,
    tag_policy_id UUID NOT NULL REFERENCES tag_policies(id) ON DELETE CASCADE,
    violation_type VARCHAR(50) NOT NULL CHECK (violation_type IN ('MISSING_TAG', 'INVALID_VALUE', 'UNTAGGED')),
    missing_tags JSONB,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'REMEDIATED', 'IGNORED', 'RESOLVED')),
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    remediation_action TEXT,
    resolved_by UUID REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_violations_resource ON compliance_violations(aws_resource_id);
CREATE INDEX idx_violations_policy ON compliance_violations(tag_policy_id);
CREATE INDEX idx_violations_status ON compliance_violations(status);
CREATE INDEX idx_violations_severity ON compliance_violations(severity);
CREATE INDEX idx_violations_detected ON compliance_violations(detected_at DESC);

-- Scan Jobs (track scanning operations)
CREATE TABLE scan_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aws_account_id UUID NOT NULL REFERENCES aws_accounts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    regions JSONB NOT NULL,
    resource_types JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    resources_scanned INTEGER DEFAULT 0,
    violations_found INTEGER DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_scan_jobs_account ON scan_jobs(aws_account_id, started_at DESC);
CREATE INDEX idx_scan_jobs_user ON scan_jobs(user_id, started_at DESC);
CREATE INDEX idx_scan_jobs_status ON scan_jobs(status);

-- Trigger to update updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_aws_accounts_updated_at BEFORE UPDATE ON aws_accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tag_policies_updated_at BEFORE UPDATE ON tag_policies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default AWS regions for common use
-- These can be enabled/disabled per account
COMMENT ON TABLE aws_account_regions IS 'Tracks which AWS regions are enabled for scanning per account';
COMMENT ON TABLE tag_policies IS 'User-defined tag compliance policies with required tags and allowed values';
COMMENT ON TABLE compliance_violations IS 'Tracks resources that violate tag policies';
COMMENT ON TABLE scan_jobs IS 'History of all AWS resource scanning operations';