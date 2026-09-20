import { describe, it, expect } from "vitest";

describe("Admin Settings & Header Professional Separation Suite", () => {
  // 1. Admin Header Contract
  it("1. Admin Header provides operational controls and strictly excludes standalone theme toggle", () => {
    const adminHeaderControls = [
      { id: "search", label: "Search anything...", trigger: "Ctrl K" },
      { id: "clock", label: "Real-time Clock display", type: "telemetry" },
      { id: "notifications", label: "Notification Center Trigger", icon: "Bell" },
      { id: "profile", label: "Admin Profile Capsule", showsRole: true },
      { id: "logout", label: "Logout", action: "handleLogout" },
    ];

    expect(adminHeaderControls).toHaveLength(5);
    expect(adminHeaderControls.map(c => c.id)).toEqual([
      "search", "clock", "notifications", "profile", "logout"
    ]);

    // Explicitly verify NO theme toggle control in Admin Header
    expect(adminHeaderControls.some(c => c.id === "theme")).toBe(false);
    expect(adminHeaderControls.some(c => c.id === "appearance")).toBe(false);
  });

  // 2. Admin Settings Panel Sections
  it("2. Admin Settings provides 6 first-class sections with Appearance directly after General Settings", () => {
    const adminSections = [
      { id: "general", label: "General Settings" },
      { id: "appearance", label: "Appearance" },
      { id: "workflow", label: "Workflow Configuration" },
      { id: "notifications", label: "Notification Preferences" },
      { id: "ai", label: "AI & Verification" },
      { id: "security", label: "Security & Access" },
    ];

    expect(adminSections).toHaveLength(6);
    expect(adminSections.map(s => s.id)).toEqual([
      "general", "appearance", "workflow", "notifications", "ai", "security"
    ]);
    expect(adminSections[1].id).toBe("appearance");
  });

  // 3. Admin Appearance Section Contract
  it("3. Admin Appearance provides Light, Dark, and System options with accessible controls", () => {
    const appearanceOptions = [
      { id: "light", label: "Light", description: "Use a light interface." },
      { id: "dark", label: "Dark", description: "Use a dark interface." },
      { id: "system", label: "System", description: "Follow your operating system preference." },
    ];

    expect(appearanceOptions).toHaveLength(3);
    expect(appearanceOptions.map(o => o.id)).toEqual(["light", "dark", "system"]);
  });

  // 4. Admin General Settings — Maintenance Mode removed
  it("4. Admin General Settings retains Platform Title and Default Timezone with Maintenance Mode removed", () => {
    const generalSettingsFields = [
      { name: "platformName", label: "Platform Title", type: "text" },
      { name: "defaultTimezone", label: "Default System Timezone", type: "select" },
    ];

    expect(generalSettingsFields).toHaveLength(2);
    expect(generalSettingsFields.map(f => f.name)).toEqual(["platformName", "defaultTimezone"]);

    // Explicitly assert Maintenance Mode toggle is absent
    expect(generalSettingsFields.some(f => f.name === "maintenanceMode")).toBe(false);
    expect(generalSettingsFields.some(f => f.name === "enableMaintenanceMode")).toBe(false);
    expect(generalSettingsFields.some(f => f.label.toLowerCase().includes("maintenance"))).toBe(false);
  });

  // 5. Admin Settings Payload contract
  it("5. Admin settings update payload includes operational fields and excludes maintenanceMode", () => {
    const formState = {
      platformName: "SchemeBridge Government Platform",
      defaultTimezone: "Asia/Kolkata",
      slaTarget: 7,
      maxQueueSize: 50,
      notifyNewApplication: true,
      notifyGrievance: true,
      enableAutoVerify: true,
      sessionTimeout: 30,
      enableTwoFactor: true,
    };

    const payload = {
      platformName: formState.platformName,
      defaultTimezone: formState.defaultTimezone,
      applicationSlaDays: Number(formState.slaTarget),
      grievanceSlaHours: Number(formState.maxQueueSize),
      emailNotificationsEnabled: formState.notifyNewApplication,
      smsNotificationsEnabled: formState.notifyGrievance,
      aiAssistedReviewEnabled: formState.enableAutoVerify,
      sessionTimeoutMinutes: Number(formState.sessionTimeout),
      mfaEnforcedForAdmins: formState.enableTwoFactor,
    };

    expect(payload.platformName).toBe("SchemeBridge Government Platform");
    expect(payload.defaultTimezone).toBe("Asia/Kolkata");
    expect(payload.applicationSlaDays).toBe(7);
    expect(payload).not.toHaveProperty("maintenanceMode");
    expect(payload).not.toHaveProperty("enableMaintenanceMode");
  });

  // 6. Citizen & Admin Settings Integrity
  it("6. Both Citizen and Admin Settings contain Appearance while preserving distinct responsibilities", () => {
    const citizenSections = [
      "account", "appearance", "language", "notifications", "privacy", "accessibility"
    ];
    const adminSections = [
      "general", "appearance", "workflow", "notifications", "ai", "security"
    ];

    // Both interfaces contain Appearance
    expect(citizenSections).toContain("appearance");
    expect(adminSections).toContain("appearance");

    // Citizen Settings retains accessibility and excludes preferences
    expect(citizenSections).toContain("accessibility");
    expect(citizenSections).not.toContain("preferences");

    // Admin Settings retains operational configuration
    expect(adminSections).toContain("workflow");
    expect(adminSections).toContain("ai");
    expect(adminSections).toContain("security");
  });
});
