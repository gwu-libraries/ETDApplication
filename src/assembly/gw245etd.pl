#!/usr/bin/perl

##
##


use MARC::Record;
use MARC::Batch;
use MARC::Field;

my ($input,$output) = @ARGV;

die "couldn't find $input" if !(-e $input);

my $batch = MARC::Batch->new('USMARC', $input);

open (OUT, ">$output");

  while (my $record = $batch->next()) {

##Begin inserting mandatory fields


## 201006: some xml field contain only a period no other data
## review all field, delete "blank" field

   for my $fieldALL ( $record->field('[56]..')) { 
     my $fieldA = $fieldALL->subfield('a') if ($fieldALL->subfield('a'));

      if ($fieldA =~ /^\./ ) {
        print $record->field('001')->as_string(), " ", $fieldA, "\n";
        
        $record->delete_field($fieldALL);
       }
     }

## 245 initial articles (The, A and An), edit 2nd indicator value

   my $f245 = $record->field('245');
   my $f245a = $f245->as_string('a');

## rm non special char, i.e. double quotes
      $f245a =~ s/[\"\']//g;

## if title is ALL CAPS, cap only first letter
       if ( $f245a eq uc $f245a ) {    ## checking via uc command
            $f245a =~ s/([\w']+)/\u\L$1/g; ## CAP 1st character
          }
      
      $f245->delete_subfields( 'h');

    if ( $f245a =~ /^The / ) {
         $f245->update( ind2 => 4);
       }

    if ( $f245a =~ /^A / ) {
         $f245->update( ind2 => 2);
       }

    if ( $f245a =~ /^An / ) {
         $f245->update( ind2 => 3);
       }

## Updating punctuation to insert $b code and GMD

## CK to see if 245 contains a subtitle by colon

   if ( $f245a =~ /\:/ ) {
      my ( $Title, $subTitle ) = split  /\: /, $f245a;
      $subTitle =~ s/^\s+//g;

## add punctuation and spacing to $a and $b for 245 field
      $sTitle = qq{$subTitle\.};

     $f245->update('a', $Title, 'h', '[electronic resource] :', 'b', $sTitle);
    }

    else {

     $f245a =~ s/\.$//g;
     $f245->update('a', $f245a, 'h', '[electronic resource].');
    }

## print out records

 print OUT $record->as_usmarc();

}
  close OUT;
