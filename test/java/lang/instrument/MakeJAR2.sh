#!/bin/sh

#
# Copyright 2005 Sun Microsystems, Inc.  All Rights Reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
# CA 95054 USA or visit www.sun.com if you need additional information or
# have any questions.
#


AGENT="$1"
APP="$2"

if [ "${TESTSRC}" = "" ]
then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTSRC=${TESTSRC}"

if [ "${TESTJAVA}" = "" ]
then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTJAVA=${TESTJAVA}"

if [ "${TESTCLASSES}" = "" ]
then
  echo "TESTCLASSES not set.  Test cannot execute.  Failed."
  exit 1
fi

OS=`uname -s`
case "$OS" in
   SunOS | Linux )
      PATHSEP=":"
      ;;

   Windows* | CYGWIN*)
      PATHSEP=";"
      ;;

   # catch all other OSs
   * )
      echo "Unrecognized system!  $OS"
      fail "Unrecognized system!  $OS"
      ;;
esac

JAVAC="${TESTJAVA}/bin/javac -g"
JAR="${TESTJAVA}/bin/jar"

cp ${TESTSRC}/${AGENT}.java .
cp ${TESTSRC}/${APP}.java .
rm -rf ilib
mkdir ilib
cp ${TESTSRC}/ilib/*.java ilib
rm -rf bootpath
mkdir -p bootpath/bootreporter
cp ${TESTSRC}/bootreporter/*.java bootpath/bootreporter

cd bootpath
${JAVAC} bootreporter/*.java
cd ..

${JAVAC} ${AGENT}.java ilib/*.java
${JAVAC} -classpath .${PATHSEP}bootpath ${APP}.java

echo "Manifest-Version: 1.0"    >  ${AGENT}.mf
echo Premain-Class: ${AGENT} >> ${AGENT}.mf
echo Boot-Class-Path: bootpath >> ${AGENT}.mf
shift 2
while [ $# != 0 ] ; do
  echo $1 >> ${AGENT}.mf
  shift
done

${JAR} cvfm ${AGENT}.jar ${AGENT}.mf ${AGENT}*.class ilib/*.class

# rm -rf  ${AGENT}.java ilib ${AGENT}.mf ${AGENT}*.class
