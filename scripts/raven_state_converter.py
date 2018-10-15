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

# Global variables

SOURCES_LIST = []
Source_String = ''

# Function declarations

def main():
    args = sys.argv[1:]

    if not args:
        print('usage: [--flags options] [inputs] ')
        return 1

# Read in old state
    with open(sys.argv[1]) as f:
        raven_one_state = json.load(f)

    band_list = create_list_of_overlay_labels(raven_one_state)

    raven_two_state = {}
    raven_two_state["bands"] = []
    raven_two_state["unoverlaid_bands"] = []
    set_global_settings(raven_one_state[0]["viewTemplate"], raven_two_state)
    raven_two_state["pins"] = get_pins(raven_one_state)
    # raven_two_state["guides"] = get_guides(raven_one_state)
    map_fields_to_new_state_field(raven_one_state, raven_two_state, band_list)
    raven_two_state = add_overlays_to_state(raven_two_state, band_list)
    raven_two_state["name"] = raven_one_state[0]["name"]
    raven_two_state["version"] = "1.0.0"
    del raven_two_state["unoverlaid_bands"]

    print(json.dumps(raven_two_state, indent=4, sort_keys=True))


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

def get_data_from_leaf(data, original_name, label_name, key):
    pred = source_by_name(original_name=original_name, label_name=label_name)
    for _prefix, source in traverse_source_tree(data, pred):
        return source["graphSettings"][0][key]


def create_list_of_overlay_labels(raven_one_state):
    charts = raven_one_state[0]["viewTemplate"]["charts"]
    sources = charts["center"] + charts["south"]

    overlay_band_names = [
        source["overlayBand"]
        for source in sources
        if "overlayBand" in source
    ]

    # Remove duplicates by round-tripping through a set
    return list(set(overlay_band_names))

def map_fields_to_new_state_field(raven_one_state, raven_two_state, overlay_band_names):
# Figure out what type of source and sourceID from original url
    CHARTS = [
        ("0", "center"),  # Main Panel
        ("1", "south"),   # South Panel
    ]

    for (containerId, panelId) in CHARTS:
        for source in raven_one_state[0]["viewTemplate"]["charts"][panelId]:
            type_of_source, by_type = determine_type_of_source(source)

            band = {
                "containerId": containerId,
                "sourceIds": [],
                "type": type_of_source.lower() if type_of_source else None,
            }

            # Account for TOL activities by type and PEF Sequence Execution Tree
            if type_of_source == "divider":
                band["sourceIds"] = []
            elif not by_type:
                band["sourceIds"] = [find_tree_leaf(raven_one_state[0], source["originalName"], source["label"])]
            elif type_of_source == "Sequence-Tracker":
                band["sourceIds"] = [find_tree_leaf(raven_one_state[0], "Sequence-Tracker", source["name"])]
            else:
                band["sourceIds"] = [find_tree_leaf(raven_one_state[0], source["originalName"], source["originalName"])]

            # Primary data handling
            map_elements_of_state(band, source, raven_one_state[0])

            if type_of_source == "divider":
                # Divider Band
                raven_two_state["bands"].append(band)
            elif "overlayBand" in source:
                # Overlay Band Child
                band["overlayBand"] = overlay_no_units(source)
                band["name"] = source["originalName"]
                raven_two_state["unoverlaid_bands"].append(band)
            elif source["label"] in overlay_band_names:
                # Overlaid Band Parent
                wrapper_band = create_wrapper_band(source, band)
                del band["containerId"]
                del band["sortOrder"]
                band["name"] = source["originalName"]
                raven_two_state["bands"].append(wrapper_band)
            else:
                # Non-Overlaid Band
                band["name"] = source["originalName"]
                raven_two_state["bands"].append(band)

def add_overlays_to_state(raven_two_state, overlay_band_names):
    for unoverlaid_band in raven_two_state["unoverlaid_bands"]:
        for band in raven_two_state["bands"]:
            if unoverlaid_band["overlayBand"] == band["name"]:
                # print("[DEBUG] " + json.dumps(unoverlaid_band))
                del unoverlaid_band["containerId"]
                del unoverlaid_band["sortOrder"]
                del unoverlaid_band["overlayBand"]
                band["subBands"].append(unoverlaid_band)

    return raven_two_state

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

def set_global_settings(raven_one_state, raven_two_state):
    maxTimeRange = {
        "start": float(t_time_to_epoch(raven_one_state["viewStart"])),
        "end": float(t_time_to_epoch(raven_one_state["viewEnd"])),
    }

    raven_two_state.update({
        "viewTimeRange": dict(maxTimeRange),
        "maxTimeRange": dict(maxTimeRange),  # clone the dict
        "defaultBandSettings": get_default_band_settings(raven_one_state),
    })

def parse_data_source_url(url):
    BAND_TYPE_REGEX = r"/api/v2/(?P<file_type>[^_]+)_(?P<band_type>[^-]+)-(?P<db>[^/]+)/"

    url_path = urlparse.urlparse(url).path
    match = re.search(BAND_TYPE_REGEX, url_path)

    return {
        "file_type": match.group("file_type"),
        "band_type": match.group("band_type"),
        "source_name": match.group("db")
    }

def determine_type_of_source(source):
    if "url" not in source:
        return "divider", False

    source_info = parse_data_source_url(source["url"])

    if source["label"] == "Sequence-Tracker":
        type_of_source = "Sequence-Tracker"
    else:
        type_of_source = source_info["band_type"]

    # determine source type
    if "legend" in source and not type_of_source:
        return "activities", True
    elif type_of_source in ["activities", "resources", "divider"]:
        return type_of_source, False
    elif "pef" in source_info["file_type"]:
        return "pef", False
    elif "generic" in source_info["file_type"]:
        return "generic", False
    else:
        return type_of_source, False

def map_elements_of_state(band, source, raven_one_state):
    if band["type"] == "resource":
        default_band_settings = get_default_band_settings(raven_one_state)

        url = urlparse.urlparse(source["url"])
        url_qs = urlparse.parse_qs(url.query)

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
            "decimate": (url_qs.get("decimate", ["false"])[0] == "true"),
            "interpolation": source["graphSettings"]["interpolation"],
            "fill": source["graphSettings"].get("fill") or False,  # for states
            "fillColor": fillColor,
            "height": source["graphSettings"].get("height", 100),
            "heightPadding": source["graphSettings"].get("heightPadding", 10),
            "icon": default_band_settings["icon"],
            "interpolation": "linear",
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
            "points": [],
            "scientificNotation": source["graphSettings"].get("scientificNotation") or False,
            "showIcon": source["graphSettings"].get("iconEnabled") or False,
            "showLabelPin": bool(source.get("suffix") or ""),
            "showLabelUnit": True,
            "showTooltip": True,
            "sortOrder": 0,
            "tableColumns": [],
        })
    else:
        band["label"] = label_no_units(source)
        band["height"] = source["graphSettings"].get("height", 100)
        band["showIcon"] = source["graphSettings"]["iconEnabled"]

    if band["type"] == "activities":
        # print("[DEBUG] " + "activity")
        band["activityStyle"] = source["graphSettings"]["activityLayout"]
        band["activityHeight"] = source["graphSettings"]["activityHeight"]
        band["activityStyle"] = source["graphSettings"]["style"]
        band["showActivityTimes"] = source["graphSettings"]["showActivityTimes"]
        band["trimLabel"] = source["graphSettings"]["trimLabel"]
        band["labelColor"] = get_data_from_leaf(raven_one_state, source["originalName"], source["label"], "labelColor")
        band["layout"] = source["graphSettings"]["activityLayout"]

        if source["graphSettings"]["activityLayout"] == "bar":
            band["activityStyle"] = "1"
        elif source["graphSettings"]["activityLayout"] == "icon":
            band["activityStyle"] = "2"
        else:
            band["activityStyle"] = "3"

    if "suffix" in source:
        band["labelPin"] = source.get("suffix") or ""
        band["showLabelPin"] = bool(source.get("suffix") or "")

def create_wrapper_band(source, band, sortOrder=0):
    return {
        "compositeAutoScale": True,
        "compositeLogTicks": False,
        "compositeScientificNotation": False,
        "compositeYAxisLabel": False,
        "containerId": "0",
        "height": 100,  # TODO: band["height"]
        "heightPadding": 10,  # TODO: band["heightPadding"]
        "name": source["originalName"],  # TODO: band["name"]
        "overlay": False,
        "showTooltip": False,  # TODO: band["showTooltip"]
        "sortOrder": sortOrder,
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
