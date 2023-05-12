/**
 * pico-8 16 color palette from https://www.pixilart.com/palettes/pico-8-51001
 *
 * 0 "#000000"
 * 1 "#1D2B53"
 * 2 "#7E2553"
 * 3 "#008751"
 * 4 "#AB5236"
 * 5 "#5F574F"
 * 6 "#C2C3C7"
 * 7 "#FFF1E8"
 * 8 "#FF004D"
 * 9 "#FFA300"
 * a "#FFEC27"
 * b "#00E436"
 * c "#29ADFF"
 * d "#83769C"
 * e "#FF77A8"
 * f "#FFCCAA"
 */
export const colors = {
  BLACK: "0",
  BLUE: "1",
  PURPLE: "2",
  GREEN: "3",
  BROWN: "4",
  GREY: "5",
  SILVER: "6",
  WHITE: "7",
  RED: "8",
  ORANGE: "9",
  YELLOW: "a",
  BRIGHT_GREEN: "b",
  LIGHT_BLUE: "c",
  DARK_GRAY: "d",
  PINK: "e",
  TAN: "f",
} as const;

export type ColorName = keyof typeof colors;
export type Color = (typeof colors)[ColorName];
