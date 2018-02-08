/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  MpsServerActivityPointMetadata,
  StringTMap,
} from './../models';

/**
 * Helper that gets a color from metadata.
 */
export function getColorFromMetadata(metadata: MpsServerActivityPointMetadata[]): number[] {
  const colorMap: StringTMap<number[]> = {
    'Aquamarine': [193, 226, 236],
    'Cadet Blue': [92, 144, 198],
    'Dodger Blue': [66, 130, 198],
    'Hot Pink': [245, 105, 171],
    'Khaki': [249, 217, 119],
    'Lavender': [218, 154, 190],
    'Orange': [249, 189, 133],
    'Orange Red': [244, 145, 19],
    'Pink': [245, 213, 228],
    'Plum': [176, 150, 193],
    'Purple': [144, 111, 169],
    'Salmon': [255, 191, 193],
    'Sky Blue': [166, 203, 240],
    'Spring Green': [124, 191, 183],
    'Violet Red': [183, 80, 163],
    'Yellow': [245, 202, 46],
    'color1': [0x5f, 0x99, 0x00],
    'color2': [0x00, 0x93, 0xc3],
    'color3': [0xee, 0x99, 0x33],
    'color4': [0xcF, 0x30, 0x30],
    'color5': [0x89, 750, 0xD9],
    'color6': [0x3D, 0x3A, 0xAD],
    'color7': [0xEC, 0x82, 0xB2],
    'color8': [0x57, 0x57, 0x57],
  };

  let color = [0, 0, 0];

  for (let i = 0, l = metadata.length; i < l; ++i) {
    const data: MpsServerActivityPointMetadata = metadata[i];

    if (data.Name.toLowerCase() === 'color') {
      const newColor = colorMap[data.Value];

      if (newColor) {
        color = newColor;
      }
    }
  }

  return color;
}
