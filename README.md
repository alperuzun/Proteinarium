# Proteinarium

## Installation
Simply download [Proteinarium.zip](https://drive.google.com/open?id=142WSDsFFtQ4cX28BdUvpavVSe4tiOq8w "Download Proteinarium") and unzip its contents into a folder of your choosing. It contains the precompiled Proteinarium.jar file and two helper files: one for Mac OS X/Unix environments, and another for Windows environments.

##### Requirements
At least one of Java 1.8, Java 9, or Java 10 must be installed in order to run Proteinarium.

## Getting Started
If you are a new user looking to analyze or visualize a dataset for the first time, we recommend the following steps:
1. Install Proteinarium as described above
2. Create the Group 1 gene set file, `group1.txt` that Proteinarium will use. This is just a text file with one line per sample in the data set. Each line has the format `<Sample Identifier> = <HGNC Symbol 1>, <HGNC Symbol 2>, ..., <HGNC Symbol n>` for a sample with `n` genes.
3. If you wish to analyze or visualize a second gene set, for example if you have cases and controls, create the Group 2 gene set file `group2.txt` as in Step 2.
4. Create a configuration file `config.txt` with the following contents:
With only one gene set file:
```
group1GeneSetFile = group1.txt
group2GeneSetFile = group2.txt
# Only include the above line with group2GeneSetFile if you have created the corresponding file and wish to analyze it.
projectName = Project Name Here
```
5. Run Proteinarium using the steps indicated below. The `<arguments>` in this example case would just be `config=config.txt`

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

For a complete list of configuration options and explanations of how they affect Proteinarium, refer to [Configuration](Configuration.pdf).
