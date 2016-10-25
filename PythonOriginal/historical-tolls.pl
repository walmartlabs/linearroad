#!/usr/bin/perl -w

# This was the original file used to generate historical tolls from the maxcarid is a given data set.
# Note that when they say $max_xway0 they really mean the max car, or vehicle id (vid).

@ARGV == 3 or die("to generate toll-history, give me # of xways, maxcarid");

my $xway = $ARGV[0];
my $max_xway0 =$ARGV[1];
my $dir=$ARGV[2];

#open(OUT, ">$dir/xway$xway.historical-tolls");
# Put all historical tolls of different expressway in 1 files
open(OUT, ">$dir/historical-tolls.out");


for (my $vid = 1; $vid <= $max_xway0; ++$vid) {
    for (my $day = 1; $day <= 69; ++$day) {
        my $toll = int(rand(99));
        $xway1 = int(rand ($xway));
        # (vid, day, xway, tolls)
        print OUT "$vid,$day,$xway1,$toll\n";
    }
}
close (OUT);
