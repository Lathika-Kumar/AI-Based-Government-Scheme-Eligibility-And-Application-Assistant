/**
 * @file flowTestMode.js
 * @description Centralized flag for Temporary Frontend Flow Testing Mode.
 *
 * TEMPORARY TEST MODE:
 * When enabled (VITE_FRONTEND_FLOW_TEST_MODE=true in .env.development),
 * blocking frontend validation guards are bypassed to allow manual end-to-end UI navigation.
 *
 * IMPORTANT:
 * - Real backend APIs and DB constraints remain the authoritative validation layer.
 * - Original validation logic is preserved in code.
 *
 * TO RESTORE NORMAL VALIDATION:
 * Set VITE_FRONTEND_FLOW_TEST_MODE=false in .env.development.
 */
export const IS_FLOW_TEST_MODE = import.meta.env.VITE_FRONTEND_FLOW_TEST_MODE === "true";
