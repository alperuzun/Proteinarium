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
### Arguments
The `<arguments>` are allowed to take one of the following forms:
1. Show help: `-h` or `--help`
2. Show available configuration options: `-o` or `--options`
3. Show default configuration options: `-d` or `--default-config`
**Note**: you can use the output of this command to generate an easy-to-modify configuration file.
4. Specify the configuration file: `config=<configuration file>`
where `<configuration file>` is the path to a text file containing all configuration options for this run of Proteinarium
5. Set the configuration options directly:
`group1GeneSetFile=<value> projectName=<value> maxPathLength=<value> ...`
This option is provided primarily for scripting purposes; it is instead recommended to specify a configuration file as detailed above.

For a complete list of configuration options and explanations of how they affect Proteinarium, refer to the *Configurability* section of the [Methods](Methods.pdf) document.
