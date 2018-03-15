ActivityBand.prototype = new Band();
ActivityBand.prototype.constructor = ActivityBand;

function ActivityBand(obj) {
  if(typeof obj === "undefined") return;

  // indicates if the height is automatically determined
  this.autoHeight = ("autoHeight" in obj) ? obj.autoHeight : false;

  if(!("decorator" in obj)) { obj.decorator = new Decorator(obj); }
  if(!("painter" in obj)) { obj.painter = new ActivityPainter(obj); }
  Band.prototype.constructor.call(this, obj);

  // callback functions
  this.onIsSelectedActivity = ("onIsSelectedActivity" in obj) ? obj.onIsSelectedActivity : null;
  this.onIsDraggable = ("onIsDraggable" in obj) ? obj.onIsDraggable : null;
  this.onDragStart = ("onDragStart" in obj) ? obj.onDragStart : null;
  this.onDragStop = ("onDragStop" in obj) ? obj.onDragStop : null;
  this.onIsDroppable = ("onIsDroppable" in obj) ? obj.onIsDroppable : null;
  this.onDrop = ("onDrop" in obj) ? obj.onDrop : null;

  // only necessary if draggable
  if(this.onIsDraggable) {
    this.draggedAct = null;
    this.draggableHelper = null;

    // we need to create a new div as the source for enabling
    // dragging since we are trying to use the same div for dragging
    // as dropping.  The event to start the drag processing is
    // dispatched in the main div mouse down handler
    this._actHiddenDragSource = document.createElement("div");
    this.div.appendChild(this._actHiddenDragSource);

    var oDraggableOptions = ("draggableOptions" in obj) ? obj.draggableOptions : {};
    this.startDraggable = ("startDraggable" in oDraggableOptions) ? oDraggableOptions.startDraggable : false;
    this.endDraggable = ("endDraggable" in oDraggableOptions) ? oDraggableOptions.endDraggable : false;
    this.helperParent = ("helperParent" in oDraggableOptions) ? oDraggableOptions.helperParent : null;
    this.snap = ("snap" in oDraggableOptions) ? Math.max(1,oDraggableOptions.snap) : 1;
    this.draggableOptions = {
      axis: ("axis" in oDraggableOptions) ? oDraggableOptions.axis : null,
      delay: ("delay" in oDraggableOptions) ? oDraggableOptions.delay : 0,
      cursor: ("cursor" in oDraggableOptions) ? oDraggableOptions.cursor : "move",
      distance: ("distance" in oDraggableOptions) ? oDraggableOptions.distance : 0,
      disabled: true,
      cursorAt: {top:0, left:0},
      helper: this.getHelperAct.bind(this),
      start: this.handleActDragStart.bind(this),
      drag: this.handleActDrag.bind(this),
      stop: this.handleActDragStop.bind(this)
    };
    $(this._actHiddenDragSource).draggable(this.draggableOptions);
  }
  if(this.autoHeight) {
    this.height = this.computeAutoHeight();
  }
}

ActivityBand.DRAG_MOVE       = 1;
ActivityBand.DRAG_START_TIME = 2;
ActivityBand.DRAG_END_TIME   = 3;

ActivityBand.prototype.setIntervals = function(intervals, index) {
  Band.prototype.setIntervals.call(this, intervals, index);
};

ActivityBand.prototype.addInterval = function(interval, index) {
  Band.prototype.addInterval.call(this, interval, index);
};

ActivityBand.prototype.addIntervals = function(intervals, index) {
  Band.prototype.addIntervals.call(this, intervals, index);
};

ActivityBand.prototype.removeInterval = function(id) {
  Band.prototype.removeInterval.call(this, id);
};

ActivityBand.prototype.removeIntervals = function(ids) {
  Band.prototype.removeIntervals.call(this, ids);
};

ActivityBand.prototype.computeNumRowsWaterfallLayout = function() {
  // waterfall view has a one activity per row, so we can quickly
  // compute the number of rows based on interval count.  Since a
  // band contains a list of list of activities and rendering acts
  // for each list starts back at the y origin , we only need to
  // count the max number of activities in a list.
  var numRows = 1;
  for(i=0, ilength=this.intervalsList.length; i<ilength; ++i) {
    var intervals = this.intervalsList[i];
    numRows = Math.max(numRows, intervals.length);
  }
  return numRows;
};

ActivityBand.prototype.computeNumRowsCompactLayout = function() {
  var ctx = this.canvas.getContext('2d');
  var painter = this.painter;
  var start = this.timeAxis.start;
  var end = this.timeAxis.end;
  var viewTimeAxis = this.viewTimeAxis;
  // always only consider VISIBLE_INTERVALS
  start = viewTimeAxis.start;
  end = viewTimeAxis.end;

  // compute a more compact view, which ensures that activities
  // don't overlap.  If there is overlap (including label), then
  // render the activity on the next row
  var maxRows = 1;
  var actsList = this.getIntervalsInTimeRange(start, end);
  for(var i=0, ilength=actsList.length; i<ilength; ++i) {
    var intervals = actsList[i];

    var rows = 0;
    var openIntervals = intervals.slice(0);
    while(openIntervals.length > 0) {
      var prevDrawEnd = null;
      var newOpenIntervals = [];

      rows++;
      for(var j=0, jlength=openIntervals.length; j<jlength; ++j) {
        var interval = openIntervals[j];
        if(interval.end <= start || interval.start >= end) {
          continue;
        }
        if(prevDrawEnd !== null && prevDrawEnd > interval.start) {
          newOpenIntervals.push(interval);
        }
        else {
          prevDrawEnd = interval.end;
          if(this.painter.autoFit && !this.painter.trimLabel && interval.label !== null) {
            // since we using the viewtime axis, tracks may be computed off the canvas so
            // we need to use the no clamping function to compute the x value
            var labelX1 = viewTimeAxis.getXFromTimeNoClamping(interval.start) + painter.labelPadding;
            var labelWidth = ctx.measureText(interval.label).width;
            prevDrawEnd = Math.max(prevDrawEnd, viewTimeAxis.getTimeFromX(labelX1+labelWidth));
          }
        }
      }
      openIntervals = newOpenIntervals;
    }
    if(rows === 0) { rows = 1; }

    if(rows > maxRows) {
      maxRows = rows;
    }
  }
  return maxRows;
};

ActivityBand.prototype.computeNumRows = function() {
  var painter = this.painter;
  if(painter.layout === ActivityPainter.COMPACT_LAYOUT) {
    return this.computeNumRowsCompactLayout();
  }
  else if(painter.layout === ActivityPainter.WATERFALL_LAYOUT) {
    return this.computeNumRowsWaterfallLayout();
  }
  // default to 1 row
  return 1;
};

ActivityBand.prototype.computeAutoHeight = function() {
  if(!this.painter.rowHeight) { return this.height; }

  var numRows = this.computeNumRows();
  return numRows*this.painter.rowHeight;
};

ActivityBand.prototype.mousedown = function(e) {
  if(this.onLeftClick === null &&
     this.onRightClick === null &&
     this.onUpdateView === null &&
     this.onIsDraggable === null) return true;

  var x = e.pageX - $(e.target).offset().left;
  var y = e.pageY - $(e.target).offset().top;
  var mouseTime = this.viewTimeAxis.getTimeFromX(x);

  var interval = null;
  var intervals = this.findIntervals(x, y);
  if(intervals.length !== 0) {
    interval = intervals[intervals.length-1];
  }

  if(e.which === 1) {
    // left click
    if(this.onLeftClick !== null) { this.onLeftClick(e, {band:this, interval:interval, time:mouseTime}); }

    if(interval !== null) {
      if(this.onIsDraggable && this.onIsDraggable(e, {band:this, interval:interval})) {
        // store the act being dragged, enable the draggable component and fire
        // trigger the event so that dragging can begin
        var left = x - this.viewTimeAxis.getXFromTime(interval.start);
        this.draggedAct = interval;
        $(this._actHiddenDragSource).draggable("option", "cursorAt", { top:0, left:left } );
        $(this._actHiddenDragSource).draggable("enable");
        $(this._actHiddenDragSource).triggerHandler(e);
      }
    }
    else if(x >= this.viewTimeAxis.x1 && this.onUpdateView !== null) {
      // enable dragging if on timeline area
      $(this._panHiddenDragSource).draggable("enable");
      $(this._panHiddenDragSource).triggerHandler(e);
    }
  }
  else {
    // right click
    if(this.onRightClick === null) return true;
    if(interval === null) {
      // search the background intervals
      var backgroundIntervals = this.findBackgroundIntervals(x, y);
      if(backgroundIntervals.length !== 0) {
        interval = backgroundIntervals[0];
      }
    }
    this.onRightClick(e, {band:this, interval:interval, time:mouseTime});
  }
  return true;
};

// handler for the dbl click
ActivityBand.prototype.dblclick = function(e) {
  if(this.onDblLeftClick === null) return true;

  // disable dragging
  if(this.onIsDraggable) {
    this.draggedAct = null;
    $(this._actHiddenDragSource).draggable("disable");
  }

  var x = e.pageX - $(e.target).offset().left;
  var y = e.pageY - $(e.target).offset().top;
  var interval = null;
  var intervals = this.findIntervals(x, y);
  if(intervals.length !== 0) {
    interval = intervals[intervals.length-1];
  }
  var obj = {band:this, interval:interval};
  this.onDblLeftClick(e, obj);

  return true;
};

// get the element that represents the dragged act
ActivityBand.prototype.getHelperAct = function() {
  this.draggableHelper = new DraggableHelper({band:this});
  if(this.helperParent !== null) {
    this.helperParent.appendChild(this.draggableHelper.div);
  }
  return this.draggableHelper.div;
};

// hook for when dragging starts
ActivityBand.prototype.handleActDragStart = function(e, ui) {
  if(this.onDragStart) {
    this.onDragStart(e, {band:this, interval:this.draggedAct});
    var dragType = this.getDragType(e, ui);
    this.draggableHelper.setDragType(dragType);
    this.draggableHelper.dragStart(e, ui);
  }
};

// hook during dragging
ActivityBand.prototype.handleActDrag = function(e, ui) {
  if(this.onIsDraggable) {
    this.draggableHelper.dragging(e, ui);
  }
};

// hook when dragging is done
ActivityBand.prototype.handleActDragStop = function(e, ui) {
  if(this.onDragStop) {
    this.draggableHelper.dragStop(e, ui);
    this.onDragStop(e, {band:this, interval:this.draggedAct});
  }
};

// enable this band as droppable
ActivityBand.prototype.enableDroppable = function() {
  if(this.onIsDroppable && this.onIsDroppable({band:this, interval:this.draggedAct})) {
    $(this.div).droppable({
      activeClass: "ui-state-highlight",
      tolerance: "pointer",
      drop: this.handleActDrop.bind(this)
    });
  }
};

// disable this band as droppable
ActivityBand.prototype.disableDroppable = function() {
  if($(this.div).data("ui-droppable")) {
    $(this.div).droppable("destroy");
  }
};

// hook when a legal drop occurred
ActivityBand.prototype.handleActDrop = function(e, ui) {
  if(this.onDrop === null) return;

  // a drop may occur on a different band.  This draggable
  // helper is owned by the band that initiated the drag
  var draggableHelper = ui.helper[0].draggableHelper;

  var dropStart = draggableHelper.intervalStart;
  var dropEnd = draggableHelper.intervalEnd;
  this.onDrop(e, {band:this, interval:this.draggedAct, dropStart:dropStart, dropEnd:dropEnd});
};

ActivityBand.prototype.repaint = function() {
  if(this.autoHeight) {
    var newHeight = this.computeAutoHeight();
    if(newHeight !== this.height) {
      this.height = newHeight;
      this.revalidate();
    }
  }
  Band.prototype.repaint.call(this);
};

// returns the type of drag being started
ActivityBand.prototype.getDragType = function(e, ui) {
  if(!this.startDraggable && !this.endDraggable) {
    return ActivityBand.DRAG_MOVE;
  }
  else {
    var distance = 6;
    var startX = this.viewTimeAxis.getXFromTime(this.draggedAct.start);
    var endX = this.viewTimeAxis.getXFromTime(this.draggedAct.end);
    var mouseX = e.pageX - $(e.target).offset().left;
    if((mouseX - startX) < distance) {
      return ActivityBand.DRAG_START_TIME;
    } else if((endX - mouseX) < distance) {
      return ActivityBand.DRAG_END_TIME;
    }
  }
  return ActivityBand.DRAG_MOVE;
};

//------------------------------------------------------------------------------

function DraggableHelper(obj) {
  this.band = obj.band;
  this.dragType = ActivityBand.DRAG_MOVE;

  // indicates the start/end time of the interval we're dragging
  this.intervalStart = 0;
  this.intervalEnd = 0;
  this.intervalCoords = this.band.findIntervalCoords(this.band.draggedAct.id);
  this.snap = this.band.snap;

  this.interval = document.createElement("div");
  this.interval.setAttribute("class", "draggablehelperinterval");

  this.caption = document.createElement("div");
  this.caption.setAttribute("class", "draggablehelpercaption");

  this.div = document.createElement("div");
  this.div.setAttribute("class", "draggablehelperdiv");
  this.div.appendChild(this.interval);
  this.div.appendChild(this.caption);

  // set the back pointer
  this.div.draggableHelper = this;
}

DraggableHelper.prototype.setDragType = function(type) {
  this.dragType = type;
};

DraggableHelper.prototype.dragStart = function(e, ui) {
  var band = this.band;
  var painter = this.band.painter;
  var draggedAct = this.band.draggedAct;

  // update the interval times
  this.intervalStart = draggedAct.start;
  this.intervalEnd = draggedAct.end;

  // update the interval to look like the dragged act
  var bgColor = Util.rgbaToString(painter.getColor(draggedAct), draggedAct.opacity);
  $(this.interval).css("backgroundColor", bgColor);
  $(this.interval).css("height", band.painter.activityHeight);
};

DraggableHelper.prototype.dragging = function(e, ui) {
  var band = this.band;
  var draggedAct = this.band.draggedAct;
  var timeAxis = this.band.timeAxis;
  var viewTimeAxis = this.band.viewTimeAxis;
  var timeZone = timeAxis.timeZone;
  var viewStart = viewTimeAxis.start;
  var viewEnd = viewTimeAxis.end;

  var actStart = draggedAct.start;
  var actEnd = draggedAct.end;
  var actLatestStart = (draggedAct.latestStart!==null) ? draggedAct.latestStart : actStart;
  var actEarliestEnd = (draggedAct.earliestEnd!==null) ? draggedAct.earliestEnd : actEnd;

  var actStartX = viewTimeAxis.getXFromTime(actStart);
  var actEndX = viewTimeAxis.getXFromTime(actEnd);

  var helperStart = actStart;
  var helperEnd = actEnd;
  var helperLeft = 0;
  var helperStartX = ui.offset.left - $(band.div).offset().left;

  var axis = band.draggableOptions.axis;
  var snapOffset = 0;
  var helperWidth;
  if(axis === null || axis === "x") {
    switch(this.dragType) {
    case ActivityBand.DRAG_MOVE:
      // used for the caption later below
      helperStart = viewTimeAxis.getTimeFromX(helperStartX);
      helperEnd = helperStart + (actEnd - actStart);
      snapOffset = this.getSnapOffset(helperStart);
      helperLeft = viewTimeAxis.getXFromTime(helperStart - snapOffset) - viewTimeAxis.getXFromTime(helperStart);
      helperStart -= snapOffset;
      helperEnd -= snapOffset;
      break;
    case ActivityBand.DRAG_START_TIME:
      helperStart = viewTimeAxis.getTimeFromX(helperStartX);
      if(helperStart < helperEnd) {
        snapOffset = this.getSnapOffset(helperStart);
        if(helperStart - snapOffset >= helperEnd) {
          snapOffset += this.snap;
        }
        helperLeft = viewTimeAxis.getXFromTime(helperStart - snapOffset) - viewTimeAxis.getXFromTime(helperStart);
        helperStart -= snapOffset;
      } else {
        var endOffset = this.getSnapOffset(helperEnd);
        if(endOffset <= 0) {
          endOffset += this.snap;
        }
        helperStart = helperEnd - endOffset;
        snapWidth = viewTimeAxis.getXFromTime(helperStart + endOffset) - viewTimeAxis.getXFromTime(helperStart);
        helperLeft = actEndX - helperStartX - snapWidth;
      }
      break;
    case ActivityBand.DRAG_END_TIME:
      helperEnd = viewTimeAxis.getTimeFromX(helperStartX+(actEndX-actStartX));
      if(helperEnd > helperStart) {
        snapOffset = this.getSnapOffset(helperEnd);
        if(helperEnd - snapOffset <= helperStart) {
          snapOffset -= this.snap;
        }
        helperEnd -= snapOffset;
      } else {
        var startOffset = this.getSnapOffset(helperStart);
        if(startOffset >= 0) {
          startOffset -= this.snap;
        }
        helperEnd = helperStart - startOffset;
      }
      helperLeft = actStartX - helperStartX;
      break;
    }

    helperWidth = viewTimeAxis.getXFromTime(helperEnd) - viewTimeAxis.getXFromTime(helperStart);

    var viewStartOffset = this.getSnapOffset(viewStart);
    if(viewStartOffset > 0) {
      viewStartOffset -= this.snap;
    }
    var timeOffset;
    if(this.dragType !== ActivityBand.DRAG_END_TIME && helperStart <= viewStart-viewStartOffset) {
      // addition of constants to helperLeft are to align with edge of band properly
      if(helperStart < viewStart) {
        timeOffset = viewStart+1-helperStart-snapOffset;
        helperLeft = (timeOffset >= 0) ? this.getXWidthOfTime(Math.abs(timeOffset)) : -1*(this.getXWidthOfTime(Math.abs(timeOffset))) - 1;
        helperWidth = viewTimeAxis.getXFromTime(helperEnd) - viewTimeAxis.getXFromTime(viewStart);
      } else if(snapOffset < viewStartOffset) {
        helperLeft = this.getXWidthOfTime(viewStart-viewStartOffset - helperStart - snapOffset);
      }
    }
    if(this.dragType !== ActivityBand.DRAG_START_TIME && helperEnd > viewEnd) {
      var viewEndOffset = this.getSnapOffset(viewEnd);
      if(viewEndOffset < 0) {
        viewEndOffset += this.snap;
      }
      if(helperStart >= viewEnd-viewEndOffset) {
        if(helperStart > viewEnd) {
          // subtracting extra 2 so the edge appears, as it does on the start.
          timeOffset = helperStart - viewEnd + snapOffset;
          helperLeft = (timeOffset >= 0) ? -1*(this.getXWidthOfTime(timeOffset)) - 1: this.getXWidthOfTime(Math.abs(timeOffset));
        } else if(snapOffset > viewEndOffset) {
          helperLeft = -1*(this.getXWidthOfTime(helperStart - (viewEnd-viewEndOffset) + snapOffset) + 1);
        }
        helperWidth = viewTimeAxis.getXFromTime(viewEnd) - viewTimeAxis.getXFromTime(helperStart);
      }
    }
  }  else if(axis === "y") {
    // nothing to do when dragging in the y-axis
    if(helperStart < viewTimeAxis.start && helperEnd > viewTimeAxis.end) {
      helperLeft = 0;
      helperWidth = this.getXWidthOfTime(viewTimeAxis.end - viewTimeAxis.start - 1);
    } else if(helperStart < viewTimeAxis.start) {
      helperLeft = 0;
      helperWidth = this.getXWidthOfTime(helperEnd - viewTimeAxis.start);
    } else if(helperEnd > viewTimeAxis.end) {
      helperWidth = this.getXWidthOfTime(viewTimeAxis.end - helperStart);
    } else {
      helperWidth = this.getXWidthOfTime(helperEnd - helperStart);
    }
  }

  // set the width and x position of the helper
  $(this.interval).css({width: helperWidth,
                        left: helperLeft});
  $(this.caption).css({width: helperWidth,
                       left: helperLeft});

  if (axis === "x") {
    // fix the y-position for x-axis dragging
    ui.position.top = $(this.band.div).offset().top + this.intervalCoords[2];
  }

  // update the label and caption text
  if(band.painter.showLabel) {
    this.interval.innerHTML = draggedAct.label;
    if(band.painter.trimLabel) {
      $(this.interval).css({"text-overflow": "none",
                            "white-space": "nowrap",
                            "overflow": "hidden"});
    }
  }
  // include the start/end times
  var timeFormat = "HHmm";
  var helperLatestStart = helperStart + (actLatestStart - actStart);
  var helperEarliestEnd = helperEnd - (actEnd - actEarliestEnd);
  var caption = Util.toTimeString(helperStart, {format:timeFormat, timeZone:timeZone}) + "-" + Util.toTimeString(helperEnd, {format:timeFormat, timeZone:timeZone});
  if(helperLatestStart !== helperStart || helperEarliestEnd !== helperEnd) {
    caption += "<br>";
    caption += Util.toTimeString(helperLatestStart, {format:timeFormat, timeZone:timeZone}) + "-" + Util.toTimeString(helperEarliestEnd, {format:timeFormat, timeZone:timeZone});
  }
  this.caption.innerHTML = caption;

  // update the start/end times of where we dragged to
  this.intervalStart = helperStart;
  this.intervalEnd   = helperEnd;
};

DraggableHelper.prototype.dragStop = function(e, ui) {
  this.dragType = ActivityBand.DRAG_MOVE;
};

DraggableHelper.prototype.getSnapOffset = function(time) {
  if(this.snap > 60) {
    var zoneShift = moment.utc(time*1000).tz(this.band.timeAxis.timeZone).utcOffset();
    time += zoneShift*60;
  }
  var offset = time % this.snap;
  return (offset < this.snap/2) ? offset : offset - this.snap;
};

DraggableHelper.prototype.getXWidthOfTime = function(time) {
  var viewTimeAxis = this.band.viewTimeAxis;
  var maxTimeWidth = viewTimeAxis.end - viewTimeAxis.start;
  var maxXWidth = viewTimeAxis.getXFromTime(viewTimeAxis.end) - viewTimeAxis.getXFromTime(viewTimeAxis.start);
  var numberWidths = Math.floor(time / maxTimeWidth);
  return numberWidths*maxXWidth + ( viewTimeAxis.getXFromTime(viewTimeAxis.start + time%maxTimeWidth) - viewTimeAxis.getXFromTime(viewTimeAxis.start) );
};

ActivityPainter.prototype = new Painter();
ActivityPainter.prototype.constructor = ActivityPainter;

// the painter of drawable activities in bar mode
function ActivityPainter(obj) {
  if(typeof obj === "undefined") { obj = {}; }

  Painter.prototype.constructor.call(this, obj);

  // the height of the rows between activities on different y value
  this.rowHeight         = ("rowHeight" in obj) ? obj.rowHeight : 24;
  // the additional padding added to a row
  this.rowPadding        = ("rowPadding" in obj) ? obj.rowPadding : 2;
  // the height to render an activity
  this.activityHeight    = ("activityHeight" in obj) ? obj.activityHeight : 20;
  // the height to render the start range of an activity
  this.startRangeHeight  = ("startRangeHeight" in obj) ? obj.startRangeHeight : 4;
  // the height to render the end range of an activity
  this.endRangeHeight    = ("endRangeHeight" in obj) ? obj.endRangeHeight : 4;
  // if true, automatically determines the rowHeight to ensure that all the activites are visible
  this.autoFit           = ("autoFit" in obj) ? obj.autoFit : null;

  // determines if activities are rendered as a line or a bar
  this.style = ActivityPainter.BAR_STYLE;
  if("style" in obj) { this.setStyle(obj.style); }

  // determines if activities are layed out compactly or waterfall mode
  this.layout = ActivityPainter.COMPACT_LAYOUT;
  if("layout" in obj) { this.setLayout(obj.layout); }
}

ActivityPainter.BAR_STYLE  = 1;
ActivityPainter.LINE_STYLE = 2;
ActivityPainter.ICON_STYLE = 3;

ActivityPainter.COMPACT_LAYOUT = 1;
ActivityPainter.WATERFALL_LAYOUT = 2;

ActivityPainter.prototype.setStyle = function(style) {
  if(style === ActivityPainter.BAR_STYLE || style === "bar") {
    this.style = ActivityPainter.BAR_STYLE;
  }
  else if(style === ActivityPainter.LINE_STYLE || style === "line") {
    this.style = ActivityPainter.LINE_STYLE;
  }
  else if(style === ActivityPainter.ICON_STYLE || style === "icon") {
    this.style = ActivityPainter.ICON_STYLE;
  }
};

ActivityPainter.prototype.getStyle = function() {
  return this.style;
};

ActivityPainter.prototype.setLayout = function(layout) {
  if(layout === ActivityPainter.COMPACT_LAYOUT || layout === "compact") {
    this.layout = ActivityPainter.COMPACT_LAYOUT;
  }
  else if(layout === ActivityPainter.WATERFALL_LAYOUT || layout === "waterfall") {
    this.layout = ActivityPainter.WATERFALL_LAYOUT;
  }
};

ActivityPainter.prototype.getLayout = function() {
  return this.layout;
};

ActivityPainter.prototype.computeAutoFit = function() {
  if(this.band === null) return this.rowHeight;
  if(!this.autoFit) return this.rowHeight;

  var numRows = this.band.computeNumRows();
  if(numRows === 1) {
    return this.band.height;
  }

  var rowHeight = (this.band.height - this.activityHeight - 1*this.rowPadding) / (numRows - 1);
  return Math.max(0, rowHeight);
};

ActivityPainter.prototype.getColor = function(act) {
  if(act.color !== null) {
    return act.color;
  }
  else if(this.autoColor) {
    return Painter.getAutoColor(act.label);
  }
  else {
    return this.color;
  }
};

ActivityPainter.prototype.paintActivityAsBar = function(rowY, act, previousAct, previousActX1, previousActX2, lastPaintedTimeX2, lastPaintedTime) {
  var ctx = this.band.canvas.getContext('2d');
  rowY = rowY - this.rowPadding;

  // paint the activity
  var viewTimeAxis = this.band.viewTimeAxis;
  var actX1 = viewTimeAxis.getXFromTime(act.start);
  var actX2 = viewTimeAxis.getXFromTime(act.end);
  var actY1 = rowY - this.activityHeight;
  var actY2 = rowY;
  var actWidth = Math.max(1.0, actX2 - actX1);
  var actHeight = this.activityHeight;

  // draw the colored activity
  var actColor = this.getColor(act);
  // RAVEN -- if activity width is very small, don't draw the border and set opacity to 1.0
  var opacity = act.opacity;
  if(this.borderWidth > 0 && actWidth < this.borderWidth *2) {
      opacity = 1.0;
      // double the width since border won't be drawn
      actWidth = actWidth*1.5;
  }

  var color = Util.rgbaToString(actColor, opacity);
  ctx.fillStyle = color;

  // raven; draw box if style is bar
  if (this.style === ActivityPainter.BAR_STYLE) {
      ctx.fillRect(actX1, actY1, actWidth, actHeight);
  }

  // draw the black box representing the start range
  if(act.latestStart !== null) {
    var latestStartX1 = viewTimeAxis.getXFromTime(act.latestStart);
    var startRangeWidth = latestStartX1 - actX1;
    ctx.fillStyle = Util.rgbaToString([75, 75, 75], act.opacity);
    ctx.fillRect(actX1, actY1, startRangeWidth, this.startRangeHeight);
  }

  // draw the black box representing the end range
  if(act.earliestEnd !== null) {
    var earliestEndX2 = viewTimeAxis.getXFromTime(act.earliestEnd);
    var endRangeWidth = actX2 - earliestEndX2;
    ctx.fillStyle = Util.rgbaToString([75, 75, 75], act.opacity);
    ctx.fillRect(earliestEndX2, actY1, endRangeWidth, this.endRangeHeight);
  }

  if(act.isConflicted) {
    ctx.fillStyle = Util.rgbaToString([255, 0, 0], act.opacity);
    ctx.fillRect(actX1, actY1 + actHeight - 2, actWidth, 2);
  }

  // paint the icon if icon on or style is icon and icon specified
  if((this.showIcon || this.style === ActivityPainter.ICON_STYLE) && act.icon !== null && (act.icon in this.iconPainters)) {
    var iconPainter = this.iconPainters[act.icon];
    var iconColor = iconPainter.color ? Util.rgbaToString(iconPainter.color, 1.0) : Util.rgbaToString(actColor, 1.0);
    var iconWidth = iconPainter.width;
    iconPainter.paint({band:this.band,
                       interval:act,
                       color:iconColor,
                       width:iconWidth,
                       ll:{x:actX1, y:actY2},
                       ul:{x:actX1, y:actY1},
                       ur:{x:actX2, y:actY1},
                       lr:{x:actX2, y:actY2}});
  }

  if (this.showLabel) {
      // autofit already includes space for label
      if (this.autoFit) {
          if (act.label !== null) {
              this.paintLabel({interval:act,
                     ll:{x:actX1, y:actY2},
                     ul:{x:actX1, y:actY1},
                     ur:{x:actX2, y:actY1},
                     lr:{x:actX2, y:actY2}});
          }
      }
      // paint the label of the previous act in the row
      else if (previousAct && previousAct.label !== null) {
          // paint previous activity label if there is room
          this.paintLabel({interval:previousAct,
                     ll:{x:previousActX1, y:actY2},
                     ul:{x:previousActX1, y:actY1},
                     ur:{x:previousActX2, y:actY1},
                     lr:{x:previousActX2, y:actY2},
                     maxr: {x:actX1, y:actY2}});
      }
  }

  // paint start and end times of activity
  if (this.showActivityTimes && this.rowPadding > 12) {
      paintedTimeX2 = this.paintActivityTimes({interval:act,
                     ll:{x:actX1, y:actY2},
                     ul:{x:actX1, y:actY1},
                     ur:{x:actX2, y:actY1},
                      lr:{x:actX2, y:actY2},
                     lastPaintedTimeX2: lastPaintedTimeX2,
                     lastPaintedTime: lastPaintedTime});
      if (paintedTimeX2 !== lastPaintedTimeX2) {
        lastPaintedTimeX2 = paintedTimeX2;
        lastPaintedTime = act.start;
      }
  }

  // check to see if it was selected
  if(this.band.onIsSelectedActivity && this.band.onIsSelectedActivity(act)) {
    // draw an X style
    ctx.lineWidth = 1;
    ctx.strokeStyle = "#000000";
    ctx.beginPath();
    ctx.moveTo(actX1, actY1);
    ctx.lineTo(actX2, actY2);
    ctx.moveTo(actX1, actY2);
    ctx.lineTo(actX2, actY1);
    ctx.closePath();
    ctx.stroke();
  }

  // draw the border of the track (RAVEN -- if activity length not too short)
  if(this.style === ActivityPainter.BAR_STYLE && this.borderWidth > 0) {
    ctx.lineWidth = this.borderWidth;
    ctx.strokeStyle = Util.rgbaToString([0, 0, 0], 0.5);
    ctx.strokeRect(actX1, actY1, actWidth, actHeight);
  }

  var actCoord = [actX1, actX2, actY1, actY2];
  var drawCoord = [actX1, actX2, actY1, actY2];
  if(this.showLabel && !this.trimLabel && act.label !== null) {
    var labelWidth = ctx.measureText(act.label).width;
    drawCoord[1] = Math.max(drawCoord[1], actX1+labelWidth+this.labelPadding);
  }

  return {actCoord:actCoord, drawCoord:drawCoord, lastPaintedTimeX2: lastPaintedTimeX2, lastPaintedTime: lastPaintedTime};
};

ActivityPainter.prototype.paintActivityAsLine = function(rowY, act) {
  // rowY is the bottom left corner
  var ctx = this.band.canvas.getContext('2d');
  rowY = rowY - this.rowPadding;

  // paint the activity
  var halfHashHeight = this.activityHeight / 2;
  var viewTimeAxis = this.band.viewTimeAxis;
  var actX1 = viewTimeAxis.getXFromTime(act.start);
  var actX2 = viewTimeAxis.getXFromTime(act.end);
  var actY  = rowY - halfHashHeight;
  var actWidth = actX2 - actX1;

  var hashY1 = actY - halfHashHeight;
  var hashY2 = actY + halfHashHeight;

  var actColor = this.getColor(act);
  if(act.isConflicted) { actColor = [255,0,0]; }
  //??var color = Util.rgbaToString(actColor, act.opacity);
  var color = Util.rgbaToString(actColor, 1.0);
  ctx.strokeStyle = color;
  ctx.lineWidth = 1;
  ctx.beginPath();

  // draw the activity left hash
  ctx.moveTo(actX1, hashY1);
  ctx.lineTo(actX1, hashY2);

  // draw the activity horizontal line
  ctx.moveTo(actX1, actY);
  ctx.lineTo(actX2, actY);

  // draw the activity right hash
  ctx.moveTo(actX2, hashY1);
  ctx.lineTo(actX2, hashY2);

  if(act.latestStart !== null) {
    var latestStartX1 = viewTimeAxis.getXFromTime(act.latestStart);
    // draw the track left hash
    ctx.moveTo(latestStartX1, hashY1);
    ctx.lineTo(latestStartX1, hashY2);
  }

  if(act.earliestEnd !== null) {
    var earliestEndX2 = viewTimeAxis.getXFromTime(act.earliestEnd);
    // draw the track right hash
    ctx.moveTo(earliestEndX2, hashY1);
    ctx.lineTo(earliestEndX2, hashY2);
  }
  ctx.stroke();
  ctx.closePath();

  // paint the icon if specified
  if(this.showIcon && act.icon !== null && (act.icon in this.iconPainters)) {
    var iconPainter = this.iconPainters[act.icon];
    var iconColor = iconPainter.color ? Util.rgbaToString(iconPainter.color, act.opacity) : color;
    var iconWidth = iconPainter.width;
    iconPainter.paint({band:this.band,
                       interval:act,
                       color:iconColor,
                       width:iconWidth,
                       ll:{x:actX1, y:hashY2},
                       ul:{x:actX1, y:hashY1},
                       ur:{x:actX2, y:hashY1},
                       lr:{x:actX2, y:hashY2}});
  }

  // paint the label
  if(this.showLabel && act.label !== null) {
    this.paintLabel({interval:act,
                     ll:{x:actX1, y:actY},
                     ul:{x:actX1, y:actY},
                     ur:{x:actX2, y:actY},
                     lr:{x:actX2, y:actY}});
  }

  var actCoord = [actX1, actX2, hashY1, hashY2];
  var drawCoord = [actX1, actX2, hashY1, hashY2];
  if(this.showLabel && !this.trimLabel && act.label !== null) {
    var labelWidth = ctx.measureText(act.label).width;
    drawCoord[1] = Math.max(drawCoord[1], actX1+labelWidth+this.labelPadding);
  }

  return {actCoord:actCoord, drawCoord:drawCoord};
};

ActivityPainter.prototype.paintActivity = function(rowY, act, previousAct, previousActX1, previousActX2, lastPaintedTimeX2, lastPaintedTime) {
  if(this.style === ActivityPainter.LINE_STYLE) {
    return this.paintActivityAsLine(rowY, act, previousAct, previousActX1, previousActX2, lastPaintedTimeX2, lastPaintedTime);
  }
  else { // ActivityPainter.BAR_STYLE or ICON_STYLE
    return this.paintActivityAsBar(rowY, act, previousAct, previousActX1, previousActX2, lastPaintedTimeX2, lastPaintedTime);
  }
};

ActivityPainter.prototype.paintActivitiesWaterfallLayout = function(acts) {
  // 0,0 corresponds to the upper left hand corner.
  //this.showLabel = true;
  this.trimLabel = false;
  this.showActivityTimes = true;
  // set min row height to 5
  this.rowHeight = Math.max(5,Math.floor(this.band.height/acts.length));
  this.rowPadding = Math.ceil(this.rowHeight/3);
  this.activityHeight = Math.min (20, this.rowHeight - this.rowPadding);
  this.rowPadding = this.rowHeight - this.activityHeight;
  var rowY = this.rowHeight;
  var actCoords = [];
  for(var i=0, ilength=acts.length; i<ilength; ++i) {
    var act = acts[i];
    var coord = this.paintActivity(rowY, act);
    if (this.activityHeight > 5 && act.label) {
        this.paintLabel({interval:act,
                     ll:{x:coord.drawCoord[0], y:coord.drawCoord[3]},
                     ul:{x:coord.drawCoord[0], y:coord.drawCoord[2]},
                     ur:{x:coord.drawCoord[1], y:coord.drawCoord[2]},
                     lr:{x:coord.drawCoord[1], y:coord.drawCoord[3]}});
    }
    actCoords.push([act].concat(coord.actCoord));
    if (rowY + this.rowHeight <= this.band.height) {
       rowY += this.rowHeight;
    }
  }
  return actCoords;
};

ActivityPainter.prototype.paintActivitiesCompactLayout = function(acts) {
  var viewTimeAxis = this.band.viewTimeAxis;
  // 0,0 corresponds to the upper left hand corner.  We want to
  // painter from the bottom up so we're initializing the first
  // row to start at the band height.
  var rowY = this.band.height;

  // selected act info
  var selectedActs = [];

  var actCoords = [];
  var openActs = acts.slice(0);
  var showLabels = this.showLabel;
  //this.showLabel = false;
  while(openActs.length > 0) {
    if (!this.autoFit) { // packed
        this.trimLabel = true;
    }
    else
        this.trimLabel = false;
    var prevDrawEnd = null;
    var newOpenActs = [];

    var previousAct = null;
    var previousActX1 = null;
    var previousActX2 = null;
    var lastPaintedTimeX2 = null;
    var lastPaintedTime = null;
    for(var i=0, ilength=openActs.length; i<ilength; ++i) {
      var act = openActs[i];
      if(prevDrawEnd !== null && prevDrawEnd > act.start) {
        newOpenActs.push(act);
      }
      else {
        if(this.band.onIsSelectedActivity && this.band.onIsSelectedActivity(act)) {
          selectedActs.push({act:act, y:rowY});
          prevDrawEnd = act.end;
        }
        else {
          var coord = this.paintActivity(rowY, act, previousAct, previousActX1, previousActX2, lastPaintedTimeX2, lastPaintedTime);
          // save previous act and where it is drawn
          previousAct = act;
          previousActX1 = coord.drawCoord[0];
          previousActX2 = coord.drawCoord[1];
          lastPaintedTimeX2 = coord.lastPaintedTimeX2;
          lastPaintedTime = coord.lastPaintedTime;
          actCoords.push([act].concat(coord.actCoord));
          prevDrawEnd = viewTimeAxis.getTimeFromX(coord.drawCoord[1]);
        }
      }
    }

    // put label on the last activity in this row
    if (!this.autoFit && previousAct && this.showLabel && this.rowHeight > 5) {
        this.trimLabel = false;
        // paint previous activity label if there is room
        this.paintLabel({interval:act,
                     ll:{x:previousActX1, y:coord.drawCoord[3]},
                     ul:{x:previousActX1, y:coord.drawCoord[2]},
                     ur:{x:previousActX2, y:coord.drawCoord[2]},
                     lr:{x:previousActX2, y:coord.drawCoord[3]},
                   maxr:{x:coord.drawCoord[1], y:coord.drawCoord[3]}});

        this.trimLabel = true;
    }


    openActs = newOpenActs;
    rowY -= this.rowHeight;

    // RAVEN -- don't draw off screen; clamp at the top
    // clamp activities in the top row if it is going to be off the screen
    if (rowY < 0) {
        rowY = this.activityHeight -5;
    }
  }

  for(var j=0, jlength=selectedActs.length; j<jlength; ++j) {
    var selectedAct = selectedActs[j];
    var selectedCoord = this.paintActivity(selectedAct.y, selectedAct.act);
    actCoords.push([selectedAct.act].concat(selectedCoord.actCoord));
  }
  return actCoords;
}


ActivityPainter.prototype.paintActivities = function(acts) {
  if(this.layout === ActivityPainter.COMPACT_LAYOUT) {
    return this.paintActivitiesCompactLayout(acts);
  }
  else if(this.layout === ActivityPainter.WATERFALL_LAYOUT) {
    return this.paintActivitiesWaterfallLayout(acts);
  }
  return null;
};

ActivityPainter.prototype.paint = function() {
  if(this.band === null) return;
  if(this.autoFit) { this.rowHeight = this.computeAutoFit(); }

  // applies to all acts
  var ctx = this.band.canvas.getContext('2d');
  ctx.font = this.font;
  ctx.textBaseline = "middle";

  var viewTimeAxis = this.band.viewTimeAxis;
  var viewStart = viewTimeAxis.start;
  var viewEnd   = viewTimeAxis.end;
  var actsList = this.band.getIntervalsInTimeRange(viewStart, viewEnd);

  var actCoords = [];
  for(var i=0, length=actsList.length; i<length; ++i) {
    var acts = actsList[i];
    var coords = this.paintActivities(acts);
    actCoords = actCoords.concat(coords);
  }
  return actCoords;
};

function Band(obj) {
  if(typeof obj === "undefined") { return; }

  // required
  this.timeAxis     = obj.timeAxis;
  this.viewTimeAxis = obj.viewTimeAxis;
  this.id           = obj.id;
  this.decorator    = obj.decorator;
  this.painter      = obj.painter;

  if(this.decorator) { this.decorator.band = this; }
  if(this.painter)   { this.painter.band = this; }

  // optional
  this.label         = ("label" in obj) ? obj.label : obj.id;
  this.labelColor    = ("labelColor" in obj) ? obj.labelColor : [0, 0, 0];
  this.minorLabels   = ("minorLabels" in obj) ? obj.minorLabels : [];
  this.height        = ("height" in obj) ? obj.height : Band.DEFAULT_HEIGHT;
  this.heightPadding = ("heightPadding" in obj) ? obj.heightPadding : 0;
  this.tooltipDelay  = ("tooltipDelay" in obj) ? obj.tooltipDelay : 250;
  this.properties    = ("properties" in obj) ? obj.properties : null;

  // callback functions
  this.onFilterTooltipIntervals = ("onFilterTooltipIntervals" in obj) ? obj.onFilterTooltipIntervals : null;
  this.onShowTooltip = ("onShowTooltip" in obj) ? obj.onShowTooltip : null;
  this.onHideTooltip = ("onHideTooltip" in obj) ? obj.onHideTooltip : null;
  this.onUpdateView  = ("onUpdateView" in obj) ? obj.onUpdateView : null;
  this.onLeftClick   = ("onLeftClick" in obj) ? obj.onLeftClick : null;
  this.onRightClick  = ("onRightClick" in obj) ? obj.onRightClick : null;
  this.onDblLeftClick = ("onDblLeftClick" in obj) ? obj.onDblLeftClick : null;

  // create the div that everything is attached to
  this.div = document.createElement("div");
  this.div.setAttribute('class', 'banddiv');
  this.div.setAttribute('label', this.label);
  this.div.setAttribute('id', this.id);

  // create the canvas
  this.canvas = document.createElement("canvas");
  this.canvas.setAttribute('class', 'bandcanvas');
  this.canvas.height = this.height;
  this.div.appendChild(this.canvas);

  // add the canvas hook, and click handlers
  $(this.canvas).mousedown(this.mousedown.bind(this));
  $(this.canvas).mouseup(this.mouseup.bind(this));
  $(this.canvas).dblclick(this.dblclick.bind(this));
  $(this.canvas).click(this.click.bind(this));

  // add the tooltip hook
  $(this.canvas).mousemove(this.mousemove.bind(this));
  $(this.canvas).mouseout(this.mouseout.bind(this));

  // an array of an array of intervals to highlight
  this.intervalsList = [];
  this.backgroundIntervalsList = [];
  this.foregroundIntervalsList = [];

  // an array of child bands that is displayed right below this band
  this.parentBand = null;
  this.childBands = {};

  // we need to create a new div as the source for enabling
  // panning.  The event to start the drag processing is
  // dispatched in the main div mouse down handler
  this._panHiddenDragSource = document.createElement("div");
  this.div.appendChild(this._panHiddenDragSource);
  $(this._panHiddenDragSource).draggable({
    disabled: true,
    distance: 0,
    axis: "x",
    cursor:"grabbing",
    revert:false,
    start: this.handlePanDragStart.bind(this),
    drag: this.handlePanDrag.bind(this),
    stop: this.handlePanDragStop.bind(this)
  });
  // x values of when pan dragging occurred
  this._panX1 = null;
  this._panX2 = null;

  if("intervals" in obj) {
    this.intervalsList.push(obj.intervals);
  }

  // cache the x/y location of the intervals
  this._intervalCoords = null;
  this._tooltipTimeout = null;

  // these parameters are used to scale the canvas if needed, see revalidate
  var ctx = this.canvas.getContext('2d');
  this.devicePixelRatio = window.devicePixelRatio || 1;
  this.backingStoreRatio = ctx.webkitBackingStorePixelRatio ||
    ctx.mozBackingStorePixelRatio ||
    ctx.msBackingStorePixelRatio ||
    ctx.oBackingStorePixelRatio ||
    ctx.backingStorePixelRatio || 1;
}

Band.DEFAULT_HEIGHT = 35;
Band.INITIAL_WATERFALL_HEIGHT = 200;
MAX_CANVAS_HEIGHT = 32767;

Band.localID = -1;
Band.getNextLocalID = function() {
  var id = Band.localID--;
  return id;
};

Band.prototype.revalidate = function() {
  var height = this.height + this.heightPadding;
  var width = this.div.offsetWidth;

  // upscale the canvas if the 2 ratios don't match
  // setting the width/height clears out scaling
  // see https://www.html5rocks.com/en/tutorials/canvas/hidpi
  if(this.devicePixelRatio !== this.backingStoreRatio) {
    var ratio = this.devicePixelRatio / this.backingStoreRatio;
    this.canvas.width = width * ratio;
    this.canvas.height = height * ratio;
    this.canvas.style.width = width + 'px';
    this.canvas.style.height = height + 'px';

    // scale the context to counter the fact that we've
    // manually scaled our canvas element
    var ctx = this.canvas.getContext('2d');
    ctx.scale(ratio, ratio);
  }
  else {
    // cap height to max canvas limit 32767, bad things happened when height was 35000
    if (height > MAX_CANVAS_HEIGHT)
      height = MAX_CANVAS_HEIGHT;

    this.canvas.width = width;
    this.canvas.height = height;
  }

  for(var i in this.childBands) {
    var childBand = this.childBands[i];
    childBand.revalidate();
  }
};

Band.prototype.setHeight = function(height) {
  this.height = height;
};

Band.prototype.setHeightPadding = function(heightPadding) {
  this.heightPadding = heightPadding;
};

Band.prototype.setCanvas = function(canvas) {
  this.canvas = canvas;
  // this should remove the references to the div and canvas,
  // allowing the gc to cleanup the elements
  // this.div = null; Changed (6/19/2017).
};

Band.prototype.setDecorator = function(decorator) {
  this.decorator = decorator;
  if(decorator !== null) {
    decorator.band = this;
  }
};

Band.prototype.setPainter = function(painter) {
  this.painter = painter;
  if(painter !== null) {
    painter.band = this;
  }
};

Band.prototype.setBackgroundIntervals = function(intervals, index) {
  this.backgroundIntervalsList[index] = intervals;
};

Band.prototype.clearBackgroundIntervals = function(index) {
  this.backgroundIntervalsList[index] = null;
};

Band.prototype.clearAllBackgroundIntervals = function() {
  this.backgroundIntervalsList = [];
};

Band.prototype.findBackgroundIntervals = function(x, y) {
  var matchingIntervals = [];
  for(var i=0, ilength=this.backgroundIntervalsList.length; i<ilength; ++i) {
    var intervals = this.backgroundIntervalsList[i];
    if(intervals === undefined || intervals === null) { continue; }

    for(var j=0, jlength=intervals.length; j<jlength; ++j) {
      var interval = intervals[j];
      var x1 = this.viewTimeAxis.getXFromTime(interval.start);
      var x2 = this.viewTimeAxis.getXFromTime(interval.end);
      if(x >= x1 && x <= x2) {
        matchingIntervals.push(interval);
      }
    }
  }
  return matchingIntervals;
};

Band.prototype.setForegroundIntervals = function(intervals, index) {
  this.foregroundIntervalsList[index] = intervals;
};

Band.prototype.clearForegroundIntervals = function(index) {
  this.foregroundIntervalsList[index] = null;
};

Band.prototype.clearAllForegroundIntervals = function() {
  this.foregroundIntervalsList = [];
};

Band.prototype.findForegroundIntervals = function(x, y) {
  var matchingIntervals = [];
  for(var i=0, ilength=this.foregroundIntervalsList.length; i<ilength; ++i) {
    var intervals = this.foregroundIntervalsList[i];
    if(intervals === undefined || intervals === null) { continue; }

    for(var j=0, length=intervals.length; j<length; ++j) {
      var interval = intervals[j];
      var x1 = this.viewTimeAxis.getXFromTime(interval.start);
      var x2 = this.viewTimeAxis.getXFromTime(interval.end);
      if(x >= x1 && x <= x2) {
        matchingIntervals.push(interval);
      }
    }
  }
  return matchingIntervals;
};

Band.prototype.findIntervals = function(x, y) {
  if (this instanceof CompositeBand) {
      let bands = this.bands;
      let first = true;
      var matchingIntervals = [];
      for (let k=0; k<bands.length; k++) {
          let band = bands[k];
          if(band._intervalCoords === null) { return []; }
    
          for(var i=0, length=band._intervalCoords.length; i<length; ++i) {
            var coord = band._intervalCoords[i];
            var interval = coord[0];
            // RAVEN look within +/- 2 pixels
            var x1 = coord[1]-2;
            var x2 = coord[2]+2;
            var y1 = coord[3]-2;
            var y2 = coord[4]+2;
            if(x >= x1 && x <= x2 && y >= y1 && y <= y2) {
              matchingIntervals.push(interval);
            }
          }
          if (matchingIntervals.length > 0) {
              return matchingIntervals;
          }
      }
  }
  else {
      if(this._intervalCoords === null) { return []; }

      var matchingIntervals = [];
      for(var i=0, length=this._intervalCoords.length; i<length; ++i) {
        var coord = this._intervalCoords[i];
        var interval = coord[0];
        // RAVEN look within +/- 2 pixels
        var x1 = coord[1]-2;
        var x2 = coord[2]+2;
        var y1 = coord[3]-2;
        var y2 = coord[4]+2;
        if(x >= x1 && x <= x2 && y >= y1 && y <= y2) {
          matchingIntervals.push(interval);
        }
      }
      return matchingIntervals;
    }
};

Band.prototype.findIntervalCoords = function(id) {
  for(var i=0, length=this._intervalCoords.length; i<length; ++i) {
    var coord = this._intervalCoords[i];
    var interval = coord[0];
    if(interval.id == id) {
      var x1 = coord[1];
      var x2 = coord[2];
      var y1 = coord[3];
      var y2 = coord[4];
      return [x1, x2, y1, y2];
    }
  }
  return null;
};

Band.prototype.getIntervalsInTimeRange = function(start, end) {
  var intervalsListInTimeRange = [];

  for(var i=0, ilength=this.intervalsList.length; i<ilength; ++i) {
    var intervals = this.intervalsList[i];

    var intervalsInTimeRange = [];
    for(var j=0, jlength=intervals.length; j<jlength; ++j) {
      var interval = intervals[j];
      if(interval.end < start) {
        // ends before start
      }
      else if(interval.start > end) {
        // starts after end.
        // We can break since we assume the intervals are ordered by start
        break;
      }
      else {
        // intervals intersect the time range
        intervalsInTimeRange.push(interval);
      }
    }
    intervalsListInTimeRange.push(intervalsInTimeRange);
  }
  return intervalsListInTimeRange;
};

Band.prototype.setIntervals = function(intervals, index) {
  if(index === undefined || index === null) { index = 0; }

  this.intervalsList[index] = intervals;
  this._intervalCoords = null;
};

Band.prototype.addInterval = function(interval, index) {
  if(index === undefined || index === null) { index = 0; }
  if(this.intervalsList[index] === undefined) {
    this.intervalsList[index] = [];
  }

  this.intervalsList[index].push(interval);

  // TODO PERF - we don't need always need to sort on each add
  // Maybe have the caller call sort when done?
  this.intervalsList[index].sort(DrawableInterval.earlyStartEarlyEnd);
};

Band.prototype.addIntervals = function(intervals, index) {
  if(index === undefined || index === null) { index = 0; }
  if(this.intervalsList[index] === undefined) {
    this.intervalsList[index] = [];
  }
  this.intervalsList[index] = this.intervalsList[index].concat(intervals);

  // TODO PERF - we don't need always need to sort on each add
  // Maybe have the caller call sort when done?
  this.intervalsList[index].sort(DrawableInterval.earlyStartEarlyEnd);
};

// remove an interval from the band based on the id
// returns the interval removed if found, otherwise null
Band.prototype.removeInterval = function(id) {
  // remove the first occurrence of interval with id
  for(var i=0, ilength=this.intervalsList.length; i<ilength; ++i) {
    var intervals = this.intervalsList[i];
    for(var j=0, jlength=intervals.length; j<jlength; ++j) {
      var interval = intervals[j];
      if(id === interval.id) {
        intervals.splice(j, 1);
        return interval;
      }
    }
  }
  return null;
};

// remove an set of intervals based on the id
// returns an array of the intervals removed
Band.prototype.removeIntervals = function(ids) {
  // remove the first occurrence of interval with id
  var done = false;
  var intervalsRemoved = [];
  for(var i=0, ilength=this.intervalsList.length; i<ilength && !done; ++i) {
    var intervals = this.intervalsList[i];
    for(var j=intervals.length-1; j>=0 && !done; --j) {
      var interval = intervals[j];
      var k = $.inArray(interval.id, ids);
      if(k !== -1) {
        // remove it from the intervalsList, ids, and push to the ret list
        intervals.splice(j, 1);
        ids.splice(k, 1);
        intervalsRemoved.push(interval);
        if(ids.length === 0) {
          done = true;
        }
      }
    }
  }
  return intervalsRemoved;
};

// return an interval based on the id
Band.prototype.getInterval = function(id) {
  for(var i=0, ilength=this.intervalsList.length; i<ilength; ++i) {
    var intervals = this.intervalsList[i];
    for(var j=0, jlength=intervals.length; j<jlength; ++j) {
      var interval = intervals[j];
      if(id === interval.id) {
        return interval;
      }
    }
  }
  return null;
};

Band.prototype.repaint = function() {
  if(this.painter === null) { return; }

  // clear out the previous painting
  // don't use the canvas height/width since its may be scaled
  // see revalidate for when scaling occurs
  var height = this.height + this.heightPadding;
  var width = this.div.offsetWidth;
  var ctx = this.canvas.getContext('2d');
  ctx.clearRect(0, 0, width, height);

  if(this.decorator !== null) {
    this.decorator.paint();
  }

  // cache the x/y location of the painted intervals
  this._intervalCoords = this.painter.paint();

  for(var i in this.childBands) {
    var childBand = this.childBands[i];
    if($(childBand.div).css("display") !== "none") {
      childBand.repaint();
    }
  }

  if(this.decorator !== null) {
    this.decorator.paintForegroundIntervals();
    this.decorator.paintGuideTimes();
    this.decorator.paintNow();
  }
};

Band.prototype._mousemove = function(e) {
  if(this.onShowTooltip === null) { return true; }

  var x = e.pageX - $(e.target).offset().left;
  var y = e.pageY - $(e.target).offset().top;
  var mouseTime = this.viewTimeAxis.getTimeFromX(x);
  var tooltipText = "";

  var i, length;

  // check to see if we're over the label
  if(x >= 0 && x < this.viewTimeAxis.x1) {
    if(this.label !== "") {
      this.onShowTooltip(e, this.label);
    }
    return true;
  }

  var separator = "<hr class='tooltiphr'/>";
  // check to see if we moused over a foreground interval
  var foregroundIntervals = this.findForegroundIntervals(x, y);
  if(foregroundIntervals.length !== 0) {
    // walking in reverse order so that the top interval is displayed first
    for(length=foregroundIntervals.length-1, i=length; i>=0; --i) {
      if(i!==length) { tooltipText += separator; }
      var fgInterval = foregroundIntervals[i];
      if(fgInterval.onGetTooltipText !== null) {
        tooltipText += fgInterval.onGetTooltipText(e, {band:this, interval:fgInterval, time:mouseTime});
      }
    }
    this.onShowTooltip(e, tooltipText);
    return true;
  }

  // RAVEN -- only get one interval from each band
  if (this instanceof CompositeBand) {
      let bands = this.bands;
      let first = true;
      bands.forEach((band) => {
          // check to see if we're over a interval
          var intervals = band.findIntervals(x, y);
          // callback to filter out intervals that match the x,y position
          if(band.onFilterTooltipIntervals !== null) {
            intervals = band.onFilterTooltipIntervals({band:band, intervals:intervals, time:mouseTime});
            if (intervals.length !== 0) {
              let interval = intervals[0];
              if(first)
                 first = false;
              else {
                  tooltipText += separator;
              }
              if (interval.properties && interval.properties.message)
                  tooltipText = '<p>'+Util.toDOYDate(interval.start)+'</p>'+interval.properties.message;
              else if(interval.onGetTooltipText !== null) {
                  tooltipText += interval.onGetTooltipText(e, {band:band, interval:interval, time:mouseTime});
              }
            }
          }
      });
  }
  else {
      // check to see if we're over a interval
      var intervals = this.findIntervals(x, y);
      // callback to filter out intervals that match the x,y position
      if(this.onFilterTooltipIntervals !== null) {
        intervals = this.onFilterTooltipIntervals({band:this, intervals:intervals, time:mouseTime});
      }
      if (intervals.length !== 0) {
        var interval = intervals[0];
        if (interval.properties && interval.properties.message)
          tooltipText = '<p>'+Util.toDOYDate(interval.start)+'</p>'+interval.properties.message;
        else if(interval.onGetTooltipText !== null) {
          tooltipText += interval.onGetTooltipText(e, {band:this, interval:interval, time:mouseTime});
        }
      }
  }

  if (tooltipText !== '') {
    this.onShowTooltip(e, tooltipText);
    return true;
  }

  // check to see if we moused over a background interval
  var backgroundIntervals = this.findBackgroundIntervals(x, y);
  if(backgroundIntervals.length !== 0) {
    // walking in reverse order so that the top interval is displayed first
    for(length=backgroundIntervals.length-1, i=length; i>=0; --i) {
      if(i!==length) { tooltipText += separator; }
      var bgInterval = backgroundIntervals[i];
      if(bgInterval.onGetTooltipText !== null) {
        tooltipText += bgInterval.onGetTooltipText(e, {band:this, interval:bgInterval, time:mouseTime});
      }
    }
    this.onShowTooltip(e, tooltipText);
    return true;
  }

  // just hide it if we aren't over something
  if(this.onHideTooltip) { this.onHideTooltip(); }
  return true;
};

Band.prototype.mousemove = function(e) {
  if(this._tooltipTimeout !== null) {
    clearTimeout(this._tooltipTimeout);
    if(this.onHideTooltip) { this.onHideTooltip(); }
  }

  // RAVEN -- display annotation
  // Make sure not to show other tooltop is annotationTooltipShown is true.
  // This is a hacky fix for Chrome issue: mousemove event is getting fired right after dblclick event.
  if (!this.annotationTooltipShown) {
    if(this._tooltipTimeout !== null) {
      if(this.onHideTooltip) { this.onHideTooltip(); }
    }

    if(this.tooltipDelay > 0) {
      this._tooltipTimeout = setTimeout(this._mousemove.bind(this, e), this.tooltipDelay);
    }
    else {
      this._mousemove(e);
    }
  }
  else {
      this.annotationTooltipShown = false;
  }

  return true;
};

Band.prototype.mouseout = function(e) {
  if(this._tooltipTimeout !== null) {
    clearTimeout(this._tooltipTimeout);
    this._tooltipTimeout = null;
  }
  if(this.onHideTooltip) { this.onHideTooltip(); }
  return true;
};

Band.prototype.mousedown = function(e) {
  if(this.onLeftClick === null &&
     this.onRightClick === null &&
     this.onUpdateView === null) { return true; }

  var x = e.pageX - $(e.target).offset().left;
  var y = e.pageY - $(e.target).offset().top;
  var mouseTime = this.viewTimeAxis.getTimeFromX(x);

  var interval = null;
  var intervals = this.findIntervals(x, y);
  if(intervals.length !== 0) {
    interval = intervals[intervals.length-1];
  }

  if(e.which === 1) {
    if(this.onLeftClick !== null) { this.onLeftClick(e, {band:this, interval:interval, time:mouseTime}); }

    if(x >= this.viewTimeAxis.x1 && this.onUpdateView !== null) {
      // enable dragging if on timeline area
      $(this._panHiddenDragSource).draggable("enable");
      $(this._panHiddenDragSource).trigger(e);
    }
  }
  else {
    if(this.onRightClick === null) { return true; }

    // right click
    if(interval === null) {
      // search the background intervals
      var backgroundIntervals = this.findBackgroundIntervals(x, y);
      if(backgroundIntervals.length !== 0) {
        interval = backgroundIntervals[0];
      }
    }
    this.onRightClick(e, {band:this, interval:interval, time:mouseTime});
  }
  return true;
};

Band.prototype.mouseup = function(e) {
  if(e.which === 1) {
    $(this._panHiddenDragSource).draggable("disable");
  }
};

Band.prototype.click = function(e) {
};

Band.prototype.dblclick = function(e) {
  // RAVEN -- dbl click to show annotation text
  var x = e.pageX - $(e.target).offset().left;
  var y = e.pageY - $(e.target).offset().top;

  // search the background intervals
  var backgroundIntervals = this.findBackgroundIntervals(x, y);
  if (backgroundIntervals.length !== 0) {
      interval = backgroundIntervals[0];
      this.onDblLeftClick(e, {band:this, backgroundInterval:interval});

      let lines = interval.properties.text.split('\n');

      let htmlText = `
        <span class="header">
          ${interval.label}
        </span>
      `;

      lines.forEach((line) => {
          htmlText += `
            <p>
              ${line}
            </p>
          `;
      });

      this.annotationTooltipShown = true;
      this.onShowTooltip(e, htmlText);
  }
  else {
      this.annotationTooltipShown = false;
      if (this.onDblLeftClick !== null) { this.onDblLeftClick(e, {band:this}); }
  }
};

Band.prototype.addChildBand = function(childBand) {
  childBand.parentBand = this;
  this.childBands[childBand.id] = childBand;
  this.div.appendChild(childBand.div);
  childBand.revalidate();
};

Band.prototype.removeChildBand = function(childBand) {
  try {
    this.div.removeChild(childBand.div);
    delete this.childBands[childBand.id];
    childBand.parentBand = null;
  }
  catch(err) {
    // silently ignore the error
  }
};

Band.prototype.clearAllChildBands = function() {
  for(var i in this.childBands) {
    var childBand = this.childBands[i];
    try {
      childBand.parentBand = null;
      this.div.removeChild(childBand.div);
    }
    catch(err) {
      // silently ignore the error
    }
  }
  this.childBands = {};
};

Band.prototype.showChildBands = function() {
  for(var i in this.childBands) {
    var childBand = this.childBands[i];
    $(childBand.div).show({duration:0});
  }
  this.revalidate();
  this.repaint();
};

Band.prototype.hideChildBands = function() {
  for(var i in this.childBands) {
    var childBand = this.childBands[i];
    $(childBand.div).hide({duration:0});
  }
  this.revalidate();
  this.repaint();
};

Band.prototype.toggleChildBands = function() {
  for(var i in this.childBands) {
    var childBand = this.childBands[i];
    $(childBand.div).slideToggle({duration:0});
  }
  this.revalidate();
  this.repaint();
};

Band.prototype.getLevel = function() {
  var level = 0;
  var parent = this.parentBand;
  while(parent !== null) {
    parent = parent.parentBand;
    level += 1;
  }
  return level;
};

Band.prototype.isExpanded = function() {
  // returns false if any of the children band is hidden
  for(var i in this.childBands) {
    var childBand = this.childBands[i];
    if($(childBand.div).css("display") === "none") {
      return false;
    }
  }
  return true;
};

Band.prototype.handlePanDragStart = function(e, ui) {
};

Band.prototype.handlePanDrag = function(e, ui) {
  if(this.onUpdateView === null) return;

  if(this._panX1) {
    this._panX2 = e.pageX;
    // actual times are irrelevant and incorrect since we're using pageX.
    // We just want the relative difference.
    var start = this.viewTimeAxis.getTimeFromX(this._panX1);
    var end = this.viewTimeAxis.getTimeFromX(this._panX2);
    // its start - end so that we are panning in the opposite
    // direction of the drag
    var duration = start - end;

    var newStart = this.viewTimeAxis.start + duration;
    var newEnd = this.viewTimeAxis.end + duration;
    this.onUpdateView(newStart, newEnd);
  }
  this._panX1 = e.pageX;
};

Band.prototype.handlePanDragStop = function(e, ui) {
  this._panX1 = null;
  this._panX2 = null;
};

CompositeBand.prototype = new Band();
CompositeBand.prototype.constructor = CompositeBand;

// can be used to overlay multiple datasources into the same canvas
// a bit of a hack but we would have to restructure the band to
// contain multiple datasources and associate labels/painters with each
function CompositeBand(obj) {
  if(typeof obj === "undefined") return;

  if(!("decorator" in obj)) { obj.decorator = new Decorator(obj); }
  Band.prototype.constructor.call(this, obj);

  this.bands = [];
}

CompositeBand.prototype.setHeight = function(height) {
  for(var i=0, ilength=this.bands.length; i<ilength; ++i) {
    var band = this.bands[i];
    band.setHeight(height);
  }
  Band.prototype.setHeight.call(this, height);
};

CompositeBand.prototype.setHeightPadding = function(heightPadding) {
  for(var i=0, ilength=this.bands.length; i<ilength; ++i) {
    var band = this.bands[i];
    band.setHeightPadding(heightPadding);
  }
  Band.prototype.setHeightPadding.call(this, heightPadding);
};

CompositeBand.prototype.addBand = function(band) {
  // Changed (6/19/2017) - addBand may get called if there is no band, so make sure there is one.
  if (band) {
    band.setCanvas(this.canvas);
    band.height = this.height;
    band.heightPadding = this.heightPadding;
    this.bands.push(band);
  }
};

CompositeBand.prototype.removeBand = function(id) {
  for(var i=0, ilength=this.bands.length; i<ilength; ++i) {
    var band = this.bands[i];
    if(band.id === id) {
      this.bands.splice(i, 1);
      break;
    }
  }
};

CompositeBand.prototype.repaint = function() {
  // clear out the previous painting
  // don't use the canvas height/width since its may be scaled
  // see revalidate for when scaling occurs
  var height = this.height + this.heightPadding;
  var width = this.div.offsetWidth;
  var ctx = this.canvas.getContext('2d');
  ctx.clearRect(0, 0, width, height);

  this.decorator.paintTimeTicks();

  var labelY = 0;
  var valueTicksX = this.viewTimeAxis.x1;
  this._intervalCoords = [];
  for(var i=0, length=this.bands.length; i<length; ++i) {
    var band = this.bands[i];
    if(band.decorator !== null) {
      labelY = band.decorator.paintLabel(labelY);
      if(band.decorator.paintValueTicks) {
        valueTicksX = band.decorator.paintValueTicks(valueTicksX);
      }
    }
    var coords = band.painter.paint();
    band._intervalCoords = coords;
  }

  for(var id in this.childBands) {
    var childBand = this.childBands[id];
    if($(childBand.div).css("display") !== "none") {
      childBand.repaint();
    }
  }

  if(this.decorator !== null) {
    this.decorator.paintForegroundIntervals();
    this.decorator.paintGuideTimes();
    this.decorator.paintNow();
  }
};

function Decorator(obj) {
  if(typeof obj === "undefined") { return; }

  this.font = ("font" in obj) ? obj.font : "normal 9px Verdana";
  this.labelFontSize = ("labelFontSize" in obj) ?obj.labelFontSize : 10;
  this.guideColor = ("guideColor" in obj) ? obj.guideColor : [34,139,34];
  this.showIcon = ("showIcon" in obj) ? obj.showIcon : false;

  // set when assigned to a band
  this.band = null;
  this.hideBackgroundIntervals = false;
}

Decorator.prototype._paintInterval = function(interval) {
  // paint the highlight
  var ctx = this.band.canvas.getContext('2d');

  var viewTimeAxis = this.band.viewTimeAxis;
  var bgX1 = viewTimeAxis.getXFromTime(interval.start);
  var bgX2 = viewTimeAxis.getXFromTime(interval.end);
  var bgWidth = Math.max(0.5, bgX2 - bgX1); // ensure we don't have a 0pixel interval

  ctx.fillStyle = Util.rgbaToString(interval.color, interval.opacity);
  ctx.fillRect(bgX1, 0, bgWidth, this.band.height);

  return { x1: bgX1, x2: bgX2 };
};

Decorator.prototype.paintBackgroundIntervals = function() {
  if (!this.hideBackgroundIntervals) {
      // paint the highlight
      var backgroundIntervalsList = this.band.backgroundIntervalsList;
      for(var i=0, ilength=backgroundIntervalsList.length; i<ilength; ++i) {
        var intervals = backgroundIntervalsList[i];
        if(intervals === undefined || intervals === null) { continue; }

        for(var j=0, jlength=intervals.length; j<jlength; ++j) {
          var interval = intervals[j];
          let rect = this._paintInterval(interval);

          // Paint background interval label.
          this.band.painter.paintLabel({
            interval,
            ll: { x: rect.x1, y: 18 },
            ul: { x: 0, y: 10 },
            ur: { x: 0, y: 10 },
            lr: { x: rect.x2, y: 30 },
            annotationLabel: true
          });
        }
      }
  }
};

Decorator.prototype.paintForegroundIntervals = function() {
  // paint the highlight
  var foregroundIntervalsList = this.band.foregroundIntervalsList;
  for(var i=0, ilength=foregroundIntervalsList.length; i<ilength; ++i) {
    var intervals = foregroundIntervalsList[i];
    if(intervals === undefined || intervals === null) { continue; }

    for(var j=0, jlength=intervals.length; j<jlength; ++j) {
      var interval = intervals[j];
      this._paintInterval(interval);
    }
  }
};

Decorator.prototype.paintGuideTimes = function() {
  var guideTimes = this.band.timeAxis.guideTimes;
  if(guideTimes.length === 0) { return; }

  var viewTimeAxis = this.band.viewTimeAxis;
  var viewStart = viewTimeAxis.start;
  var viewEnd = viewTimeAxis.end;

  var bandHeight = this.band.height + this.band.heightPadding;
  var ctx = this.band.canvas.getContext('2d');
  ctx.lineWidth = 2;
  ctx.strokeStyle = Util.rgbaToString(this.guideColor, 0.8);

  ctx.beginPath();
  for(var i=0, length=guideTimes.length; i<length; ++i) {
    var guideTime = guideTimes[i];
    if(guideTime >= viewStart && guideTime <= viewEnd) {
      var guideX = viewTimeAxis.getXFromTime(guideTime);
      ctx.moveTo(guideX, 0);
      ctx.lineTo(guideX, bandHeight);
    }
  }
  ctx.stroke();
  ctx.closePath();
};

Decorator.prototype.paintTimeTicks = function() {
  var viewTimeAxis = this.band.viewTimeAxis;
  var now = viewTimeAxis.now;
  var viewStart = viewTimeAxis.start;
  var viewEnd = viewTimeAxis.end;
  var tickTimes = viewTimeAxis.tickTimes;
  var labelWidth = viewTimeAxis.x1;
  var bandHeight = this.band.height + this.band.heightPadding;
  var bandWidth = this.band.div.offsetWidth;

  // we paint the dashed markers for the tick boundary
  var ctx = this.band.canvas.getContext('2d');
  ctx.lineWidth = 0.5;

  // draw a border around the canvas
  ctx.strokeStyle = Util.rgbaToString([0,0,0], 0.5);
  ctx.strokeRect(0, 0, bandWidth, bandHeight);

  ctx.beginPath();
  ctx.moveTo(labelWidth, 0);
  ctx.lineTo(labelWidth, bandHeight);
  for(var i=0, ilength=tickTimes.length; i<ilength; ++i) {
    var time = tickTimes[i];
    if(time < viewStart || time > viewEnd) { continue; }
    var timeX = viewTimeAxis.getXFromTime(time);

    var delta = (time % TimeUnit.DAY === 0) ? bandHeight : 4;
    for(var j=0; j<bandHeight; j+=delta*2) {
      ctx.moveTo(timeX, j);
      ctx.lineTo(timeX, j+delta);
    }
  }
  ctx.stroke();
  ctx.closePath();
};

Decorator.prototype.paintNow = function() {
  var viewTimeAxis = this.band.viewTimeAxis;
  var now = viewTimeAxis.now;
  var viewStart = viewTimeAxis.start;
  var viewEnd = viewTimeAxis.end;
  if(now !== null && now >= viewStart && now < viewEnd) {
    var ctx = this.band.canvas.getContext('2d');
    var bandHeight = this.band.height + this.band.heightPadding;

    var nowX = viewTimeAxis.getXFromTime(now);
    ctx.lineWidth = this.timeCursorWidth || 4;
    ctx.strokeStyle = Util.rgbaToString(this.timeCursorColor || [255,0,0], 0.8);
    ctx.beginPath();
    ctx.moveTo(nowX, 0);
    ctx.lineTo(nowX, bandHeight);
    ctx.stroke();
    ctx.closePath();
  }
};

Decorator.prototype.paintLabel = function(yStart) {
  var ctx = this.band.canvas.getContext('2d');
  let originalFont = ctx.font;
  ctx.font = this.labelFontSize+"px Georgia";
  ctx.textAlign = "left";
  var labelWidth = this.band.viewTimeAxis.x1;

  var x = 2;
  var y = this.labelFontSize + yStart;
  var yDelta = this.labelFontSize;
  ctx.fillStyle = Util.rgbaToString(this.band.labelColor, 1);
  if(labelWidth > 2) {
    var label = Util.trimToWidth(this.band.label, labelWidth - x, ctx);
    ctx.fillText(label, x, y);

    x += 5;
    y += yDelta;
    for(var k=0, klength=this.band.minorLabels.length; k<klength; ++k) {
      var minorLabel = Util.trimToWidth(this.band.minorLabels[k], labelWidth - x, ctx);
      ctx.fillText(minorLabel, x, y);
      y += yDelta;
    }
  }
  else {
    // if there is no label area, print the text near the top
    ctx.fillText(this.band.label, x, y);
  }

  // restore orignal font
  ctx.font = originalFont;
  return y;
};


Decorator.prototype.paint = function() {
  if(this.band === null) { return; }

  var ctx = this.band.canvas.getContext('2d');
  ctx.font = this.font;
  ctx.textAlign = "left";
  ctx.textBaseline = "middle";

  this.paintTimeTicks();
  this.paintLabel(0);
  this.paintBackgroundIntervals();
};

// a drawable interval object
function DrawableInterval(obj) {
  if(typeof obj === "undefined") return;

  // PUBLIC variables
  // id - the id for this interval
  this.id = obj.id;
  // obj - the source object for this interval
  this.source = ("source" in obj) ? obj.source : null;
  // string - the label to display for the interval
  this.label = ("label" in obj) ? obj.label : null;
  // int - the earliest start time for the interval
  this.start = obj.start;
  // int - the latest start time for the interval
  this.latestStart = ("latestStart" in obj) ? obj.latestStart : null;
  // int - the earliest end time for the interval
  this.earliestEnd = ("earliestEnd" in obj) ? obj.earliestEnd : null;
  // int - the latest end time for the interval
  this.end = obj.end;
  // array (optional) - rgb array structure to paint the interval
  this.color = ("color" in obj) ? obj.color : null;
  // float (optional)
  this.opacity = ("opacity" in obj) ? obj.opacity : 1;
  // string (optional) - the color of the interval text
  this.labelColor = ("labelColor" in obj) ? obj.labelColor : null;
  // float (optional) - the opacity of the interval text
  this.labelOpacity = ("labelOpacity" in obj) ? obj.labelOpacity : 1;
  // string (optional) - a string to be interpreted by the painter
  this.icon = ("icon" in obj) ? obj.icon : null;
  // any type - handled by painter
  this.startValue = ("startValue") in obj ? obj.startValue : null;
  // any type - handled by painter
  this.endValue = ("endValue") in obj ? obj.endValue : null;
  // boolean (optional) - indicates if the interval is in conflict
  this.isConflicted = ("isConflicted" in obj) ? obj.isConflicted : false;
  // optional array or map of name/value pairs
  this.properties = ("properties" in obj) ? obj.properties : null;
  // function (optional) - callback to get the tooltip text
  this.onGetTooltipText = ("onGetTooltipText" in obj) ? obj.onGetTooltipText : DrawableInterval.getTooltipText;
}

DrawableInterval.getTooltipText = function(e, obj) {
  var band      = obj.band;
  var interval  = obj.interval;
  var mouseTime = obj.time;
  var timeZone  = band.timeAxis.timeZone;
  var timeRangeStr = Util.toTimeRangeString(interval.start, interval.end, {timeZone:timeZone});

  var tooltipText = "";
  tooltipText += "<table class='tooltiptable'>";
  tooltipText += "<tr><td class='tooltiptablecell'><b>Interval:</b></td><td class='tooltiptablecell'>" + timeRangeStr + "</td></tr>";
  if(interval.properties !== null) {
    if($.isArray(interval.properties)) {
      for(var i=0, length=interval.properties.length; i<length; ++i) {
        var property = interval.properties[i];
        tooltipText += "<tr><td class='tooltiptablecell'><b>"+property.name+":</b></td><td class='tooltiptablecell'>"+property.value+"</td></tr>";
      }
    }
    else {
      for(var name in interval.properties) {
        var value = interval.properties[name];
        tooltipText += "<tr><td class='tooltiptablecell'><b>"+name+":</b></td><td class='tooltiptablecell'>"+value+"</td></tr>";
      }
    }
  }
  tooltipText += "</table>";
  return tooltipText;
};


// rules for writing comparators
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/sort
//
// If compareFunction(a, b) is less than 0, sort a to a lower index than b,
//      i.e. a comes first.
// If compareFunction(a, b) returns 0, leave a and b unchanged with respect
//      to each other, but sorted with respect to all different elements.
//      Note: the ECMAscript standard does not guarantee this behaviour, and
//      thus not all browsers (e.g. Mozilla versions dating back to at least
//      2003) respect this.
// If compareFunction(a, b) is greater than 0, sort b to a lower index than a.
//
// compareFunction(a, b) must always return the same value when given a specific
//      pair of elements a and b as its two arguments. If inconsistent results
//      are returned then the sort order is undefined

// RAVEN -- comparator
var Comparator = {
  drawableInterval: function(a, b) {
    return a.end - b.start;
  },

  drawableIntervalStart: function(a, b) {
    if(a.start == b.start) {
      return a.end - b.end;
    }
    return a.start - b.start;
  },

  drawableIntervalEnd: function(a, b) {
    if(a.end == b.end) {
      return a.start - b.start;
    }
    return a.end - b.end;
  }
};

DrawableInterval.earlyStartEarlyEnd = function(a, b) {
  if(a.start == b.start) {
    return a.end - b.end;
  }
  return a.start - b.start;
};

function EditableGrid(obj) {
  SimpleGrid.prototype.constructor.call(this, obj);
  SimpleGrid.prototype.initTableRows.call(this);

  this.editableCell = null;
}

EditableGrid.prototype = Object.create(SimpleGrid.prototype);
EditableGrid.prototype.constructor = EditableGrid;

EditableGrid.prototype.mousedown = function(e) {
  var target = e.target;
  if(target.nodeName !== "TD") { return true; }
  if($(target).hasClass("filter")) { return true; }
  var parent = target.parentNode;
  if(parent.nodeName !== "TR") { return true; }
  if($(parent).hasClass("filters")) { return true; }
  var row = parent;

  // use setTimeout so we don't perform this operation in the event thread
  this.clearEditableRow();

  this.selectRow(row, false);
  return true;
};

EditableGrid.prototype.clearEditableRow = function() {
  if(this.editableCell === null) { return; }

  var event = new Event("onchange");
  this.editableCell.dispatchEvent(event);

  var rowSpec = this.rowSpecs[this.editableCell.parentNode.sectionRowIndex];
  this.initRow(this.editableCell.parentNode, rowSpec.data);
  this.editableCell = null;
  this.resizeStickyHeaders();
};

EditableGrid.prototype.dblclick = function(e) {
  var target = e.target;
  if(target.nodeName !== "TD") { return true; }
  if($(target).hasClass("filter")) { return true; }
  var parent = target.parentNode;
  if(parent.nodeName !== "TR") { return true; }
  if($(parent).hasClass("filters")) { return true; }
  var cell = target;
  var row = parent;

  if(cell === this.editableCell) { return true; }
  var rowSpec = this.rowSpecs[row.sectionRowIndex];

  this.clearEditableRow();
  this.editableCell = this.initEditableCell(cell);
  return true;
};

EditableGrid.prototype.initEditableCell = function(cell) {
  var row = cell.parentNode;
  var rowSpec = this.rowSpecs[row.sectionRowIndex];

  if (!rowSpec.editableFields) { return null; }
  if (!rowSpec.editableFields[cell.cellIndex]) { return null; }
  if (!rowSpec.editableFields[cell.cellIndex].factory) { return null; }

  var factory = rowSpec.editableFields[cell.cellIndex].factory;
  var editableField = factory(rowSpec.data[cell.cellIndex]);
  editableField.onchange = this.editObjectOnChange.bind(this);
  $(editableField).addClass("editable-cell");

  var cellIndex = cell.cellIndex;
  $(row).empty();
  var result = null;
  for(var i=0; i<rowSpec.data.length; ++i) {
    var value;
    if(i === cellIndex) {
      value = editableField;
    }
    else {
      value = rowSpec.data[i];
    }
    var td = document.createElement("td");
    if(typeof value === "string" || typeof value === "number") {
      td.innerHTML = value;
    }
    else {
      td.appendChild(value);
    }

    if(i<this.columnSpecs.length) {
      var columnSpec = this.columnSpecs[i];
      $(td).addClass(columnSpec.css);
      $(td).attr("width", columnSpec.width);
    }

    row.appendChild(td);

    if (i === cellIndex) {
      result = td;
    }
  }

  return result;
};

EditableGrid.prototype.editObjectOnChange = function(e) {
  var target = e.target;
  var rowSpec = this.rowSpecs[this.editableCell.parentNode.sectionRowIndex];
  var originalValue = rowSpec.data[this.editableCell.cellIndex];

  var value = target.value.trim();
  var result = rowSpec.editableFields[this.editableCell.cellIndex].parse(value);
  parsedValue = result[0];
  newCell = result[1];
  if(parsedValue === null) {
    target.value = origValue;
    $(target).addClass("inputerror");
  }
  else {
    $(target).removeClass("inputerror");
    rowSpec.data[this.editableCell.cellIndex] = newCell;

    rowSpec.editableFields[this.editableCell.cellIndex].onchange(rowSpec, parsedValue);
    this.clearEditableRow();
  }
  return true;
};

var KeyCode = {
  UP: 38,
  DOWN: 40,
  LEFT: 37,
  RIGHT: 39,

  CTRL: 17,
  OSLEFT: 91,
  OSRIGHT: 92,

  0: 48,
  1: 49,
  2: 50,
  3: 51,
  4: 52,
  5: 53,
  6: 54,
  7: 55,
  8: 56,
  9: 57,

  A: 65,
  B: 66,
  C: 67,
  D: 68,
  E: 69,
  F: 70,
  G: 71,
  H: 72,
  I: 73,
  J: 74,
  K: 75,
  L: 76,
  M: 77,
  N: 78,
  O: 79,
  P: 80,
  Q: 81,
  R: 82,
  S: 83,
  T: 84,
  U: 85,
  V: 86,
  W: 87,
  X: 88,
  Y: 89,
  Z: 90
};

function Painter(obj) {
  if(typeof obj === "undefined") { obj = {}; }

  this.color        = ("color" in obj) ? obj.color : [0,0,0];
  this.autoColor    = ("autoColor" in obj) ? obj.autoColor : false;
  this.font         = ("font" in obj) ? obj.font : "normal 9px Verdana";
  this.alignLabel   = Painter.ALIGN_LEFT;
  if("alignLabel" in obj) { this.setAlignLabel(obj.alignLabel); }
  this.baselineLabel   = Painter.BASELINE_BOTTOM;
  if("baselineLabel" in obj) { this.setBaselineLabel(obj.baselineLabel); }
  this.showLabel    = ("showLabel" in obj) ? obj.showLabel : true;
  this.trimLabel    = ("trimLabel" in obj) ? obj.trimLabel : true;
  this.labelPadding = ("labelPadding" in obj) ? obj.labelPadding : 2;
  this.borderWidth  = ("borderWidth" in obj) ? obj.borderWidth : 0.2;
  this.showIcon     = ("showIcon" in obj) ? obj.showIcon : true;
  this.iconFill     = ("iconFill" in obj) ? obj.iconFill : false;
  this.iconPainters = {
    "plus":     {paint:Painter.paintPlus, width:12},
    "cross":    {paint:Painter.paintCross, width:12},
    "circle":   {paint:Painter.paintCircle, width:8},
    "triangle": {paint:Painter.paintTriangle, width:10},
    "Triangle": {paint:Painter.paintTriangle, width:10},
    "square":   {paint:Painter.paintSquare, width:10},
    "diamond":  {paint:Painter.paintDiamond, width:10},
    "Diamond":  {paint:Painter.paintDiamond, width:10}
  };
  if("iconPainters" in obj) {
    $.extend(this.iconPainters, obj.iconPainters);
  }

  // set when the painter is assigned to a band
  this.band = null;
}

// alignLabel
Painter.ALIGN_LEFT = 1;
Painter.ALIGN_RIGHT = 2;
Painter.ALIGN_CENTER = 3;

// baselineLabel
Painter.BASELINE_TOP = 1;
Painter.BASELINE_BOTTOM = 2;
Painter.BASELINE_CENTER = 3;

Painter.prototype.setAlignLabel = function(alignLabel) {
  if(alignLabel === Painter.ALIGN_LEFT || alignLabel === "left") {
    this.alignLabel = Painter.ALIGN_LEFT;
  }
  else if(alignLabel === Painter.ALIGN_RIGHT || alignLabel === "right") {
    this.alignLabel = Painter.ALIGN_RIGHT;
  }
  else if(alignLabel === Painter.ALIGN_CENTER || alignLabel === "center") {
    this.alignLabel = Painter.ALIGN_CENTER;
  }
};

Painter.prototype.getAlignLabel = function() {
  return this.alignLabel;
};

Painter.prototype.setBaselineLabel = function(baselineLabel) {
  if(baselineLabel === Painter.BASELINE_TOP || baselineLabel === "top") {
    this.baselineLabel = Painter.BASELINE_TOP;
  }
  else if(baselineLabel === Painter.BASELINE_BOTTOM || baselineLabel === "bottom") {
    this.baselineLabel = Painter.BASELINE_BOTTOM;
  }
  else if(baselineLabel === Painter.BASELINE_CENTER || baselineLabel === "center") {
    this.baselineLabel = Painter.BASELINE_CENTER;
  }
};

Painter.prototype.getBaselineLabel = function() {
  return this.baselineLabel;
};

Painter.prototype.setIconColor = function(icon, color) {
  if(icon in this.iconPainters) {
    this.iconPainters[icon].color = color;
  }
};

Painter.prototype.setIconWidth = function(icon, width) {
  if(icon in this.iconPainters) {
    this.iconPainters[icon].width = width;
  }
};

Painter.prototype.paintLabel = function(obj) {
  // RAVEN -- include annotation label
  var interval = obj.interval;
  var ll = obj.ll;
  var ul = obj.ul;
  var ur = obj.ur;
  var lr = obj.lr;
  var width = lr.x - ll.x;

  if (!this.autoFit) { // packed
      width = obj.maxr ? obj.maxr.x - ll.x : lr.x - ll.x;
  }
  var annotationLabel = obj.annotationLabel;

  // return early if nothing to paint
  if(interval.label === null) return;

  var ctx = this.band.canvas.getContext('2d');
  if(this.showLabel && interval.label !== null) {
    var padding = this.labelPadding;
    var alignLabel = this.alignLabel;
    var baselineLabel = this.baselineLabel;

    var label = this.trimLabel ? Util.trimToWidth(interval.label, width-padding*2, ctx) : interval.label;
    if(label !== "") {
      var labelColor = (interval.labelColor!==null) ? interval.labelColor : [0,0,0];
      ctx.fillStyle = Util.rgbaToString(labelColor, interval.labelOpacity);

      var labelX1 = null;
      var labelY1 = null;
      if (annotationLabel) {
        labelX1 = ll.x + ctx.measureText(label).width + padding;
      }
      else if(alignLabel === Painter.ALIGN_CENTER) {
        // Ensure text does not appear over band label area
        labelX1 = Math.max(ll.x + (lr.x-ll.x)/2 - ctx.measureText(label).width/2, ll.x + padding);
      }
      else if(alignLabel === Painter.ALIGN_RIGHT){
        labelX1 = Math.max(lr.x - ctx.measureText(label).width - padding, ll.x + padding);
      }
      else {
        //default is left aligned
        labelX1 = ll.x + padding;
      }

      if(baselineLabel === Painter.BASELINE_CENTER) {
        ctx.textBaseline = "middle";
        labelY1 = ul.y + ((ll.y-ul.y) / 2);
      }
      else if(baselineLabel === Painter.BASELINE_TOP) {
        ctx.textBaseline = "top";
        labelY1 = ul.y;
      }
      else {
        //default is bottom baselined
        ctx.textBaseline = "bottom";
        labelY1 = ll.y;
      }
      ctx.fillText(label, labelX1, labelY1);
    }
  }
};

// RAVEN -- display activity times
Painter.prototype.paintActivityTimes = function (obj) {
  var interval = obj.interval;
  var ll = obj.ll;
  var ul = obj.ul;
  var ur = obj.ur;
  var lr = obj.lr;
  var lastPaintedTimeX2 = obj.lastPaintedTimeX2;
  var lastPaintedTime = obj.lastPaintedTime;
  var width = lr.x - ll.x;

  if (!lastPaintedTimeX2 || (ll.x > lastPaintedTimeX2+5)) {
      var ctx = this.band.canvas.getContext('2d');
      var labelColor = (interval.labelColor!==null) ? interval.labelColor : [0,0,0];
      ctx.fillStyle = Util.rgbaToString(labelColor, interval.labelOpacity);

      if (!lastPaintedTime || (lastPaintedTime && lastPaintedTime < interval.start)) {
        // if activity area is large enough paint start and end times (hh:mm)
        if (width > ctx.measureText("hh:mm:ss  hh:mm:ss").width) {
            let startTimeStr = Util.toDOYDate(interval.start, false);
            startTimeStr = startTimeStr.substring (startTimeStr.length-8);
            let endTimeStr = Util.toDOYDate(interval.end, false);
            endTimeStr = endTimeStr.substring (endTimeStr.length-8);
            ctx.fillText(startTimeStr, ll.x, ll.y+8);
            ctx.fillText(endTimeStr, lr.x-ctx.measureText("hh:mm:s").width, lr.y+8);
            return ll.x+ctx.measureText("hh:mm:ss  hh:mm:ss").width;
        }
        else {
            let startTimeStr = Util.toDOYDate(interval.start, false);
            if (lastPaintedTime) {
              let lastPaintTimeStr = Util.toDOYDate(lastPaintedTime, false);
              const regExp = /(\d\d\d\d)-(\d\d\d)T(\d\d):(\d\d):(\d\d)/;
              let matStart = startTimeStr.match(regExp);
              let matLastPaintTime = lastPaintTimeStr.match(regExp);
              if (matStart[1] === matLastPaintTime[1] && matStart[2] === matLastPaintTime[2]) {
                  // if less than a day, just show hours, mins, secs
                  startTimeStr = `${matStart[3]}:${matStart[4]}:${matStart[5]}`;
              }
              else if (matStart[1] === matLastPaintTime[1]) {
                  // if less than a year, just show days, hours etc
                  startTimeStr = `${matStart[2]}T${matStart[3]}:${matStart[4]}:${matStart[5]}`;
              }
            }
            ctx.fillText(startTimeStr, ll.x, ll.y+8);
            return ll.x+ctx.measureText(startTimeStr).width;
        }
      }
  }

  return lastPaintedTimeX2;
};

Painter.prototype.paintStateChangeTime = function (obj) {
  var interval = obj.interval;
  var ll = obj.ll;
  var ul = obj.ul;
  var ur = obj.ur;
  var lr = obj.lr;
  var lastPaintedTimeX2 = obj.lastPaintedTimeX2;
  var lastPaintedTime = obj.lastPaintedTime;
  var width = lr.x - ll.x;

  if (!lastPaintedTimeX2 || (ll.x > lastPaintedTimeX2+5)) {
      var ctx = this.band.canvas.getContext('2d');
      var labelColor = (interval.labelColor!==null) ? interval.labelColor : [0,0,0];
      ctx.fillStyle = Util.rgbaToString(labelColor, interval.labelOpacity);

      if (!lastPaintedTime || (lastPaintedTime && lastPaintedTime < interval.start)) {
        let startTimeStr = Util.toDOYDate(interval.start, false);
        if (lastPaintedTime) {
          let lastPaintTimeStr = Util.toDOYDate(lastPaintedTime, false);
          const regExp = /(\d\d\d\d)-(\d\d\d)T(\d\d):(\d\d):(\d\d)/;
          let matStart = startTimeStr.match(regExp);
          let matLastPaintTime = lastPaintTimeStr.match(regExp);
          if (matStart[1] === matLastPaintTime[1] && matStart[2] === matLastPaintTime[2]) {
              // if less than a day, just show hours, mins, secs
              startTimeStr = `${matStart[3]}:${matStart[4]}:${matStart[5]}`;
          }
          else if (matStart[1] === matLastPaintTime[1]) {
              // if less than a year, just show days, hours etc
              startTimeStr = `${matStart[2]}T${matStart[3]}:${matStart[4]}:${matStart[5]}`;
          }
        }
        ctx.fillText(startTimeStr, ll.x, ll.y+8);
        return ll.x+ctx.measureText(startTimeStr).width;
      }
  }
  return lastPaintedTimeX2;
};


Painter.prototype.paint = function() {
  // implemented by derived classes
};

Painter.paintPlus = function(obj) {
  var band = obj.band;
  var interval = obj.interval;
  var color = obj.color;
  var width = obj.width;
  var ll = obj.ll;
  var ul = obj.ul;
  var ur = obj.ur;
  var lr = obj.lr;

  // draw the + centered
  var x = ll.x+(lr.x-ll.x)/2;
  var y = ll.y-(ll.y-ul.y)/2;

  var ctx = band.canvas.getContext('2d');
  ctx.lineWidth = 3;
  ctx.strokeStyle = color;
  ctx.beginPath();
  ctx.moveTo(x,         y-width/2);
  ctx.lineTo(x,         y+width/2);
  ctx.moveTo(x-width/2, y);
  ctx.lineTo(x+width/2, y);
  ctx.closePath();
  ctx.stroke();
};

Painter.paintCross = function(obj) {
  var band = obj.band;
  var interval = obj.interval;
  var color = obj.color;
  var width = obj.width;
  var ll = obj.ll;
  var ul = obj.ul;
  var ur = obj.ur;
  var lr = obj.lr;

  // draw the X centered
  var x = ll.x+(lr.x-ll.x)/2;
  var y = ll.y-(ll.y-ul.y)/2;

  var ctx = band.canvas.getContext('2d');
  ctx.lineWidth = 3;
  ctx.strokeStyle = color;
  ctx.beginPath();
  ctx.moveTo(x-width/2, y-width/2);
  ctx.lineTo(x+width/2, y+width/2);
  ctx.moveTo(x-width/2, y+width/2);
  ctx.lineTo(x+width/2, y-width/2);
  ctx.closePath();
  ctx.stroke();
};

Painter.paintCircle = function(obj) {
  var band = obj.band;
  var painter = band.painter;
  var interval = obj.interval;
  var color = obj.color;
  var width = obj.width;
  var ll = obj.ll;
  var ul = obj.ul;
  var ur = obj.ur;
  var lr = obj.lr;

  // draw the circle centered
  var x = ll.x+(lr.x-ll.x)/2;
  var y = ll.y-(ll.y-ul.y)/2;
  var radius = width/2;

  var ctx = band.canvas.getContext('2d');
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.arc(x,y,radius,0,2*Math.PI);
  if(painter.iconFill) {
    ctx.fillStyle = color;
    ctx.fill();
  }
  else {
    ctx.strokeStyle = color;
    ctx.stroke();
  }
};

Painter.paintTriangle = function(obj) {
  var band = obj.band;
  var painter = band.painter;
  var interval = obj.interval;
  var color = obj.color;
  var width = obj.width;
  var ll = obj.ll;
  var ul = obj.ul;
  var ur = obj.ur;
  var lr = obj.lr;

  // draw the triangle centered
  var x = ll.x+(lr.x-ll.x)/2;
  var y = ll.y-(ll.y-ul.y)/2;

  var ctx = band.canvas.getContext('2d');
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(x,         y-width/2);
  ctx.lineTo(x-width/2, y+width/2);
  ctx.lineTo(x+width/2, y+width/2);
  ctx.lineTo(x,         y-width/2);
  if(painter.iconFill) {
    ctx.fillStyle = color;
    ctx.fill();
  }
  else {
    ctx.strokeStyle = color;
    ctx.stroke();
  }
};

Painter.paintSquare = function(obj) {
  var band = obj.band;
  var painter = band.painter;
  var interval = obj.interval;
  var color = obj.color;
  var width = obj.width;
  var ll = obj.ll;
  var ul = obj.ul;
  var ur = obj.ur;
  var lr = obj.lr;

  // draw the square centered
  var x = ll.x+(lr.x-ll.x)/2;
  var y = ll.y-(ll.y-ul.y)/2;

  var ctx = band.canvas.getContext('2d');
  if(painter.iconFill) {
    ctx.fillStyle = color;
    ctx.fillRect(x-width/2, y-width/2, width, width);
  }
  else {
    ctx.lineWidth = 2;
    ctx.strokeStyle = color;
    ctx.strokeRect(x-width/2, y-width/2, width, width);
  }
};

Painter.paintDiamond = function(obj) {
  var band = obj.band;
  var painter = band.painter;
  var interval = obj.interval;
  var color = obj.color;
  var width = obj.width;
  var ll = obj.ll;
  var ul = obj.ul;
  var ur = obj.ur;
  var lr = obj.lr;

  // draw the diamond centered
  var x = ll.x+(lr.x-ll.x)/2;
  var y = ll.y-(ll.y-ul.y)/2;

  var ctx = band.canvas.getContext('2d');
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(x,         y-width/2);
  ctx.lineTo(x-width/2, y);
  ctx.lineTo(x,         y+width/2);
  ctx.lineTo(x+width/2, y);
  ctx.lineTo(x,         y-width/2);
  if(painter.iconFill) {
    ctx.fillStyle = color;
    ctx.fill();
  }
  else {
    ctx.strokeStyle = color;
    ctx.stroke();
  }
};

Painter.colors = [
  [255, 127, 191],
  [255, 127, 254],
  [191, 127, 255],
  [127, 127, 255],
  [127, 191, 255],
  [127, 254, 255],
  [127, 255, 191],
  [127, 255, 127],
  [191, 255, 127],
  [254, 255, 127],
  [255, 191, 127],
  [191, 63, 127],
  [191, 63, 191],
  [127, 63, 191],
  [63, 63, 191],
  [63, 127, 191],
  [63, 191, 191],
  [63, 191, 127],
  [63, 191, 63],
  [127, 191, 63],
  [191, 191, 63],
  [191, 127, 63],
  [223, 159, 191],
  [223, 159, 223],
  [191, 159, 223],
  [159, 159, 223],
  [159, 191, 223],
  [159, 223, 223],
  [159, 223, 191],
  [159, 223, 159],
  [191, 223, 159],
  [223, 223, 159],
  [223, 191, 159]
  // red is a reserved color
];

// autoColors is static so that the same value will map
// to the same color regardless of which band it is on
Painter.autoColors = {};
Painter.nextAutoColorIndex = 0;

Painter.getAutoColor = function(key) {
  if(!key) { return [0,0,0]; }

  // check to see if a default color has been assigned
  if(!(key in Painter.autoColors)) {
    // if not, assign one
    var index = Painter.nextAutoColorIndex++ % Painter.colors.length;
    Painter.autoColors[key] = Painter.colors[index];
  }
  // return the assigned color
  return Painter.autoColors[key];
};

ResourceBand.prototype = new Band();
ResourceBand.prototype.constructor = ResourceBand;

function ResourceBand(obj) {
  if(typeof obj === "undefined") return;

  if(!("decorator" in obj)) { obj.decorator = new ResourceDecorator(obj); }
  if(!("painter" in obj)) { obj.painter = new ResourcePainter(obj); }
  Band.prototype.constructor.call(this, obj);

  // the maximum limit (capacity) for this resource
  this.maxLimit = ("maxLimit" in obj) ? obj.maxLimit : null;
  // the minimum limit for this resource
  this.minLimit = ("minLimit" in obj) ? obj.minLimit : null;
  // the default value for the resource
  this.defaultValue = ("defaultValue" in obj) ? obj.defaultValue : this.minLimit;
  // if all_intervals - the top and bottom of the resource is maxValue and minValue
  // if visible_intervals - the top and bottom of the resource is the max/min value of the intervals in view
  // if null, the top is max(maxValue,maxLimit) and the bottom is min(minValue,minLimit)
  this.autoScale = null;
  if("autoScale" in obj) { this.setAutoScale(obj.autoScale); }
  // optional list of values to draw horizontal ticks
  this.tickValues = ("tickValues" in obj) ? obj.tickValues : [];
  // indicates if default tick values (maxLimit,0,minLimit) should be shown
  this.autoTickValues = ("autoTickValues" in obj) ? obj.autoTickValues : false;
  // indicates if tick values should be hidden
  this.hideTicks = ("hideTicks" in obj) ? obj.hideTicks : false;

  // RAVEN -- log ticks
  this.logTicks = ("logTicks" in obj) ? obj.logTicks : false; // If true, use Log ticks. False otherwise.
  this.logTickToCanvasHeight = {};                            // Maps log tick values to height drawn on Canvas.

  // callbacks
  this.onGetInterpolatedTooltipText = ("onGetInterpolatedTooltipText" in obj) ? obj.onGetInterpolatedTooltipText : ResourceBand.getInterpolatedTooltipText;
  this.onFilterTooltipIntervals = ("onFilterTooltipIntervals" in obj) ? obj.onFilterTooltipIntervals : ResourceBand.filterTooltipIntervals;
  this.onFormatTickValue = ("onFormatTickValue" in obj) ? obj.onFormatTickValue : null;

  // the min/max value and the min/max paintable values
  this.maxValue = this.defaultValue;
  this.minValue = this.defaultValue;
  this.maxPaintValue = this.minLimit;
  this.minPaintValue = this.maxLimit;
  this.computeMinMaxValues();
  this.computeMinMaxPaintValues();

  // the list of internally created interpolated interval ids
  this.interpolatedIntervalIDs = [];

  // supported values is can be string constant and linear or the corresponding enum
  // setting the interpolation flag may call computeInterpolatedIntervals
  this.interpolation = null;
  if("interpolation" in obj) { this.setInterpolation(obj.interpolation); }
}

// autoScale
ResourceBand.VISIBLE_INTERVALS = 1;
ResourceBand.ALL_INTERVALS     = 2;

// support values can be a string visiable_intervals or all_intervals or the corresponding enum
ResourceBand.prototype.setAutoScale = function(autoScale) {
  this.autoScale = null;
  if(autoScale === ResourceBand.VISIBLE_INTERVALS || autoScale === "visible_intervals") {
    this.autoScale = ResourceBand.VISIBLE_INTERVALS;
  }
  else if(autoScale === ResourceBand.ALL_INTERVALS || autoScale === "all_intervals") {
    this.autoScale = ResourceBand.ALL_INTERVALS;
  }
};

ResourceBand.prototype.getAutoScale = function() {
  return this.autoScale;
};

// interpolation
ResourceBand.CONSTANT = 1;
ResourceBand.LINEAR   = 2;

// supported values can be string constant and linear or the corresponding enum
ResourceBand.prototype.setInterpolation = function(interpolation) {
  this.interpolation = null;
  if(interpolation === ResourceBand.CONSTANT || interpolation === "constant") {
    this.interpolation = ResourceBand.CONSTANT;
  }
  else if(interpolation === ResourceBand.LINEAR || interpolation === "linear") {
    this.interpolation = ResourceBand.LINEAR;
  }

  if(this.interpolation === null) {
    this.clearInterpolatedIntervals();
  }
  else {
    this.computeInterpolatedIntervals();
  }
};

// return the enum value for the interpolation type
ResourceBand.prototype.getInterpolation = function() {
  return this.interpolation;
};

// RAVEN log ticks
ResourceBand.prototype.getYFromValueLog = function(value) {
  if (value > 0) {
    let ticks = Object.keys(this.logTickToCanvasHeight).sort();
    let minTick = 0;
    let maxTick = 0;

    // Find which ticks the value is within.
    // I.e. minTick <= value <= maxTick.
    for (let i = 0; i < ticks.length; ++i) {
      let tick = ticks[i];
      let nextTick =  ticks[i + 1];

      if (i < ticks.length - 1 && value >= tick && value <= nextTick) {
        minTick = tick;
        maxTick = nextTick;
        break;
      }
      else {
        minTick = ticks[i - 1];
        maxTick = tick;
      }
    }

    // Feature scaling.
    // This moves value to a number in [0, 1].
    let b = (value - minTick) / (maxTick - minTick);

    // Find the height on the canvas that the value should be located at.
    // This uses a logTickToCanvasHeight dictionary that gets the height of a log tick on the Canvas.
    let y = this.logTickToCanvasHeight[minTick] + b * (this.logTickToCanvasHeight[maxTick] - this.logTickToCanvasHeight[minTick]);

    return y;
  }
}

ResourceBand.prototype.getYFromValue = function(value) {
  var range = this.maxPaintValue - this.minPaintValue;
  if(range === 0)
    return 0;
  var scale = this.height / range;
  return this.height - ((value-this.minPaintValue) * scale) + this.heightPadding;
};

// compute the min max values based on the all the intervals assigned to the band
ResourceBand.prototype.computeMinMaxValues = function() {
  // defaultValue may be null at this point
  var maxValue = this.defaultValue;
  var minValue = this.defaultValue;

  // Log scale.
  if (this.logTicks && this.tickValues.length > 0) {
      maxValue = this.tickValues[this.tickValues.length - 1];
      minValue = this.tickValues[0];
  }
  // Linear scale.
  else {
    for(var i=0, ilength=this.intervalsList.length; i<ilength; ++i) {
        var intervals = this.intervalsList[i];
        for(var j=0, jlength=intervals.length; j<jlength; ++j) {
            var interval = intervals[j];
            var startValue = interval.startValue;
            var endValue = interval.endValue;

            var maxIntervalValue = Math.max(startValue, endValue);
            var minIntervalValue = Math.min(startValue, endValue);
            if(maxValue === null || maxIntervalValue > maxValue) {
                maxValue = maxIntervalValue;
            }
            if(minValue === null || minIntervalValue < minValue) {
                minValue = minIntervalValue;
            }
        }
    }
  }

  this.minValue = minValue;
  this.maxValue = maxValue;
  this.computeMinMaxPaintValues();
};

// compute the min max paint values
ResourceBand.prototype.computeMinMaxPaintValues = function() {
  var maxPaintValue = null;
  var minPaintValue = null;
  var start = this.viewTimeAxis.start;
  var end = this.viewTimeAxis.end;

  // Log scale.
  if (this.logTicks && this.tickValues.length > 0) {
      maxPaintValue = this.tickValues[this.tickValues.length - 1];
      minPaintValue = this.tickValues[0];
  }
  else {
    if(this.autoScale === ResourceBand.VISIBLE_INTERVALS &&
       (start !== this.timeAxis.start || end !== this.timeAxis.end)) {

      // find the intervals in view and determine the min/max values.  These become the min/max paint values.
      for(var i=0, ilength=this.intervalsList.length; i<ilength; ++i) {
        var intervals = this.intervalsList[i];
        for(var j=0, jlength=intervals.length; j<jlength; ++j) {
          var interval = intervals[j];
          if(interval.start > end) { break; } // break since remaining intervals are out of view
          if(interval.end < start || interval.start > end) { continue; } // skip if no intersection

          var startValue = interval.startValue;
          var endValue = interval.endValue;
          if(startValue !== endValue) {
            // interpolate if the start/end values differ
            if(interval.start < start) {
              startValue = Util.interpolateY3(interval.start, interval.startValue, interval.end, interval.endValue, start);
            }
            if(interval.end > end) {
              endValue = Util.interpolateY3(interval.start, interval.startValue, interval.end, interval.endValue, end);
            }
          }

          var maxIntervalValue = Math.max(startValue, endValue);
          var minIntervalValue = Math.min(startValue, endValue);
          if(maxPaintValue === null || maxIntervalValue > maxPaintValue) {
            maxPaintValue = maxIntervalValue;
          }
          if(minPaintValue === null || minIntervalValue < minPaintValue) {
            minPaintValue = minIntervalValue;
          }
        }
      }
    }
    else {
      // autoScale is either null or all_intervals
      minPaintValue = this.minValue;
      maxPaintValue = this.maxValue;

      // if not auto scaling, consider the min and max limit as well
      if(this.autoScale === null && this.minLimit !== null) {
        minPaintValue = Math.min(this.minValue, this.minLimit);
      }
      if(this.autoScale === null && this.maxLimit !== null) {
        maxPaintValue = Math.max(this.maxValue, this.maxLimit);
      }
    }

    // handle special case where min/max are the same.  Add some padding
    // so that value is rendered in the middle of the band
    if(minPaintValue === maxPaintValue) {
      if(minPaintValue === 0) {
        minPaintValue = -1;
        maxPaintValue = 1;
      }
      else {
        minPaintValue = minPaintValue - Math.abs(minPaintValue*0.1);
        maxPaintValue = maxPaintValue + Math.abs(maxPaintValue*0.1);
      }
    }
  }

  this.minPaintValue = minPaintValue;
  this.maxPaintValue = maxPaintValue;
};

ResourceBand.prototype.setIntervals = function(intervals, index) {
  Band.prototype.setIntervals.call(this, intervals, index);
  this.computeMinMaxValues();
  this.computeMinMaxPaintValues();
  this.computeInterpolatedIntervals();
};

ResourceBand.prototype.addInterval = function(interval, index) {
  Band.prototype.addInterval.call(this, interval, index);
  this.computeMinMaxValues();
  this.computeMinMaxPaintValues();
  this.computeInterpolatedIntervals();
};

ResourceBand.prototype.addIntervals = function(intervals, index) {
  Band.prototype.addInterval.call(this, intervals, index);
  this.computeMinMaxValues();
  this.computeMinMaxPaintValues();
  this.computeInterpolatedIntervals();
};

ResourceBand.prototype.removeInterval = function(id) {
  Band.prototype.removeInterval.call(this, id);
  this.computeMinMaxValues();
  this.computeMinMaxPaintValues();
  this.computeInterpolatedIntervals();
};

ResourceBand.prototype.removeIntervals = function(ids) {
  Band.prototype.removeIntervals.call(this, ids);
  this.computeMinMaxValues();
  this.computeMinMaxPaintValues();
  this.computeInterpolatedIntervals();
};

ResourceBand.prototype.clearInterpolatedIntervals = function() {
  // clear out previous interpolated intervals
  Band.prototype.removeIntervals.call(this, this.interpolatedIntervalIDs);
};

ResourceBand.prototype.computeInterpolatedIntervals = function() {
  if(this.interpolation === null) return;

  // clear out previous interpolated intervals
  this.clearInterpolatedIntervals();

  for(var i=0, ilength=this.intervalsList.length; i<ilength; ++i) {
    var intervals = this.intervalsList[i];

    var interpolatedIntervals = [];
    var prevInterval = null;
    for(var j=0, jlength=intervals.length; j<jlength; ++j) {
      var interval = intervals[j];
      if(prevInterval !== null && prevInterval.end < interval.start) {
        var interpolatedInterval = new DrawableInterval(prevInterval);
        interpolatedInterval.id = Band.getNextLocalID();
        interpolatedInterval.start = prevInterval.end;
        interpolatedInterval.end = interval.start;
        interpolatedInterval.startValue = prevInterval.endValue;
        interpolatedInterval.endValue = (this.interpolation === ResourceBand.CONSTANT) ? prevInterval.endValue : interval.startValue;
        interpolatedInterval.icon = null;
        interpolatedInterval.interpolated = true;
        interpolatedInterval.onGetTooltipText = this.onGetInterpolatedTooltipText;

        interpolatedIntervals.push(interpolatedInterval);
        this.interpolatedIntervalIDs.push(interpolatedInterval.id);
      }
      prevInterval = interval;
    }
    // add in the interpolated intervals
    Band.prototype.addIntervals.call(this, interpolatedIntervals, i);
  }
};

ResourceBand.getInterpolatedTooltipText = function(e, obj) {
  var band     = obj.band;
  var interval = obj.interval;
  var time     = obj.time;
  var timeZone = band.timeAxis.timeZone;

  var minLimit = (band.minLimit !== null) ? band.minLimit : band.minValue;
  var maxLimit = (band.maxLimit !== null) ? band.maxLimit : band.maxValue;

  var timeRangeStr = Util.toTimeRangeString(interval.start, interval.end, {timeZone:timeZone});

  var tooltipText = "";
  tooltipText += "<table class='tooltiptable'>";
  tooltipText += "<tr><td class='tooltiptablecell'><b>Interval:</b></td><td class='tooltiptablecell'>" + timeRangeStr + "</td></tr>";
  percentStr = " (" + Util.computePercent(interval.startValue, minLimit, maxLimit) + "%)";
  tooltipText += "<tr><td class='tooltiptablecell'><b>Start Value:</b></td><td class='tooltiptablecell'>"+interval.startValue+percentStr+"</td></tr>";
  percentStr = " (" + Util.computePercent(interval.endValue, minLimit, maxLimit) + "%)";
  tooltipText += "<tr><td class='tooltiptablecell'><b>End Value:</b></td><td class='tooltiptablecell'>"+interval.endValue+percentStr+"</td></tr>";
  var valueAtTime = Util.interpolateY3(interval.start, interval.startValue, interval.end, interval.endValue, time);
  percentStr = " (" + Util.computePercent(valueAtTime, minLimit, maxLimit) + "%)";
  tooltipText += "<tr><td class='tooltiptablecell'><b>Value:</b></td><td class='tooltiptablecell'>"+valueAtTime+percentStr+"</td></tr>";

  if(interval.properties !== null) {
    if($.isArray(interval.properties)) {
      for(var i=0, length=interval.properties.length; i<length; ++i) {
        var property = interval.properties[i];
        tooltipText += "<tr><td class='tooltiptablecell'><b>"+property.name+":</b></td><td class='tooltiptablecell'>"+property.value+"</td></tr>";
      }
    }
    else {
      for(var name in interval.properties) {
        var value = interval.properties[name];
        tooltipText += "<tr><td class='tooltiptablecell'><b>"+name+":</b></td><td class='tooltiptablecell'>"+value+"</td></tr>";
      }
    }
  }

  tooltipText += "</table>";
  return tooltipText;
};

// return non-interpolated if found, else return everything
ResourceBand.filterTooltipIntervals = function(obj) {
  var band      = obj.band;
  var intervals = obj.intervals;
  var time      = obj.time;

  var nonInterpolatedIntervals = [];
  for(var i=0, length=intervals.length; i<length; ++i) {
    var interval = intervals[i];
    if(!interval.interpolated) {
      nonInterpolatedIntervals.push(interval);
    }
  }

  if(nonInterpolatedIntervals.length !== 0) {
    return nonInterpolatedIntervals;
  }
  return intervals;
};

ResourceBand.prototype.repaint = function() {
  if(this.autoScale === ResourceBand.VISIBLE_INTERVALS) {
    this.computeMinMaxPaintValues();
  }
  Band.prototype.repaint.call(this);
};

ResourceDecorator.prototype = new Decorator();
ResourceDecorator.prototype.constructor = ResourceDecorator;

function ResourceDecorator(obj) {
  if(typeof obj === "undefined") return;

  Decorator.prototype.constructor.call(this, obj);
}

ResourceDecorator.prototype.paintValueTicks = function(xStart) {
  // RAVEN -- include an additional value tick if max value > last value tick
  var ctx = this.band.canvas.getContext('2d');
  ctx.lineWidth = 0.5;
  ctx.font = this.font;
  ctx.textAlign = "right";
  ctx.textBaseline = "bottom";
  ctx.fillStyle = Util.rgbaToString(this.band.labelColor, 1);
  ctx.strokeStyle = Util.rgbaToString(this.band.labelColor, 0.5);

  var labelWidth = this.band.viewTimeAxis.x1;
  //??var bandWidth = this.band.div.offsetWidth;
  var bandWidth = this.band.canvas.width;

  var autoPadding = 2;
  var axisLabelsXVal = xStart - autoPadding;

  var tickValues = [];
  if(this.band.tickValues.length !== 0) {
    // handle custom tick values
    for(var i=0, ilength=this.band.tickValues.length; i<ilength; ++i) {
      var tickValue = this.band.tickValues[i];
      if (Number(tickValue) > this.band.maxPaintValue) {
          this.band.maxPaintValue = Number(tickValue);
      }
      else if (Number(tickValue) < this.band.minPaintValue) {
          this.band.minPaintValue = Number(tickValue);
      }
      tickValues.push(tickValue);
    }
  }
  else if(this.band.autoTickValues) {
    // generate auto tick values
    if(this.band.maxPaintValue !== this.band.maxLimit) {
      tickValues.push(this.band.maxLimit);
    }
    if(this.band.minPaintValue !== this.band.minLimit) {
      tickValues.push(this.band.minLimit);
    }
    if(this.band.maxPaintValue > 0 && this.band.minPaintValue < 0) {
      // only render the tick if we're not already close to 0
      tickValues.push(0);
    }
    // include paint the min/max ticks if there is height padding
    if(this.band.heightPadding > 0) {
      tickValues.push(this.band.maxPaintValue);
      tickValues.push(this.band.minPaintValue);
    }
  }

  if (this.band.hideTicks) {
    return xStart;
  }

  // if no tick values to render, return early and return the original
  // xStart location
  if(tickValues.length === 0) {
    return xStart;
  }

  // RAVEN -- composite Y-axis should be black
  // labels should be black for composite labels
  if (this.band.parent instanceof CompositeBand && this.band.parent.compositeLabel)
      ctx.fillStyle = Util.rgbaToString([0,0,0], 1);

  // render the values if its in the renderable range
  var maxTickLabelWidth = 0;
  var seen = {};
  var yVal = 0;
  var yValue = 0;
  var step = this.band.height / tickValues.length;
  for(var j=0, jlength=tickValues.length; j<jlength; ++j) {
    var value = tickValues[j];

    // skip if previously evaluated
    if(value in seen) { continue; }
    seen[value] = 1;

    // render tick value label
    if(value !== null) {
      var renderedValue = this.band.onFormatTickValue !== null ? this.band.onFormatTickValue(value) : value;

      if (this.band.logTicks) {
        ctx.fillText(renderedValue, axisLabelsXVal, this.band.height - yVal);
        this.band.logTickToCanvasHeight[value] = this.band.height - yVal; // Maps tick values to Canvas positions.
        yVal += step;
      }
      else {
        var yVal = this.band.getYFromValue(value);
        ctx.fillText(renderedValue, axisLabelsXVal, yVal);
      }

      var valueWidth = ctx.measureText(renderedValue).width;
      maxTickLabelWidth = Math.max(valueWidth, maxTickLabelWidth);
    }

    var tmpYVal;
    if (this.band.logTicks) {
      tmpYVal = this.band.height - yValue;
      yValue += step;
    }
    else {
      tmpYVal = this.band.getYFromValue(value);
    }

    ctx.beginPath();
    var delta = 4;
    for(var x=labelWidth; x<=labelWidth+bandWidth; x+=delta*2) {
      ctx.moveTo(x,       tmpYVal);
      ctx.lineTo(x+delta, tmpYVal);
    }
    ctx.stroke();
    ctx.closePath();
  }

  // Don't draw a vertical if on labelWidth - assuming one already exists
  if(xStart !== labelWidth) {
    ctx.beginPath();
    ctx.moveTo(xStart, 0);
    ctx.lineTo(xStart, this.band.height + this.band.heightPadding);
    ctx.stroke();
    ctx.closePath();
  }

  // return the x location for the next set of tick values to be drawn
  return axisLabelsXVal - maxTickLabelWidth;
};

ResourceDecorator.prototype.paint = function() {
  if(this.band === null) return;

  var ctx = this.band.canvas.getContext('2d');
  ctx.font = this.font;
  ctx.textAlign = "left";
  ctx.textBaseline = "middle";

  this.paintTimeTicks();
  this.paintLabel(0);
  this.paintValueTicks(this.band.viewTimeAxis.x1);
  this.paintBackgroundIntervals();
};

ResourcePainter.prototype = new Painter();
ResourcePainter.prototype.constructor = ResourcePainter;

function ResourcePainter(obj) {
  if(typeof obj === "undefined") { obj = {}; }

  Painter.prototype.constructor.call(this, obj);

  this.fill = ("fill" in obj) ? obj.fill : true;
  this.fillColor = ("fillColor" in obj) ? obj.fillColor : null;
}

ResourcePainter.getAutoColor = function(unit, valueRange, minValue) {
  if(valueRange === 0) {
    return Painter.colors[0];
  }
  var heightPercentage = (unit.startValue-minValue)/valueRange;
  var index = Math.floor(heightPercentage * (Painter.colors.length-1));
  if(index > (Painter.colors.length - 1)) {
    index = 0;
  }
  return Painter.colors[index];
};

ResourcePainter.prototype.getColor = function(unit) {
  // if color specified in unit, return it
  // else if specified in the painter, return it
  // else choose from color wheel
  if(unit.color !== null) {
    return unit.color;
  }
  else if(this.autoColor) {
    var valueRange = this.band.maxValue - this.band.minValue;
    return ResourcePainter.getAutoColor(unit, valueRange, this.band.minValue);
  }
  else {
    return this.color;
  }
};

ResourcePainter.prototype.paintUnit = function(prevUnit, unit) {
  // paint the unit
  var viewTimeAxis = this.band.viewTimeAxis;
  var viewStart = viewTimeAxis.start;
  var viewEnd   = viewTimeAxis.end;

  var ctx = this.band.canvas.getContext('2d');
  var unitColor = this.getColor(unit);
  var color = Util.rgbaToString(unitColor, unit.opacity);

  var drawStartTime = unit.start;
  var drawEndTime = unit.end;
  var drawStartVal = unit.startValue;
  var drawEndVal = unit.endValue;
  if(unit.start < viewStart) {
    // clamp the start time and recalculate the start val
    drawStartTime = viewStart;
    drawStartVal = Util.interpolateY3(unit.start, unit.startValue, unit.end, unit.endValue, drawStartTime);
  }
  if(unit.end > viewEnd) {
    // clamp the end time and recalculate the end val
    drawEndTime = viewEnd;
    drawEndVal = Util.interpolateY3(unit.start, unit.startValue, unit.end, unit.endValue, drawEndTime);
  }
  var drawX1 = viewTimeAxis.getXFromTime(drawStartTime);
  var drawX2 = viewTimeAxis.getXFromTime(drawEndTime);
  var drawY1 = this.band.logTicks ? this.band.getYFromValueLog(drawStartVal) : this.band.getYFromValue(drawStartVal);
  var drawY2 = this.band.logTicks ? this.band.getYFromValueLog(drawEndVal) : this.band.getYFromValue(drawEndVal);

  if(this.fill) {
    var yZero = this.band.logTicks ? this.band.getYFromValueLog(0) : this.band.getYFromValue(0);
    // draw the unit
    ctx.fillStyle = this.fillColor ? Util.rgbaToString(this.fillColor, unit.opacity) : color;
    ctx.beginPath();
    ctx.moveTo(drawX1, drawY1);
    ctx.lineTo(drawX2, drawY2);
    ctx.lineTo(drawX2, yZero);
    ctx.lineTo(drawX1, yZero);
    ctx.closePath();
    ctx.fill();

    // draw the border of the unit
    if(this.borderWidth > 0) {
      ctx.lineWidth = this.borderWidth;
      ctx.strokeStyle = Util.rgbaToString([0,0,0], unit.opacity);
      ctx.stroke();
    }
  }

  // draw the line connecting the startValue to the endValue
  ctx.lineWidth = 2;
  ctx.strokeStyle = color;
  ctx.beginPath();
  ctx.moveTo(drawX1, drawY1);
  ctx.lineTo(drawX2, drawY2);
  if(prevUnit !== null && this.band.interpolation === ResourceBand.CONSTANT) {
    // draw the line at the start from the startValue to the prevUnit endValue
    var drawY3 = this.band.logTicks ? this.band.getYFromValueLog(prevUnit.endValue) : this.band.getYFromValue(prevUnit.endValue);
    ctx.moveTo(drawX1, drawY1);
    ctx.lineTo(drawX1, drawY3);
  }
  ctx.closePath();
  ctx.stroke();

  if(this.showIcon && unit.icon !== null && (unit.icon in this.iconPainters)) {
    var iconPainter = this.iconPainters[unit.icon];
    var iconColor = iconPainter.color ? Util.rgbaToString(iconPainter.color, unit.opacity) : color;
    var iconWidth = iconPainter.width;
    iconPainter.paint({band:this.band,
                       interval:unit,
                       color:iconColor,
                       width:iconWidth,
                       ll:{x:drawX1, y:drawY2},
                       ul:{x:drawX1, y:drawY1},
                       ur:{x:drawX2, y:drawY1},
                       lr:{x:drawX2, y:drawY2}});
    drawX1 -= iconWidth/2;
    drawX2 += iconWidth/2;
  }

  // paint the label
  if(this.showLabel && unit.label !== null) {
    this.paintLabel({interval:unit,
                     ll:{x:drawX1, y:drawY2},
                     ul:{x:drawX1, y:drawY1},
                     ur:{x:drawX2, y:drawY1},
                     lr:{x:drawX2, y:drawY2}});
  }

  return [drawX1, drawX2, 0, this.band.height + this.band.heightPadding];
};

ResourcePainter.prototype.paint = function() {
  if(this.band === null) return;

  // applies to all units
  var ctx = this.band.canvas.getContext('2d');
  ctx.font = this.font;
  ctx.textBaseline = "middle";

  // paint the units
  var viewTimeAxis = this.band.viewTimeAxis;
  var viewStart = viewTimeAxis.start;
  var viewEnd   = viewTimeAxis.end;
  var unitsList = this.band.getIntervalsInTimeRange(viewStart, viewEnd);

  var unitCoords = [];
  for(var i=0, ilength=unitsList.length; i<ilength; ++i) {
    var units = unitsList[i];
    var prevUnit = null;
    for(var j=0, jlength=units.length; j<jlength; ++j) {
      var unit = units[j];

      var coord = this.paintUnit(prevUnit, unit);
      unitCoords.push([unit].concat(coord));
      prevUnit = unit;
    }
  }
  return unitCoords;
};

function ResourceTimeLine(obj) {
  this.start = obj.start;
  this.end = obj.end;
  this.maxLimit = obj.maxLimit;
  this.minLimit = obj.minLimit;
  this.defaultValue = ("defaultValue" in obj) ? obj.defaultValue : obj.minLimit;
  this.properties = ("properties" in obj) ? obj.properties : {};

  this.duration      = this.end - this.start;
  this.minValue      = this.defaultValue;
  this.maxValue      = this.defaultValue;
  this.normalizedAvg = 0.0;
  this.reservations  = [];
  this.units         = [];
}

ResourceTimeLine.prototype.clone = function() {
  var timeline = new ResourceTimeLine({start:this.start,
                                       end:this.end,
                                       maxLimit:this.maxLimit,
                                       minLimit:this.minLimit,
                                       defaultValue:this.defaultValue,
                                       properties:this.properties});
  timeline.minValue = this.minValue;
  timeline.maxValue = this.maxValue;
  timeline.reservations = this.reservations.slice(0);
  timeline.units = this.units.slice(0);
  timeline.normalizedAvg = this.normalizedAvg;
  return timeline;
};

ResourceTimeLine.prototype.clear = function() {
  this.minValue      = this.defaultValue;
  this.maxValue      = this.defaultValue;
  this.normalizedAvg = 0.0;
  this.reservations  = [];
  this.units         = [];
};

ResourceTimeLine.prototype.addReservation = function(reservation) {
  this.reservations.push(reservation);
  this.computeUnits();
};

ResourceTimeLine.prototype.addReservations = function(reservations) {
  this.reservations = this.reservations.concat(reservations);
  this.computeUnits();
};

ResourceTimeLine.prototype.removeReservation = function(reservation) {
  for(var i=0, length=this.reservations.length; i<length; ++i) {
    var other = this.reservations[i];
    if(other.start == reservation.start &&
       other.end == reservation.end &&
       other.value == reservation.value) {

      // remove reservation and recompute units
      this.reservations.splice(i, 1);
      this.computeUnits();
      return;
    }
  }
  return true;
};

ResourceTimeLine.prototype.removeReservations = function(reservations) {
  var reservationsToRemove = reservations.slice(0);

  var remainingReservations = [];
  for(var i=0, ilength=this.reservations.length; i<ilength; ++i) {
    var other = this.reservations[i];

    var found = false;
    for(var j=0, jlength=reservationsToRemove.length; j<jlength; ++j) {
      var reservation = reservationsToRemove[j];
      if(other.start == reservation.start &&
         other.end == reservation.end &&
         other.value == reservation.value) {
        // skip the matching one and remove from open list
        reservationsToRemove.splice(j, 1);
        found = true;
        break;
      }
    }
    if(!found) {
      remainingReservations.push(other);
    }
  }

  if(remainingReservations.length != this.reservations.length) {
    this.reservations = remainingReservations;
    this.computeUnits();
  }
  return true;
};

ResourceTimeLine.prototype.computeUnits = function() {
  // reset everything
  this.units = [];
  this.minValue = this.defaultValue;
  this.maxValue = this.defaultValue;
  this.normalizedAvg = 0.0;

  // sort the reservations by earliest start, earliest end
  this.reservations.sort(function(a,b) {
    if(a.start == b.start) {
      return a.end - b.end;
    }
    return a.start - b.start;
  });

  var times = [];
  var units = {};
  for(var i=0, ilength=this.reservations.length; i<ilength; ++i) {
    var reservation = this.reservations[i];
    var start = reservation.start;
    var end = reservation.end;
    var resValue = reservation.value;
    if(!(start in units)) { units[start] = this.defaultValue; times.push(start); }
    if(!(end in units)) { units[end] = this.defaultValue; times.push(end); }
    units[start] = units[start] + resValue;
    units[end] = units[end] - resValue;
  }
  times.sort();

  var value = this.defaultValue;
  var prevTime = null;
  for(var j=0, jlength=times.length; j<jlength; ++j) {
    var time = times[j];
    if(prevTime !== null) {
      var unit = { start:prevTime, end:time, value:value };
      this.units.push(unit);

      var unitDur = Math.min(unit.end, this.end) - Math.max(unit.start, this.start);
      if(unitDur > 0) {
        this.normalizedAvg += ((unitDur*value)/(this.maxLimit * this.duration));
        this.maxValue = Math.max(value, this.maxValue);
        this.minValue = Math.min(value, this.minValue);
      }
    }
    value += units[time];
    prevTime = time;
  }
};

ResourceTimeLine.prototype.getAverage = function() {
  return this.normalizedAvg * this.maxLimit;
};

ResourceTimeLine.prototype.getAverageInTimeRange = function(start, end) {
  return this.getNormalizedAverageInTimeRange(start, end) * this.maxLimit;
};

ResourceTimeLine.prototype.getNormalizedAverage = function() {
  return this.normalizedAvg;
};

ResourceTimeLine.prototype.getNormalizedAverageInTimeRange = function(start, end) {
  var duration = end - start;
  var normalizedAvg = 0.0;
  for(var i=0, length=this.units.length; i<length; ++i) {
    var unit = this.units[i];
    if(unit.start <= end && unit.end >= start) {
      var value = unit.value;
      var unitDur = Math.min(unit.end, end) - Math.max(unit.start, start);
      normalizedAvg += ((unitDur*value)/(this.maxLimit * duration));
    }
    else if(unit.end > start) {
      // assume units are in time order so we can break early
      break;
    }
  }
  return normalizedAvg;
};

ResourceTimeLine.prototype.hasConflict = function() {
  var hasConflict = this.minValue < this.minLimit || this.maxValue > this.maxLimit;
  return hasConflict;
};

ResourceTimeLine.prototype.hasConflictInTimeRange = function(start, end) {
  for(var i=0, length=this.units.length; i<length; ++i) {
    var unit = this.units[i];
    var value = unit.value;
    if(unit.start <= end && unit.end >= start) {
      var hasConflict = value < this.minLimit || value > this.maxLimit;
      if(hasConflict) { return true; }
    }
  }
  return false;
};

ResourceTimeLine.prototype.findConflictingUnitsInTimeRange = function(start, end) {
  var conflicts = [];
  for(var i=0, length=this.units.length; i<length; ++i) {
    var unit = this.units[i];
    var value = unit.value;
    if(unit.start <= end && unit.end >= start) {
      var hasConflict = value < this.minLimit || value > this.maxLimit;
      if(hasConflict) {
        conflicts.push(unit);
      }
    }
  }
  return conflicts;
};

ResourceTimeLine.prototype.getDurationInConflict = function() {
  var durationInConflict = 0;
  for(var i=0, length=this.units.length; i<length; ++i) {
    var unit = this.units[i];
    var value = unit.value;
    if(unit.start <= this.end && unit.end >= this.start) {
      var hasConflict = value < this.minLimit || value > this.maxLimit;
      if(hasConflict) {
        var unitDur = Math.min(unit.end, this.end) - Math.max(unit.start, this.start);
        durationInConflict += unitDur;
      }
    }
  }
  return durationInConflict;
};

ResourceTimeLine.prototype.getValueAt = function(time) {
  if(this.units.length === 0) return this.defaultValue;
  for(var i=0, length=this.units.length; i<length; ++i) {
    var unit = this.units[i];
    if(time >= unit.start && time < unit.end) {
      return unit.value;
    }
  }
  return this.defaultValue;
};

function SimpleGrid(obj) {
  if(typeof obj === "undefined") return;

  // required
  this.id                 = obj.id;
  this.columnSpecs        = obj.columnSpecs;
  this.rowSpecs           = ("rowSpecs" in obj) ? obj.rowSpecs : null;
  this.rowSpecsCallback   = ("rowSpecsCallback") ? obj.rowSpecsCallback : null;

  // callback functions
  this.onLeftClick   = ("onLeftClick" in obj) ? obj.onLeftClick : null;
  this.onRightClick  = ("onRightClick" in obj) ? obj.onRightClick : null;

  // create the div that everything is attached to
  this.div = document.createElement("div");
  this.div.setAttribute("id", this.id);
  this.div.setAttribute('class', 'simplegriddiv');

  this.stickyHeaderDiv = document.createElement("div");
  this.tableDiv = document.createElement("div");
  this.stickyHeaderDiv.setAttribute("class", "simplegridtablediv");
  this.tableDiv.setAttribute("class", "simplegridtablediv");
  this.div.appendChild(this.stickyHeaderDiv);
  this.div.appendChild(this.tableDiv);

  // create the sticky header table
  this.stickyHeader = document.createElement("table");
  this.stickyHeader.setAttribute("class", "simplegridtable");
  this.stickyHeaderDiv.appendChild(this.stickyHeader);

  // create the table
  this.table = document.createElement("table");
  this.table.setAttribute("class", "simplegridtable");
  this.tableDiv.appendChild(this.table);
  this.thead = document.createElement("thead");
  this.tbody = document.createElement("tbody");
  this.table.appendChild(this.thead);
  this.table.appendChild(this.tbody);

  // initialize the header and sorter
  this.initTableHeader();
  this.initTableSorter();

  // array of all the rows in the table
  this.rows = [];
  // the row that is currently selected
  this.selectedRow = null;

  $(this.table).mousedown(this.mousedown.bind(this));
  $(this.table).dblclick(this.dblclick.bind(this));
}

SimpleGrid.prototype.resize = function(height) {
  $(this.div).css("height", height);

  var tableHeight = this.div.offsetHeight - this.stickyHeaderDiv.offsetHeight;
  $(this.tableDiv).css("height", tableHeight);

  this.resizeStickyHeaders();
};

SimpleGrid.prototype.resizeStickyHeaders = function() {
  $(this.thead).show();

  // make sure the sticky headers match the table headers
  var tableHeaderCells = $(this.thead).find("tr th");
  var stickyHeaderCells = $(this.stickyHeader).find("thead tr th");
  stickyHeaderCells.each(function(i) {
                      var cell = tableHeaderCells.eq(i);
                      $(this).attr("width", cell.attr("width"));
              });
  $(this.thead).hide();
};

SimpleGrid.prototype.initTableHeader = function() {
  $(this.thead).empty();
  $(this.thead).show();

  var row = document.createElement("tr");
  this.thead.appendChild(row);

  for(var i=0, length=this.columnSpecs.length; i<length; ++i) {
    var columnSpec = this.columnSpecs[i];
    var name = columnSpec.name;
    var width = columnSpec.width;

    var th = document.createElement("th");
    th.setAttribute("class", "simplegridtableth");
    $(th).attr("width", width);
    th.innerHTML = name;

    th.columnSpec = columnSpec;
    row.appendChild(th);
  }
};

SimpleGrid.prototype.initTableSorter = function() {
  // init the tablesorter
  var headers = {};
  for(var i=0, length=this.columnSpecs.length; i<length; ++i) {
    var columnSpec = this.columnSpecs[i];
    if(columnSpec.sorter) {
      headers[i] = {sorter: columnSpec.sorter};
    }
  }

  var tableSorterOptions = {
    cssAsc: "tablesorterHeaderSortUp",
    cssDesc: "tablesorterHeaderSortDown",
    widgets: [ "filter" ],
    headers: headers
  };
  $(this.table)
  .tablesorter(tableSorterOptions)
  .bind("sortEnd", this.sortEnd.bind(this));

  this.initStickyHeader();
};

SimpleGrid.prototype.sortEnd = function() {
  // make sure the sticky headers match the table headers
  var tableHeaderCells = $(this.thead).find("tr th");
  var stickyHeaderCells = $(this.stickyHeader).find("thead tr th");
  stickyHeaderCells.each(function(i) {
                           $(this).attr("class", tableHeaderCells.eq(i).attr("class"));
                         });
  this.scrollToSelectedRow();
};

SimpleGrid.prototype.isPopulated = function() {
  return this.rowSpecs !== null;
};

SimpleGrid.prototype.clearTableRows = function() {
  this.rows = [];
  this.selectedRow = null;
  this.rowSpecs = null;
  $(this.tbody).empty();
  $(this.table).trigger("update");
};

SimpleGrid.prototype.initTableRows = function() {
  // REVIEW
  // we have a cyclical reference between a row (DOM) and rowSpec (js)
  // that will result in a memory leak.  To break the reference, we
  // need to remove one of the references
  // UPDATE: Evidently this is only needed to avoid leaks on IE, so we
  // comment out for now for performance
  //for(var i=0, length=this.rows.length; i<length; ++i) {
  //  var row = this.rows[i];
  //  row.rowSpec = null;
  //}

  this.rows = [];
  this.selectedRow = null;
  if(this.rowSpecs === null) {
    this.rowSpecs = this.rowSpecsCallback();
  }

  // create the rows in the table
  $(this.tbody).empty();
  for(var i=0, length=this.rowSpecs.length; i<length; ++i) {
    var rowSpec = this.rowSpecs[i];
    this.addRow(rowSpec);
  }
  $(this.table).trigger("update");

  // trigger a filter on the new table
  var stickyHeaderFilters = $(this.stickyHeader).find("thead tr td input");
  stickyHeaderFilters.each(function(i) {
			    $(this).trigger("keyup");
                           });
};

SimpleGrid.prototype.initStickyHeader = function() {
  $(this.stickyHeader).empty();

  var thead = $(this.thead).clone().get(0);
  this.stickyHeader.appendChild(thead);

  // bind the events for the sticky headers to invoke the hidden ones
  var tableHeaderCells = $(this.thead).find("tr th");
  var stickyHeaderCells = $(this.stickyHeader).find("thead tr th");
  stickyHeaderCells.each(function(i) {
    var cell = tableHeaderCells.eq(i);
    $(this).attr("width", cell.attr("width"));
    $(this).bind("click", function(e) { cell.trigger(e); });
    $(this).bind("mousedown", function(e) { cell.trigger(e); });
    $(this).bind("mouseup", function(e) { cell.trigger(e); });
  });

  // bind the events for the filter input boxes
  var tableHeaderFilters = $(this.thead).find("tr td input");
  var stickyHeaderFilters = $(this.stickyHeader).find("thead tr td input");
  stickyHeaderFilters
  .each(function(i) {
          var input = tableHeaderFilters.eq(i);
          $(this).bind("keyup",
                       function(e) { input.val(this.value); input.trigger(e); });
  });
  $(this.thead).hide();
};

SimpleGrid.prototype.addRow = function(rowSpec) {
  var source = rowSpec.source;

  var row = document.createElement("tr");
  $(row).addClass("simplegridtabletr");
  $(row).addClass(rowSpec.css);

  row.rowSpec = rowSpec;
  source.row = row;

  this.tbody.appendChild(row);
  this.rows.push(row);

  this.initRow(row, rowSpec.data);
};

SimpleGrid.prototype.initRow = function(row, data) {
  $(row).empty();
  var values = data;
  for(var i=0, length=values.length; i<length; ++i) {
    var td = document.createElement("td");
    if (values[i] instanceof Element) {
      td.appendChild(values[i]);
    } else {
      td.innerHTML = values[i];
    }

    if(i<this.columnSpecs.length) {
      var columnSpec = this.columnSpecs[i];
      $(td).addClass(columnSpec.css);
      $(td).attr("width", columnSpec.width);
    }
    row.appendChild(td);
  }
};

SimpleGrid.prototype.scrollToSelectedRow = function() {
  if(this.selectedRow !== null) {
    $(this.tableDiv).scrollTop(this.selectedRow.offsetTop);
  }
};

SimpleGrid.prototype.findRow = function(source) {
  if("row" in source) {
    return source.row;
  }
  return null;
};

SimpleGrid.prototype.selectRow = function(row, scrollTo) {
  if(this.selectedRow !== null && this.selectedRow !== row) {
    $(this.selectedRow).removeClass("selected");
  }

  this.selectedRow = row;
  $(this.selectedRow).addClass("selected");

  if(scrollTo) {
    $(this.tableDiv).scrollTop(row.offsetTop);
  }
};

SimpleGrid.prototype.hideHeader = function() {
  $(this.stickyHeaderDiv).hide();
};

SimpleGrid.prototype.showHeader = function() {
  $(this.stickyHeaderDiv).show();
};

SimpleGrid.prototype.mousedown = function(e) {
  var target = e.target;
  if(target.nodeName !== "TD") return true;
  if($(target).hasClass("filter")) return true;
  var parent = target.parentNode;
  if(parent.nodeName !== "TR") return true;
  if($(parent).hasClass("filters")) return true;

  var row = parent;
  if(e.which === 1) {
    // left click
    if(this.onLeftClick !== null) {
      this.onLeftClick(e, {grid:this, row:row});
    }
  }
  else {
    // right click
    if(this.onRightClick !== null) {
      this.onRightClick(e, {grid:this, row:row});
    }
  }
  this.selectRow(row, false);
  return true;
};

SimpleGrid.prototype.dblclick = function(e) {
  return true;
};

//
// if($.tablesorter) {
//   // add some custom time parsers
//   $.tablesorter.addParser({
//     id: "YYYY-DDDD/HH:mm:ss",
//     is: function(s) { return false; },
//     format: function(s) { return Util.fromTimeString(s, {format:"YYYY-DDDD/HH:mm:ss"}); },
//     type: "numeric"
//   });
//
//   $.tablesorter.addParser({
//     id: "YYYY-DDDDTHH:mm:ss",
//     is: function(s) { return false; },
//     format: function(s) { return Util.fromTimeString(s, {format:"YYYY-DDDDTHH:mm:ss"}); },
//     type: "numeric"
//   });
//
//   $.tablesorter.addParser({
//     id: "YYYY-MM-DD/HH:mm:ss",
//     is: function(s) { return false; },
//     format: function(s) { return Util.fromTimeString(s, {format:"YYYY-MM-DD/HH:mm:ss"}); },
//     type: "numeric"
//   });
//
//   $.tablesorter.addParser({
//     id: "YYYY-MM-DD (DDDD) HH:mm:ss",
//     is: function(s) { return false; },
//     format: function(s) { return Util.fromTimeString(s, {format:"YYYY-MM-DD (DDDD) HH:mm:ss"}); },
//     type: "numeric"
//   });
//
//   $.tablesorter.addParser({
//     id: "dhm",
//     is: function(s) { return false; },
//     format: function(s) { return Util.fromDHMString(s); },
//     type: "numeric"
//   });
//
//   $.tablesorter.addParser({
//     id: "HH:mm:ss",
//     is: function(s) { return false; },
//   format: function(s) { return Util.fromDurationString(s); },
//     type: "numeric"
//   });
// }

StateBand.prototype = new Band();
StateBand.prototype.constructor = StateBand;

function StateBand(obj) {
  if(typeof obj === "undefined") return;

  if(!("decorator" in obj)) { obj.decorator = new Decorator(obj); }
  if(!("painter" in obj)) { obj.painter = new StatePainter(obj); }
  Band.prototype.constructor.call(this, obj);

  this.onFilterTooltipIntervals = ("onFilterTooltipIntervals" in obj) ? obj.onFilterTooltipIntervals : StateBand.filterTooltipIntervals;

  // the list of internally created interpolated interval ids
  this.interpolatedIntervalIDs = [];

  // supported values is can be string constant or the corresponding enum
  // setting the interpolation flag may call computeInterpolatedIntervals
  this.interpolation = null;
  if("interpolation" in obj) { this.setInterpolation(obj.interpolation); }
}

// interpolation
StateBand.CONSTANT = 1;

// supports either string or integer
// if string, internally converted to integer type
StateBand.prototype.setInterpolation = function(interpolation) {
  this.interpolation = null;
  if(interpolation === StateBand.CONSTANT || interpolation === "constant") {
    this.interpolation = StateBand.CONSTANT;
  }
  if(this.interpolation === null) {
    this.clearInterpolatedIntervals();
  }
  else {
    this.computeInterpolatedIntervals();
  }
};

StateBand.prototype.getInterpolation = function() {
  return this.interpolation;
};

StateBand.prototype.setIntervals = function(intervals, index) {
  Band.prototype.setIntervals.call(this, intervals, index);
  this.computeInterpolatedIntervals();
};

StateBand.prototype.addInterval = function(interval, index) {
  Band.prototype.addInterval.call(this, interval);
  this.computeInterpolatedIntervals();
};

StateBand.prototype.addIntervals = function(intervals, index) {
  Band.prototype.addIntervals.call(this, intervals);
  this.computeInterpolatedIntervals();
};

StateBand.prototype.removeInterval = function(id) {
  Band.prototype.removeInterval.call(this, id);
  this.computeInterpolatedIntervals();
};

StateBand.prototype.removeIntervals = function(ids) {
  Band.prototype.removeIntervals.call(this, ids);
  this.computeInterpolatedIntervals();
};

StateBand.prototype.clearInterpolatedIntervals = function() {
  // clear out previous interpolated intervals
  Band.prototype.removeIntervals.call(this, this.interpolatedIntervalIDs);
};

StateBand.prototype.computeInterpolatedIntervals = function() {
  if(this.interpolation === null) return;

  // clear out previous interpolated intervals
  this.clearInterpolatedIntervals();

  for(var i=0, ilength=this.intervalsList.length; i<ilength; ++i) {
    var intervals = this.intervalsList[i];

    var interpolatedIntervals = [];
    var prevInterval = null, interval = null, interpolatedInterval = null;
    for(var j=0, jlength=intervals.length; j<jlength; ++j) {
      interval = intervals[j];
      if(prevInterval !== null && prevInterval.end < interval.start) {
        // inherit the value and properties from the previous unit
        interpolatedInterval = new DrawableInterval(prevInterval);
        interpolatedInterval.id = Band.getNextLocalID();
        interpolatedInterval.start = prevInterval.end;
        interpolatedInterval.end = interval.start;
        interpolatedInterval.startValue = prevInterval.endValue;
        interpolatedInterval.endValue = (this.interpolation === StateBand.CONSTANT) ? prevInterval.endValue : interval.startValue;
        interpolatedInterval.icon = null;

        interpolatedIntervals.push(interpolatedInterval);
        this.interpolatedIntervalIDs.push(interpolatedInterval.id);
      }
      prevInterval = interval;
    }

    // interpolate between the last unit and the end of the band
    if(intervals.length !== 0) {
      interval = intervals[intervals.length-1];
      if(interval.end < this.timeAxis.end) {
        // inherit the value and properties from the last unit
        interpolatedInterval = new DrawableInterval(interval);
        interpolatedInterval.id = Band.getNextLocalID();
        interpolatedInterval.start = interval.end;
        interpolatedInterval.end = this.timeAxis.end;
        interpolatedInterval.icon = null;
        interpolatedInterval.interpolated = true;

        interpolatedIntervals.push(interpolatedInterval);
        this.interpolatedIntervalIDs.push(interpolatedInterval.id);
      }
    }

    // add in the interpolated intervals
    Band.prototype.addIntervals.call(this, interpolatedIntervals, i);
  }
};

// return non-interpolated if found, else return everything
StateBand.filterTooltipIntervals = function(obj) {
  var band      = obj.band;
  var intervals = obj.intervals;
  var time      = obj.time;

  var nonInterpolatedIntervals = [];
  for(var i=0, length=intervals.length; i<length; ++i) {
    var interval = intervals[i];
    if(!interval.interpolated) {
      nonInterpolatedIntervals.push(interval);
    }
  }

  if(nonInterpolatedIntervals.length !== 0) {
    return nonInterpolatedIntervals;
  }
  return intervals;
};

StatePainter.prototype = new Painter();
StatePainter.prototype.constructor = StatePainter;

function StatePainter(obj) {
  if(typeof obj === "undefined") { obj = {}; }

  Painter.prototype.constructor.call(this, obj);
}

StatePainter.prototype.getColor = function(unit) {
  if(unit.color !== null) {
    return unit.color;
  }
  else if(this.autoColor) {
    return Painter.getAutoColor(unit.startValue);
  }
  else {
    return this.color;
  }
};

StatePainter.prototype.paintUnit = function(unit, lastPaintedTimeX2, lastPaintedTime) {
  var ctx = this.band.canvas.getContext('2d');

  // paint the unit
  var viewTimeAxis = this.band.viewTimeAxis;
  var unitX1 = viewTimeAxis.getXFromTime(unit.start);
  var unitX2 = viewTimeAxis.getXFromTime(unit.end);
  var unitY1 = 0;
  var unitY2 = this.band.height;
  var unitWidth = unitX2 - unitX1;
  var unitHeight = unitY2 - unitY1;

  var unitColor = this.getColor(unit);
  var color = Util.rgbaToString(unitColor, unit.opacity);

  if(unitX1 == unitX2) {
    ctx.strokeStyle = color;
    ctx.beginPath();
    ctx.moveTo(unitX1, unitY1);
    ctx.lineTo(unitX2, unitY2);
    ctx.closePath();
    ctx.fill();
  }
  else {
    // draw the unit
    ctx.fillStyle = color;
    ctx.fillRect(unitX1, unitY1, unitWidth, unitHeight);

    // draw the border of the unit
    if(this.borderWidth > 0) {
      ctx.strokeStyle = Util.rgbaToString([0,0,0], unit.opacity);
      ctx.strokeRect(unitX1, unitY1, unitWidth, unitHeight);
    }
  }

  // paint the icon
  if(this.showIcon && unit.icon !== null && (unit.icon in this.iconPainters)) {
    var iconPainter = this.iconPainters[unit.icon];
    var iconColor = iconPainter.color ? Util.rgbaToString(iconPainter.color, unit.opacity) : color;
    var iconWidth = iconPainter.width;
    iconPainter.paint({band:this.band,
                       interval:unit,
                       color:iconColor,
                       width:iconWidth,
                       ll:{x:unitX1, y:unitY2},
                       ul:{x:unitX1, y:unitY1},
                       ur:{x:unitX1, y:unitY1},
                       lr:{x:unitX2, y:unitY2}});
    unitX1 -= iconWidth/2;
    unitX2 += iconWidth/2;
  }

  // paint the label
  if(this.showLabel && unit.label !== null) {
    this.paintLabel({interval:unit,
                     ll:{x:unitX1, y:unitY2},
                     ul:{x:unitX1, y:unitY1},
                     ur:{x:unitX2, y:unitY1},
                     lr:{x:unitX2, y:unitY2}});
  }

  // paint time of state change
  if (this.showStateChangeTimes) {
    let paintedTimeX2 = this.paintStateChangeTime({interval:unit,
                     ll:{x:unitX1, y:unitY2},
                     ul:{x:unitX1, y:unitY1},
                     ur:{x:unitX2, y:unitY1},
                     lr:{x:unitX2, y:unitY2},
                     lastPaintedTimeX2: lastPaintedTimeX2,
                     lastPaintedTime: lastPaintedTime});
    if (paintedTimeX2 !== lastPaintedTimeX2) {
        lastPaintedTimeX2 = paintedTimeX2;
        lastPaintedTime = unit.start;
      }
  }


  return {coord: [unitX1, unitX2, 0, this.band.height + this.band.heightPadding], lastPaintedTimeX2, lastPaintedTime};
};

StatePainter.prototype.paint = function() {
  if(this.band === null) return;

  // applies to all units
  var ctx = this.band.canvas.getContext('2d');
  ctx.font = this.font;
  ctx.textBaseline = "middle";
  ctx.lineWidth = this.borderWidth;

  // paint the units
  var viewTimeAxis = this.band.viewTimeAxis;
  var viewStart = viewTimeAxis.start;
  var viewEnd   = viewTimeAxis.end;
  var unitsList = this.band.getIntervalsInTimeRange(viewStart, viewEnd);

  var unitCoords = [];
  var lastPaintedTimeX2 = null;
  var lastPaintedTime = null;
  for(var i=0, ilength=unitsList.length; i<ilength; ++i) {
    var units = unitsList[i];
    for(var j=0, jlength=units.length; j<jlength; ++j) {
      var unit = units[j];
      var coordPlus = this.paintUnit(unit, lastPaintedTimeX2, lastPaintedTime);
      unitCoords.push([unit].concat(coordPlus.coord));
      lastPaintedTimeX2 = coordPlus.lastPaintedTimeX2;
      lastPaintedTime = coordPlus.lastPaintedTime;
    }
  }
  return unitCoords;
};

function TabSet(tabs) {
  var tabListElement, tabHeaderElement;
  this.tabs = tabs;

  this.div = document.createElement("div");
  this.div.setAttribute("class", "tabset");

  this.header = document.createElement("ul");
  this.tabsDiv = document.createElement("div");
  for (var i = 0; i < this.tabs.length; i++) {
    tabHeaderElement = TabSet.generateTabHeaderElement(this.tabs[i]);
    tabListElement = TabSet.generateTabListElement(this.tabs[i]);
    this.header.appendChild(tabHeaderElement);
    this.tabsDiv.appendChild(tabListElement);
  }
  this.div.appendChild(this.header);
  this.div.appendChild(this.tabsDiv);

  $(this.div).tabs({activate: this.tabSelected.bind(this)});
}

TabSet.generateTabHeaderElement = function(tab) {
  var tabHeaderLink, tabHeaderElement;

  tabHeaderLink = document.createElement("a");
  tabHeaderLink.setAttribute("href", "#" + tab.tabId);
  tabHeaderLink.innerHTML = tab.tabName;
  tabHeaderElement = document.createElement("li");
  tabHeaderElement.appendChild(tabHeaderLink);
  tabHeaderElement.ondragstart = this.cancelDrag;

  return tabHeaderElement;
};

TabSet.generateTabListElement = function(tab) {
  var tabListElement;

  tabListElement = document.createElement("div");
  tabListElement.setAttribute("id", tab.tabId);
  tabListElement.appendChild(tab.div);

  return tabListElement;
};

TabSet.prototype.cancelDrag = function() { return false; };

TabSet.prototype.resize = function(height) {
  for (var i = 0; i < this.tabs.length; i++) {
    var tab = this.tabs[i];
    if (tab.resize) {
      tab.resize(height);
    }
  }
};

TabSet.prototype.tabSelected = function(event, ui) {
  var tab = this.tabs[ui.newTab.index()];
  if (tab.tabSelected) {
    tab.tabSelected();
  }
};

TabSet.prototype.addTab = function(index, tab) {
  var tablist = this.div.childNodes[0];
  var tabset = this.div.childNodes[1];

  var tabHeaderElement = TabSet.generateTabHeaderElement(tab);
  var tabListElement = TabSet.generateTabListElement(tab);

  if (index === tablist.childNodes.length) {
    tablist.appendChild(tabHeaderElement);
    tabset.appendChild(tabListElement);
  } else {
    tablist.insertBefore(tabHeaderElement, tablist.childNodes[index]);
    tabset.insertBefore(tabListElement, tabset.childNodes[index]);
  }
  this.tabs.splice(index, 0, tab);
  $(this.div).tabs("refresh");
};

TabSet.prototype.removeTab = function(index) {
  var tablist = this.div.childNodes[0];
  var tabset = this.div.childNodes[1];

  tablist.removeChild(tablist.childNodes[index]);
  tabset.removeChild(tabset.childNodes[index]);
  this.tabs.splice(index, 1);
  $(this.div).tabs("refresh");
};

TabSet.prototype.replaceTab = function(index, tab) {
  var tablist = this.div.childNodes[0];
  var tabset = this.div.childNodes[1];

  var active = false;
  if ($(this.div).tabs("option", "active") === index) {
    active = true;
  }

  var tabHeaderElement = TabSet.generateTabHeaderElement(tab);
  var tabListElement = TabSet.generateTabListElement(tab);

  tablist.insertBefore(tabHeaderElement, tablist.childNodes[index]);
  tablist.removeChild(tablist.childNodes[index + 1]);
  tabset.insertBefore(tabListElement, tabset.childNodes[index]);
  tabset.removeChild(tabset.childNodes[index + 1]);

  this.tabs.splice(index, 1, tab);
  $(this.div).tabs("refresh");

  if (active) {
    $(this.div).tabs("option", "active", index);
  }
};

function TimeAxis(obj) {
  // mandatory
  // the time at the start of the axis
  this.start = obj.start;
  // the time at the end of the axis
  this.end = obj.end;
  // the x coordinate of the start time
  this.x1 = obj.x1;
  // the x coordinate of the end time
  this.x2 = obj.x2;

  // optional
  // the current time now
  this.now = ("now" in obj) ? obj.now : null;
  // the timezone representation
  this.timeZone = ("timeZone" in obj) ? obj.timeZone : "UTC";
  // the set of time units to consider when computing tick time
  this.tickUnits = ("tickUnits" in obj) ? obj.tickUnits : null;
  // the minimum width of each tick.
  this.minTickWidth = ("minTickWidth" in obj) ? Math.max(obj.minTickWidth, 100) : 100;
  // the minimum tick time unit based on the view
  // for example, if minTickUnits = {TimeUnit.WEEK: TimeUnit.DAY, TimeUnit.DAY: TimeUnit:TWO_HOUR},
  // then if the viewport is a week, ast most day tick times are shown, though additional times such
  // as 12 hour times would fit.  If the viewport is a day, at most 2 hour tick times are shown.
  this.minTickUnits = ("minTickUnits" in obj) ? obj.minTickUnits : {};
  // the times to draw vertical lines
  this.guideTimes = ("guideTimes" in obj) ? obj.guideTimes : [];

  // the time unit between tick times
  this.tickUnit = null;
  // the times to draw ticks marks
  this.tickTimes = [];
}

TimeAxis.prototype.reinit = function(obj) {
  this.start = obj.start;
  this.end = obj.end;
  this.x1 = obj.x1;
  this.x2 = obj.x2;
  this.now = ("now" in obj) ? obj.now : null;

  this.computeTickTimes();
};

TimeAxis.prototype.computeTickTimes = function() {
  // compute the best unit for the full time range
  var timeRange = this.end - this.start;
  var timeRangeUnit = TimeUnit.getBestFitTimeUnit(timeRange);
  var maxTicks = Math.max(Math.floor((this.x2 - this.x1) / this.minTickWidth) - 1, 1); // CHANGED (10/26/17).

  // compute the max tick unit to use when rendering ticks
  this.tickUnit = TimeUnit.computeTickTimeUnit(timeRange, maxTicks, this.tickUnits);
  if(timeRangeUnit in this.minTickUnits) {
    this.tickUnit = Math.max(this.tickUnit, this.minTickUnits[timeRangeUnit]);
  }

  this.tickTimes = [];
  var quantizedStart = TimeUnit.quantizeUpByTimeUnit(this.start, 0, this.tickUnit);
  if(this.start == quantizedStart) {
    this.tickTimes.push(this.start);
  }
  for(var time = TimeUnit.quantizeUpByTimeUnit(this.start, 1, this.tickUnit);
      time < this.end;
      time = TimeUnit.quantizeUpByTimeUnit(time, 1, this.tickUnit)) {
    this.tickTimes.push(time);
  }
};

TimeAxis.prototype.updateXCoordinates = function(x1, x2) {
  if(x1 >= x2) return false;

  this.x1 = x1;
  this.x2 = x2;
  this.computeTickTimes();
  return true;
};

TimeAxis.prototype.updateTimes = function(start, end) {
  if(start >= end) return false;

  this.start = start;
  this.end = end;
  this.computeTickTimes();
  return true;
};

TimeAxis.prototype.getXFromTime = function(time) {
  var timeScale = (this.x2 - this.x1) / (this.end - this.start);
  var x = Math.floor(((time - this.start) * timeScale) + this.x1);
  x = Math.max(x, this.x1);
  x = Math.min(x, this.x2);
  x = (x+0.5) | 0;
  return x;
};

TimeAxis.prototype.getXFromTimeNoClamping = function(time) {
  var timeScale = (this.x2 - this.x1) / (this.end - this.start);
  var x = Math.floor(((time - this.start) * timeScale) + this.x1);
  x = (x+0.5) | 0;
  return x;
};

TimeAxis.prototype.getTimeFromX = function(x) {
  var timeScale = (this.x2 - this.x1) / (this.end - this.start);
  var time = Math.ceil(((x - this.x1)/ timeScale) + this.start);
  return time;
};

// guide times
TimeAxis.prototype.addGuideTime = function(time) {
  this.guideTimes.push(time);
};

TimeAxis.prototype.clearGuideTimes = function() {
  this.guideTimes = [];
};

// the TimeBand
function TimeBand(obj) {
  if(typeof obj === "undefined") return;

  //mandatory args
  this.timeAxis = obj.timeAxis;
  this.viewTimeAxis = obj.viewTimeAxis;

  // optional args
  this.scrollDelta = ("scrollDelta" in obj) ? obj.scrollDelta : (6*3600);
  this.zoomDelta   = ("zoomDelta" in obj) ? obj.zoomDelta : (6*3600);
  this.height      = ("height" in obj) ? obj.height : 37;
  this.label       = ("label" in obj) ? obj.label : "UTC";
  this.minorLabels = ("minorLabels" in obj) ? obj.minorLabels : [];
  this.font        = ("font" in obj) ? obj.font : "normal 9px Verdana";

  // callback functions
  this.onShowTooltip = ("onShowTooltip" in obj) ? obj.onShowTooltip : null;
  this.onHideTooltip = ("onHideTooltip" in obj) ? obj.onHideTooltip : null;
  this.onUpdateView  = ("onUpdateView" in obj) ? obj.onUpdateView : null;
  this.onFormatTimeTick  = ("onFormatTimeTick" in obj) ? obj.onFormatTimeTick : TimeBand.formatTimeTick;
  this.onFormatNow       = ("onFormatNow" in obj) ? obj.onFormatNow : TimeBand.formatNow;

  // create the div that everything is attached to
  this.div = document.createElement("div");
  this.div.setAttribute('class', 'timebanddiv');
  $(this.div).css("height", this.height);

  // create the canvas
  this.canvas = document.createElement("canvas");
  this.canvas.setAttribute('class', 'timebandcanvas');
  this.div.appendChild(this.canvas);

  // add event handlers for the canvas
  $(this.canvas).mousedown(this.mousedown.bind(this));
  $(this.canvas).mouseup(this.mouseup.bind(this));
  $(this.canvas).mouseout(this.mouseout.bind(this));
  $(this.canvas).mousemove(this.mousemove.bind(this));

  this.contextMenu = new TimeBandContextMenu($.extend({timeBand:this}, obj));
  this.div.appendChild(this.contextMenu.div);

  // we need to create a new div as the source for enabling
  // dragging since we are trying to use the same div for dragging
  // as dropping.  The event to start the drag processing is
  // dispatched in the main div mouse down handler
  this._hiddenDragSource = document.createElement("div");
  this.div.appendChild(this._hiddenDragSource);
  $(this._hiddenDragSource).draggable({
    disabled: true,
    distance: 0,
    axis: "x",
    revert:true,
    start: this.handleDragStart.bind(this),
    drag: this.handleDrag.bind(this),
    stop: this.handleDragStop.bind(this)
  });

  // PRIVATE variable

  // parameters used to store zoom info
  this._zoomX1 = null;
  this._zoomX2 = null;

  // these parameters are used to scale the canvas if needed, see revalidate
  var ctx = this.canvas.getContext('2d');
  this.devicePixelRatio = window.devicePixelRatio || 1;
  this.backingStoreRatio = ctx.webkitBackingStorePixelRatio ||
    ctx.mozBackingStorePixelRatio ||
    ctx.msBackingStorePixelRatio ||
    ctx.oBackingStorePixelRatio ||
    ctx.backingStorePixelRatio || 1;
}

// get a set of formatted time string from a time tick
// returns an array for multi row time strings
TimeBand.formatTimeTick = function(obj) {
  var time = obj.time;
  var band = obj.timeBand;
  var timeZone = band.timeAxis.timeZone;

  var mom = moment.utc(time*1000).tz(timeZone);
  var formattedTimes = [];

  // generate the upper time string
  var upperTimeStr = mom.format("YYYY/MM/DD (DDDD)");
  // using default x, y position for now
  formattedTimes.push({formattedTime:upperTimeStr});

  // generate the lower time string
  var lowerTimeStr = mom.format("HH:mm:ss");
  var weekTime = TimeUnit.quantizeUpByTimeUnit(time, 0, TimeUnit.WEEK);
  if(weekTime == time) {
    lowerTimeStr += " WK " + mom.format("WW");
  }
  // using default x, y position for now
  formattedTimes.push({formattedTime:lowerTimeStr});
  return formattedTimes;
};

TimeBand.formatNow = function(obj) {
  var time = obj.time;
  var band = obj.timeBand;
  var timeZone = band.timeAxis.timeZone;

  var mom = moment.utc(time*1000).tz(timeZone);
  var nowStr = mom.format("HH:mm:ss");

  var formattedTimes = [];
  formattedTimes.push({formattedTime:nowStr, y:30});

  return formattedTimes;
};

TimeBand.prototype.revalidate = function() {
  var height = this.height;
  var width = this.div.offsetWidth;

  // upscale the canvas if the 2 ratios don't match
  // setting the width/height clears out scaling
  // see https://www.html5rocks.com/en/tutorials/canvas/hidpi
  if(this.devicePixelRatio !== this.backingStoreRatio) {
    var ratio = this.devicePixelRatio / this.backingStoreRatio;
    this.canvas.width = width * ratio;
    this.canvas.height = height * ratio;
    this.canvas.style.width = width + 'px';
    this.canvas.style.height = height + 'px';
    // now scale the context to counter the fact that we've
    // manually scaled our canvas element
    var ctx = this.canvas.getContext('2d');
    ctx.scale(ratio, ratio);
  }
  else {
    this.canvas.width = width;
    this.canvas.height = height;
  }
};

TimeBand.prototype.paintTicks = function() {
  var viewStart = this.viewTimeAxis.start;
  var viewEnd = this.viewTimeAxis.end;
  var ctx = this.canvas.getContext('2d');
  var bandHeight = this.height;

  ctx.lineWidth = 0.5;
  ctx.strokeStyle = "rgba(0, 0, 0, 0.5)";
  ctx.fillStyle = "black";
  ctx.textBaseline = "middle";
  ctx.font = this.font;

  var tickTimes = this.viewTimeAxis.tickTimes;
  for(var i=0, ilength=tickTimes.length; i<ilength; ++i) {
    var time = tickTimes[i];
    if(time < viewStart || time > viewEnd) continue;
    var timeX = this.viewTimeAxis.getXFromTime(time);

    ctx.beginPath();
    var delta = (time % TimeUnit.DAY === 0) ? bandHeight : 4;
    for(var tickY=0, height=bandHeight; tickY<height; tickY+=delta*2) {
      ctx.moveTo(timeX, tickY);
      ctx.lineTo(timeX, tickY+delta);
    }
    ctx.stroke();
    ctx.closePath();

    // compute the date/time string
    var formattedTimes = this.onFormatTimeTick({timeBand:this, time:time});
    for(var j=0, jlength=formattedTimes.length; j<jlength; ++j) {
      var obj = formattedTimes[j];
      var formattedTime = obj.formattedTime;
      var x = ("x" in obj) ? obj.x : timeX+2;
      var y = ("y" in obj) ? obj.y : 10*(j+1);
      ctx.fillText(formattedTime, x, y);
    }
  }
};

TimeBand.prototype.paintLabel = function() {
  var ctx = this.canvas.getContext('2d');
  var labelWidth = this.viewTimeAxis.x1;

  var x, y;
  var yDelta = 10;
  if(labelWidth > 2) {
    ctx.fillStyle = Util.rgbaToString([0,0,0], 1);

    x = 2;
    y = yDelta;
    var label = Util.trimToWidth(this.label, labelWidth - x, ctx);
    if(label !== "") {
      ctx.fillText(label, x, y);
      x = x + 5;
      y += yDelta;
    }

    for(var i=0, ilength=this.minorLabels.length; i<ilength; ++i) {
      var minorLabel = Util.trimToWidth(this.minorLabels[i], labelWidth - x, ctx);
      ctx.fillText(minorLabel, x, y);
      y += yDelta;
    }
  }
};

// paint the ticks and date
TimeBand.prototype.repaint = function() {
  var ctx = this.canvas.getContext('2d');
  var bandWidth = this.div.offsetWidth;
  var bandHeight = this.height;

  ctx.font = this.font;
  ctx.lineWidth = 0.5;
  ctx.fillStyle = "black";
  ctx.textBaseline = "middle";
  ctx.strokeStyle = Util.rgbaToString([0,0,0], 0.5);

  // clear out the previous painting
  // don't use the canvas height/width since its may be scaled
  // see revalidate for when scaling occurs
  ctx.clearRect(0, 0, bandWidth, bandHeight);

  // draw a border around the canvas
  ctx.strokeRect(0, 0, bandWidth, bandHeight);

  // paint the highlighted interval
  if(this._zoomX1 !== null && this._zoomX2 !== null) {
    var zoomWidth = Math.abs(this._zoomX2 - this._zoomX1);
    ctx.fillStyle = "rgba(200, 200, 200, 0.50)";
    ctx.fillRect((this._zoomX1<this._zoomX2) ? this._zoomX1 : this._zoomX2,
		 0,
		 zoomWidth,
		 bandHeight);
  }

  // paint the border on the right
  var labelWidth = this.viewTimeAxis.x1;
  ctx.strokeStyle = Util.rgbaToString([0,0,0], 0.5);
  ctx.lineWidth = 0.5;
  ctx.beginPath();
  ctx.moveTo(labelWidth, 0);
  ctx.lineTo(labelWidth, bandHeight);
  ctx.stroke();
  ctx.closePath();

  // paint the ticks
  this.paintTicks();
  // paint the label
  this.paintLabel();

  // paint now
  var now        = this.viewTimeAxis.now;
  var viewStart  = this.viewTimeAxis.start;
  var viewEnd    = this.viewTimeAxis.end;
  if(now !== null && now >= viewStart && now < viewEnd) {
    // paint the now line
    var nowX = this.viewTimeAxis.getXFromTime(now);
    ctx.lineWidth = this.timeCursorWidth || 4;
    ctx.strokeStyle = Util.rgbaToString(this.timeCursorColor || [255,0,0], 0.8);
    ctx.beginPath();
    ctx.moveTo(nowX, 0);
    ctx.lineTo(nowX, bandHeight);
    ctx.stroke();
    ctx.closePath();

    if(this.onFormatNow !== null) {
      ctx.fillStyle = this.timeCursorColor?  Util.rgbaToString(this.timeCursorColor, 0.8) : "red";
      var formattedTimes = this.onFormatNow({timeBand:this, time:now});
      for(var j=0, jlength=formattedTimes.length; j<jlength; ++j) {
        var obj = formattedTimes[j];
        var formattedTime = obj.formattedTime;
        var x = ("x" in obj) ? obj.x : nowX+ctx.lineWidth+1;
        var y = ("y" in obj) ? obj.y : 10*(j+1);
        ctx.fillText(formattedTime, x, y);
      }
    }
  }
};

TimeBand.prototype.mousedown = function(e) {
  if(this.onUpdateView === null) return;

  if(e.which === 1) {
    // enable the draggable component and fire trigger the event
    // so that dragging can begin
    var x = e.pageX - $(e.target).offset().left;
    if(x >= this.viewTimeAxis.x1) {
      $(this._hiddenDragSource).draggable("enable");
      $(this._hiddenDragSource).trigger(e);
    }
  }
  else {
    $(this._hiddenDragSource).draggable("disable");
  }
  return true;
};

TimeBand.prototype.mouseup = function(e) {
  if(e.which === 1) {
    $(this._hiddenDragSource).draggable("disable");
  }
  else if(e.which === 3) {
    if(this.onHideTooltip) { this.onHideTooltip(); }

    var bandX = e.pageX - $(e.target).offset().left;
    this.contextMenu.raise(bandX, e.clientX, e.clientY);
  }
  this.mouseout(e);
};

TimeBand.prototype.mouseout = function(e) {
  if(this.onHideTooltip) {
    this.onHideTooltip();
  }
};

TimeBand.prototype.mousemove = function(e) {
  if(this.onShowTooltip === null) return;

  var x = e.pageX - $(e.target).offset().left;
  var mouseTime = this.viewTimeAxis.getTimeFromX(x);
  var timeZone = this.timeAxis.timeZone;
  var mouseTimeStr = Util.toTimeString(mouseTime, {timeZone:timeZone});

  var tooltipText = "";
  tooltipText += "<table class='tooltiptable'>";
  tooltipText += "<tr><td class='tooltiptablecell'><b>Time:</b></td><td class='tooltiptablecell'>" + mouseTimeStr + "</td></tr>";
  tooltipText += "</table>";
  this.onShowTooltip(e, tooltipText);
};

TimeBand.prototype.resetView = function() {
  if(this.onUpdateView === null) return;
  this.onUpdateView(this.timeAxis.start, this.timeAxis.end);
};

TimeBand.prototype.zoomTo = function(timeUnit) {
  if(this.onUpdateView === null) return;

  var bandX = this.contextMenu.bandX;
  var time = this.viewTimeAxis.getTimeFromX(bandX);
  var start = TimeUnit.quantizeUpByTimeUnit(time, 0, timeUnit);
  var end = TimeUnit.quantizeUpByTimeUnit(time, 1, timeUnit);
  this.onUpdateView(start, end);
};

TimeBand.prototype.handleDragStart = function(e, ui) {
  var x = e.pageX - $(e.target).offset().left;
  this._zoomX1 = x;
};

TimeBand.prototype.handleDrag = function(e, ui) {
  if(this._zoomX1 === null) return;

  var x = this._zoomX1 + e.target.offsetLeft;
  x = Math.max(x, this.viewTimeAxis.x1);
  this._zoomX2 = x;
  this.repaint();
};

TimeBand.prototype.handleDragStop = function(e, ui) {
  if(this._zoomX1 === null || this._zoomX2 === null) return;

  var start, end;
  if(this._zoomX1 < this._zoomX2) {
    start = this.viewTimeAxis.getTimeFromX(this._zoomX1);
    end = this.viewTimeAxis.getTimeFromX(this._zoomX2);
  }
  else {
    start = this.viewTimeAxis.getTimeFromX(this._zoomX2);
    end = this.viewTimeAxis.getTimeFromX(this._zoomX1);
  }
  // clear out the highlight
  this._zoomX1 = null;
  this._zoomX2 = null;

  if(this.onUpdateView) { this.onUpdateView(start, end); }
};

function TimeBandContextMenu(obj) {
  this.timeBand = obj.timeBand;
  this.timeAxis = obj.timeAxis;
  this.viewTimeAxis = obj.viewTimeAxis;

  this.divId = "timeBandContextMenu";
  this.divClassName = "timebandcontextmenudiv";
  this.selector = "." + this.divClassName;

  // create the div that everything is attached to
  this.div = document.createElement("div");
  this.div.setAttribute("id", this.divId);
  this.div.setAttribute("class", this.divClassName);

  this.bandX = null; // x value on the band menu was invoked
  this.menuX = null; // x value on the page menu was invoked
  this.menuY = null; // y value on the page menu was invoked
  this.items = null;
}

TimeBandContextMenu.prototype.raise = function(bandX, menuX, menuY) {
  // raise the context menu: configure, enable, show
  this.bandX = bandX;
  this.menuX = menuX;
  this.menuY = menuY;
  this.configure();
  this.enable();
  this.show();
  return false;
};

TimeBandContextMenu.prototype.enable = function() {
  // enable the right-click listener
  $.contextMenu({
    selector: this.selector,
    callback: this.defaultCallback.bind(this),
    events: {hide: this.beforeHide.bind(this)},
    position: this.position.bind(this),
    items: this.items
  });
};

TimeBandContextMenu.prototype.disable = function() {
  // disable as a right-click listener
  $.contextMenu("destroy", this.selector);
};

TimeBandContextMenu.prototype.show = function() {
  // display the context menu
  $(this.selector).contextMenu();
};

TimeBandContextMenu.prototype.position = function(opt, x, y) {
  // determine contextMenu position
  // the arguments "x" and "y" refer to the div position,
  // which is a dummy; ignore these and use the menuX and
  // menuY which came from the TimeBand
  var offset = {top: this.menuY, left: this.menuX};

  // correct offset if viewport demands it
  var $win = $(window),
  bottom = $win.scrollTop() + $win.height(),
  right = $win.scrollLeft() + $win.width(),
  height = opt.$menu.height(),
  width = opt.$menu.width();

  if (offset.top + height > bottom) {
    offset.top -= height;
  }
  if (offset.left + width > right) {
    offset.left -= width;
  }

  opt.$menu.css(offset);
};

TimeBandContextMenu.prototype.beforeHide = function(opt) {
  this.disable();
};

TimeBandContextMenu.prototype.defaultCallback = function(key, opt) {
  alert("No action specified for: " + key);
};

TimeBandContextMenu.prototype.configure = function() {
  var timeBand = this.timeBand;
  var dur = this.timeAxis.end - this.timeAxis.start;

  var candidateUnits = [];
  if(dur <= TimeUnit.HOUR) {
    candidateUnits.push({unit:TimeUnit.MINUTE,     label:"Minute"  });
    candidateUnits.push({unit:TimeUnit.TEN_MINUTE, label:"10Minute"});
    candidateUnits.push({unit:TimeUnit.HOUR,       label:"Hour"    });
  }
  else if(dur <= TimeUnit.TWELVE_HOUR) {
    candidateUnits.push({unit:TimeUnit.HOUR,        label:"Hour"  });
    candidateUnits.push({unit:TimeUnit.TWO_HOUR,    label:"2Hour" });
    candidateUnits.push({unit:TimeUnit.SIX_HOUR,    label:"6Hour" });
    candidateUnits.push({unit:TimeUnit.EIGHT_HOUR,  label:"6Hour" });
    candidateUnits.push({unit:TimeUnit.TWELVE_HOUR, label:"12Hour"});
  }
  else {
    candidateUnits.push({unit:TimeUnit.TWELVE_HOUR, label:"12Hour"});
    candidateUnits.push({unit:TimeUnit.DAY,         label:"Day"   });
    candidateUnits.push({unit:TimeUnit.THREE_DAY,   label:"3Day"  });
    candidateUnits.push({unit:TimeUnit.WEEK,        label:"Week"  });
    candidateUnits.push({unit:TimeUnit.MONTH,       label:"Month" });
    candidateUnits.push({unit:TimeUnit.THREE_MONTH, label:"3Month"});
    candidateUnits.push({unit:TimeUnit.YEAR,        label:"Year"  });
  }

  this.items = {};
  this.items.home = {name: "Home", callback: timeBand.resetView.bind(timeBand) };
  this.items.sep = "-";

  for(var i=0, ilength=candidateUnits.length; i<ilength; ++i) {
    var unit = candidateUnits[i].unit;
    if(dur >= unit) {
      var label = candidateUnits[i].label;
      this.items[label] = {
        name: label,
        callback: timeBand.zoomTo.bind(timeBand, unit)
      };
    }
  }
};

// the TimeScrollBar
function TimeScrollBar(obj) {
  if(typeof obj === "undefined") return;

  // mandatory
  this.timeAxis = obj.timeAxis;
  this.viewTimeAxis = obj.viewTimeAxis;

  //optional
  this.updateOnDrag = ("updateOnDrag" in obj) ? obj.updateOnDrag : true;
  this.height = ("height" in obj) ? obj.height : 15;
  this.label = ("label" in obj) ? obj.label : null;
  this.font = ("font" in obj) ? obj.font : "bold 11px Verdana";

  // callback functions
  this.onUpdateView = ("onUpdateView" in obj) ? obj.onUpdateView : null;
  this.onFormatTimeTick = ("onFormatTimeTick" in obj) ? obj.onFormatTimeTick : TimeScrollBar.formatTimeTick;

  // create the div that everything is attached to
  this.div = document.createElement("div");
  this.div.setAttribute('class', 'timescrollbardiv');
  this.div.setAttribute('id', 'timescrollbar');

  this.buttonPane = document.createElement("div");
  this.buttonPane.setAttribute("class", "timescrollbarbutton");

  // create the canvas
  this.canvas = document.createElement("canvas");
  this.canvas.setAttribute('class', 'timescrollbarcanvas');

  // add the canvas hook, and click handlers
  $(this.canvas).mousedown(this.mousedown.bind(this));
  $(this.canvas).mouseup(this.mouseup.bind(this));

  this.div.appendChild(this.canvas);
  this.div.appendChild(this.buttonPane);

  $(this.buttonPane).draggable({
    axis: "x",
    drag: this._handleDrag.bind(this),
    stop: this._handleDragStop.bind(this)
  });

  $(this.div).css("height", this.height);
  $(this.buttonPane).css("height", this.height);

  // these parameters are used to scale the canvas if needed, see revalidate
  var ctx = this.canvas.getContext('2d');
  this.devicePixelRatio = window.devicePixelRatio || 1;
  this.backingStoreRatio = ctx.webkitBackingStorePixelRatio ||
    ctx.mozBackingStorePixelRatio ||
    ctx.msBackingStorePixelRatio ||
    ctx.oBackingStorePixelRatio ||
    ctx.backingStorePixelRatio || 1;
}

TimeScrollBar.prototype.updateContainmentArray = function() {
  // since the position for the buttonpane is absolute, we need to account
  // for where the parent div is wrt to the document
  var left = $(this.div).offset().left;
  var xRange = this.timeAxis.x2 - this.timeAxis.x1;

  // containing the y can be ignored so we use 0
  var x1 = this.timeAxis.x1 + left;
  var x2 = x1 + xRange - this.buttonWidth;
  var y1 = 0;
  var y2 = 0;
  $(this.buttonPane).draggable("option", "containment", [x1, y1, x2, y2]);
};

TimeScrollBar.prototype.updateScrollButton = function() {
  var x1 = this.timeAxis.getXFromTime(this.viewTimeAxis.start);
  var x2 = this.timeAxis.getXFromTime(this.viewTimeAxis.end);

  this.buttonWidth = Math.max(x2 - x1, 1);
  $(this.buttonPane).css("left", x1);
  $(this.buttonPane).css("width", this.buttonWidth);
  this.updateContainmentArray();
};

TimeScrollBar.prototype._handleDrag = function(event, ui) {
  if(this.updateOnDrag) {
    this._handleDragStop(event, ui);
  }
};

TimeScrollBar.prototype._handleDragStop = function(event, ui) {
  // since the position for the buttonpane is absolute, we need to account
  // for where the parent div is wrt to the document
  var left = $(this.div).offset().left;

  var x1 = ui.offset.left - left;
  var x2 = x1 + this.buttonWidth;
  var start = this.timeAxis.getTimeFromX(x1);
  var end = this.timeAxis.getTimeFromX(x2);

  if(this.onUpdateView) {
    this.onUpdateView(start, end);
  }
};

TimeScrollBar.prototype.revalidate = function() {
  var width = this.div.offsetWidth;
  var height = this.div.offsetHeight;

  // upscale the canvas if the 2 ratios don't match
  // setting the width/height clears out scaling
  // see https://www.html5rocks.com/en/tutorials/canvas/hidpi
  if(this.devicePixelRatio !== this.backingStoreRatio) {
    var ratio = this.devicePixelRatio / this.backingStoreRatio;
    this.canvas.width = width * ratio;
    this.canvas.height = height * ratio;
    this.canvas.style.width = width + 'px';
    this.canvas.style.height = height + 'px';
    // now scale the context to counter the fact that we've
    // manually scaled our canvas element
    var ctx = this.canvas.getContext('2d');
    ctx.scale(ratio, ratio);
  }
  else {
    this.canvas.width = width;
    this.canvas.height = height;
  }
};

TimeScrollBar.formatTimeTick = function(obj) {
  var time = obj.time;
  var timeScrollBar = obj.timeScrollBar;
  var timeZone = timeScrollBar.timeAxis.timeZone;
  var tickUnit = timeScrollBar.timeAxis.tickUnit;

  var mom = moment.utc(time*1000).tz(timeZone);
  var doy = mom.format("DDDD");
  var includeYear = false;
  var includeDoy = true;
  if(tickUnit === TimeUnit.YEAR) {
    includeYear = true;
    includeDoy = false;
  }
  else if(doy === "001") {
    includeYear = true;
  }
  var str = "";
  if(includeYear && includeDoy) {
    str += mom.format("YYYY-DDDD");
  }
  else if(includeDoy) {
    str += mom.format("DDDD");
  }
  // pad some of the timestring parameters
  if(tickUnit < TimeUnit.TWELVE_HOUR) {
    str += " " + mom.format("HH:mm:ss");
  }
  return [{formattedTime:str}];
};

TimeScrollBar.prototype.paintTicks = function(ctx) {
  var start = this.timeAxis.start;
  var end = this.timeAxis.end;

  var tickTimes = this.timeAxis.tickTimes;
  for(var i=0, length=tickTimes.length; i<length; ++i) {
    var time = tickTimes[i];
    if(time < start || time > end) continue;
    var timeX = this.timeAxis.getXFromTime(time);

    ctx.beginPath();
    ctx.moveTo(timeX, 0);
    ctx.lineTo(timeX, this.height);
    ctx.stroke();
    ctx.closePath();

    // compute the date/time string
    var formattedTimes = this.onFormatTimeTick({timeScrollBar:this, time:time});
    for(var j=0, jlength=formattedTimes.length; j<jlength; ++j) {
      var obj = formattedTimes[j];
      var formattedTime = obj.formattedTime;
      var y = ("y" in obj) ? obj.y : (this.height/2*(j+1)+1);
      ctx.fillText(formattedTime, timeX+2, y);
    }
  }
};

// paint the ticks and date
TimeScrollBar.prototype.repaint = function() {
  // clear out the previous painting
  // don't use the canvas height/width since its may be scaled
  // see revalidate for when scaling occurs
  var width = this.div.offsetWidth;
  var height = this.div.offsetHeight;
  var ctx = this.canvas.getContext('2d');
  ctx.clearRect(0, 0, width, height);

  ctx.font = this.font;
  ctx.textBaseline = "middle";
  ctx.fillStyle = "black";
  ctx.lineWidth = 0.5;
  ctx.strokeStyle = Util.rgbaToString([0,0,0], 0.5);

  // draw a border around the scrollbar
  ctx.strokeRect(0, 0, width, height);
  // draw a border around the label area
  ctx.strokeRect(0, 0, this.timeAxis.x1, height);

  // paint the label
  if(this.label !== null) {
    var textWidth = ctx.measureText(this.label).width;
    var x = this.timeAxis.x1/2 - textWidth/2;
    var y = this.height/2 + 2;
    ctx.fillText(this.label, x, y);
  }

  // paint the ticks
  this.paintTicks(ctx);
  this.updateScrollButton();
};

TimeScrollBar.prototype.mousedown = function(e) {
  if(this.onUpdateView === null) return;

  if(e.which === 1) {
    // left click
    var mouseX = e.pageX - $(e.target).offset().left;
    var mouseTime = this.timeAxis.getTimeFromX(mouseX);
    var timeDelta = this.timeAxis.getTimeFromX(this.buttonWidth) - this.timeAxis.getTimeFromX(0);

    var viewStart = this.viewTimeAxis.start;
    var viewEnd = this.viewTimeAxis.end;
    var minStart = this.timeAxis.start;
    var maxEnd = this.timeAxis.end;

    var newViewStart, newViewEnd;
    if(mouseTime < viewStart && mouseTime >= minStart) {
      // scroll left
      newViewStart = Math.max(minStart, viewStart - timeDelta);
      newViewEnd = viewEnd - (viewStart - newViewStart);
      this.onUpdateView(newViewStart, newViewEnd);
    }
    else if(mouseTime > viewEnd && mouseTime <= maxEnd) {
      // scroll right
      newViewEnd = Math.min(maxEnd, viewEnd + timeDelta);
      newViewStart = viewStart + (newViewEnd - viewEnd);
      this.onUpdateView(newViewStart, newViewEnd);
    }
  }
  return true;
};

TimeScrollBar.prototype.mouseup = function(e) {
};

function TimeUnit() {}

// mostly these are used as an enum
// but in some cases can be used as seconds per foo
// (don't use, for example, for as a number of seconds per
// month or year because month days may not be 30
// and it may be a leap year)
TimeUnit.SECOND      = 1;
TimeUnit.TEN_SECOND  = 10;
TimeUnit.MINUTE      = 60;
TimeUnit.TEN_MINUTE  = 600;
TimeUnit.HOUR        = 3600;
TimeUnit.TWO_HOUR    = 7200;
TimeUnit.SIX_HOUR    = 21600;
TimeUnit.EIGHT_HOUR  = 28800;
TimeUnit.TWELVE_HOUR = 43200;
TimeUnit.DAY         = 86400;
TimeUnit.TWO_DAY     = 172800;
TimeUnit.THREE_DAY   = 259200;
TimeUnit.FIVE_DAY    = 432000;
TimeUnit.WEEK        = 604800;
TimeUnit.TEN_DAY     = 864000;
TimeUnit.FOUR_WEEK   = 2419200;
TimeUnit.MONTH       = 2592000;   // 30 days
TimeUnit.THREE_MONTH = 7776000;   // 90 days
TimeUnit.YEAR        = 31536000;  // 365 days

TimeUnit.ALL_UNITS = [TimeUnit.SECOND,
          TimeUnit.TEN_SECOND,
          TimeUnit.MINUTE,
          TimeUnit.TEN_MINUTE,
          TimeUnit.HOUR,
          TimeUnit.TWO_HOUR,
          TimeUnit.SIX_HOUR,
          TimeUnit.EIGHT_HOUR,
          TimeUnit.TWELVE_HOUR,
          TimeUnit.DAY,
          TimeUnit.TWO_DAY,
          TimeUnit.THREE_DAY,
          TimeUnit.FIVE_DAY,
          TimeUnit.WEEK,
          TimeUnit.TEN_DAY,
          TimeUnit.FOUR_WEEK,
          TimeUnit.MONTH,
          TimeUnit.THREE_MONTH,
          TimeUnit.YEAR];

TimeUnit.toString = function(timeUnit) {
  switch(timeUnit) {
  case TimeUnit.SECOND:
  return "second";
  case TimeUnit.TEN_SECOND:
  return "10second";
  case TimeUnit.MINUTE:
  return "minute";
  case TimeUnit.TEN_MINUTE:
  return "10minute";
  case TimeUnit.HOUR:
  return "hour";
  case TimeUnit.TWO_HOUR:
  return "2hour";
  case TimeUnit.SIX_HOUR:
  return "6hour";
  case TimeUnit.EIGHT_HOUR:
  return "8hour";
  case TimeUnit.TWELVE_HOUR:
  return "12hour";
  case TimeUnit.DAY:
  return "day";
  case TimeUnit.TWO_DAY:
  return "2day";
  case TimeUnit.THREE_DAY:
  return "3day";
  case TimeUnit.FIVE_DAY:
  return "5day";
  case TimeUnit.WEEK:
  return "week";
  case TimeUnit.TEN_DAY:
  return "10day";
  case TimeUnit.FOUR_WEEK:
  return "4week";
  case TimeUnit.MONTH:
  return "month";
  case TimeUnit.THREE_MONTH:
  return "3month";
  case TimeUnit.YEAR:
  return "year";
  }
  return null;
};

TimeUnit.fromString = function(timeUnitStr) {
  if(timeUnitStr =="second")
    return TimeUnit.SECOND;
  else if(timeUnitStr =="10second")
    return TimeUnit.TEN_SECOND;
  else if(timeUnitStr =="minute")
    return TimeUnit.MINUTE;
  else if(timeUnitStr =="10minute")
    return TimeUnit.TEN_MINUTE;
  else if(timeUnitStr =="hour")
    return TimeUnit.HOUR;
  else if(timeUnitStr =="2hour")
    return TimeUnit.TWO_HOUR;
  else if(timeUnitStr =="6hour")
    return TimeUnit.SIX_HOUR;
  else if(timeUnitStr =="8hour")
    return TimeUnit.EIGHT_HOUR;
  else if(timeUnitStr =="12hour")
    return TimeUnit.TWELVE_HOUR;
  else if(timeUnitStr =="day")
    return TimeUnit.DAY;
  else if(timeUnitStr =="2day")
    return TimeUnit.TWO_DAY;
  else if(timeUnitStr =="3day")
    return TimeUnit.THREE_DAY;
  else if(timeUnitStr =="5day")
    return TimeUnit.FIVE_DAY;
  else if(timeUnitStr =="week")
    return TimeUnit.WEEK;
  else if(timeUnitStr =="10day")
    return TimeUnit.TEN_DAY;
  else if(timeUnitStr =="4week")
    return TimeUnit.FOUR_WEEK;
  else if(timeUnitStr =="month")
    return TimeUnit.MONTH;
  else if(timeUnitStr =="3month")
    return TimeUnit.THREE_MONTH;
  else if(timeUnitStr =="year")
    return TimeUnit.YEAR;
  return null;
};

TimeUnit.quantizeUpByTimeUnit = function(time, quantum, timeUnit) {
  // TODO PERF this is general, but not as effecient
  // as just adding (and moduloing) integers
  var d1, d2, rem, diff;
  d1 = new Date(time * 1000);
  d2 = new Date(d1);
  if(timeUnit == TimeUnit.SECOND) {
    d2.setUTCSeconds(d2.getUTCSeconds() + quantum);
  }
  else if(timeUnit == TimeUnit.TEN_SECOND) {
    rem = d2.getUTCSeconds() % 10;
    d2.setUTCSeconds(d2.getUTCSeconds() + (quantum * 10) - rem);
  }
  else if(timeUnit == TimeUnit.MINUTE) {
    d2.setUTCMinutes(d2.getUTCMinutes() + quantum);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.TEN_MINUTE) {
    rem = d2.getUTCMinutes() % 10;
    d2.setUTCMinutes(d2.getUTCMinutes() + (quantum * 10) - rem);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.HOUR) {
    d2.setUTCHours(d2.getUTCHours() + quantum);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.TWO_HOUR) {
    rem = d2.getUTCHours() % 2;
    d2.setUTCHours(d2.getUTCHours() + (quantum * 2) - rem);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.SIX_HOUR) {
    rem = d2.getUTCHours() % 6;
    d2.setUTCHours(d2.getUTCHours() + (quantum * 6) - rem);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.EIGHT_HOUR) {
    rem = d2.getUTCHours() % 8;
    d2.setUTCHours(d2.getUTCHours() + (quantum * 8) - rem);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.TWELVE_HOUR) {
    rem = d2.getUTCHours() % 12;
    d2.setUTCHours(d2.getUTCHours() + (quantum * 12) - rem);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.DAY) {
    d2.setUTCDate(d2.getUTCDate() + quantum);
    d2.setUTCHours(0);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.TWO_DAY) {
    d2.setUTCDate(d2.getUTCDate() + (quantum * 2));
    d2.setUTCHours(0);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.THREE_DAY) {
    d2.setUTCDate(d2.getUTCDate() + (quantum * 3));
    d2.setUTCHours(0);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.FIVE_DAY) {
    d2.setUTCDate(d2.getUTCDate() + (quantum * 5));
    d2.setUTCHours(0);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.WEEK) {
    diff = (quantum * 7) - ((d2.getUTCDay() + 6) % 7);
    d2.setUTCDate(d2.getUTCDate() + diff);
    d2.setUTCHours(0);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.TEN_DAY) {
    d2.setUTCDate(d2.getUTCDate() + (quantum * 10));
    d2.setUTCHours(0);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.FOUR_WEEK) {
    diff = (quantum * 7*4) - ((d2.getUTCDay() + 6) % 7);
    d2.setUTCDate(d2.getUTCDate() + diff);
    d2.setUTCHours(0);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.MONTH) {
    d2.setUTCMonth(d2.getUTCMonth() + quantum);
    d2.setUTCHours(0);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.THREE_MONTH) {
    rem = d2.getUTCMonth() % 3;
    d2.setUTCMonth(d2.getUTCMonth() + (quantum * 3) - rem);
    d2.setUTCHours(0);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  else if(timeUnit == TimeUnit.YEAR) {
    d2.setUTCFullYear(d2.getUTCFullYear() + quantum);
    d2.setUTCHours(0);
    d2.setUTCMinutes(0);
    d2.setUTCSeconds(0);
  }
  diff = (d2.getTime() - d1.getTime()) / 1000;
  //console.log("d1 = " + d1);
  //console.log("d2 = " + d2);
  //console.log("diff = " + diff);
  return time + diff;
};

TimeUnit.computeTickTimeUnit = function(dur, maxTicks, timeUnits) {
  var secsPerTick = dur / maxTicks;

  if(timeUnits === null) {
    timeUnits = TimeUnit.ALL_UNITS;
  }
  for(var i=0, length=timeUnits.length; i<length; ++i) {
    var timeUnit = timeUnits[i];
    if((secsPerTick / timeUnit) <= 1) {
      return timeUnit;
    }
  }
  return TimeUnit.YEAR;
};

TimeUnit.getBestFitTimeUnit = function(dur) {
  if(dur <= TimeUnit.TEN_SECOND)
    return TimeUnit.TEN_SECOND;
  else if(dur <= TimeUnit.MINUTE)
    return TimeUnit.MINUTE;
  else if(dur <= TimeUnit.TEN_MINUTE)
    return TimeUnit.TEN_MINUTE;
  else if(dur <= TimeUnit.HOUR)
    return TimeUnit.HOUR;
  else if(dur <= TimeUnit.TWO_HOUR)
    return TimeUnit.TWO_HOUR;
  else if(dur <= TimeUnit.SIX_HOUR)
    return TimeUnit.SIX_HOUR;
  else if(dur <= TimeUnit.EIGHT_HOUR)
    return TimeUnit.EIGHT_HOUR;
  else if(dur <= TimeUnit.TWELVE_HOUR)
    return TimeUnit.TWELVE_HOUR;
  else if(dur <= TimeUnit.DAY)
    return TimeUnit.DAY;
  else if(dur <= TimeUnit.TWO_DAY)
    return TimeUnit.TWO_DAY;
  else if(dur <= TimeUnit.THREE_DAY)
    return TimeUnit.THREE_DAY;
  else if(dur <= TimeUnit.FIVE_DAY)
    return TimeUnit.FIVE_DAY;
  else if(dur <= TimeUnit.WEEK)
    return TimeUnit.WEEK;
  else if(dur <= TimeUnit.TEN_DAY)
    return TimeUnit.TEN_DAY;
  else if(dur <= TimeUnit.FOUR_WEEK)
    return TimeUnit.FOUR_WEEK;
  else if(dur <= TimeUnit.MONTH)
    return TimeUnit.MONTH;
  else if(dur <= TimeUnit.THREE_MONTH)
    return TimeUnit.THREE_MONTH;
  return TimeUnit.YEAR;
};

function Timer() {
  this.start = new Date().getTime();
}

Timer.prototype.reset = function() {
  this.start = new Date().getTime();
};

Timer.prototype.getDuration = function() {
  var end = new Date().getTime();
  var durms = end - this.start;
  var durs = durms / 1000;
  return durms + "ms " + durs + "s";
};

function Tooltip(obj) {
  if(obj === 'undefined') obj = {};

  // create the div that shows the tooltip
  this.div = document.createElement("div");
  this.div.setAttribute('class', 'tooltipdiv');

  this.shift = ("shift" in obj) ? obj.shift : {x:15, y:15};
}

Tooltip.prototype.show = function(text, x, y) {
  this.div.innerHTML = text;

  var offset = {top: y + this.shift.y, left: x + this.shift.x};
  // correct offset if viewport demands it
  var $win = $(window),
  bottom = $win.scrollTop() + $win.height(),
  right = $win.scrollLeft() + $win.width(),
  height = $(this.div).outerHeight(true),
  width = $(this.div).outerWidth(true);

  if(offset.top + height > bottom) {
    offset.top -= height + (this.shift.y*2);
    if(offset.top < 0) { offset.top = 0; }
  }

  if(offset.left + width > right) {
    offset.left -= width + (this.shift.x*2);
    if(offset.left < 0) { offset.left = 0; }
  }

  $(this.div).css(offset);
  $(this.div).show();
};

Tooltip.prototype.hide = function() {
  $(this.div).hide();
};

var Util = {
  quantizeUp: function(time, quantum) {
    var remainder = time % quantum;
    if(remainder === 0) return time;
    return time + quantum - remainder;
  },

  quantizeDown: function(time, quantum) {
    var remainder = time % quantum;
    if(remainder === 0) return time;
    return time - remainder;
  },

  zeroPad: function(num, count) {
    var str = num + "";
    while(str.length < count) {
      str = "0" + str;
    }
    return str;
  },

  spacePad: function(str, count) {
    if(str === null) str = "";
    while(str.length < count) {
      str += " ";
    }
    return str;
  },

  escapeXML: function(str){
    var XML_CHAR_MAP = {
      '<': '&lt;',
      '>': '&gt;',
      '&': '&amp;',
      '"': '&quot;',
      "'": '&apos;'
    };
    return str.replace(/[<>&"']/g, function (ch) {
      return XML_CHAR_MAP[ch];
    });
  },

  getDOY: function(date) {
    var d = Date.UTC(date.getUTCFullYear(), 0, 0);
    return Math.floor((date.getTime()-d)/8.64e+7);
  },

  // from jquery.datepicker.iso8601Week(date);
  // iso8601Week uses local time function calls.  Modified to use UTC calls instead
  getWeek: function(date) {
    var checkDate = new Date(date.getTime());
    // Find Thursday of this week starting on Monday
    checkDate.setUTCDate(checkDate.getUTCDate() + 4 - (checkDate.getUTCDay() || 7));
    var time = checkDate.getTime();
    checkDate.setUTCMonth(0); // Compare with Jan 1
    checkDate.setUTCDate(1);
    return Math.floor(Math.round((time - checkDate.getTime()) / 8.64e+7) / 7) + 1;
  },

  toDHMString: function(duration) {
    if(duration === 0) { return "0m"; }

    var negative = duration < 0;
    if(negative) {
      duration = Math.abs(duration);
    }

    var days = 0;
    var hrs  = 0;
    var mins = 0;
    var secs = 0;

    var remainder = duration;
    if(remainder !== 0) {
      days = Math.floor(remainder / (24*60*60));
      remainder %= (24*60*60);
    }
    if(remainder !== 0) {
      hrs = Math.floor(remainder / (60*60));
      remainder %= (60*60);
    }
    if(remainder !== 0) {
      mins = Math.floor(remainder / 60);
      remainder %= 60;
    }
    secs = Math.floor(remainder);

    var durationStr = negative ? '-' : '';
    if(days !== 0) {
      durationStr += days + 'd';
    }
    if(hrs !== 0) {
      durationStr += hrs + 'h';
    }
    if(mins !== 0) {
      durationStr += mins + 'm';
    }
    if(secs !== 0) {
      durationStr += secs + 's';
    }
    return durationStr;
  },

  fromDHMString: function(durStr) {
    if(durStr === null) return null;

    var re = /^((\d+)d)?((\d+)h)?((\d+)m)?((\d+)s)?$/;
    var results = durStr.match(re);
    if(results === null) { return null; }

    var days  = 0;
    var hours = 0;
    var mins  = 0;
    var secs  = 0;
    if(results[2] !== undefined) {
      days = parseInt(results[1], 10);
    }
    if(results[4] !== undefined) {
      hours = parseInt(results[3], 10);
    }
    if(results[6] !== undefined) {
      mins = parseInt(results[5], 10);
    }
    if(results[8] !== undefined) {
      secs = parseInt(results[7], 10);
    }

    return days*24*3600 + hours*3600 + mins*60 + secs;
  },

  durationToHourMinuteString: function(duration) {
    var hrs  = 0;
    var mins = 0;

    var remainder = duration;
    if(remainder !== 0) {
      hrs = Math.floor(remainder / (60*60));
      remainder %= (60*60);
    }
    if(remainder !== 0) {
      mins = Math.floor(remainder / 60);
      remainder %= 60;
    }

    return hrs + "" + Util.zeroPad(mins,2);
  },

  hourMinuteStringToDuration: function(durationStr) {
    var hrs = 0;
    var mins = 0;

    if(durationStr.length <= 2) {
      mins = parseInt(durationStr, 10);
    }
    else {
      var minStr = durationStr.substr(durationStr.length - 2, 2);
      var hourStr = durationStr.substr(0, durationStr.length - 2);
      hrs = parseInt(hourStr, 10);
      mins = parseInt(minStr, 10);
    }

    return hrs*3600 + mins*60;
  },

  // See https://github.com/jsmreese/moment-duration-format for format specs
  toDurationString: function(dur, options) {
    if(!options) { options = {}; }
    var format = ("format" in options) ? options.format : "HH:mm:ss";
    var trim = ("trim" in options) ? options.trim : false;
    return moment.duration(dur, "seconds").format(format, {trim:trim});
  },

  fromDurationString: function(durStr, options) {
    // is this safe?
    // we're using the moment time library in order to compute the duration
    if(!options) { options = {}; }
    //var format = ("format" in options) ? options.format : "HH:mm:ss";
    //var trim = ("trim" in options) ? options.trim : false;
    return moment.duration(durStr).as("seconds");
  },

  // See http://momentjs.com/docs/#/displaying for format specs
  toTimeString: function(utcTime, options) {
    if(!options) { options = {}; }
    var format = ("format" in options) ? options.format : "YYYY-MM-DD (DDDD) HH:mm:ss";
    var timeZone = ("timeZone" in options) ? options.timeZone : "UTC";

    var m = moment.utc(utcTime*1000).tz(timeZone);
    return m.format(format);
  },

  // See http://momentjs.com/docs/#/displaying for format specs
  fromTimeString: function(timeStr, options) {
    if(!options) { options = {}; }
    var format = ("format" in options) ? options.format : "YYYY-MM-DD (DDDD) HH:mm:ss";
    var timeZone = ("timeZone" in options) ? options.timeZone : "UTC";

    var m = moment.tz(timeStr, format, true, timeZone);
    if(m.isValid()) {
      return parseInt(m.format("X"), 10);
    }
    return null;
  },

  toTimeRangeString: function(start, end, options) {
    var startStr = Util.toTimeString(start, options);
    var endStr   = Util.toTimeString(end, options);
    var durStr   = Util.toDHMString(end - start);
    return startStr+' - '+endStr+' ('+durStr+')';
  },

  // RAVEN -- DOY format
  toDOYDate: function(date){
    var dateOb = new Date(date*1000);
    var doy = Util.zeroPad(Util.getDOY(dateOb), 3);
    var hour = dateOb.getUTCHours();
    var min = dateOb.getUTCMinutes();
    var sec = dateOb.getUTCSeconds();
    return dateOb.getUTCFullYear() +"-" + doy + "T" + Util.zeroPad(hour, 2) + ":" + Util.zeroPad(min, 2) + ":" + Util.zeroPad(sec, 2);
  },

  trimToWidth: function(text, width, cxt) {
    width = Math.max(width, 0);
    // Trim based on the width ratio
    // TODO this is not perfect for variable length fonts
    var trimmed = text.toString();
    var textWidth = cxt.measureText(text).width;
    if(textWidth > width) {
      var newLength = Math.floor(trimmed.length * (width / textWidth));
      trimmed = trimmed.substring(0, newLength - 2);
      if (trimmed.length > 2) {
        // replace last two chars with '..'
        trimmed = trimmed.substring(0, trimmed.length -2)+'..';
      }
      else
          trimmed = '';
    }
    return trimmed;
  },

  // Flanagan, David (2011-04-25). JavaScript: The Definitive Guide (Definitive Guides) (p. 195).
  // OReilly Media - A. Kindle Edition.
  // A utility function to convert an array-like object (or suffix of it)
  // to a true array. Used below to convert arguments objects to real arrays.
  array: function(a, n) { return Array.prototype.slice.call(a, n || 0); },

  // Flanagan, David (2011-04-25). JavaScript: The Definitive Guide (Definitive Guides) (p. 195).
  // OReilly Media - A. Kindle Edition.
  // The arguments to this function serve as a template. Undefined values
  // in the argument list are filled in with values from the inner set.
  partial: function(f /*, ... */) {
    var args = arguments;
    // Save the outer arguments array
    return function() {
      var a = Util.array(args, 1); // Start with an array of outer args
      var i=0, j=0; // Loop through those args, filling in undefined values from inner
      for(; i < a.length; i++)
        if (a[i] === undefined)
          a[i] = arguments[j++]; // Now append any remaining inner arguments
      a = a.concat(Util.array(arguments, j));
      return f.apply(this, a);
    };
  },

  lowerBoundHelper: function(items, item, comparator, firstIndex, lastIndex) {
    var middleIndex, middleItem;
    middleIndex = firstIndex + Math.floor((lastIndex - firstIndex) / 2);
    if(middleIndex == firstIndex) {
      return firstIndex;
    }
    if(middleIndex == lastIndex) {
      return lastIndex;
    }
    middleItem = items[middleIndex];
    if(comparator(middleItem, item) < 0) {
      // the item is in the upper half
      if(middleIndex >= lastIndex || comparator(items[middleIndex + 1], item) > 0) {
        // the middle item and the one after the middle bound the given item
        return middleIndex;
      }
      else {
        // recurse on the upper half of the array
        return Util.lowerBoundHelper(items, item, comparator, middleIndex, lastIndex);
      }
    }
    else if(comparator(middleItem, item) > 0) {
      // the item is in the lower half
      if(middleIndex <= firstIndex || comparator(items[middleIndex - 1], item) < 0) {
        // the middle item and the one before the middle bound the given item
        return middleIndex - 1;
      }
      else {
        // recurse on the lower half of the array
        return Util.lowerBoundHelper(items, item, comparator, firstIndex, middleIndex);
      }
    }
    else {
      // found an equal item - walk back to the first equal item
      while(middleIndex > firstIndex &&
            comparator(items[middleIndex - 1], middleItem) === 0) {
        middleIndex = middleIndex - 1;
      }
      return middleIndex;
    }
  },

  lowerBound: function(items, item, comparator) {
    if(items.length === 0) {
      return -1;
    }
    var pos = Util.lowerBoundHelper(items, item, comparator, 0, items.length - 1);
    return pos;
  },

  rgbaToString: function(color, opacity) {
    opacity = !!color[3] ? color[3] : opacity;
    return "rgba(" + color[0] + "," + color[1] + "," + color[2] + "," + opacity +")";
  },

  computePercent: function(val, min, max) {
    return Math.round(100.0 * ((val - min) / (max - min)));
  },

  interpolateY3: function(x1, y1, x2, y2, x3) {
    if(x1 === x2) {
      return y1;
    }
    else {
      return y2 - ((x2 - x3) * ((y2 - y1) / (x2 - x1)));
    }
  },

  exportToFile: function(filename, text) {
    var a = document.createElement("a");
    a.setAttribute("href", "data:text/plain;charset=utf-9," + encodeURIComponent(text));
    a.setAttribute("download", filename);
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  },

  // Fisher-Yates shuffle algorithm
  shuffleArray: function(myArray) {
    var i = myArray.length;
    if ( i === 0 ) return;
    while ( --i ) {
      var j = Math.floor( Math.random() * ( i + 1 ) );
      var tempi = myArray[i];
      var tempj = myArray[j];
      myArray[i] = tempj;
      myArray[j] = tempi;
    }
  }
};

//# sourceMappingURL=ctl.js.map
