import { describe, it, expect } from "vitest";
import {
  loginSchema,
  signupSchema,
  forgotPasswordSchema,
  profileSchema,
  validateSchema,
  getFieldError,
  hasFieldError,
} from "./validation";

describe("validation utils — Zod Form Schemas & Helper Functions", () => {
  describe("loginSchema", () => {
    it("validates valid login credentials", () => {
      const result = validateSchema(loginSchema, {
        email: "citizen@example.com",
        password: "Password123",
      });
      expect(result.success).toBe(true);
      expect(result.data.email).toBe("citizen@example.com");
    });

    it("rejects invalid email formats", () => {
      const result = validateSchema(loginSchema, {
        email: "notanemail",
        password: "Password123",
      });
      expect(result.success).toBe(false);
      expect(hasFieldError(result, "email")).toBe(true);
      expect(getFieldError(result, "email")).toBe("Invalid email address");
    });
  });

  describe("signupSchema", () => {
    it("validates a complete signup payload", () => {
      const payload = {
        name: "Rahul Verma",
        email: "rahul@example.com",
        phone: "9876543210",
        password: "Password123",
        confirmPassword: "Password123",
      };
      const result = validateSchema(signupSchema, payload);
      expect(result.success).toBe(true);
    });

    it("errors when passwords do not match", () => {
      const payload = {
        name: "Rahul Verma",
        email: "rahul@example.com",
        phone: "9876543210",
        password: "Password123",
        confirmPassword: "DifferentPassword123",
      };
      const result = validateSchema(signupSchema, payload);
      expect(result.success).toBe(false);
      expect(getFieldError(result, "confirmPassword")).toBe("Passwords do not match");
    });
  });

  describe("forgotPasswordSchema", () => {
    it("validates valid email address", () => {
      const result = validateSchema(forgotPasswordSchema, { email: "user@domain.com" });
      expect(result.success).toBe(true);
    });
  });

  describe("profileSchema", () => {
    it("validates citizen profile payload", () => {
      const profileData = {
        name: "Sunita Devi",
        age: 34,
        gender: "Female",
        occupation: "Homemaker",
        annualIncome: 120000,
        caste: "OBC",
        state: "Uttar Pradesh",
        district: "Lucknow",
        pincode: "226001",
        address: "123 Village Rampur, Station Road",
      };
      const result = validateSchema(profileSchema, profileData);
      expect(result.success).toBe(true);
    });

    it("rejects age under 18", () => {
      const profileData = {
        name: "Sunita Devi",
        age: 16,
        gender: "Female",
        occupation: "Student",
        annualIncome: 0,
        caste: "General",
        state: "Delhi",
        district: "Central Delhi",
        pincode: "110001",
        address: "123 Connaught Place Main Street",
      };
      const result = validateSchema(profileSchema, profileData);
      expect(result.success).toBe(false);
      expect(getFieldError(result, "age")).toBe("You must be at least 18 years old");
    });
  });
});
