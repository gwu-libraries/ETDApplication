#! /usr/bin/perl

use MARC::Record;
use MARC::Batch;
use MARC::File::MARCMaker;


my ($input,$output) = @ARGV;

die "couldn't find $input" if !(-e $input);

open (OUT,">$output");
 

my $batch = MARC::Batch->new('MARCMaker', $input);

while (my $record = $batch->next()) {
  print OUT $record->as_usmarc();
}
