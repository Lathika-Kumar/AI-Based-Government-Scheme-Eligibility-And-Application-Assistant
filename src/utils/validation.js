import { z } from "zod";

/**
 * Validation schemas using Zod
 * Provides type-safe validation for forms and API requests
 */

// ── Common Validators ───────────────────────────────────────────────────────

const emailSchema = z
  .string()
  .min(1, "Email is required")
  .email("Invalid email address")
  .toLowerCase()
  .trim();

const passwordSchema = z
  .string()
  .min(8, "Password must be at least 8 characters")
  .max(128, "Password must be less than 128 characters")
  .regex(
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/,
    "Password must contain at least one uppercase letter, one lowercase letter, and one number"
  );

const phoneSchema = z
  .string()
  .min(10, "Phone number must be at least 10 digits")
  .max(15, "Phone number must be less than 15 digits")
  .regex(/^[0-9+\s-]+$/, "Phone number can only contain digits, +, -, and spaces");

const nameSchema = z
  .string()
  .min(2, "Name must be at least 2 characters")
  .max(100, "Name must be less than 100 characters")
  .regex(/^[a-zA-Z\s\.]+$/, "Name can only contain letters, spaces, and periods")
  .trim();

const pincodeSchema = z
  .string()
  .length(6, "PIN code must be exactly 6 digits")
  .regex(/^\d+$/, "PIN code must contain only digits");

// ── Auth Schemas ───────────────────────────────────────────────────────────

export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, "Password is required"),
});

export const signupSchema = z.object({
  name: nameSchema,
  email: emailSchema,
  phone: phoneSchema,
  password: passwordSchema,
  confirmPassword: z.string().min(1, "Please confirm your password"),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
});

export const forgotPasswordSchema = z.object({
  email: emailSchema,
});

export const resetPasswordSchema = z.object({
  password: passwordSchema,
  confirmPassword: z.string().min(1, "Please confirm your password"),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
});

// ── Profile Schemas ────────────────────────────────────────────────────────

export const profileSchema = z.object({
  name: nameSchema,
  age: z
    .number()
    .int("Age must be a whole number")
    .min(18, "You must be at least 18 years old")
    .max(100, "Age must be less than 100"),
  gender: z.enum(["Male", "Female", "Other"], {
    errorMap: () => ({ message: "Please select a gender" }),
  }),
  occupation: z
    .string()
    .min(2, "Occupation must be at least 2 characters")
    .max(100, "Occupation must be less than 100 characters")
    .trim(),
  annualIncome: z
    .number()
    .min(0, "Income cannot be negative")
    .max(50000000, "Income value is too high"),
  caste: z.enum(["General", "OBC", "SC", "ST"], {
    errorMap: () => ({ message: "Please select a social category" }),
  }),
  state: z
    .string()
    .min(1, "State is required")
    .max(50, "State name is too long")
    .trim(),
  district: z
    .string()
    .min(1, "District is required")
    .max(50, "District name is too long")
    .trim(),
  pincode: pincodeSchema,
  address: z
    .string()
    .min(10, "Address must be at least 10 characters")
    .max(500, "Address must be less than 500 characters")
    .trim(),
});

// ── Onboarding Schemas ─────────────────────────────────────────────────────

export const onboardingStep1Schema = z.object({
  age: z
    .number()
    .int("Age must be a whole number")
    .min(18, "You must be at least 18 years old")
    .max(100, "Age must be less than 100"),
  gender: z.enum(["Male", "Female", "Other"], {
    errorMap: () => ({ message: "Please select a gender" }),
  }),
  caste: z.enum(["General", "OBC", "SC", "ST"], {
    errorMap: () => ({ message: "Please select a social category" }),
  }),
});

export const onboardingStep2Schema = z.object({
  occupation: z
    .string()
    .min(2, "Occupation must be at least 2 characters")
    .max(100, "Occupation must be less than 100 characters")
    .trim(),
  annualIncome: z
    .number()
    .min(0, "Income cannot be negative")
    .max(50000000, "Income value is too high"),
});

export const onboardingStep3Schema = z.object({
  state: z
    .string()
    .min(1, "State is required")
    .max(50, "State name is too long")
    .trim(),
  district: z
    .string()
    .min(1, "District is required")
    .max(50, "District name is too long")
    .trim(),
  pincode: pincodeSchema,
});

// ── Document Schemas ────────────────────────────────────────────────────────

export const documentUploadSchema = z.object({
  name: z
    .string()
    .min(2, "Document name must be at least 2 characters")
    .max(100, "Document name must be less than 100 characters")
    .trim(),
  type: z.enum([
    "Aadhaar Card",
    "PAN Card",
    "Voter ID",
    "Driving License",
    "Passport",
    "Income Certificate",
    "Caste Certificate",
    "Birth Certificate",
    "Land Records",
    "Bank Passbook",
    "Other",
  ], {
    errorMap: () => ({ message: "Please select a document type" }),
  }),
  file: z
    .any()
    .refine((file) => file instanceof File, "Please upload a file")
    .refine((file) => file.size <= 5 * 1024 * 1024, "File size must be less than 5MB")
    .refine(
      (file) =>
        ["application/pdf", "image/jpeg", "image/jpg", "image/png", "image/webp"].includes(
          file.type
        ),
      "File must be PDF, JPG, PNG, or WebP format"
    ),
});

// ── Grievance Schemas ─────────────────────────────────────────────────────

export const grievanceSchema = z.object({
  relatedScheme: z
    .string()
    .min(1, "Please select a related scheme")
    .trim(),
  category: z.enum([
    "Payment Delayed",
    "Document Verification",
    "Application Status",
    "Eligibility Issue",
    "Technical Issue",
    "Other",
  ], {
    errorMap: () => ({ message: "Please select a category" }),
  }),
  description: z
    .string()
    .min(20, "Description must be at least 20 characters")
    .max(2000, "Description must be less than 2000 characters")
    .trim(),
  phone: phoneSchema,
  email: emailSchema,
  supportingNote: z
    .string()
    .max(1000, "Supporting note must be less than 1000 characters")
    .trim()
    .optional(),
});

// ── Scheme Schemas ────────────────────────────────────────────────────────

export const schemeCreateSchema = z.object({
  name: z
    .string()
    .min(5, "Scheme name must be at least 5 characters")
    .max(200, "Scheme name must be less than 200 characters")
    .trim(),
  ministry: z
    .string()
    .min(2, "Ministry name must be at least 2 characters")
    .max(100, "Ministry name must be less than 100 characters")
    .trim(),
  category: z
    .string()
    .min(1, "Category is required")
    .trim(),
  description: z
    .string()
    .min(50, "Description must be at least 50 characters")
    .max(2000, "Description must be less than 2000 characters")
    .trim(),
  eligibility: z.object({
    minAge: z.number().int().min(0).max(100).optional(),
    maxAge: z.number().int().min(0).max(100).optional(),
    minIncome: z.number().min(0).max(50000000).optional(),
    maxIncome: z.number().min(0).max(50000000).optional(),
    states: z.array(z.string()).optional(),
    castes: z.array(z.string()).optional(),
    genders: z.array(z.string()).optional(),
    occupations: z.array(z.string()).optional(),
  }),
  benefits: z
    .string()
    .min(10, "Benefits description must be at least 10 characters")
    .max(1000, "Benefits description must be less than 1000 characters")
    .trim(),
  requiredDocuments: z.array(z.string()).min(1, "At least one required document must be specified"),
  applicationDeadline: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "Invalid date format. Use YYYY-MM-DD")
    .optional(),
  status: z.enum(["draft", "published", "archived"]).default("draft"),
});

// ── Utility Functions ───────────────────────────────────────────────────────

/**
 * Validate data against a schema and return formatted errors
 * @param {Object} schema - Zod schema
 * @param {Object} data - Data to validate
 * @returns {Object} { success: boolean, data?: Object, errors?: Object }
 */
export function validateSchema(schema, data) {
  const result = schema.safeParse(data);

  if (!result.success) {
    const errors = {};
    result.error.errors.forEach((error) => {
      const path = error.path.join(".");
      errors[path] = error.message;
    });
    return { success: false, errors };
  }

  return { success: true, data: result.data };
}

/**
 * Get a single field error from validation result
 * @param {Object} validationResult - Result from validateSchema
 * @param {string} field - Field name
 * @returns {string|undefined}
 */
export function getFieldError(validationResult, field) {
  return validationResult.errors?.[field];
}

/**
 * Check if a field has an error
 * @param {Object} validationResult - Result from validateSchema
 * @param {string} field - Field name
 * @returns {boolean}
 */
export function hasFieldError(validationResult, field) {
  return !!validationResult.errors?.[field];
}
