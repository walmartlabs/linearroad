# How to generate data files

## Notes
The scripts are relatively raw and will be refined over time.

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

Install and set up the PostgreSQL database (these instructions may vary based on the version of PostgreSQL).  For version 8.4.0 that the default CentOS 6.5/6 repo in Azure installs:

```
sudo yum -y install postgresql postgresql-server
sudo service postgresql initdb
sudo service postgresql start
sudo su postgres
psql
psql> create user <linux username>;  # this should be the same username from which scripts will be run
psql> alter role <linux username> with superuser login;
psql> create database test;
```

Install gcc and make if not already installed.
```
sudo yum -y install gcc make
```
Install the appropriate Perl modules for the scripts to interact with postgresql.
```
sudo perl -MCPAN -e "install DBI"
sudo perl -MCPAN -e "install DBD::PgPP"
sudo perl -MCPAN -e "install Math::Random"
```
Install the 32-bit compatibility pack:
```
sudo yum -y install compat-libstdc++-296.i686
```
You should now have PostgreSQL setup with an appropriate user and database along with the proper Perl modules.  To test database connectivity modify the included *test.pl* file to point to the new database connection: 
```
DBI->connect("DBI:PgPP:dbname=test", "<linux username>", "")
```
and insert a `print $dbh;` statement after the connection statement to test for connectivity.  If it prints something like DBI::db=HASH(0x138f1a0) the connection should be good.

### Running the script
To start the data creation process you primarily edit two files:
`mitsim.config` and `linear-road.pl`

Note that due to differences in PostgreSQL 8.4.0+ from 7.x.x, the latter being the version used by the original code, line 197 of `DuplicateCars.pl` should be changed from:
```
$dbquery="UPDATE input SET carid=carstoreplace.carid WHERE carid=carstoreplace.cartoreplace;";
```
to:
```
$dbquery="UPDATE input SET carid=carstoreplace.carid FROM carstoreplace WHERE input.carid=carstoreplace.cartoreplace;";
```
Note that this is not necessary if all we're generating are the raw files for later processing.

In `mitsim.config`: change the `directoryforoutput` to a directory of your choosing, `databasename` to "test", set the `databasepassword` to `databasepassword=` if you don't have a password for the user, and select any number of expressways.

NOTE: remove any trailing blank lines in `mitsim.config` to avoid `use of uninitialized value` errors.

In `linear-road.pl` you have can control a variety of parameters but the only ones we've adjusted are `my $cars_per_hour`, increasing the value to 1000, and `my $endtime`, setting to however long we want the simulation to run.

To kick off the script `./run mitsim.config`

NOTE: if SELinux is present it may need to be disabled: `sudo setenforce 0`

NOTE: the table `input` must be manually dropped or cleared between runs.  This table is not automatically dropped because if file permissions are not right the final data can still be found in the `input` table even if it's not written out as `cardatapoints.out`.  `cardatapoints.outN` are the raw files.  `cardatapoints.out` is the final output after running duplications or re-entrants--as we've called them.

To drop the database table `input`:
```
psql -d test  # use the -d flag to choose a database, otherwise psql will default to trying to connect to a database with the same name as the user
psql> drop table input;
```
And also, remove the output files from the chosen output directory, moving any of the raw `cardatapoints.outN` files first if desired.

For convenience, add the following lines to `DuplicateCars.pl` before the statements that create the table `input`:
```
writeToLog ( $logfile, $logvar, "Dropping input table.");
$dbquery="DROP TABLE IF EXISTS input;";
$sth=$dbh->prepare("$dbquery") or die $DBI::errstr;
$sth->execute;
unlink glob $dir."/*";  # remove previous files from output directory
```
Depending on the endtime and number of expressways chosen the program can run for hours, if not days or more.  Each 3 hour 1 expressway set can take ~3-5 hours to generate.

The raw data is found under the `directoryforoutput` as N files named `cardatapoints.out`N.  N being 0 .. `numberofexpressways`-1.

The script `DuplicateCars.pl` can perform the process of combining the multiple raw data files but cannot handle in reasonable time a very large number of expressways.  The self-join query mentioned in the general introduction explains why (the progressive slowdown of self-join query that finds duplicates).  The `directoryforoutput` must also be readable and writeable by the user `postgres`.

In lieu of `DuplicateCars.pl` the directions below can be followed to create arbitrarily large datasets with duplicates.

### Creating a single combined data file
As stated in the README, datasets of arbitrary sizes can be generated on a single machine or by parallelizing the expressway generation on multiple machines.  But, after generation, these must be cleaned (if desired) and combined.  

**These are the scripts and commands used for cleaning raw files--run on the individual raw files.  (Any number of additional steps can be added as desired.)**

```
dataval.py <raw_file>  <temp_outfile>
datarm2.py <temp_outfile> > <temp_outfile2>  # remove carids with only <=2 tuples
datamakeexit.py <temp_outfile2> > <temp_outfile3>  # make the last type 0 record an exit lane tuple
mv <temp_outfile3> <clean_file>
```
After cleaning, merge the _n_ "clean" files.
```
datacombine.py  <dir_of_cleaned_files>  <combined_cleaned_file>
```
Then, create the tolls and the random re-entrant cars.
```
combine.py <combined_cleaned_file> <output_dir> <num_xways>
  # combine.py uses: p_duplicates.py, historical-tolls.pl
  # Also, pre-create the following files in the <output_dir> and change permissions accordingly:
touch carsandtimes.csv; touch carstoreplace.csv; chmod 777 carsandtimes.csv; chmod 777 carstoreplace.csv 
  #These steps are necessary as some databases write out files with owner read permissions only, but c
```
Clean the generated tolls to match the tuples present in the position reports.
```
datafixtype3.py <output_dir>/my.data.out <output_dir>/my.tolls.out <output_dir>/my.tolls.clean
```

**Recap of scripts and order of usage:**

> On each raw file:
```
dataval.py <raw_file> <temp_file_1>
datarm2.py <temp_file_1> > <temp_file_2>
datamakeexit.py <temp_file_2> > <temp_file_3>
```
> Using the cleaned files create a single file:
```
datacombine.py <dir_of_cleaned_files>/ <output_dir>/clean.combined
```
> On the single combined file:
```
combine.py <output_dir>/clean.combined <output_dir> <num_xways>
```
> On the output toll file:
```
datafixtype3.py <output_dir>/my.data.out <output_dir>/my.tolls.out <output_dir>/my.tolls.clean
```
### Final outputs
The final outputs will be: 
```
<output_dir>/my.data.out
<output_dir>/my.tolls.clean
```
The scripts `preprawdata.sh` and `prepcleandata.sh` combine all the scripts and take a directory of raw or clean files, respectively, and output the final files.
