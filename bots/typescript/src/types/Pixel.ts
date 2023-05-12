import { Color } from "./Color";

export interface Pixel {
  color: Color;
  position: {
    x: number;
    y: number;
  };
}
