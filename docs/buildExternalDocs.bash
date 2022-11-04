#!/bin/bash

# Build the EDSL documentation and insert it into the current build directory
../gradlew publishDocs -p ../


mkdir -p ./source/constraints-edsl-api
cp -a ../merlin-server/constraints-dsl-compiler/build/docs/. ./source/constraints-edsl-api
rm -f ./source/constraints-edsl-api/.nojekyll

mkdir -p ./source/scheduling-edsl-api
cp -a ../scheduler-worker/scheduling-dsl-compiler/build/docs/. ./source/scheduling-edsl-api
rm -f ./source/scheduling-edsl-api/.nojekyll
rm -f ./source/scheduling-edsl-api/README.md
rm -rf ./source/scheduling-edsl-api/*/Constraint_eDSL.*

# Remove the extra navigation bar from the generated Constraints EDSL files
tail -n +3 "./source/constraints-edsl-api/README.md" > "./source/constraints-edsl-api/README.tmp" && mv "./source/constraints-edsl-api/README.tmp" "./source/constraints-edsl-api/README.md"
sed -i -e 's/README/index/g' ./source/constraints-edsl-api/README.md
# Remove the word 'Documentation' in the title
sed -i -e 's/Documentation//' ./source/constraints-edsl-api/README.md

for file in ./source/constraints-edsl-api/classes/*.md
do
  [ -e "$file" ] || continue
  tail -n +3 "$file" > "$file.tmp" && mv "$file.tmp" "$file"
  sed -i -e 's/README/index/g' $file #StreamEDitor -in-place 'backupExtension' 'Substitution/matchString/replaceString/Global'
done
for file in ./source/constraints-edsl-api/enums/*.md
do
  [ -e "$file" ] || continue
  tail -n +3 "$file" > "$file.tmp" && mv "$file.tmp" "$file"
  sed -i -e 's/README/index/g' $file
done

# Remove the extra navigation bar from the generated Scheduling EDSL files
tail -n +3 "./source/scheduling-edsl-api/modules/Scheduling_eDSL.md" > "./source/scheduling-edsl-api/Scheduling_eDSL.tmp" && mv "./source/scheduling-edsl-api/Scheduling_eDSL.tmp" "./source/scheduling-edsl-api/Scheduling_eDSL.md"
sed -i -e 's/README/index/g' ./source/scheduling-edsl-api/Scheduling_eDSL.md
sed -i -e 's/Scheduling_eDSL.md/index.md/g' ./source/scheduling-edsl-api/Scheduling_eDSL.md
# Replace any references to the Constraints EDSL files
 # These must be written to replace the calls to other folders first
sed -i -e 's,../classes/Constraint_eDSL.,../../constraints-edsl-api/classes/,g' ./source/scheduling-edsl-api/Scheduling_eDSL.md
sed -i -e 's,../enums/Constraint_eDSL.,../../constraints-edsl-api/enums/,g' ./source/scheduling-edsl-api/Scheduling_eDSL.md
sed -i -e 's,../interfaces/Constraint_eDSL.,../../constraints-edsl-api/interfaces/,g' ./source/scheduling-edsl-api/Scheduling_eDSL.md
sed -i -e 's,Constraint_eDSL.md,../../constraints-edsl-api/index.md,g' ./source/scheduling-edsl-api/Scheduling_eDSL.md
# Update the references to the other Scheduling EDSL files
sed -i -e 's,../classes,./classes,g' ./source/scheduling-edsl-api/Scheduling_eDSL.md
sed -i -e 's,../enums,./enums,g' ./source/scheduling-edsl-api/Scheduling_eDSL.md
sed -i -e 's,../interfaces,./interfaces,g' ./source/scheduling-edsl-api/Scheduling_eDSL.md
# Remove the word 'Module' in front of the title
sed -i -e 's/Module: //' ./source/scheduling-edsl-api/Scheduling_eDSL.md

for file in ./source/scheduling-edsl-api/classes/*.md
do
  [ -e "$file" ] || continue
  tail -n +3 "$file" > "$file.tmp" && mv "$file.tmp" "$file"
  sed -i -e 's/README/index/g' $file
  sed -i -e 's,../modules/Scheduling_eDSL.md,../index.md,g' $file
  sed -i -e 's,../enums/Constraint_eDSL.,../../constraints-edsl-api/enums/,g' $file
  sed -i -e 's,../interfaces/Constraint_eDSL.,../../constraints-edsl-api/enums/,g' $file
  sed -i -e 's,Constraint_eDSL.,../../constraints-edsl-api/classes/,g' $file
done
for file in ./source/scheduling-edsl-api/enums/*.md
do
  [ -e "$file" ] || continue
  tail -n +3 "$file" > "$file.tmp" && mv "$file.tmp" "$file"
  sed -i -e 's/README/index/g' $file
  sed -i -e 's,../modules/Scheduling_eDSL.md,../index.md,g' $file
  sed -i -e 's,../classes/Constraint_eDSL.,../../constraints-edsl-api/classes/,g' $file
  sed -i -e 's,../interfaces/Constraint_eDSL.,../../constraints-edsl-api/interfaces/,g' $file
  sed -i -e 's,Constraint_eDSL.,../../constraints-edsl-api/enums/,g' $file
done
for file in ./source/scheduling-edsl-api/interfaces/*.md
do
  [ -e "$file" ] || continue
  tail -n +3 "$file" > "$file.tmp" && mv "$file.tmp" "$file"
  sed -i -e 's/README/index/g' $file
  sed -i -e 's,../modules/Scheduling_eDSL.md,../index.md,g' $file
  sed -i -e 's,../classes/Constraint_eDSL.,../../constraints-edsl-api/classes/,g' $file
  sed -i -e 's,../enums/Constraint_eDSL.,../../constraints-edsl-api/enums/,g' $file
  sed -i -e 's,Constraint_eDSL.,../../constraints-edsl-api/interfaces/,g' $file
done

# Generate an index.md for constraints-dsl-api
echo '
```{eval-rst}
.. toctree::
   :hidden:
   :glob:

   classes/*
   enums/*
```' | cat ./source/constraints-edsl-api/README.md - > ./source/constraints-edsl-api/index.md

# Generate an index.md for scheduling-dsl-api
echo '
```{eval-rst}
.. toctree::
   :hidden:
   :glob:

   classes/*
   enums/*
   interfaces/*
```' | cat ./source/scheduling-edsl-api/Scheduling_eDSL.md - > ./source/scheduling-edsl-api/index.md

# Remove unneeded files
rm -f ./source/constraints-edsl-api/README.md
rm -f ./source/scheduling-edsl-api/Scheduling_eDSL.md
rm -rf ./source/scheduling-edsl-api/modules
# These are only generated when run using BSD (the version of SED on Mac)
rm -f ./source/constraints-edsl-api/**/*-e
rm -f ./source/scheduling-edsl-api/**/*-e
rm -f ./source/constraints-edsl-api/README.md-e
rm -f ./source/scheduling-edsl-api/Scheduling_eDSL.md-e
