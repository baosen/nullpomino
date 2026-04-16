#!/bin/sh
mvn package || exit $?

resolve_app_jar() {
	final_name=$(awk -F'[<>]' '/<finalName>/{print $3; exit}' pom.xml)
	if [ -n "$final_name" ]; then
		printf '%s\n' "target/$final_name.jar"
		return
	fi

	artifact_id=$(awk -F'[<>]' '/<artifactId>/{print $3; exit}' pom.xml)
	version=$(awk -F'[<>]' '/<version>/{print $3; exit}' pom.xml)
	printf '%s\n' "target/$artifact_id-$version.jar"
}

JAR=$(resolve_app_jar)
if [ ! -f "$JAR" ]; then
	echo "Built jar not found: $JAR" >&2
	exit 1
fi

export LD_LIBRARY_PATH="/usr/local/lib:$LD_LIBRARY_PATH"
java -Djna.library.path=/usr/local/lib:/usr/lib/x86_64-linux-gnu -cp "$JAR:target/lib/*" mu.nu.nullpo.gui.sdl.NullpoMinoSDL
