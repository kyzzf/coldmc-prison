#!/bin/sh
# Builds Prison.jar against the live server's API jars. Install with mv (never cp over a live
# jar - [[replace-a-live-jar-with-mv-not-cp]]) into core/server/plugins/; it loads on restart.
set -e
cd "$(dirname "$0")"
S=${COLDMC_SERVER:-../../../../core/server}; J=/usr/local/opt/openjdk@25/bin
CP="$S/libraries/cn/dreeam/leaf/leaf-api/1.21.11-R0.1-SNAPSHOT/leaf-api-1.21.11-R0.1-SNAPSHOT.jar:$S/plugins/FastAsyncWorldEdit.jar:$S/plugins/worldguard.jar:$S/plugins/nexo-1.28.jar:$S/plugins/MythicMobs-5.11.2.jar:$S/plugins/MMOCore-1.13.1 -54.jar:$S/plugins/MythicLib-dist-1.7.1 90.jar"
for j in $(find $S/libraries/net/md-5 -name "bungeecord-chat*.jar") $(find $S/libraries/net/kyori -name '*4.26*.jar' -o -name 'examination*.jar' -o -name 'adventure-key*.jar') $(find $S/libraries/org/jetbrains/annotations -name '*.jar' | head -1) $(find $S/libraries/com/google/guava/guava -name "guava-*.jar") $(find $S/libraries/org/yaml -name "snakeyaml*.jar" | head -1); do CP="$CP:$j"; done
rm -rf build && mkdir build
$J/javac --release 21 -nowarn -Xlint:-options -d build -cp "$CP" src/se/coldmc/prison/*.java
cp -R res/. build/
rm -f Prison.jar && $J/jar cf Prison.jar -C build .
echo built Prison.jar
