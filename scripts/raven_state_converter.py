#!/usr/bin/env python3 -tt
"""
The purpose of this script is given a state that was saved in raven pre-34.8.0
and convert that state to a state that is compatible with Raven2 or 34.8.0 and
beyond.
"""

# Imports
import sys
import os
import json
import time
import urllib.parse as urlparse
import re
from datetime import datetime
import calendar


def main():
    args = sys.argv[1:]

    if not args:
        print('usage: [--flags options] [inputs] ')
        return 1

    # Read in old state
    with open(sys.argv[1]) as f:
        raven_one_state = json.load(f)

    raven_two_state = {
        "bands": convert_raven_bands(raven_one_state[0]),
        "defaultBandSettings": get_default_band_settings(raven_one_state[0]["viewTemplate"]),
        # "guides": get_guides(raven_one_state),
        "maxTimeRange": convert_time_range(raven_one_state[0]["viewTemplate"]),
        "name": raven_one_state[0]["name"],
        "pins": get_pins(raven_one_state),
        "version": "1.0.0",
        "viewTimeRange": convert_time_range(raven_one_state[0]["viewTemplate"]),
    }

    print(json.dumps(raven_two_state, indent=4, sort_keys=True))

def convert_raven_bands(raven_one_state):
    band_list = create_list_of_overlay_labels(raven_one_state["viewTemplate"]["charts"])
    new_state = map_fields_to_new_state_field(raven_one_state, band_list)
    add_overlays_to_state(new_state["bands"], new_state["unoverlaid_bands"])

    return flatten_singular_bands(new_state["bands"])

def flatten_singular_bands(bands):
    def flatten_band(band):
        if len(band["subBands"]) != 1:
            return band
        else:
            flattened_band = dict(band["subBands"][0])
            flattened_band.update({
                "containerId": band["containerId"],
                "sortOrder": band["sortOrder"],
            })
            return flattened_band

    return [flatten_band(band) for band in bands]

def convert_time_range(viewTemplate):
    return {
        "start": float(t_time_to_epoch(viewTemplate["viewStart"])),
        "end": float(t_time_to_epoch(viewTemplate["viewEnd"])),
    }

def tab_source_to_pin(tab_source):
    # print("[DEBUG] " + tab_source["tabName"])
    if "fs" in str(tab_source["url"]):
        source_id = tab_source["url"].split("fs")[1].split("/",1)[1]
    elif "list" in str(tab_source["url"]):
        source_id = tab_source["url"].split("list")[1].split("/",1)[1]

    return {
        "name": tab_source["tabName"],
        "sourceId": source_id,
    }

def get_pins(data):
    return [
        tab_source_to_pin(tab_source)
        for tab_source in data[0]["tabSources"]
        if "home" not in tab_source
    ]

def get_guides(data):
    return [
        t_time_to_epoch(guide)
        for guide in data[0]["viewTemplate"].get("guides", [])
    ]

def traverse_source_tree(data, predicate, prefix="/"):
    for source in data["sources"]:
        if predicate(source):
            yield prefix, source
        yield from traverse_source_tree(source, predicate, prefix + source["name"] + "/")


def source_by_name(*, original_name, label_name):
    def predicate(source):
        return source["name"] == original_name and source["graphSettings"][0]["label"] == label_name
    return predicate

def find_tree_leaf(data, original_name, label_name):
    pred = source_by_name(original_name=original_name, label_name=label_name)
    for prefix, _source in traverse_source_tree(data, pred):
        return prefix + original_name

def get_leaf(data, original_name, label_name):
    pred = source_by_name(original_name=original_name, label_name=label_name)
    for _prefix, source in traverse_source_tree(data, pred):
        return source

def get_data_from_leaf(data, original_name, label_name, key):
    source = get_leaf(data, original_name, label_name)
    if source:
        return source["graphSettings"][0][key]



def create_list_of_overlay_labels(charts):
    sources = charts["center"] + charts["south"]

    overlay_band_names = [
        source["overlayBand"]
        for source in sources
        if "overlayBand" in source
    ]

    # Remove duplicates by round-tripping through a set
    return list(set(overlay_band_names))

def map_fields_to_new_state_field(raven_one_state, overlay_band_names):
# Figure out what type of source and sourceID from original url
    CHARTS = [
        ("0", "center"),  # Main Panel
        ("1", "south"),   # South Panel
    ]

    new_state = {
        "bands": [],
        "unoverlaid_bands": [],
    }

    for (containerId, panelId) in CHARTS:
        for source in raven_one_state["viewTemplate"]["charts"][panelId]:
            type_of_source, by_type = determine_type_of_source(source)

            # Account for TOL activities by type and PEF Sequence Execution Tree
            if type_of_source == "divider":
                source_ids = []
            elif by_type:
                source_ids = [find_tree_leaf(raven_one_state, source["originalName"], source["originalName"])]
            elif type_of_source == "Sequence-Tracker":
                source_ids = [find_tree_leaf(raven_one_state, "Sequence-Tracker", source["name"])]
            else:
                source_ids = [find_tree_leaf(raven_one_state, source["originalName"], source["label"])]

            band = {
                "sourceIds": source_ids,
                "type": type_of_source.lower() if type_of_source else None,
            }

            # Primary data handling
            map_elements_of_state(band, source, raven_one_state)

            if "overlayBand" not in source:
                # Top-level band
                new_state["bands"].append(create_wrapper_band(band, containerId=containerId))
            else:
                # Overlay band
                band["overlayBand"] = overlay_no_units(source)
                new_state["unoverlaid_bands"].append(band)

    return new_state

def add_overlays_to_state(bands, unoverlaid_bands):
    # Add each unoverlaid band to its associated parent
    for unoverlaid_band in unoverlaid_bands:
        overlayBandName = unoverlaid_band["overlayBand"]
        del unoverlaid_band["overlayBand"]

        for band in bands:
            if band["name"] == overlayBandName:
                # This band is the unoverlaid band's parent
                # print("[DEBUG] " + json.dumps(unoverlaid_band))
                band["subBands"].append(unoverlaid_band)

def get_default_band_settings(raven_one_state):
    return {
        "activityLayout": 0,
        "icon": "circle",
        "iconEnabled": False,
        "labelFont": "Georgia",
        "labelFontSize": raven_one_state.get("globalLabelFontSize", 9),
        "labelWidth": raven_one_state.get("bandLabelWidth", 150),
        "resourceColor": "#000000",
        "resourceFillColor": "#000000",
        "showTimeCursor": False,
        "showTooltip": raven_one_state.get("tooltipEnabled", True),
    }

def parse_data_source_url(url_str):
    BAND_TYPE_REGEX = r"/api/v2/(?P<file_type>[^_]+)_(?P<band_type>[^-]+)-(?P<db>[^/]+)/(?P<path>.+)"

    url = urlparse.urlparse(url_str)
    url_path = url.path
    url_qs = urlparse.parse_qs(url.query)

    match = re.search(BAND_TYPE_REGEX, url_path)

    return {
        "file_type": match.group("file_type"),
        "band_type": match.group("band_type"),
        "source_name": match.group("db"),
        "path": "/" + match.group("path"),
        "parameters": url_qs,
    }

def determine_type_of_source(source):
    if "url" not in source:
        return "divider", False

    source_info = parse_data_source_url(source["url"])

    # determine source type
    if source["label"] == "Sequence-Tracker":
        return "Sequence-Tracker", False
    elif source_info["band_type"] in ["activities_by_legend"]:
        return "activity", False
    elif source_info["band_type"] in ["resource"]:
        return "resource", False
    elif "legend" in source:
        return "activity", True
    elif "pef" in source_info["file_type"]:
        return "activity", False
    elif "generic" in source_info["file_type"]:
        return "generic", False
    else:
        return "activity", False

def map_elements_of_state(band, source, raven_one_state):
    default_band_settings = get_default_band_settings(raven_one_state)

    if band["type"] == "resource":
        source_info = parse_data_source_url(source["url"])

        if "lineColorCustom" in source["graphSettings"]:
            color = convert_color(source["graphSettings"]["lineColorCustom"])
        else:
            color = default_band_settings["resourceColor"]

        if "fillColorCustom" in source["graphSettings"]:
            fillColor = convert_color(source["graphSettings"]["fillColorCustom"])
        else:
            fillColor = default_band_settings["resourceFillColor"]

        band.update({
            "addTo": False,
            "autoScale": True,
            "color": color,
            "decimate": (source_info["parameters"].get("decimate", ["false"])[0] == "true"),
            "fill": source["graphSettings"].get("fill") or False,  # for states
            "fillColor": fillColor,
            "height": source["graphSettings"].get("height", 100),
            "heightPadding": source["graphSettings"].get("heightPadding", 10),
            "icon": default_band_settings["icon"],
            "interpolation": source["graphSettings"].get("interpolation", "linear"),
            "isDuration": False,  # TODO: (metadata.hasValueType.toLowerCase() === 'duration')
            "isTime": False,      # TODO: (metadata.hasValueType.toLowerCase() === 'time')
            "label": label_no_units(source),
            "labelColor": "#000000",
            "labelFont": default_band_settings["labelFont"],
            "labelPin": source.get("suffix") or "",
            "labelUnit": extract_units(source),
            "logTicks": source["graphSettings"].get("logTicks") or False,
            "maxTimeRange": {
                "start": 0,
                "end": 0,
            },
            "name": source["originalName"],
            "points": [],
            "scientificNotation": source["graphSettings"].get("scientificNotation") or False,
            "showIcon": source["graphSettings"].get("iconEnabled") or False,
            "showLabelPin": bool(source.get("suffix") or ""),
            "showLabelUnit": True,
            "showTooltip": True,
            "tableColumns": [],
        })
    elif band["type"] == "activity":
        # print("[DEBUG] " + "activity")
        ACTIVITY_STYLES = {
            "bar":  "1",
            "icon": "2",
        }
        DEFAULT_ACTIVITY_STYLE = "0"

        leaf = get_leaf(raven_one_state, source["originalName"], source["label"])

        band.update({
            "activityHeight": leaf["graphSettings"][0]["activityHeight"],
            "activityStyle": leaf["graphSettings"][0]["activityLayout"],
            "addTo": False,
            "alignLabel": 3,
            "baselineLabel": 3,
            "borderWidth": 1,
            "filterTarget": None,
            "height": source["graphSettings"].get("height", 50),
            "heightPadding": 10,
            "icon": default_band_settings["icon"],
            "label": label_no_units(source),
            "labelColor": leaf["graphSettings"][0].get("labelColor", [0, 0, 0]),
            "labelFont": default_band_settings["labelFont"],
            "labelPin": source.get("suffix") or "",
            "layout": int(ACTIVITY_STYLES.get(leaf["graphSettings"][0]["style"], DEFAULT_ACTIVITY_STYLE)),
            "legend": source.get("legend", ""),
            "maxTimeRange": {
                "start": 0,
                "end": 0,
            },
            "minorLabels": [source["graphSettings"].get("filter")] if source["graphSettings"].get("filter") else [],
            "name": source.get("legend", ""),
            "points": [],
            "showActivityTimes": leaf["graphSettings"][0].get("showActivityTimes", False),
            "showLabel": True,  # !isMessageTypeActivity(legends[legend][0]),  # Don't show labels for message type activities such as error, warning etc.
            "showLabelPin": bool(source.get("suffix") or ""),
            "showTooltip": True,
            "tableColumns": [],
            "trimLabel": leaf["graphSettings"][0].get("trimLabel", True),
        })
    elif band["type"] == "divider":
        band.update({
            "height": source["graphSettings"].get("height", 100),
            "label": label_no_units(source),
            "labelPin": source.get("suffix") or "",
            "name": source["originalName"],
            "showLabelPin": bool(source.get("suffix") or ""),
            "showIcon": source["graphSettings"]["iconEnabled"],
        })
    elif band["type"] == "state":
        band.update({
            "height": source["graphSettings"].get("height", 100),
            "label": label_no_units(source),
            "labelPin": source.get("suffix") or "",
            "name": source["originalName"],
            "showLabelPin": bool(source.get("suffix") or ""),
            "showIcon": source["graphSettings"]["iconEnabled"],
        })
    else:
        raise Exception("Unknown band type \""+band["type"]+"\"")

def create_wrapper_band(band, containerId="0"):
    return {
        "compositeAutoScale": True,
        "compositeLogTicks": False,
        "compositeScientificNotation": False,
        "compositeYAxisLabel": False,
        "containerId": containerId,
        "height": band["height"],
        "heightPadding": band["heightPadding"],
        "name": band["name"],
        "overlay": False,
        "showTooltip": band["showTooltip"],
        "sortOrder": 0,
        "subBands": [band],
        "type": "composite",
    }

def extract_units(source):
    label_string = source["label"]
    if label_string.endswith(')'):
        units = source["label"][:source["label"].rfind(")")].rsplit("(",1)[1]
    else:
        units = ""
    return units

def label_no_units(source):
    label_string = source["label"]
    if label_string.endswith(')'):
        label = source["label"][:source["label"].rfind(")")].split("(")[0]
        label = label[:label.rfind(" ")]
    else:
        label = source["label"]
    return label

def overlay_no_units(source):
    label_string = source["overlayBand"]
    if label_string.endswith(')') and source["suffix"] != "null":
        label = source["overlayBand"][:source["overlayBand"].rfind(")")].split("(")[0]
        label = label[:label.rfind(" ")]
    else:
        label = source["overlayBand"]
    return label

def t_time_to_epoch(t_time):
    year = t_time.split("-")[0]
    doy = t_time.split("-")[1].split("T")[0]
    hour = int(t_time.split("T")[1].split(":")[0])
    minute = int(t_time.split(":")[1])
    second = int(t_time.split(":")[2].split(".")[0])
    milli = t_time.split(".")[1]

    doy_string = year + ' ' + doy
    mo = int(datetime.strptime(doy_string , '%Y %j').month)
    day = int(datetime.strptime(doy_string , '%Y %j').day)
    time_tuple = [int(year), mo, day, hour, minute, second, 0 , int(doy) , 0]
    epoch_time = time.struct_time(time_tuple)

    return str(calendar.timegm(epoch_time)) + '.' + milli

def convert_color(rgb_color):
    return '#%02x%02x%02x' % tuple(rgb_color)

# Main body
if __name__ == '__main__':
    exit(main() or 0)
