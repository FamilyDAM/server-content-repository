#!/bin/sh
#
# FamilyDAM Java Application

. /etc/service.subr

prog_dir=`dirname \`realpath $0\``

name="FamilyDAM"
version="0.0.1"
description="Digital Asset Manager for Families"
framework_version="2.1"
depends="java8"
webui=""

prog_dir="$(dirname "$(realpath "${0}")")"
java_tmp_dir="${prog_dir}/tmp"
tmp_dir="/tmp/DroboApps/${name}"
pidfile="${tmp_dir}/pid.txt"
logfile="${tmp_dir}/log.txt"
statusfile="${tmp_dir}/status.txt"
errorfile="${tmp_dir}/error.txt"
daemon="${DROBOAPPS_DIR}/java8/bin/java"
jarpath="${prog_dir}/FamilyDAM.jar"
java_opts=-Djcr.repo.home=./familydam-repo

start()
{
  # if this file doesn't exist, client connections get some ugly warnings.
  touch /var/log/lastlog

  if [ ! -d "${java_tmp_dir}" ]; then
    mkdir -p "${java_tmp_dir}"
  fi


    setsid "${daemon}" ${java_opts} -jar ${jarpath} &
    if [ $! -gt 0 ]; then
        local pid=$!
        echo "${pid}" > "${pidfile}"
        renice 19 "${pid}"
    fi
}



case "$1" in
  start)
    start_service
    ;;
  stop)
    stop_service
    ;;
  restart)
    stop_service
    sleep 3
    start_service
    ;;
  status)
    status
    ;;
  *)
    echo "Usage: $0 [start|stop|restart|status]"
    exit 1
    ;;
esac

