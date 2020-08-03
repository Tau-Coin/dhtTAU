#!/bin/bash

JTAU_APP="io.taucoin.jtau.cmd.JTau"

JTAU_Process=`ps -aux | grep -i ${JTAU_APP} | grep -v grep`

if [ -z "${JTAU_Process}" ];then
    echo "JTau has't been running, please execute ./run.sh"
    exit 1
fi

Cmds='/'${JTAU_Process#*/}

echo $Cmds

# create daemon shell script
Scpits="#!/bin/bash\n""nohup $Cmds &"

echo -e $Scpits | tee 'daemon.sh'
chmod +x 'daemon.sh'

echo 'Daemon shell script is generated successfully.'
echo 'Please run daemon.sh.'
echo 'Please kill this daemon like this:kill -15 <pid>'
