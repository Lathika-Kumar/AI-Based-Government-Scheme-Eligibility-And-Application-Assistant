import { describe, it, expect } from "vitest";
import { normalizeAuthResponse } from "./authService";

describe("normalizeAuthResponse", () => {
  it("maps backend auth payload to the frontend user shape", () => {
    const response = {
      accessToken: "access-123",
      refreshToken: "refresh-456",
      user: {
        id: "u1",
        fullName: "Asha Rao",
        email: "asha@example.com",
        roles: ["CITIZEN"],
        status: "ACTIVE",
        onboardingCompleted: true,
      },
    };

    const normalized = normalizeAuthResponse(response);

    expect(normalized.token).toBe("access-123");
    expect(normalized.refreshToken).toBe("refresh-456");
    expect(normalized.user.name).toBe("Asha Rao");
    expect(normalized.user.role).toBe("citizen");
    expect(normalized.user.onboardingComplete).toBe(true);
  });
});
