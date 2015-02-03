This directory contains all source, library and batch files to compile and run the DataWarrior
application as a datawarrior.jar file on any Linux or Macintosh OSX operating system.
Once the datawarrior.jar file is created, it can be used on a Windows platform as well.

The compilation process requires a correctly installed Java Development Kit (JDK).

This directory does not contain instructions or batch files to create platform specific installers,
measures for platform specific DataWarrior file type registration nor any sample data files.
Thus, if you make changes to the source code and compile an updated datawarrior.jar for yourself,
then it is still recommended to download and run the platform specific datawarrior installer from
www.openmolecules.org/datawarrior/download.html and to replace afterwards the datawarrior.jar file
of the original installation with your new one.

Unfortunately, this does not work on Windows platforms, because a proper platform integration on
Windows requires the application to be an .exe file. Therefore, on Windows the datawarrior.jar,
the application icon and all document icons are embedded in the DataWarrior.exe file as resources.

Note: The datawarrior.jar file of the platform specific installers are obfuscated, which makes the
datawarrior.jar file significantly smaller, because all class, method and variable names are
replaced by much shorter alternatives and because unused methods are removed from all classes.

To compile DataWarrior from the source code change the current directory to this directory and run
the buildDataWarrior shell script from a terminal window as:

cd publicSource
./buildDataWarrior

To then run the application run the runDataWarrior shell script from a terminal

./runDataWarrior

(you may run 'chmod 755 buildDataWarrior' and 'chmod 755 runDataWarrior' if the scripts miss the
executable permission)

