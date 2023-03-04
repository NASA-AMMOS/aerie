#!/bin/bash

mkdir results
mkdir comparison

PGCMPINPUT1=./pgdumpmigrated/AerieMerlinMigrated PGCMPINPUT2=./pgdumpraw/AerieMerlinRaw PGCLABEL1=AerieMerlinMigrated PGCLABEL2=AerieMerlinRaw PGCFULLOUTPUT=./comparison/fulloutputMerlin.txt PGCUNEXPLAINED=./comparison/unexplainedMerlin.txt PGCBADEXPLAIN=./comparison/badexplanationsMerlin.txt PGDB=postgres PGBINDIR=/usr/bin ./pgcmp
return_code=$?
if [ $return_code -ne 0 ]; then
    echo "AerieMerlin comparison failed - return code=$return_code"
    touch results/fulloutput
    touch results/unexplained
    touch results/badexplanations
    touch results/perform-comparison.log
    printf "==================\nAerieMerlin Results\n==================\n" >> results/fulloutput
    printf "==================\nAerieMerlin Results\n==================\n" >> results/unexplained
    printf "==================\nAerieMerlin Results\n==================\n" >> results/badexplanations
    printf "==================\nAerieMerlin Results\n==================\n" >> results/perform-comparison.log
    cat ./comparison/fulloutputMerlin.txt >> results/fulloutput
    cat ./comparison/unexplainedMerlin.txt >> results/unexplained
    cat ./comparison/badexplanationsMerlin.txt >> results/badexplanations
    cat /tmp/perform-comparison.log >> results/perform-comparison.log
else
    echo "AerieMerlin comparison succeeded"
fi

PGCMPINPUT1=./pgdumpmigrated/AerieSchedulerMigrated PGCMPINPUT2=./pgdumpraw/AerieSchedulerRaw PGCLABEL1=AerieSchedulerMigrated PGCLABEL2=AerieSchedulerRaw PGCFULLOUTPUT=./comparison/fulloutputScheduler.txt PGCUNEXPLAINED=./comparison/unexplainedScheduler.txt PGCBADEXPLAIN=./comparison/badexplanationsScheduler.txt PGDB=postgres PGBINDIR=/usr/bin ./pgcmp
retcode=$?
if [ $retcode -gt $return_code ]; then
return_code=$retcode
fi

if [ $retcode -ne 0 ]; then
    echo "AerieScheduler comparison failed - return code=$retcode"
    touch results/fulloutput
    touch results/unexplained
    touch results/badexplanations
    touch results/perform-comparison.log
    printf "\n==================\nAerieScheduler Results\n==================\n" >> results/fulloutput
    printf "\n==================\nAerieScheduler Results\n==================\n" >> results/unexplained
    printf "\n==================\nAerieScheduler Results\n==================\n" >> results/badexplanations
    printf "\n==================\nAerieScheduler Results\n==================\n" >> results/perform-comparison.log
    cat ./comparison/fulloutputScheduler.txt >> results/fulloutput
    cat ./comparison/unexplainedScheduler.txt >> results/unexplained
    cat ./comparison/badexplanationsScheduler.txt >> results/badexplanations
    cat /tmp/perform-comparison.log >> results/perform-comparison.log
else
    echo "AerieScheduler comparison succeeded"
fi

PGCMPINPUT1=./pgdumpmigrated/AerieSequencingMigrated PGCMPINPUT2=./pgdumpraw/AerieSequencingRaw PGCLABEL1=AerieSequencingMigrated PGCLABEL2=AerieSequencingRaw PGCFULLOUTPUT=./comparison/fulloutputSequencing.txt PGCUNEXPLAINED=./comparison/unexplainedSequencing.txt PGCBADEXPLAIN=./comparison/badexplanationsSequencing.txt PGDB=postgres PGBINDIR=/usr/bin ./pgcmp
retcode=$?
if [ $retcode -gt $return_code ]; then
return_code=$retcode
fi

if [ $retcode -ne 0 ]; then
    echo "AerieSequencing comparison failed - return code=$retcode"
    touch results/fulloutput
    touch results/unexplained
    touch results/badexplanations
    touch results/perform-comparison.log
    printf "\n==================\nAerieSequencing Results\n==================\n" >> results/fulloutput
    printf "\n==================\nAerieSequencing Results\n==================\n" >> results/unexplained
    printf "\n==================\nAerieSequencing Results\n==================\n" >> results/badexplanations
    printf "\n==================\nAerieSequencing Results\n==================\n" >> results/perform-comparison.log
    cat ./comparison/fulloutputSequencing.txt >> results/fulloutput
    cat ./comparison/unexplainedSequencing.txt >> results/unexplained
    cat ./comparison/badexplanationsSequencing.txt >> results/badexplanations
    cat /tmp/perform-comparison.log >> results/perform-comparison.log
else
    echo "AerieSequencing comparison succeeded"
fi

PGCMPINPUT1=./pgdumpmigrated/AerieUIMigrated PGCMPINPUT2=./pgdumpraw/AerieUIRaw PGCLABEL1=AerieUIMigrated PGCLABEL2=AerieUIRaw PGCFULLOUTPUT=./comparison/fulloutputUI.txt PGCUNEXPLAINED=./comparison/unexplainedUI.txt PGCBADEXPLAIN=./comparison/badexplanationsUI.txt PGDB=postgres PGBINDIR=/usr/bin ./pgcmp
retcode=$?
if [ $retcode -gt $return_code ]; then
return_code=$retcode
fi

if [ $retcode -ne 0 ]; then
    echo "AerieUI comparison failed - return code=$retcode"
    touch results/fulloutput
    touch results/unexplained
    touch results/badexplanations
    touch results/perform-comparison.log
    printf "\n==================\nAerieUI Results\n==================\n" >> results/fulloutput
    printf "\n==================\nAerieUI Results\n==================\n" >> results/unexplained
    printf "\n==================\nAerieUI Results\n==================\n" >> results/badexplanations
    printf "\n==================\nAerieUI Results\n==================\n" >> results/perform-comparison.log
    cat ./comparison/fulloutputUI.txt >> results/fulloutput
    cat ./comparison/unexplainedUI.txt >> results/unexplained
    cat ./comparison/badexplanationsUI.txt >> results/badexplanations
    cat /tmp/perform-comparison.log >> results/perform-comparison.log
else
    echo "AerieUI comparison succeeded"
fi

exit $return_code
