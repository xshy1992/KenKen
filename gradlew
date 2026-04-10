#!/usr/bin/env sh
##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPts='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonzero=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    nonzero=true
    ;;
  Darwin* )
    darwin=true
    nonzero=true
    ;;
  MINGW* )
    msys=true
    nonzero=true
    ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched.
if $cygwin ; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# For MSYS, convert paths
if $msys ; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cd "$JAVA_HOME" && pwd`
fi

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$nonzero" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

for arg in "$@" ; do
    case "$arg" in
        --*cygwin*) cygwin=true ;;
           *) ;;
    esac

    case "$arg" in
        --*msys) msys=true ;;
           *) ;;
    esac
done

# For Cygwin, switch paths to Windows format before running java
if $cygwin ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    JAVACMD=`cygpath --unix "$JAVACMD"`
    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 1 -name 'cygwin*' 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^$ROOTDIRS)"
    # shellcheck disable=SC2016
    ROOTDIRSRAW=`echo "$ROOTDIRSRAW" | sed -e 's/\\\\/\\\\\\\\/g'`
    OURCYGPATTERN="(^$ROOTDIRS)"
    # shellcheck disable=SC2016
    OURCYGPATTERN="$OURCYGPATTERN(^.*$)"
    # shellcheck disable=SC2016
    for arg in "$@" ; do
        case "$arg" in
            /*) arg=`cygpath --absolute "$arg"` ;;
        esac
    done
    case "$arg" in
        --*cygwin*) cygwin=true ;;
    esac
    case "$arg" in
        /*) arg=`cygpath --absolute "$arg"` ;;
    esac
fi

# Finally, prepare the command line.
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the extra JVM options to pass to the Gradle daemon.
if [ "x$GRADLE_OPTS" = "x" ]; then
    GRADLE_OPTS="$DEFAULT_JVM_OPTS"
else
    GRADLE_OPTS="$DEFAULT_JVM_OPTS $GRADLE_OPTS"
fi
export GRADLE_OPTS

exec "$JAVACMD" $GRADLE_OPTS \
    -Dorg.gradle.appname=$APP_BASE_NAME \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
