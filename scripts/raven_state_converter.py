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
    raven_two_state["unoverlaid_bands"] = []
    raven_two_state["pins"] = []
    raven_two_state["guides"] = []
    set_global_settings(raven_one_state[0]["viewTemplate"], raven_two_state)
    raven_two_state["pins"] = get_pins(raven_one_state)
    raven_two_state["guides"] = get_guides(raven_one_state)
    raven_two_state = map_fields_to_new_state_field(raven_one_state, raven_two_state, band_list)
    raven_two_state = add_overlays_to_state(raven_two_state, band_list)
    

    del raven_two_state["unoverlaid_bands"]

    print json.dumps (raven_two_state)
    
def get_raven_state(data):
    sources = data[0]["viewTemplate"]
    return sources

def get_pins(data):
    pins = []
    for tab_source in data[0]["tabSources"]:
        if "home" not in tab_source:
            pin = {}
            print tab_source["tabName"]
            pin["name"] = tab_source["tabName"]
            if "fs" in str(tab_source["url"]): 
                pin["sourceId"] = tab_source["url"].split("fs")[1].split("/",1)[1]
            elif "list" in str(tab_source["url"]):
                pin["sourceId"] = tab_source["url"].split("list")[1].split("/",1)[1]
            pins.append(pin)
    return pins

def get_guides(data):
    guides = []
    if "guides" in data[0]["viewTemplate"]:
        for guide in data[0]["viewTemplate"]["guides"]:
            guides.append(t_time_to_epoch(guide))
            print guides
    return guides    

def find_tree_leaf(data, original_name, label_name):
 
    for source in data["sources"]:
        if source["name"] == original_name:
            if source["graphSettings"][0]["label"] == label_name:
                print original_name
                return original_name
        path = find_tree_leaf(source, original_name, label_name)
        if path:
            print source["name"]
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

def map_fields_to_new_state_field(raven_one_state, raven_two_state, overlay_band_names):

# Figure out what type of source and sourceID from original url  

    for source in raven_one_state[0]["viewTemplate"]["charts"]["center"]:
        
        by_type = False
        if "url" not in source:
            type_of_source = "divider"
        else:
            type_of_source = source["url"].split("v2")[1].split("-")[0].split("/")[1].split("_")[1]

        if "legend" in source:
            type_of_source = "activities"
            by_type = True

        #  determine source type
        if type_of_source not in ["activities", "resources", "divider"]:
            if "pef" in source["url"].split("v2")[1].split("-")[0].split("/")[1]:
                type_of_source = "pef"
            elif "generic" in source["url"].split("v2")[1].split("-")[0].split("/")[1]:
                type_of_source = "generic"
        
        band = {}
        band["sourceIds"] = []
        if type_of_source:
            band["type"] = type_of_source.lower()
        # Account for TOL activities by type and PEF Sequence Execution Tree
        if not by_type and type_of_source != "divider":
            band["sourceIds"].append(find_tree_leaf(raven_one_state[0], source["originalName"], source["label"]))
        elif type_of_source == "divider":
            band["sourceIds"] = []
        else:
            band["sourceIds"].append(find_tree_leaf(raven_one_state[0], source["originalName"], source["originalName"]))    

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
        
        if "suffix" in source:
            band["labelPin"] = source["suffix"]
            band["showLabelPin"] = True 

        if source["label"] not in overlay_band_names and "overlayBand" not in source and type_of_source != "divider":
            band["name"] = source["originalName"]
            raven_two_state["bands"].append(band)
    
        elif source["label"] in overlay_band_names and "overlayBand" not in source and type_of_source != "divider":
            wrapper_band = {}
            wrapper_band["compositeAutoScale"] = True
            wrapper_band["compositeLogTicks"] = False
            wrapper_band["compositeScientificNotation"] = False
            wrapper_band["compositeYAxisLabel"] = False
            wrapper_band["containerId"] = "0"
            wrapper_band["height"] = 100
            wrapper_band["heightPadding"] = 10
            wrapper_band["name"] = source["originalName"]
            wrapper_band["type"]= "composite"
            subBands = []
            subBands.append(band)
            wrapper_band["subBands"] = subBands
            raven_two_state["bands"].append(wrapper_band)
        
        elif "overlayBand" in source and type_of_source != "divider":
            # print "lBEL"  + label_no_units(source) 
            band["overlayBand"] = overlay_no_units(source)
            raven_two_state["unoverlaid_bands"].append(band)    
        else:
            raven_two_state["bands"].append(band)

    return raven_two_state

def add_overlays_to_state(raven_two_state, overlay_band_names):
    for unoverlaid_band in raven_two_state["unoverlaid_bands"]:
        for band in raven_two_state["bands"]:
            if unoverlaid_band["overlayBand"] == band["name"]:
                # print json.dumps(unoverlaid_band)
                band["subBands"].append(unoverlaid_band)

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
    main()


# wrapper_band[




# wrapper_band[Create list of bands
# wrapper_band[Look for unique values of overlayband
# wrapper_band[For n number of unique values
#     wrapper_band[Create a container band 
#     wrapper_band[Find all bands associated with the unique value and add to container band
#     wrapper_band[pull them out of list of existing bands and into the container band

#     wrapper_band[Case 1: I am a parent Band:  Create a Container band and put information in subband
#     wrapper_band[Case 2: I am an overlaid band:  Put the band into the container that has the the overlayBand name
#     wrapper_band[Case 3: I am a standard non-overlaid band
