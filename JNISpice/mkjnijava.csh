#! /bin/csh
#
#   mkjnijava script for JNISpice Java code; Mac Intel/OSX/64bit
#   version.
#
#   This script builds JNISpice Java classes. It should be executed
#   from the JNISpice directory in the following tree:
#
#                      package
#                         |
#                         |
#       +------+------+------+------+------+
#       |      |      |      |      |      |
#     data    doc    etc    exe    lib    src
#                                          |
#                                          |
#                         +----------+----------+------- ... ------+
#                         |          |          |                  |
#                     JNISpice  product_2  product_3    ...   product_n
#
#   Upon execution it does the following:
#
#     1)  Deletes all .class files from the subdirectories under
#         ``cspice'' directory, the current directory. Deletes from
#         ``../../exe'' directory all .class files that were also
#         present the current directory.
#
#     2)  Compiles all .java source files in the current directory and
#         under the subdirectories under the ``cspice'' directory.
#
#     3)  Copies all re-compiled .class files from the current
#         directory to the ``../../exe''.
#
#   The Java compiler used by the script is set using environment
#   variable JAVACOMPILER. If this variables is set prior to executing
#   this script, that Java compiler is used. If this variable is not
#   set prior to executing this script, it is set within the script as
#   a local variable.
#
#   Change History:
#   ===============
#
#   Version 1.0.0  Jan 31, 2010   Nat Bachman, Boris Semenov
#

#
#  Choose your Java compiler.
#
if ( $?JAVACOMPILER ) then
    echo " "
    echo "      Using Java compiler: "
    echo "      $JAVACOMPILER"
else
    set JAVACOMPILER  =  "javac"
    echo " "
    echo "      Setting default compiler:"
    echo $JAVACOMPILER
endif

#
#  Delete existing .class files.
#
\ls *.class >& /dev/null

if ( $status == 0 ) then

    echo " "
    echo "      Deleting existing .class files from current directory"

    foreach CLASSFILE ( *.class )
        \rm ../../exe/$CLASSFILE
    end

    \rm *.class

endif

\ls spice/*/*.class >& /dev/null

if ( $status == 0 ) then

    echo " "
    echo "      Deleting existing .class files from package subdirectories"

    \rm spice/*/*.class

endif

#
#  Compile all JNISpice .java files.
#
\ls spice/*/*.java *.java >& /dev/null

if ( $status == 0 ) then

    echo " "
    echo "      Compiling .java files"

    $JAVACOMPILER spice/*/*.java *.java

else

    echo " "
    echo "      No .java files in the current directory and/or under "
    echo "      'spice' subdirectory. Nothing to compile."
    echo " "
    exit 1

endif

#
#  Copy all .class files from the current directory to ``../../exe''. 
#
\ls *.class >& /dev/null

if ( $status == 0 ) then

    echo " "
    echo "      Copying JNISpice Java executables to ../../exe"

    cp *.class ../../exe

endif

#
#  All done.
#
echo " "
exit 0

