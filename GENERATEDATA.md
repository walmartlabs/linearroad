# How to generate data files

## Notes
The scripts are relatively raw and will be refined over time.

To create the datafiles download the data generator from http://www.cs.brandeis.edu/~linearroad/tools.html.

### Using the original generator
Some extra steps beyond those explained on the tools site were necessary to get the original generator to work.  These will be documented shortly.

### Creating a single combined data file
As stated in the README, datasets of arbitrary sizes can be generated on a single machine or by parallelizing the expressway generation on multiple machines.  But, after generation, these must be cleaned (if desired) and combined.  

**These are the scripts and commands used for cleaning raw files--run on the individual raw files.  (Any number of additional steps can be added as desired.)**

> dataval.py _raw_file_  _temp_outfile_

> datarm2.py _temp_outfile_ > _temp_outfile2_  # remove carids with only <=2 tuples

> datamakeexit.py _temp_outfile2_ > _temp_outfile3_  # make the last type 0 record an exit lane tuple

> mv _temp_outfile3_ _clean_file_

After cleaning, merge the _n_ "clean" files.

> datacombine.py  _directory_of_cleaned_files_  _combined_cleaned_file_

Then, create the tolls and the random re-entrant cars.

> combine.py _combined_cleaned_file_ _directory_for_output_ _number_of_xways_

> * Uses: p_duplicates.py, historical-tolls.pl

>   * Pre-create the following files in the _directory_for_output_ and change permissions accordingly:

>     * touch carsandtimes.csv; touch carstoreplace.csv; chmod 777 carsandtimes.csv; chmod 777 carstoreplace.csv 

> * These steps are necessary as some databases write out files with owner read permissions only.


Clean the generated tolls to match the tuples present in the position reports.

> datafixtype3.py _directory_for_output_/my.data.out _directory_for_output_/my.tolls.out _directory_for_output_/my.tolls.clean



**Recap of scripts and order of usage:**

> On each raw file:

> * dataval.py _raw_file_ _temp_file_1_

> * datarm2.py _temp_file_1_ > _temp_file_2_

> * datamakeexit.py _temp_file_2 > _temp_file_3_

> Using the cleaned files create a single file:

> * datacombine.py _directory_of_cleaned_files_/ _directory_for_output_/clean.combined

> On the single combined file:

> combine.py _directory_for_output_/clean.combined _directory_for_output_ _num_of_xways_

> On the output toll file:

> datafixtype3.py _directory_for_output_/my.data.out _directory_for_output_/my.tolls.out _directory_for_output_/my.tolls.clean

### Final outputs
The final outputs will be: 

> _directory_for_output_/my.data.out

> _directory_for_output_/my.tolls.clean

The scripts _preprawdata.sh_ and _prepcleandata.sh_ combine all the scripts and take a directory of raw or clean files, respectively, and output the final files.
