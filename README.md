# Proteinarium

## Installation
Simply download [Proteinarium.zip](https://drive.google.com/open?id=142WSDsFFtQ4cX28BdUvpavVSe4tiOq8w "Download Proteinarium") and unzip its contents into a folder of your choosing. It contains the precompiled Proteinarium.jar file and two helper files: one for Mac OS X/Unix environments, and another for Windows environments. You can download Graphical User Interface (GUI) version from this link, ProteinariumGUIversion.zip.

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

## Output
All output files go to the folder specified by the `outputDirectory` configuration option. By default, this goes to a folder called `output` in the same directory that you run Proteinarium from.
* **\<projectName\>_ClusterAnalyses.csv**: cluster analysis files
* **\<projectName\>_Dendrogram.png**: dendrogram image
* **\<projectName\>_Dendrogram.txt**: representation of the dendrogram in Newick tree format
**Note**: if the entry is a single gene list, the above output files will not be generated. The output on the single sample will still be available as described below.

**To analyze and visualize any cluster or individual sample from the dendrogram, enter \<cluster or sample ID\> (branch number) on the command line (ex: *C12*).** The corresponding analysis information and images will be available in the `\<outputDirectory\>/<cluster or sample ID\>` folder. For example, if with default options, one were to analyze cluster *C12*, the output would be located in `output/C12/`. The following output files are generated:
* **\<cluster or sample ID\>_Dendrogram.txt**
Then, for each of the five possible output networks--Group 1, Group 2, [Group 1 + Group 2], [Group 1 - Group 2], [Group 2 - Group 1], three files are generated to summarize that network. For example:
* **\<cluster or sample ID\>_Group1_GeneSet.txt**: list of genes in the network and information about which input set they originated from (i.e. from Group 1, Group 2, or imputed from the interactome) and on how many samples the gene was found
* **\<cluster or sample ID\>_Group1_Interactions.txt**: network interaction matrix
* **\<cluster or sample ID\>_Group1.png**: image of the network

To view the summary information for a particular cluster or sample, enter "info \<cluster ID\>" (ex: *info C87*) into the command line. The following information will be displayed:
* *Average Distance (Height)*
* *Bootstrapping Confidence*
* *Total Number of Samples*
* *Number in Group 1* (number of samples)
* *Number in Group 2* (number of samples)
* *p-value (Fisher Exact test for Group 1 and Group 2)*
* *Group 1 and Group 2 Clustering Coefficient*
* *Group 1 Clustering Coefficient*
* *Group 2 Clustering Coefficient*
* *Group 1 minus Group 2 Clustering Coefficient*
* *Group 2 minus Group 1 Clustering Coefficient*
* *Group 1 Patients* (Sample IDs of the individuals)
* *Group 2 Patients* (Sample IDs of the individuals)
**Note**: the above information is available for all patients at any time in the **\<projectName\>_ClusterAnalyses.csv** output file.
