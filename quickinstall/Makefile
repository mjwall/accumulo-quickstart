.PHONY : install test stop clean

install :
	./bin/install

test :
	./bin/test-accumulo.sh

stop :
	source ./install-home/bin/cloud-env && stop-all

clean :
	./bin/sbt clean removeInstallPath
