This application is run every month by the RDG department to process the Electronic thesis and Dissertation for the previous month.

12/2014, justinlittman:  This is highly legacy code.  I have attempted to clean it up just enough so that it will run.
Also, I discovered by decompiling the bytecode on the server that the latest code was never committed to source control.

To build:

```
mvn clean package
```

This will produce target/etdapp-bin.zip.

To install:

Unzip etdapp-bin.zip, rename example.etd.props to etd.props and update.  **YOU MUST ADD THE SFTP PASSWORDS.**

To execute:

```
./etdapp.sh [MMYYYY]
```
If MMYYYY is omitted, the previous MMYYYY is used.

Notes:

* Output is logged to log.txt.
* When rerunning a month, make sure to remove MMYYYY from cWorkDir/record.
* The following are some requirements:
	* java
  	* perl with MARC::Record and MARC::File::MARCMaker
  	* yaz-marcdump
  	* unzip