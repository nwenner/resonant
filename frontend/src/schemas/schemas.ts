import {z} from 'zod';

// Tag Policy Schema
export const tagPolicySchema = z.object({
  name: z.string()
      .min(1, 'Policy name is required')
      .max(100, 'Name must be less than 100 characters'),

  description: z.string()
      .max(500, 'Description must be less than 500 characters')
      .optional()
      .or(z.literal('')),

  severity: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']),

  enabled: z.boolean(),

  resourceTypes: z.array(z.string())
      .min(1, 'Select at least one resource type'),

  requiredTags: z.array(
      z.object({
        key: z.string()
            .min(1, 'Tag key is required')
            .regex(/^[\w\s\-:.\/]+$/, 'Invalid tag key format'),
        allowedValues: z.string(),
        anyValue: z.boolean(),
      })
  ).min(1, 'At least one tag is required'),
}).refine(
    (data) => {
      // Ensure at least one tag has a key
      return data.requiredTags.some(tag => tag.key.trim().length > 0);
    },
    {
      message: 'At least one tag with a valid key is required',
      path: ['requiredTags'],
    }
);

export type TagPolicyFormData = z.infer<typeof tagPolicySchema>;

// AWS Account Schema
export const awsAccountSchema = z.object({
  accountId: z.string()
      .regex(/^\d{12}$/, 'AWS Account ID must be 12 digits'),

  accountAlias: z.string()
      .min(1, 'Account alias is required')
      .max(100, 'Alias must be less than 100 characters'),

  roleArn: z.string()
      .regex(
          /^arn:aws:iam::\d{12}:role\/[\w+=,.@\-]+$/,
          'Invalid IAM Role ARN format'
      ),

  externalId: z.string().optional(),
});

export type AwsAccountFormData = z.infer<typeof awsAccountSchema>;

// Login Schema
export const loginSchema = z.object({
  email: z.string()
      .email('Invalid email address'),

  password: z.string()
      .min(1, 'Password is required'),
});

export type LoginFormData = z.infer<typeof loginSchema>;

// Registration Schema
export const registrationSchema = z.object({
  name: z.string()
      .min(1, 'Full name is required')
      .max(100, 'Name must be less than 100 characters'),

  email: z.string()
      .email('Invalid email address'),

  password: z.string()
      .min(8, 'Password must be at least 8 characters')
      .regex(
          /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/,
          'Password must contain uppercase, lowercase, and number'
      ),

  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

export type RegistrationFormData = z.infer<typeof registrationSchema>;