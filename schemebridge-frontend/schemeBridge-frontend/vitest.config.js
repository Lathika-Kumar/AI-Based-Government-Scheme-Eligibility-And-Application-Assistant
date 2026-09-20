import { defineConfig } from "vitest/config";
import path from "path";

export default defineConfig({
  resolve: {
    alias: {
      "@admin":    path.resolve(__dirname, "src/admin"),
      "@user":     path.resolve(__dirname, "src/user"),
      "@public":   path.resolve(__dirname, "src/public"),
      "@context":  path.resolve(__dirname, "src/context"),
      "@data":     path.resolve(__dirname, "src/data"),
      "@utils":    path.resolve(__dirname, "src/utils"),
      "@components": path.resolve(__dirname, "src/components"),
      "@config":   path.resolve(__dirname, "src/config"),
      "@services": path.resolve(__dirname, "src/services"),
      "@assets":   path.resolve(__dirname, "src/assets"),
      "@constants": path.resolve(__dirname, "src/constants"),
    },
  },
  test: {
    environment: "node",
    globals: true,
    include: ["src/**/*.test.js", "src/**/*.test.jsx"],
    exclude: ["node_modules", "dist"],
  },
});
