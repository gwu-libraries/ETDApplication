#!/usr/bin/perl -w
# 20090106
# rv from Jim's jimtest.pl ; Mike's testEDT.pl

$path = $ARGV[0];
chdir($path) or die "Cant chdir to $path $!";
system("ls -lt");
system("umask 000");


#use strict;

my $destinationXML = 'destinationXML';
my $destinationPDF = 'destinationPDF';

# CREATE DIRECTORIES UNDER CURRENT DIRECTORY, 
# IF THEY DO NOT ALREADY EXIST

if (!(-d $destinationXML)) { system ("mkdir ./destinationXML "); }
if (!(-d $destinationPDF)) { system ("mkdir ./destinationPDF "); }


# CREATE ARRAY OF ZIP FILE NAMES
# For my test, I used a gzip file, extension .gz instead of .zip 

my @zipFiles = `ls *.zip`;

# UNZIP THE FILES
# For my test, I changed the unzip file command to the syntax for gzip

foreach my $file (@zipFiles) {
    print "Unzipping $file";
    system ("unzip -qq -o $file");
}

########## INSERT related segments from runAll.pl

   # create mrk, change file name, etc by running a java program
my @files = <*.pdf>; #store all listed files of dir

foreach $i (@files){

    #if(-d $i){
            $length=length($i);
            $length_=$length-4;
            $i_s = substr($i, 0, $length_);
            print "Running Etd2Marc on $i: $i_s\n";
            #$JH = $ENV{'JAVA_HOME'};
            print `java -cp "../lib/*" Etd2Marc $i_s*.xml $i_s*.pdf  GW_etd`;

    #} 
}

@files = <*>; #store all listed files of dir
print `rm gwu\`date +%Y%m%d | sed -e 's/-/_/' -e 's/-/_/'\`.mrk`;
foreach $j (@files){
    if(-d $j && $j ne 'PDF' && $j ne 'XML' && $j ne 'destinationPDF' && $j ne 'destinationXML'){
        print `cat $j/$j.mrk >> gwu\`date +%Y%m%d | sed -e 's/-/_/' -e 's/-/_/'\`.mrk`;
        print "Directory: ", $j, "\n";
        }
}

###################


# MOVE XML FILES TO destinationXML and PDF files to destinationPDF
# For my test, the files are not really xml and pdf, just named that way

system ("mv *.xml $destinationXML");
system ("mv *.pdf $destinationPDF");

# READ THE PDF FILES. DO NOT ATTEMPT TO CD, DO IT FROM HERE
       my @pdfFiles = `ls destinationPDF/*.pdf`;
	
	# NEED TO REMOVE NEWLINE CHARACTER FROM FILE NAME 
	# OTHERWISE MV COMMAND CAN'T READ THE DESTINATION FILE!
	chomp(@pdfFiles);
       
	# RENAME, ie MV the original file to the shorter filename 
	foreach my $pdf (@pdfFiles) {
            if  ($pdf !~ /^\d/) { ##ignore file name begin with numeric value 
		my ($shortpdf) = ($pdf =~ /(\d{5}\.pdf)/g);
		my $newpdf='destinationPDF/'.$shortpdf;
		system("mv $pdf $newpdf");
              }##CLOSE if numeric check
         }

