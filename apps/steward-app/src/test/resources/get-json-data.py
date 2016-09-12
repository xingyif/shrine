#!/usr/bin/env python
import json
import sys
import subprocess

def json_print(json_thing):
  print json.dumps(json_thing)

def curl(url_after_steward, user, password):
  print('called!')
  input = subprocess.check_output([
    "curl",
    "https://shrine-dev1.catalyst:6443/steward/" + url_after_steward,
    "-k",
    "--user",
    "%s:%s" % (user, password)
  ])
  print(input)
  if input == "Authentication Failed":
    print("Authentication Failed", file=sys.stderr)
    sys.exit()
  else:
    return json.loads(input.decode("utf-8"))

def topic_ids(user, password):
  json_input = curl("researcher/topics/", user, password)
  topics = json_input["topics"]
  result = []
  for topic in topics:
    result.append(topic["id"])
  result.sort()
  return result


def query_history_for_topic(user, password, topic):
  return curl("researcher/queryHistory/topic/%s" % topic, user, password)

def query_history_for_all_topics(user, password):
  return curl("researcher/queryHistory", user, password)

def write_topic_history_to_file(user, password, topic):
  result = query_history_for_topic(user, password, topic)
  if result["totalCount"] > 0:  
    json_file = open('%s_topic_%d.json' % (user, topic), 'w')
    json_file.write(json.dumps(result))
    json_file.close()

def write_histories_to_file(user, password):
  topics = topic_ids(user, password)
  for topic in topics:
    write_topic_history_to_file(user, password, topic)
  json_file = open('%s_all_topics.json' % user, 'w')
  json_file.write(json.dumps(query_history_for_all_topics(user, password)))
  json_file.close()

user = sys.argv[1]
password = sys.argv[2]

if '-topics' in sys.argv:
  json_print(topic_ids(user, password))
if '-t' in sys.argv:
  topic = sys.argv[1 + sys.argv.index('-t')]
  json_print(query_history_for_topic(user, password, topic))
if '-h' in sys.argv:
  json_print(query_history_for_all_topics(user, password))
if '-f' in sys.argv:
  write_histories_to_file(user, password)
