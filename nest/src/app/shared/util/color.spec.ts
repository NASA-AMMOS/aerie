/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { colorHexToRgbArray, colorRgbArrayToHex, colorRgbToHex } from './color';

describe('colorHexToRgbArray', () => {
  it(`should convert a non hex value to a black color array`, () => {
    expect(colorHexToRgbArray('42')).toEqual([0, 0, 0]);
  });

  it(`should properly convert a hex color to an array of colors`, () => {
    expect(colorHexToRgbArray('#000000')).toEqual([0, 0, 0]);
    expect(colorHexToRgbArray('#FF0000')).toEqual([255, 0, 0]);
    expect(colorHexToRgbArray('#00FF00')).toEqual([0, 255, 0]);
    expect(colorHexToRgbArray('#0000FF')).toEqual([0, 0, 255]);
    expect(colorHexToRgbArray('#FFFFFF')).toEqual([255, 255, 255]);
  });
});

describe('colorRgbArrayToHex', () => {
  it(`should convert an empty color array to a black hex color value`, () => {
    expect(colorRgbArrayToHex([])).toEqual('#000000');
  });

  it(`should properly convert an rgb color array to a hex color value`, () => {
    expect(colorRgbArrayToHex([0, 0, 0])).toEqual('#000000');
    expect(colorRgbArrayToHex([255, 0, 0])).toEqual('#FF0000');
    expect(colorRgbArrayToHex([0, 255, 0])).toEqual('#00FF00');
    expect(colorRgbArrayToHex([0, 0, 255])).toEqual('#0000FF');
    expect(colorRgbArrayToHex([255, 255, 255])).toEqual('#FFFFFF');
  });
});

describe('colorRgbToHex', () => {
  it(`should convert a rgb number to a hex value`, () => {
    expect(colorRgbToHex(0)).toEqual('00');
    expect(colorRgbToHex(255)).toEqual('FF');
  });
});
