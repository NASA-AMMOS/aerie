/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { StringTMap } from '../models';

export const colorMap: StringTMap<string> = {
  Aquamarine: '#c1e2ec',
  'Cadet Blue': '#5c90c6',
  'Dodger Blue': '#4282c6',
  'Hot Pink': '#f569ab',
  Khaki: '#f9d977',
  Lavender: '#da9abe',
  Orange: '#f9bd85',
  'Orange Red': '#f49113',
  Pink: '#f5d5e4',
  Plum: '#b096c1',
  Purple: '#906fa9',
  Salmon: '#ffbfc1',
  'Sky Blue': '#a6cbf0',
  'Spring Green': '#7cbfb7',
  'Violet Red': '#b750a3',
  Yellow: '#f5ca2e',
  color1: '#5f9900',
  color2: '#0093c3',
  color3: '#ee9933',
  color4: '#cf3030',
  color5: '#8950d9',
  color6: '#3d3aad',
  color7: '#ec82b2',
  color8: '#575757',
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
