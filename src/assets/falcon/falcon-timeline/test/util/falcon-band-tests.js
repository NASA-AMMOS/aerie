/* global test, assert, fixture */

/* eslint-disable prefer-arrow-callback, no-multi-spaces */

/**
 * Set prop to x via attribute and make sure it's equal to x in Polymer and CTL.
 * Then programatically change prop to y and make sure it's correct in Polymer and CTL.
 */
function testPropChange(el, ctlObject, prop, x, y) {
  // Prop set via attribute to x is correct in Polymer and CTL.
  assert.deepEqual(el[prop], x);        // Polymer.
  assert.deepEqual(ctlObject[prop], x); // CTL.

  // Prop set programatically to y is correct in Polymer and CTL.
  el[prop] = y;
  assert.deepEqual(el[prop], y);        // Polymer.
  assert.deepEqual(ctlObject[prop], y); // CTL.
}

/**
 * General tests that apply to every band in Falcon.
 *
 * @param {any} ctlBandName
 */
function falconBandTests(ctlBandName) {
  test('instantiating the element with default band properties works', function() {
    const el = fixture('BasicTestFixture');
    const ctlBand = el[`${ctlBandName}`];

    // Element Props.
    assert.equal(el.height, 100);
    assert.equal(el.heightPadding, 10);
    assert.equal(el.label, '');
    assert.deepEqual(el.labelColor, [0, 0, 0]);
    assert.equal(el.labelWidth, 100);
    assert.deepEqual(el.maxTimeRange, { end: 0, start: 0 });
    assert.deepEqual(el.minorLabels, []);
    assert.equal(el.name, '');
    assert.equal(el.noDraw, false);
    assert.equal(el.id, '');
    assert.deepEqual(el.viewTimeRange, { end: 0, start: 0 });

    // CTL Options.
    assert.equal(ctlBand.height, 100);
    assert.equal(ctlBand.heightPadding, 10);
    assert.equal(ctlBand.label, '');
    assert.deepEqual(ctlBand.labelColor, [0, 0, 0]);
    assert.deepEqual(ctlBand.minorLabels, []);
    assert.equal(ctlBand.name, '');
    assert.equal(ctlBand.timeAxis.start, 0);
    assert.equal(ctlBand.timeAxis.end, 0);
    assert.equal(ctlBand.viewTimeAxis.start, 0);
    assert.equal(ctlBand.viewTimeAxis.end, 0);
  });

  test('setting the height property on the element works', function() {
    const el = fixture('CtlBandPropertyTestFixture');
    const ctlBand = el[`${ctlBandName}`];

    testPropChange(el, ctlBand, 'height', 100, 50);
  });

  test('setting the heightPadding property on the element works', function() {
    const el = fixture('CtlBandPropertyTestFixture');
    const ctlBand = el[`${ctlBandName}`];

    testPropChange(el, ctlBand, 'heightPadding', 10, 20);
  });

  test('setting the label property on the element works', function() {
    const el = fixture('CtlBandPropertyTestFixture');
    const ctlBand = el[`${ctlBandName}`];

    testPropChange(el, ctlBand, 'label', 'thatWasEasy', 'thatWasHard');
  });

  test('setting the labelColor property on the element works', function() {
    const el = fixture('CtlBandPropertyTestFixture');
    const ctlBand = el[`${ctlBandName}`];

    el.labelColor = [0, 0, 0];
    testPropChange(el, ctlBand, 'labelColor', [0, 0, 0], [1, 1, 1]);
  });

  test('setting the labelWidth property on the element works', function() {
    const el = fixture('BasicTestFixture');

    el.set('labelWidth', 50);
    assert.equal(el.labelWidth, 50);
  });

  test('setting the maxTimeRange property on the element works', function() {
    const el = fixture('BasicTestFixture');
    const ctlBand = el[`${ctlBandName}`];

    el.set('maxTimeRange', { end: 10, start: 1 });
    assert.deepEqual(el.maxTimeRange, { end: 10, start: 1 });
    assert.equal(ctlBand.timeAxis.start, 1);
    assert.equal(ctlBand.timeAxis.end, 10);
  });

  test('setting the minorLabels property on the element works', function() {
    const el = fixture('CtlBandPropertyTestFixture');
    const ctlBand = el[`${ctlBandName}`];

    el.minorLabels = ['a'];
    testPropChange(el, ctlBand, 'minorLabels', ['a'], ['b']);
  });

  test('setting the noDraw property on the element works', function() {
    const el = fixture('BasicTestFixture');

    el.noDraw = true;
    assert.equal(el.noDraw, true);
  });

  test('setting the id property on the element works', function() {
    const el = fixture('BasicTestFixture');

    el.id = '111111';
    assert.equal(el.id, '111111');
  });

  test('setting the name property on the element works', function() {
    const el = fixture('CtlBandPropertyTestFixture');
    const ctlBand = el[`${ctlBandName}`];

    testPropChange(el, ctlBand, 'name', 'someName', 'someOtherName');
  });

  test('setting the viewTimeRange property on the element works', function() {
    const el = fixture('BasicTestFixture');
    const ctlBand = el[`${ctlBandName}`];

    el.set('viewTimeRange', { end: 10, start: 1 });
    assert.deepEqual(el.viewTimeRange, { end: 10, start: 1 });
    assert.equal(ctlBand.viewTimeAxis.start, 1);
    assert.equal(ctlBand.viewTimeAxis.end, 10);
  });
}

window.falconBandTests = falconBandTests;
