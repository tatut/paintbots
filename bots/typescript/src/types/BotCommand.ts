import { Color } from "./Color";

export interface BotCommand {
  id: string;
  color?: Color;
  move?: string;
  paint?: string;
  clear?: string;
  look?: string;
  msg?: string;
  bye?: string;
  bots?: string;
}
