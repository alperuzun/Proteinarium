There are two datasets to run Proteinarium.

1) Running Proteinarium on Simulated Data

FILES:
geneset files:
- SIMdataset1_original.txt
- SIMdataset2_original.txt
configuration file:
- config_SIM.txt
----------------------------------------------------------

2) Running Proteinarium on Use Case

"Gene expression in prostatic tissue from benign prostatic hyperplasia (BPH) patients."
We used this pipeline to reanalyze the gene expression analysis of prostate cancer.
This study compared the transcriptome profile of 12 SRD5A2-methylated
and 10 SRD5A2-unmethylated prostate cancer patients.
We identified top 100 seed genes for each patient based on their z score.
NCBI GEO database, GSE101486.
PMID: 28940538.

FILES:
geneset files:
- methylated_srd5a2_prostate_gene_list_top100.txt
- unmethylated_srd5a2_prostate_gene_list_top100.txt

configuration file:
- config_prostate_dataset.txt
