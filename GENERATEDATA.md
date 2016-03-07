# How to generate data files

## Notes
2016-03-07: Initially a few cleansing and combination scripts were re-written in C which yielded tremendous speed benefits.  Most required steps were halved or reduced to a third of the original time.  Then, the scripts were re-written in Java and surprisingly the performance was even faster.  All the scripts were re-written in Java (8u73).  Now a 250 expressway data set can be combined, modified, and completely prepped in less than 24 hours.  A database is no longer necessary.  Generation of raw original files also no longer requires a database.  Details, scripts, and usage follow below.  Set up of raw file generators is also modified to no longer need a database.

For the creation of re-entrant cars, using the previous method--which was still faster than going to a database--took ~30+ hours to create ~200K replacements from a set of ~780K cars with times for a 50 expressway dataset.  The newest method will produce the same number of replacements from the same ~780K cars in seconds.

Java src can be found in the Java directory.  Java cripts were written using IntelliJ 15, community edition.

Also added are stripped versions of Duplicates.pl from the original mitsim generator that no longer needs a database but simply generates raw expressway files.

To create the datafiles download the data generator from http://www.cs.brandeis.edu/~linearroad/tools.html.

### Using the original generator
To get the original generator working on CentOS6.5/6, or other modern 64-bit Linux distribution, the 32-bit compatibility pack must be installed.  If an older, 32-bit version of Linux is available (i.e. 32-bit CentOS 4.8) that works too.  Or, you could try recompiling the mitsim program into a 64-bit version. 

Both a 64-bit OS (CentOS in Azure) with the 32-bit compatibility pack installed and a 32-bit CentOS 4.8 install on a private machine were both successful.

The general steps for Centos 6.5/6 follow:

Download the original tools and unpack into arbitrary directory:

```
wget http://www.cs.brandeis.edu/~linearroad/files/mitsim.tar.gz
mkdir MITSIMLab
cd MITSIMLab
tar xf ../mitsim.tar.gz
```

Install gcc and make if not already installed.  (May no longer be necessaryy as we no longer need PostgreSQL)
```
sudo yum -y install gcc make
```

Install the 32-bit compatibility pack (for original MITSIM generator to work on 64-bit archs):
```
sudo yum -y install compat-libstdc++-296.i686
```

### Running the original data generator script (again, can parallelize by copying files to n machines after modification and starting on n machines) 
To prepare the files from the original data creation process you primarily edit two files:
`mitsim.config` and `linear-road.pl`

In `mitsim.config`: change the `directoryforoutput` to a directory of your choosing and select any number of expressways.

NOTE: remove any trailing blank lines in `mitsim.config` to avoid `use of uninitialized value` errors.

In `linear-road.pl` you have can control a variety of parameters but the only ones we've adjusted are `my $cars_per_hour`, increasing the value to 1000, and `my $endtime`, setting to however long we want the simulation to run.

Make the following changes to `DuplicateCars.pl`:
1. Remove EVERYTHING between the lines `close ( PROPERTIES); ` AND `sub logTime( {` AND keep the code section below with the following changes:
  1. add `my $hostname = hostname`
  2. changed the last `rename` to use `hostname` with an integer suffix (to help with dataorganization if you're generating on multiple macines):
2. Add `use Sys::Hostname` to the top of the file
```
use Sys::Hostname
...

close ( PROPERTIES );

# Add hostname for easier file differentiator
my $hostname = hostname

# You don't need a database to create raw files!

#Full expressway Loop (generates an extra one for half).
for( my $x=0; $x < $numberOfExpressways; $x++){
        # run linear road
        writeToLog ( $logfile, $logvar, "Linear road run number: $x");
        system ("perl linear-road.pl --dir=$dir");
        rename( $dir."/cardatapoints.out.debug" , $dir."/cardatapoints$x.out.debug" );
        rename( $dir."/cardatapoints.out$x" , $dir."/$cardatapoints" );

        rename( $dir."/$cardatapoints" , $dir."/$hostname$x" );
}

sub logTime {
...
```
NOTE: if SELinux is present it may need to be disabled: `sudo setenforce 0`

To kick off the script `./run mitsim.config`

Depending on the endtime and number of expressways chosen the raw data file generator can run for hours, if not days or more.  Each 3 hour 1 raw expressway file set can take ~3-5 hours to generate.  So, it's best to set generation on multiple small machines and leave them alone for a while.  Just ensure that your target directory has enough space to hold the raw data files.  I used 25 separate small VM's with a 70GB disk each to each hold 50 expressways.

The raw data is found under the `directoryforoutput` as N files named `$hostname`N.  N being 0 .. `numberofexpressways`-1.

The original script `DuplicateCars.pl` can perform the process of combining the multiple raw data files but cannot handle in reasonable time a very large number of expressways.  The self-join query mentioned in the general introduction explains why (the progressive slowdown of self-join query that finds duplicates).

In lieu of `DuplicateCars.pl` the directions below can be followed to create arbitrarily large datasets with duplicates.

### Creating a single combined data file
As stated in the README, datasets of arbitrary sizes can be generated on a single machine or by parallelizing the expressway generation on multiple machines.  But, after generation, these must be cleaned (if desired) and combined.  

**These are the scripts and commands used for cleaning raw files--run on the individual raw files.  (Any number of additional steps can be added as desired.)**

```
time java dataval <raw_file> <temp_outfile>
time java datarm2 <temp_outfile> <temp_outfile2>  # remove carids with only <=2 tuples
time java datamakeexit <temp_outfile2> <temp_outfile3>  # make the last type 0 record an exit lane tuple
mv <temp_outfile3> <clean_file>
```
After cleaning move all the clean files into a new directory and merge the _n_ "clean" files.
```
time java datacombine  <dir_of_cleaned_files> <outfile (combined.cleaned.file)>
```
The above command will emit the maximum carid which you need to create the historical tolls file.
Then, create the tolls and the random re-entrant cars.
NOTE: the number of expressways for historical_tolls is 0-indexed, so 250 xways requires an argument of 249.
```
time java historical_tolls <num_xways - 1> <maxcarid> <outfile (raw.toll.file)>
```
The recombination, which was previously the slowest step, now happens in minutes.
The first step creates the carsandtimes table originally performed in a database.  This version is much, much faster than the original using a database.  The overlap was set to 10 and determines the percentage of cars to use as the candidate pool for re-entrance.
```
time java create_carsandtimes_and_2replace_mt_1 <infile (clean combined file)> <overlap> <outfile (cars and times)>
```
Now, create the cars to replace.  This step only took 32 minutes for a 250 expressway set.
```
time java create_carsandtimes_and_2replace_mt_2 (infile (cars and times)> <dummy var> <outfile (cars to replace)>
```
Now perform the actual replacements.  No DB necessary, but we split into N xway separate files so we can time order the single file later.  The output is N xway files named `replaced.part-N` using the outfile prefix, a dash, and an int.
```
time java replacecars_1 <infile (cars to replace)> <infile (clean combined file)> <outfile prefix (i.e. replaced.part)>
```
Move files to a new directory to hold the individual xways.
```
mkdir temp
mv replaced.part* temp/ ; 
```
Now, combine the parts into a single, time-ordered file.
```
time java combine_after_replace temp/ <outfile (final data file)>
```
Now clean the generated tolls to match the tuples present in the position reports.
```
time java fixtolls <infile (raw toll file)> <infile (final data file)> <outfile (final toll file)>
```
Make sure you have enough space on your hardrives to handle all files and temp files.  Each xway will generate ~1GB of position data and ~330MB of toll data.  Using multiple disks is recommended for temp and final file output.  I.e. for a 250 xway set: 250 GB for individual clean files, 250GB for combined clean file, 82-7GB for raw toll file, 250GB for split replaced parts, 250GB for final file, 82-7GB for final toll file, for a total of roughly 1.5 TB of free space to generate a 250 xway set.

All the commands can be combined into a single line bash call as shown below.
`datadrive` and `datadrive2` are my data directories.
NOTE: I set an env variable to hold the maxcarid and I `cd` into the directory with my java files and use full paths for all files and directories.
```
maxcarid=0 ; cd /datadrive/java/dataval/out/production/dataval/ ; time maxcarid=$(java datacombine /datadrive/clean /datadrive2/250.combined) ; time java historical_tolls 249 $maxcarid /datadrive2/250.tolls.raw ; time java create_carsandtimes_and_2replace_mt_1 /datadrive2/250.combined 10 /datadrive2/250.carsandtimes ; time java create_carsandtimes_and_2replace_mt_2 /datadrive2/250.carsandtimes 1 /datadrive2/250.carstoreplace ; time java replacecars_1 /datadrive2/250.carstoreplace /datadrive2/250.combined /datadrive2/250.replaced.part ; mkdir /datadrive2/250.temp ; mv 250.replaced.part* /datadrive2/250.temp ; time java combine_after_replace /datadrive2/250.temp /datadrive/3h250x.dat ; time java fixtolls /datadrive2/250.tolls.raw /datadrive/3h250x.dat /datadrive/3h250x.tolls.dat
```
