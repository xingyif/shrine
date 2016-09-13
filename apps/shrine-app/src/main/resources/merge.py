#! /usr/local/bin/python3

import sys
from io import StringIO
from pyhocon import ConfigFactory, ConfigTree, HOCONConverter #pip3 install pyhocon
# Parse json file into dict representation

def parse_file(file_name):
  return ConfigFactory.parse_string(open(file_name, 'r').read())


# Merge dicts, print errors

def is_dict(thing):
  return type(thing) is dict or type(thing) is ConfigTree


def is_list(thing):
  return type(thing) is list


def is_primitive(thing):
  return type(thing) is str or type(thing) is int or type(thing) is bool or type(thing) is float


def scan_errors(config1, config2, path="", output=sys.stderr):
  for key, value in config1.items():
    if key in config2:
      compare_fields(value, config2[key], key if path == "" else path + "." + key)


def compare_fields(field1, field2, path, output=sys.stderr):
  if is_dict(field1) and is_dict(field2):
    scan_errors(field1, field2, path)
  elif is_list(field1) and is_list(field2):
    compare_lists(field1, field2, path)
  elif not (is_primitive(field1) and is_primitive(field2)):
    print("values {} and {} at the path {} had an unexected type".format(field1, field2, path), file=output)
  elif field1 != field2:
    print("values {} and {} at the path {} were not equal".format(field1, field2, path), file=output)
    
    
def compare_lists(list1, list2, path, output=sys.stderr):
  (set1, set2) = (set(list1), set(list2))
  if not (set1.issubset(set2) and set2.issubset(set1) and len(list1) == len(list2)):
    print("lists {} and {} at the path {} were not equal".format(list1, list2, path), file=output)


def merge(config1, config2):
  return ConfigTree.merge_configs(config1, config2)


# Print dicts

def write_file(config, file_name):
  print(HOCONConverter.convert(config, 'hocon'), file=open(file_name, 'w'))


conf1 = parse_file(sys.argv[1])
conf2 = parse_file(sys.argv[2])
io = StringIO()
scan_errors(conf1, conf2, output=io)
a = str(io.read())
if a:
  print(a)
  sys.exit()
else:
  write_file(merge(conf1, conf2), sys.argv[1].split(".")[0] + '+' + sys.argv[2])
