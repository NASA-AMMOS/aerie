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
    
    band_list = create_list_of_overlay_labels(raven_one_state)
    
    raven_two_state = {}
    raven_two_state["bands"] = []
    set_global_settings(raven_one_state[0]["viewTemplate"], raven_two_state)
    raven_two_state = map_fields_to_new_state_field(raven_one_state, raven_two_state, band_list)
    
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

def create_list_of_overlay_labels(raven_one_state):
    unique_overlay_bands = []
    for source in raven_one_state[0]["viewTemplate"]["charts"]["center"]:
        if "overlayBand" in source:
            if source["overlayBand"] not in unique_overlay_bands:
                unique_overlay_bands.append(source["overlayBand"])
    return unique_overlay_bands            

def map_fields_to_new_state_field(raven_one_state, raven_two_state, unique_list):
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
            band["labelUnit"] = extract_units(source)        
        
        band["label"] = label_no_units(source)
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
        
        if source["label"] not in unique_list and "overlayBand" not in source:
            band["name"] = source["originalName"]
            raven_two_state["bands"].append(band)
            print "single"
    
        elif source["label"] in unique_list and "overlayBand" not in source:
            wrapper_band = {}
                # "compositeAutoScale": true,
                # "compositeLogTicks": false,
                # "compositeScientificNotation": false,
                # "compositeYAxisLabel": false,
                # "containerId": "0",
                # "height": 100,
                # "heightPadding": 10}
            wrapper_band["name"] = source["originalName"]
            
            subBands = []
            subBands.append(band)
            wrapper_band["subBands"] = subBands
            print "unique"
            raven_two_state["bands"].append(wrapper_band)
        
        elif "overlayBand" in source:
            print "overlaid band"
            for element in raven_two_state["bands"]:
                if band["labelUnit"] != "": 
                    constructed_label = band["label"] + " (" + band["labelUnit"] + ")"
                    print constructed_label + "compares to " + source["overlayBand"]
                else:
                    constructed_label = band["label"]
                if constructed_label == source["overlayBand"]:
                    element["subBands"].append(band)
                    
            # for band in raven_two_state["bands"]:
                
                
            #     if source["label"] == band[]
            
        
        
    return raven_two_state

def set_global_settings(raven_one_state, raven_two_state):
    
    raven_two_state["viewTimeRange"]={}
    raven_two_state["viewTimeRange"]["start"] = t_time_to_epoch(raven_one_state["viewStart"])
    raven_two_state["viewTimeRange"]["end"] = t_time_to_epoch(raven_one_state["viewEnd"])

    raven_two_state["defaultBandSettings"] = {}
    raven_two_state["defaultBandSettings"]["labelFontSize"] = raven_one_state["globalLabelFontSize"]
    raven_two_state["defaultBandSettings"]["labelWidth"] = raven_one_state["bandLabelWidth"]
    raven_two_state["defaultBandSettings"]["showTooltip"] = raven_one_state["tooltipEnabled"]

def create_wrapper_band():
    print "do nothing"

def extract_units(source):
    label_string = source["label"]
    if label_string.endswith(')'):
        units = source["label"][:source["label"].rfind(")")].split("(")[1]
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


    # 




    # Create list of bands
    # Look for unique values of overlayband
    # For n number of unique values
        # Create a container band 
        # Find all bands associated with the unique value and add to container band
        # pull them out of list of existing bands and into the container band

        # Case 1: I am a parent Band:  Create a Container band and put information in subband
        # Case 2: I am an overlaid band:  Put the band into the container that has the the overlayBand name
        # Case 3: I am a standard non-overlaid band
