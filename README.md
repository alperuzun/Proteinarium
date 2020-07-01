# Proteinarium

## Installation
Simply download [Proteinarium.zip](https://drive.google.com/file/d/14Lz1EVj1qY9N5f9_KrMoiYJeqhAo8XvA/view?usp=sharing) and unzip its contents into a folder of your choosing. It contains the precompiled Proteinarium.jar file and two helper files: one for Mac OS X/Unix environments, and another for Windows environments. You can download Graphical User Interface (GUI) version from this link, [ProteinariumGUIversion.zip](https://drive.google.com/file/d/1UUslsLBRtmyPb7M3xm-DlsPCNZBsV7K0/view?usp=sharing).

##### Requirements
At least one of Java 1.8, Java 9, or Java 10 must be installed in order to run Proteinarium.

----------------------------------------------
## Running Proteinarium on Command Line

## 1. Getting Started
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

## 2. Running Proteinarium 
The `<arguments>` is `config=config.txt`

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

### Output
All output files go to the folder specified by the `outputDirectory` configuration option. By default, this goes to a folder called `output` in the same directory that you run Proteinarium from.
* **\<projectName\>_ClusterAnalyses.csv**: cluster analysis files
* **\<projectName\>_Dendrogram.png**: dendrogram image
* **\<projectName\>_Dendrogram.txt**: representation of the dendrogram in Newick tree format
**Note**: if the entry is a single gene list, the above output files will not be generated. The output on the single sample will still be available as described below.

## 3. Visualizing Clusters 
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

----------------------------------------------
## Running Proteinarium with Graphical User Interface (GUI)

## 1.	Getting Started

*	Create the Gene Set File 1.  This must be a text file with one line per sample. Each line has the format <Sample Identifier> = <HGNC Symbol 1>, <HGNC Symbol 2>, ..., <HGNC Symbol n> for a sample with n seed genes.

*	(Optional) If you wish to analyze a dichotomous phenotype, a second gene set file can be created for Gene Set 2.  This file must be formatted the same way as done for Gene Set 1.  

## 2.	Running Proteinarium 

### Selecting Files

*	In the GUI, select **File > New analysis** 

*	Choose the data files for upload.  If you are only analyzing one group or a single sample, leave the Geneset File 2 file field empty. NOTE: If you would like to select new Gene Set Files, click on the “Clear Gene Files” button before selecting new Gene Set Files. 

*	Enter a Project Name into the field “Project Name”.  If left blank, the program default to the project name “SIM” 

### Setting Parameters – Configuration File
*	Before running Proteinarium, confirm or make changes to the program’s configurable parameters.  A table with all possible parameters, their definitions and their default settings are provided by selecting **Help > Available Configurations**.  

*	Select **File > Settings** or “Settings” button to view and change parameters.  If any changes are made click “Apply Changes” before exiting. NOTE: The values of the parameters for a New Analysis are not all set to the Default values. These are the values used in our testing and validation with bootstrapping turned off (ie 0 iterations). 

*	For Advance Settings Select **File > Settings** and click on the “Advance Settings” button. Once changed click on “Set Settings” and the “Apply Changes” buttons. 

### Running Proteinarium
*	Once files are uploaded and parameters are set, click “Run Proteinarium”

*	If more than one sample is contained within the input file (s), the dendrogam will be displayed.  All output files generated by Proteinarium will be saved to the folder specified by the outputDirectory configuration option. By default, this goes to a folder called “Output” in the same directory as the Proteinarium.jar file. 

 **	**<projectName>_ClusterAnalyses.csv:** cluster analysis files
 
 **	**<projectName>_Dendrogram.png:** dendrogram image
 
 **	**<projectName>_Dendrogram.txt:** representation of the dendrogram in Newick tree format 
 

## 3.	Viewing Clusters 

*	When Proteinarium is done running, the GUI will navigate to a new window in which you can input the Cluster/Sample ID for which you are interesting in viewing or obtaining more information.

*	Enter sample ID or branch number (ex: C12) in the space provided. 

*	Select either “View Cluster” or “Get Cluster Information”.  

*	If “View Cluster” is selected, the corresponding the output files will be available in the \<outputDirectory\>/<cluster or sample ID\> folder. And the following files are generated:  
 **	**<cluster or sample ID>_Dendrogram.txt**
 
 **	For each of the five possible output networks--Group 1, Group 2, [Group 1 + Group 2], [Group 1 - Group 2], [Group 2 - Group 1], three files are generated to summarize that network. For example:

   1.	**<cluster or sample ID>_Group1_GeneSet.txt:** list of genes in the network and information about which input set they originated from (i.e. from Group 1, Group 2, or imputed from the interactome) and on how many samples the gene was found
 
   2.	**<cluster or sample ID>_Group1_Interactions.txt:** network interaction matrix
 
   3.	**<cluster or sample ID>_Group1.png:** image of the network

*	If “Get Cluster Information” is selected, the following information will be appended to the file “SystemsOutput.txt” as indicated by the text box of the GUI. 
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

*	Output files can be opened from the GUI: select File > Open to open a file explorer window.


## 4.	Change the Parameter Configuration and Re-run Proteinarium:
 
*	Select **File > Home** to return to main screen for running Proteinarium

*	Select **File > Settings**

*	Change desired parameter values 

*	Click Apply Changes 

*	Click “Run Proteinarium” to re-run Proteinarium with new configuration. NOTE: Change Project Name otherwise the previous data will be overwritten.  




