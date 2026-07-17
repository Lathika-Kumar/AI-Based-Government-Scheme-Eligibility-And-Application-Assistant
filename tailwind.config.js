/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx,ts,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        "saffron": "#FF9933",
        "saffron-dark": "#E68A00",
        "white-official": "#FFFFFF",
        "india-green": "#138808",
        "india-green-dark": "#0F6606",
        "government-blue": "#0F3460",
        "government-blue-light": "#1A4B8A",
        "government-blue-dark": "#0A2442",
        "emerald": "#10B981",
        "emerald-light": "#34D399",
        "emerald-dark": "#059669",
      },
      fontFamily: {
        inter: ["Inter", "sans-serif"],
      },
      borderRadius: {
        card: "0.75rem",
        lg: "0.5rem",
      },
      boxShadow: {
        card: "0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.06)",
        "card-hover": "0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)",
        soft: "0 2px 4px rgba(0, 0, 0, 0.05)",
      },
      lineHeight: {
        relaxed: "1.75",
      },
    },
  },
  plugins: [],
};
