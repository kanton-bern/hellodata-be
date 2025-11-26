/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {},
  },
  plugins: [
    require('tailwindcss-primeui') //  PrimeUI plugin works in v3
  ],
}
