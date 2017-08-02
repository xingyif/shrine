#!/bin/bash
# Deploys SHRINE local builds into vagrant box after maven build

#--------------MODIFIABLE------------------------
# Change this to be: a list of destination vagrant machine names
declare -a MACHINES=( shrine-hub )
# Change this to be: the .war file directory in vagrant box
VAGRANT_WAR_CONTEXT=/opt/shrine/tomcat/webapps
# Change this to be: local vagrant directory
VAGRANT_CONTEXT=~/vagrant-shrine-network

#------------DO NOT MODIFY-----------------------
BUFFER=/home/vagrant/m3shrine
APP_DIR=$PWD
# Local .war file directory
LOCAL_WAR_CONTEXT=target

# Functions
validates_war_context()
{
  local local_war_context=$1
  if [[ ! -d $local_war_context ]]; then
    echo "ERROR: given $local_war_context does not exist!"
    exit 1
  fi

  count=`ls -1 *.war 2>/dev/null | wc -l`
  if [[ $count == 0 ]]; then
    echo "ERROR: war file does not exist in dir $local_war_context!"
    echo "Run 'mvn clean install' to generate the war file"
    exit 1
  fi

  if [[ $count > 1 ]]; then
    echo "ERROR: more than 1 war file exists in $local_war_context!"
    exit 1
  fi
}

validates_vagrant_context()
{
  local vagrant_context=$1
  if [[ ! -e $vagrant_context ]]; then
    echo "ERROR: Vagrant context: $vagrant_context doesn't exist!"
    exit 1
  fi

  if [[ ! -d $vagrant_context ]]; then
    echo "ERROR: Vagrant context: $vagrant_context exists,\
      but it is not a directory!"
    exit 1
  fi
}

generates_ssh_cfg()
{
  local vagrant_context=$1
  cd $vagrant_context
  vagrant ssh-config > ssh.cfg
  for machine in ${MACHINES[@]}; do
    if [[ -d $BUFFER ]]; then
      echo "Removing previous buffer $BUFFER"
      vagrant ssh $machine -c "rm -r $BUFFER"
    fi
    vagrant ssh $machine -c "mkdir $BUFFER"
    vagrant ssh $machine -c "chmod 0711 /home/vagrant"
    vagrant ssh $machine -c "chmod 1777 $BUFFER"
  done
}

cleanup()
{
  local vagrant_context=$1
  cd $vagrant_context
  for machine in ${MACHINES[@]}; do
    vagrant ssh $machine -c "rm -r $BUFFER"
  done
  rm ssh.cfg
}

scp_war()
{
  local local_war_context=$1
  local vagrant_war_context=$2
  local vagrant_context=$3
  local app_dir=$4

  cd $app_dir
  validates_war_context $local_war_context

  local file_path=`find $local_war_context -type f -name "*.war"`
  local local_war_file=`basename $file_path`

  for machine in ${MACHINES[@]}; do
    # copy local .war file into vagrant box buffer directory
    echo "Local app .war file location: $app_dir/$local_war_context/$local_war_file"
    cd $vagrant_context
    scp -F ssh.cfg $app_dir/$local_war_context/$local_war_file $machine:$BUFFER

    vagrant ssh $machine -c "sudo -u shrine cp --no-preserve=mode $BUFFER/$local_war_file $vagrant_war_context"
    vagrant ssh $machine -c "sudo -c shrine rm $BUFFER/$local_war_file"
  done
}

# Main
echo "Starting the process..."
echo "Current app dir: $APP_DIR"
echo "Local Vagrant dir: $VAGRANT_CONTEXT"
echo "Deploying app to ${#MACHINES[@]} machines"

#vagrant ssh shrine-hub -c
validates_war_context $LOCAL_WAR_CONTEXT
validates_vagrant_context $VAGRANT_CONTEXT

generates_ssh_cfg $VAGRANT_CONTEXT
scp_war $LOCAL_WAR_CONTEXT $VAGRANT_WAR_CONTEXT $VAGRANT_CONTEXT $APP_DIR

cleanup $VAGRANT_CONTEXT
echo "SUCCESS: Process completed! App has been deployed to vagrant!"