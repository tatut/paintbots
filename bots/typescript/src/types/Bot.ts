import { Color } from "./Color";

export interface Bot {
  id: string;
  name: string;
  color?: Color;
  position?: {
    x: number;
    y: number;
  };
}
