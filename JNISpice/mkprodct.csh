#! /bin/csh
#
#   mkprodct script for JNISpice C shared object library; Mac
#   Intel/OSX/64bit version.
#
#   This script builds JNISpice C shared object library. It should be 
#   executed from the JNISpice directory in the following tree:
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
#                     JNISpice  tutils_c    product_3    ...   product_n
#
#   Upon execution it does the following:
#
#     1)  Compiles all of the .c files in the current directory
#
#     2)  Creates shared object library ``libJNISpice.jnilib''
#         (the name is set by LOCALLIB variable below)
#
#     3)  Moves the library to the ``../../lib'' directory in the tree.
#
#   The compile and link options used by the script are set using
#   environment variables SOCOMPILEOPTIONS and TKLINKOPTIONS. If these
#   variables are set prior to executing this script, those options are
#   used. If these variables are not set prior to executing this
#   script, they are set within the script as local variables.
#
#   Change History:
#   ===============
#
#   Version 1.0.0  Jan 21, 2010   Nat Bachman, Boris Semenov
#

#
#  Set the library name. 
#
set LOCALLIB = "libJNISpice.jnilib"
set LIBRARY  = "../../lib/"$LOCALLIB

#
#  Choose your C compiler.
#
if ( $?TKCOMPILER ) then
    echo " "
    echo "      Using compiler: "
    echo "      $TKCOMPILER"
else
    set TKCOMPILER  =  "cc"
    echo " "
    echo "      Setting default compiler:"
    echo $TKCOMPILER
endif

#
#  What compile options do we want to use? If they were
#  set somewhere else, use those values.  The same goes
#  for link options.
#
if ( $?SOCOMPILEOPTIONS ) then
    echo " "
    echo "      Using compile options: "
    echo "      $SOCOMPILEOPTIONS"
else

   #
   #  Compile options:
   #
   set SOOPTIONS        = "-m64 -fPIC -dynamic -fno-common"
   set INCLUDESOPTIONS  = "-I../../include -I../tutils_c -isystem."
   set WARNINGS1        = "-ansi -Wall -Wundef"
   set WARNINGS2        = " -Wpointer-arith -Wcast-align -Wsign-compare"
   set COMPILEOPTIONS   = "-c -D_REENTRANT "
   set SOCOMPILEOPTIONS =  "$SOOPTIONS $INCLUDESOPTIONS $WARNINGS1 $WARNINGS2 $COMPILEOPTIONS"

   echo " "
   echo "      Setting default compile options:"
   echo "      $SOCOMPILEOPTIONS"
endif


if ( $?TKLINKOPTIONS ) then
    echo " "
    echo "      Using link options: "
    echo "      $TKLINKOPTIONS"
else

    set TKLINKOPTIONS = "-m64 -bundle -flat_namespace -undefined suppress "
    set LINKLIB       = "../../lib/tutils_c.a  ../../lib/csupport.a  ../../lib/cspice.a -lm"
    echo " "
    echo "      Setting default link options:"
    echo "      $TKLINKOPTIONS"
endif

echo " "

#
#  Compile the .c files.
#
foreach SRCFILE ( *.c )
   echo "      Compiling: "   $SRCFILE
   $TKCOMPILER $SOCOMPILEOPTIONS $SRCFILE
end

echo " "

#
#  Link
#
set OBJS = " "

foreach OBFILE ( *.o )
   echo "      Linking: "   $OBFILE
   set OBJS = "$OBJS "$OBFILE
end

$TKCOMPILER $TKLINKOPTIONS -o $LOCALLIB $OBJS $LINKLIB

\rm *.o
\mv $LOCALLIB $LIBRARY

exit 0
