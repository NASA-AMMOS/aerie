/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { StringTMap } from '../models';

export const colorMap: StringTMap<number[]> = {
  Aquamarine: [193, 226, 236],
  'Cadet Blue': [92, 144, 198],
  'Dodger Blue': [66, 130, 198],
  'Hot Pink': [245, 105, 171],
  Khaki: [249, 217, 119],
  Lavender: [218, 154, 190],
  Orange: [249, 189, 133],
  'Orange Red': [244, 145, 19],
  Pink: [245, 213, 228],
  Plum: [176, 150, 193],
  Purple: [144, 111, 169],
  Salmon: [255, 191, 193],
  'Sky Blue': [166, 203, 240],
  'Spring Green': [124, 191, 183],
  'Violet Red': [183, 80, 163],
  Yellow: [245, 202, 46],
  color1: [0x5f, 0x99, 0x00],
  color2: [0x00, 0x93, 0xc3],
  color3: [0xee, 0x99, 0x33],
  color4: [0xcf, 0x30, 0x30],
  color5: [0x89, 750, 0xd9],
  color6: [0x3d, 0x3a, 0xad],
  color7: [0xec, 0x82, 0xb2],
  color8: [0x57, 0x57, 0x57],
};

/**
 * Helper. Converts an rgb hex color string to a rgb color array.
 */
export function colorHexToRgbArray(hex: string): number[] {
  const color = [0, 0, 0];
  const pattern = new RegExp('#(.{2})(.{2})(.{2})');
  const match = hex.match(pattern);

  if (match) {
    color[0] = parseInt(match[1], 16);
    color[1] = parseInt(match[2], 16);
    color[2] = parseInt(match[3], 16);
  }

  return color;
}

/**
 * Helper. Converts an rgb color array to an rgb hex color string.
 */
export function colorRgbArrayToHex(rgb: number[]): string {
  const [r = 0, g = 0, b = 0] = rgb;
  return `#${colorRgbToHex(r)}${colorRgbToHex(g)}${colorRgbToHex(b)}`;
}

/**
 * Helper. Converts a single rgb value [0, 255] to a hex value.
 */
export function colorRgbToHex(rgb: number): string {
  const hex = rgb.toString(16).toUpperCase();
  return hex.length === 1 ? '0' + hex : hex;
}

/**
 * Helper that returns a random color from colorArray.
 */
export function getRandomColor() {
  const colorArray: StringTMap<any>[] = [];

  for (const color of Object.keys(colorMap)) {
    colorArray.push({ name: color, color: colorMap[color] });
  }

  return colorArray[Math.floor(Math.random() * colorArray.length)];
}
