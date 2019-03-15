# Proteinarium

## Installation
Simply download [Proteinarium.zip](https://drive.google.com/open?id=142WSDsFFtQ4cX28BdUvpavVSe4tiOq8w "Download Proteinarium") and unzip its contents into a folder of your choosing. It contains the precompiled Proteinarium.jar file and two helper files: one for Mac OS X/Unix environments, and another for Windows environments.

##### Requirements
At least one of Java 1.8, Java 9, or Java 10 must be installed in order to run Proteinarium.

## Running Proteinarium
The most reliable way to run Proteinarium is to run the following command:
```bash
java -jar <path to Proteinarium.jar> <arguments>
```
This will work regardless of what operating system you are using, so long as you jave Java 1.8 or above installed. The helper files simply invoke `java -jar Proteinarium.jar` along with any additional arguments you passed in from the command line.

##### Mac OS X and Unix Environments
Open up a terminal, navigate to the folder containing Proteinarium.jar, and execute:
```bash
./proteinarium.sh <arguments>
```
##### Windows Environments
Open a command prompt, navigate to the folder containing Proteinarium.jar, and execute:
```bash
proteinarium.bat <arguments>
```
