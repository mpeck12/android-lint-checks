#!/bin/bash
packagename=`basename $1 .apk`
enjarify.sh $packagename.apk
if [ -d "$packagename" ]; then
    echo "Need to remove directory $packagename before running script"
    exit 1
fi
mkdir -p $packagename/bin/classes
java -jar APKParser.jar $packagename.apk > $packagename/AndroidManifest.xml
pushd $packagename/bin/classes
jar xf ../../../$packagename-enjarify.jar
popd
pushd $packagename
lint --check Security . --xml ../$packagename.lint.xml
popd
rm -rf $packagename
rm $packagename-enjarify.jar
