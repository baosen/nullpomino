#!/bin/sh
mvn package
export LD_LIBRARY_PATH="target/lib:$LD_LIBRARY_PATH"
java -Djava.library.path=target/lib -cp "target/nullpomino-7.6.0-SNAPSHOT.jar:target/lib/*" mu.nu.nullpo.gui.slick.NullpoMinoSlick
