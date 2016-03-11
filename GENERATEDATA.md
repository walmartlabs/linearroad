# How to generate data files

## Notes
2016-03-07: Initially a few cleansing and combination scripts were rewritten in C which yielded tremendous speed benefits.  Run-time for most required steps were halved or reduced to a third of the original time.  Then, the scripts were rewritten in Java (8u73) and surprisingly the performance was even faster.  All the scripts were rewritten in Java.  Now a 250 expressway data set can be combined, modified, and completely prepped in less than 24 hours, potentially 12 hours.  A database is no longer necessary.  Generation of raw data files also no longer requires a database.  Details, scripts, and usage follow below.

For the creation of re-entrant cars, using the previous v.1 of our scripts method--which was still faster than going to a database--took ~30+ hours to create ~200K replacements from a set of ~780K "carsandtimes" for a 50 expressway dataset.  The newest method will produce the same number of replacements from the same ~780K cars in seconds.

Making the same logic changes to the original Python code would have yielded orders of magnitude benefits in run-times as well.  The Java version will be a constant factor faster, ~2 to 3.  

Java src can be found in the Java directory.  The Java code was written using IntelliJ 15, Community Edition, which supports a very nice vi mode.

To create the datafiles first download the data generator from http://www.cs.brandeis.edu/~linearroad/tools.html.

### Using the original generator
To get the original generator working on CentOS6.5/6, or other modern 64-bit Linux distribution, a 32-bit compatibility pack must be installed.  If an older, 32-bit version of Linux is available (i.e. 32-bit CentOS 4.8) that works as well.  Or, you could try rewriting and recompiling the mitsim program into a 64-bit version. 

Both a 64-bit OS (CentOS in Azure) with the 32-bit compatibility pack installed and a 32-bit CentOS 4.8 instance on a local lab machine were both successful.

The general steps for Centos 6.5/6 follow:

Download the original tools and unpack into an arbitrary directory:

```
wget http://www.cs.brandeis.edu/~linearroad/files/mitsim.tar.gz
mkdir MITSIMLab
cd MITSIMLab
tar xf ../mitsim.tar.gz
```

Install the 32-bit compatibility pack (for original MITSIM generator to work on 64-bit archs) on CentOS 6.6:
```
sudo yum -y install compat-libstdc++-296.i686
```

Install gcc and make if not already installed.
```
sudo yum -y install gcc make
```
Install the appropriate Perl modules for mitsim.
```
sudo perl -MCPAN -e "install Math::Random"
```
Let CPAN do automatic configuration: [yes] or [Enter].

### Running the original data generator script

(Again, raw file generation can be parallelized by copying mitsim files and folders to n machines after modifying the mitsim files.) 
To prepare the files for raw data creation edit three files:
`mitsim.config`, `linear-road.pl`, and `Duplicates.pl`.

In `mitsim.config`: change the `directoryforoutput` to a directory of your choosing and select any `numberofexpressways` based on free disk-space (1 xway ~ 1GB).  The only lines necessary are `directoryforoutput` and `numberofexpressways` the rest can be deleted.  

NOTE: remove any trailing blank lines in `mitsim.config` to avoid a Perl `use of uninitialized value` errors from `Duplicates.pl`.

In `linear-road.pl` you have can control a variety of parameters but the only one we adjust is `my $cars_per_hour`, increasing the value to 1000.  `my $endtime` can also be adjusted if shorter or longer simulation times are desired.

In `DuplicateCars.pl`:
1. Remove EVERYTHING (there are quite a few lines) between the lines `close ( PROPERTIES ); ` AND `sub logTime( {` leaving only the lines below and also making the following changes:
  1. add `my $hostname = hostname` after the `close ( PROPERTIES );`
  2. modify the last `rename` line to use `hostname` with an integer suffix (to help with data organization if you're generating on multiple macines) and to rename from `cardatapoints.out$x` : 
2. Add `use Sys::Hostname` to the top of the file
  1. Optionally remove the `use DBI;` line
```
use Sys::Hostname
...
close ( PROPERTIES );

# Add hostname for easier file differentiation
my $hostname = hostname

# You don't need a database to create raw files!

#Full expressway Loop (generates an extra one for half).
for( my $x=0; $x < $numberOfExpressways; $x++){
        # run linear road
        writeToLog ( $logfile, $logvar, "Linear road run number: $x");
        system ("perl linear-road.pl --dir=$dir");
        rename( $dir."/cardatapoints.out.debug" , $dir."/cardatapoints$x.out.debug" );
        rename( $dir."/$cardatapoints" , $dir."/$hostname$x" );
}

sub logTime {
...
```
NOTE: if SELinux is present it may need to be disabled: `sudo setenforce 0`

To kick off the script `./run mitsim.config`.

Depending on the endtime (in `linear-road.pl`) and number of expressways chosen (in `mitsim.config`) the raw data file generator can run for hours, if not days or more.  Each 3 hour 1 expressway (with 1000 cars/hours/seg) raw file can take ~3-5 hours to generate.  So, it's best to set generation on multiple small machines and leave them alone for a while.  Just ensure that your target directory has enough space to hold the raw data files.  I used 25 separate small VM's with a +60GB data disk each to each hold 50 expressways.

The raw data files will be found under the `directoryforoutput` as configured in `mitsim.config` as n files named `$hostname`n.  n being 0 .. `numberofexpressways`-1.

The original script `DuplicateCars.pl` handled the process of combining the multiple raw data files along with creating re-entrants and the toll file but it could not handle, in a reasonable amount of time, large numbers of expressways.  The self-join query mentioned in the general introduction explains one of the bottlenecks (the progressive slowdown of the self-join query that finds re-entrants).

Everthing after raw file creation has been re-written, along with the addition of some data cleansing steps.  Python was used first.  Then C and Java.  Java (8u73) turned out to be the fastest for these scripts.

### Creating a single combined data file
As stated in the README, datasets of arbitrary sizes can be generated on a single machine or by parallelizing the expressway generation on multiple machines.  But, after generation, these must be cleaned (if desired) and combined.  

**These are the scripts and commands used for cleaning raw files--run on the individual raw files.  (Any number of additional steps can be added as desired.)**

```
time java dataval <raw_file> <temp_outfile>
time java datarm2 <temp_outfile> <temp_outfile2>  # remove carids with only <=2 tuples
time java datamakeexit <temp_outfile2> <temp_outfile3>  # make the last type 0 record an exit lane tuple
mv <temp_outfile3> <clean_file>
```
The raw files above can be cleansed in parallel as well as on n machines.

On each machine, or a single machine, place the raw files in a new directory and from the directory with the java classes run:
```
for f in $(ls <dir_of_raw_files>) ; do time java dataval $f t1 ; time java datarm2 t1 t2 ; time java datamakeexit t2 t3 ; mv t3 $f.clean ; done
```
Using absolute paths may be helpful.

After cleaning move all the clean files into a new directory to merge the n "clean" files.
```
time java datacombine  <dir_of_cleaned_files> <outfile (combined_cleaned_file)>
```
The above command will emit the maximum carid which you need to create the historical tolls file.

Then create the historical toll file and the random re-entrant cars.

Create the toll file.
```
time java historical_tolls <numxways> <maxcarid> <outfile (raw_toll_file)>
```
NOTE: number of expressways == 3 for historical_tolls will yield xways from 0 - 2

The finding of re-entrants, which was previously the slowest step, now happens in minutes.
The first step creates the carsandtimes table originally performed in a database.  This version of making the carsandtimes table, finding replacements, and making replacements is much, much faster than the database-dependent original or logic+scripts from v.1.  The overlap is set to 10 (can be more or less) and determines the percentage of cars to use as the candidate pool for potentially re-entrant cars.

Create the carsandtimes table.
```
time java create_carsandtimes <infile (clean_combined_file)> <overlap> <outfile (cars_and_times)>
```
Create the carstoreplace file.  (This step only took minutes for a 250 expressway set.)
```
time java create_carstoreplace (infile (cars_and_times)> <outfile (cars_to_replace)> <numxways>
```
Now perform the actual replacements.  Again, no database necessary, but we split into n xway separate files so we can time order into a single file later.  The output is n xway files named `replaced.part-N` using the outfile prefix, a dash, and an int.
```
time java replacecars <infile (cars_to_replace)> <infile (clean_combined_file)> <outfile prefix (i.e. replacedprefix)>
```
Move files to a new directory to hold the n individual xways.
```
mkdir temp
mv replaced.part* temp/ ; 
```
Combine the n parts into a single, time-ordered file.
```
time java combine_after_replace temp/ <outfile (final_data_file)>
```
Finally, clean the generated tolls to match the tuples present in the position reports.
```
time java fixtolls <infile (raw_toll_file)> <infile (final_data_file)> <outfile (final_toll_file)>
```
Make sure you have enough space on your hard drives to handle all files and temp files.  Each xway will generate ~1GB of position data and ~300MB of toll data.  Using multiple disks is recommended for temp and final file output.  I.e. for a 250 xway set: 250 GB for individual clean files, 250GB for the combined clean file, 82-7GB for the raw toll file, 250GB for the split replaced parts, 250GB for the final file, and 82-7GB for the final toll file for a total of roughly 1.5 TB of free space to generate a 250 xway set without deleting intermediate files.

All the commands (after having a directory of cleaned files) can be combined into a single line bash call as shown below.
`datadrive` and `datadrive2` are my data directories.
NOTE: I set an env variable to hold the maxcarid, `cd` into the directory containing the java class files, and use full paths for all files and directories.
```
maxcarid=0 ; cd /datadrive/java/LRDataGen/out/production/LRDataGen/ ; \
time maxcarid=$(java datacombine /datadrive/tmp_clean /datadrive2/3.combined) ; \
time java historical_tolls 3 $maxcarid /datadrive2/3.tolls.raw ; \
time java create_carsandtimes /datadrive2/3.combined 10 /datadrive2/3.carsandtimes ; \
time java create_carstoreplace /datadrive2/3.carsandtimes /datadrive2/3.carstoreplace 3 ; \
time java replacecars /datadrive2/3.carstoreplace /datadrive2/3.combined /datadrive2/3.replaced.part ; \
mkdir /datadrive2/3.temp ; \
mv /datadrive2/3.replaced.part* /datadrive2/3.temp ; \
time java combine_after_replace /datadrive2/3.temp /datadrive/3h3x.dat ; \
time java fixtolls /datadrive2/3.tolls.raw /datadrive/3h3x.dat /datadrive/3h3x.tolls.dat
```
Timings for two 3 hour 3 xway runs:
```
[datacombine]
real    2m9.941s      2m14.096s
user    2m1.897s      2m4.121s
sys     0m9.083s      0m0.301s

[historical_tolls]
real    0m29.181s     0m27.873s
user    0m18.123s     0m18.440s
sys     0m2.051s      0m2.042s

[create_carsandtimes]
real    1m12.947s     1m12.847s
user    1m12.000s     1m12.160s
sys     0m2.498s      0m2.536s

[create_carstoreplace]
real    0m1.411s      0m1.669s
user    0m2.509s      0m2.883s
sys     0m0.114s      0m0.138s

[replacecars]
real    2m8.118s      1m57.721s
user    1m43.463s     1m49.996s
sys     0m10.084s     0m8.635s

[combine_after_replace]
real    1m25.026s     1m30.243s
user    1m13.299s     1m12.807s
sys     0m11.676s     0m11.861s

[fixtolls]
real    0m58.770s     0m57.163s
user    0m52.739s     0m51.802s
sys     0m5.515s      0m4.926s

Total:  
(real)  8m22.394s      8m21.612s
```
