#!/user/bin/env python -tt
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
from datetime import datetime
import calendar
# Global variables

SOURCES_LIST = []
Source_String = ''

# Class declarations

# Function declarations

def main():
    args = sys.argv[1:]

    if not args:
        print('usage: [--flags options] [inputs] ')
        sys.exit(1)

# Read in old state
    with open(sys.argv[1]) as f:
        raven_one_state = json.load(f)

    raven_two_state = {}
    raven_two_state["bands"] = []
    set_global_settings(raven_one_state[0]["viewTemplate"], raven_two_state)
    raven_two_state = map_fields_to_new_state_field(raven_one_state, raven_two_state)
    
    print json.dumps (raven_two_state)
    
def get_raven_state(data):
    sources = data[0]["viewTemplate"]
    return sources

def find_tree_leaf(data, i, original_name, label_name):
    i = i +1
    print i
 
    for source in data["sources"]:
        if source["name"] == original_name:
            if source["graphSettings"][0]["label"] == label_name:
                print original_name
                return original_name
        path = find_tree_leaf(source, i, original_name, label_name)
        if path:
            # print source["name"]
            return source["name"] + '/' + path    

def get_data_from_leaf(data, original_name, label_name, key):
    for source in data["sources"]:
        if source["name"] == original_name:
            if source["graphSettings"][0]["label"] == label_name:
                return source["graphSettings"][0][key]
        value = get_data_from_leaf(source, original_name, label_name, key)    
        if value:
            return value

def map_fields_to_new_state_field(raven_one_state, raven_two_state):
# Figure out what type of source and sourceID from original url  
    for source in raven_one_state[0]["viewTemplate"]["charts"]["center"]:

        by_type = False
        type_of_source = source["url"].split("v2")[1].split("-mongodb")[0].split("/")[1].split("_")[1]

        if "legend" in source:
            type_of_source = "activities"
            by_type = True
        
        # determine source type
        if type_of_source not in ["activities", "resources"]:
            if "pef" in source["url"].split("v2")[1].split("-mongodb")[0].split("/")[1]:
                type_of_source = "pef"
            elif "generic" in source["url"].split("v2")[1].split("-mongodb")[0].split("/")[1]:
                type_of_source = "generic"

        source_id = source["url"].split("mongodb")[1].split("?")[0]
        print source["originalName"]
        
        band = {}
        band["source_id"] = []
        band["type"] = type_of_source.lower()
        # Account for TOL activities by type and PEF Sequence Execution Tree
        if not by_type:
            band["source_id"].append(find_tree_leaf(raven_one_state[0], 0, source["originalName"], source["label"]))
        else:
            band["source_id"].append(find_tree_leaf(raven_one_state[0], 0, source["originalName"], source["originalName"]))    
        print band["source_id"]
        print band["type"]

        if band["type"] == "resource":
            band["interpolation"] = source["graphSettings"]["interpolation"]
            band["color"] = convert_color(source["graphSettings"]["lineColorCustom"])
            if "fill" in source["graphSettings"]:    #for states
                band["fillColor"] = convert_color(source["graphSettings"]["fillColorCustom"])
                band["fill"] = source["graphSettings"]["fill"]
                band["logTicks"] = source["graphSettings"]["logTicks"]
            band["heightPadding"] = source["graphSettings"]["heightPadding"]
            if "scientificNotation" in source["graphSettings"] and source["graphSettings"]["scientificNotation"] == "null":
                band["scientificNotation"] = False

        band["height"]= source["graphSettings"]["height"]        
        band["showIcon"] = source["graphSettings"]["iconEnabled"]
        
        if band["type"] == "activities":
            print "activity"
            band["activityStyle"] = source["graphSettings"]["activityLayout"]
            band["activityHeight"] = source["graphSettings"]["activityHeight"]
            band["activityStyle"] = source["graphSettings"]["style"]
            band["showActivityTimes"] = source["graphSettings"]["showActivityTimes"]
            band["trimLabel"] = source["graphSettings"]["trimLabel"]
            band["labelColor"] = get_data_from_leaf(raven_one_state[0], source["originalName"], source["label"], "labelColor")
            band["layout"] = source["graphSettings"]["activityLayout"]
            
            if source["graphSettings"]["activityLayout"] == "bar":
                band["activityStyle"] = "1"
            elif source["graphSettings"]["activityLayout"] == "icon":
                band["activityStyle"] = "2"
            else:
                band["activityStyle"] = "3"    
        
        
        raven_two_state["bands"].append(band)
        
    return raven_two_state

def set_global_settings(raven_one_state, raven_two_state):
    
    raven_two_state["viewTimeRange"]={}
    raven_two_state["viewTimeRange"]["start"] = t_time_to_epoch(raven_one_state["viewStart"])
    raven_two_state["viewTimeRange"]["end"] = t_time_to_epoch(raven_one_state["viewEnd"])

    raven_two_state["defaultBandSettings"] = {}
    raven_two_state["defaultBandSettings"]["labelFontSize"] = raven_one_state["globalLabelFontSize"]
    raven_two_state["defaultBandSettings"]["labelWidth"] = raven_one_state["bandLabelWidth"]
    raven_two_state["defaultBandSettings"]["showTooltip"] = raven_one_state["tooltipEnabled"]

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
    main()