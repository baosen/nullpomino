#!/bin/sh
mvn package
java -cp "target/nullpomino-7.6.0-SNAPSHOT.jar:target/lib/*" mu.nu.nullpo.gui.swing.NullpoMinoSwing
