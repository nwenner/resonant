-- Create resource_type_settings table
CREATE TABLE resource_type_settings (
                                        id UUID PRIMARY KEY,
                                        resource_type VARCHAR(100) NOT NULL UNIQUE,
                                        display_name VARCHAR(255) NOT NULL,
                                        description TEXT,
                                        enabled BOOLEAN NOT NULL DEFAULT true,
                                        created_at TIMESTAMP NOT NULL,
                                        updated_at TIMESTAMP NOT NULL
);

-- Create index for enabled lookups
CREATE INDEX idx_resource_type_settings_enabled ON resource_type_settings(enabled);

-- Add trigger for updated_at
CREATE TRIGGER update_resource_type_settings_updated_at
    BEFORE UPDATE ON resource_type_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default resource types (all enabled by default)
INSERT INTO resource_type_settings (id, resource_type, display_name, description, enabled, created_at, updated_at) VALUES
   (gen_random_uuid(), 's3:bucket', 'S3 Buckets', 'Amazon S3 storage buckets', true, NOW(), NOW()),
   (gen_random_uuid(), 'cloudfront:distribution', 'CloudFront Distributions', 'Amazon CloudFront CDN distributions', true, NOW(), NOW()),
   (gen_random_uuid(), 'ec2:vpc', 'VPCs', 'Amazon EC2 Virtual Private Clouds', true, NOW(), NOW());