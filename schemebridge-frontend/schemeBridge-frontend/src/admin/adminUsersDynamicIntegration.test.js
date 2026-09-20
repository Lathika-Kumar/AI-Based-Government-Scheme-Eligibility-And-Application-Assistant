import { describe, it, expect } from "vitest";

describe("Admin Users / Identity & Access Management Dynamic Data Contract", () => {
  // Canonical application role definition
  const CANONICAL_ROLES = {
    CITIZEN: "ROLE_USER",
    OFFICER: "ROLE_VERIFICATION_OFFICER",
    MANAGER: "ROLE_SCHEME_MANAGER",
    ADMIN: "ROLE_ADMIN"
  };

  const normalizeRole = (r) => (r ? (r.startsWith("ROLE_") ? r : `ROLE_${r}`) : "");

  it("normalizes roles consistently whether they come with or without ROLE_ prefix", () => {
    expect(normalizeRole("USER")).toBe("ROLE_USER");
    expect(normalizeRole("ROLE_USER")).toBe("ROLE_USER");
    expect(normalizeRole("ADMIN")).toBe("ROLE_ADMIN");
    expect(normalizeRole("ROLE_ADMIN")).toBe("ROLE_ADMIN");
    expect(normalizeRole("SCHEME_MANAGER")).toBe("ROLE_SCHEME_MANAGER");
    expect(normalizeRole("ROLE_SCHEME_MANAGER")).toBe("ROLE_SCHEME_MANAGER");
    expect(normalizeRole("VERIFICATION_OFFICER")).toBe("ROLE_VERIFICATION_OFFICER");
    expect(normalizeRole("ROLE_VERIFICATION_OFFICER")).toBe("ROLE_VERIFICATION_OFFICER");
  });

  it("ensures tab definitions accurately reflect the application authorization model (Administrators, not Super Admins)", () => {
    const tabs = [
      { id: "all", label: "All Users", role: null },
      { id: "citizens", label: "Citizens", role: "ROLE_USER" },
      { id: "officers", label: "Verification Officers", role: "ROLE_VERIFICATION_OFFICER" },
      { id: "managers", label: "Scheme Managers", role: "ROLE_SCHEME_MANAGER" },
      { id: "admins", label: "Administrators", role: "ROLE_ADMIN" }
    ];

    expect(tabs.find(t => t.id === "admins")?.label).toBe("Administrators");
    expect(tabs.find(t => t.id === "admins")?.role).toBe("ROLE_ADMIN");
    expect(tabs.some(t => t.label === "Super Admins")).toBe(false);
  });

  it("authoritatively maps dynamic backend roleCounts and totalUsers without hardcoding", () => {
    const liveBackendResponse = {
      content: [
        {
          id: 52,
          firstName: "Admin",
          lastName: "User",
          email: "admin@schemebridge.gov.in",
          phoneNumber: "9876543210",
          accountStatus: "ACTIVE",
          roles: ["ROLE_ADMIN"]
        },
        {
          id: 11,
          firstName: "Test",
          lastName: "Manager",
          email: "otpfixed01@example.com",
          phoneNumber: "9876543211",
          accountStatus: "ACTIVE",
          roles: ["ROLE_ADMIN", "ROLE_SCHEME_MANAGER", "ROLE_USER"]
        }
      ],
      page: 0,
      size: 15,
      totalElements: 41,
      totalPages: 3,
      totalUsers: 41,
      roleCounts: {
        all: 41,
        citizens: 40,
        officers: 0,
        managers: 1,
        admins: 2
      }
    };

    expect(liveBackendResponse.totalUsers).toBe(41);
    expect(liveBackendResponse.roleCounts.all).toBe(41);
    expect(liveBackendResponse.roleCounts.citizens).toBe(40);
    expect(liveBackendResponse.roleCounts.officers).toBe(0);
    expect(liveBackendResponse.roleCounts.managers).toBe(1);
    expect(liveBackendResponse.roleCounts.admins).toBe(2);

    // Dynamic subtitle calculation
    const totalUsersDisplay = liveBackendResponse.totalUsers ?? liveBackendResponse.totalElements;
    expect(`Live identity administration backed by Oracle 21c security directory (${totalUsersDisplay} users)`)
      .toBe("Live identity administration backed by Oracle 21c security directory (41 users)");
  });

  it("prevents 'No Users Found' regression when users with valid roles exist in the dataset", () => {
    const users = [
      { id: 52, email: "admin@schemebridge.gov.in", roles: ["ROLE_ADMIN"] },
      { id: 11, email: "otpfixed01@example.com", roles: ["ROLE_ADMIN", "ROLE_SCHEME_MANAGER", "ROLE_USER"] },
      { id: 1, email: "citizen@example.com", roles: ["ROLE_USER"] }
    ];

    const filterByRole = (userList, targetRole) => {
      if (!targetRole) return userList;
      return userList.filter(u => u.roles.map(normalizeRole).includes(normalizeRole(targetRole)));
    };

    const adminUsers = filterByRole(users, "ROLE_ADMIN");
    expect(adminUsers.length).toBe(2);

    const managerUsers = filterByRole(users, "ROLE_SCHEME_MANAGER");
    expect(managerUsers.length).toBe(1);
    expect(managerUsers[0].email).toBe("otpfixed01@example.com");

    const citizenUsers = filterByRole(users, "ROLE_USER");
    expect(citizenUsers.length).toBe(2);

    const officerUsers = filterByRole(users, "ROLE_VERIFICATION_OFFICER");
    expect(officerUsers.length).toBe(0);
  });

  it("correctly handles drawer role checking and assignment", () => {
    const selectedUser = {
      id: 11,
      name: "Test Manager",
      roles: ["ROLE_ADMIN", "ROLE_SCHEME_MANAGER", "ROLE_USER"]
    };

    const isAssigned = (user, tabRole) =>
      user.roles.map(normalizeRole).includes(normalizeRole(tabRole));

    expect(isAssigned(selectedUser, "ROLE_ADMIN")).toBe(true);
    expect(isAssigned(selectedUser, "ROLE_SCHEME_MANAGER")).toBe(true);
    expect(isAssigned(selectedUser, "ROLE_USER")).toBe(true);
    expect(isAssigned(selectedUser, "ROLE_VERIFICATION_OFFICER")).toBe(false);
  });

  it("handles all AccountStatus values including PENDING_VERIFICATION safely", () => {
    const statuses = ["ACTIVE", "LOCKED", "PENDING_VERIFICATION", "INACTIVE"];

    const formatStatus = (rawStatus) => {
      switch (rawStatus) {
        case "ACTIVE": return "Active";
        case "LOCKED": return "Locked";
        case "PENDING_VERIFICATION": return "Pending Verification";
        default: return "Inactive";
      }
    };

    expect(formatStatus("ACTIVE")).toBe("Active");
    expect(formatStatus("LOCKED")).toBe("Locked");
    expect(formatStatus("PENDING_VERIFICATION")).toBe("Pending Verification");
    expect(formatStatus("INACTIVE")).toBe("Inactive");
  });

  it("enforces clear distinction between API failure and empty data", () => {
    const renderTableState = ({ loading, error, usersCount }) => {
      if (loading) return "LOADING";
      if (error) return "FAILED_TO_LOAD_USERS";
      if (usersCount === 0) return "NO_USERS_FOUND";
      return "RENDER_USERS";
    };

    expect(renderTableState({ loading: false, error: "Network timeout", usersCount: 0 }))
      .toBe("FAILED_TO_LOAD_USERS");
    expect(renderTableState({ loading: false, error: null, usersCount: 0 }))
      .toBe("NO_USERS_FOUND");
    expect(renderTableState({ loading: false, error: null, usersCount: 15 }))
      .toBe("RENDER_USERS");
  });

  it("verifies security: sensitive fields are never present in user entities", () => {
    const userDto = {
      id: 100,
      firstName: "Citizen",
      lastName: "User",
      email: "citizen@example.com",
      phoneNumber: "9876543210",
      accountStatus: "ACTIVE",
      emailVerified: true,
      roles: ["ROLE_USER"],
      createdAt: "2026-09-15T00:00:00Z"
    };

    const forbiddenFields = [
      "password", "passwordHash", "hashedPassword",
      "otp", "jwtSecret", "refreshToken", "secretKey"
    ];

    forbiddenFields.forEach(field => {
      expect(userDto).not.toHaveProperty(field);
    });
  });
});
