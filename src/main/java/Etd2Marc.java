import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.text.*;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.jsoup.safety.Whitelist;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

 /*
  * Etd2Marc
  * Takes an etd xml and pdf.  First creates a marc file (in human readible format) from the xml data. Then
  * writes the xml, pdf, and mrk files to a new directory with new names.
  *
  * Uses standard java 5.0 SAX parsing tools.
  * Author / copyright: Yan Han 
  * Date: 2006; updated: 2008-01-20
  * Release under GNU GPL v3
  */

public class Etd2Marc{

    /* A global array list which holds marc data.  Each element in the array list is equal to
       one line in the marc output file.  Use parse() to fill marc_out with appropiate xml data */
    ArrayList<String> marc_out;

    /* Properites for SHA type (key-length) and API provider when implementing SHA*/
    static final String algorithm = "SHA-256";
    static final String provider = "SUN";

    public Etd2Marc()
    {
	marc_out = new ArrayList<String>();
    }

    public static void main (String[] args) {
	if (args.length != 3){
	    System.out.println("Usage: java Etd2Marc <xmlfile> <pdfFile> <instFileNamingPattern>");

	    for(int a = 0; a < args.length; a++)
		System.out.println(a + ": " + args[a]);

	    System.exit(1);
	}

	String xmlFileName = new String();
	String pdfFileName = new String();
	String fileNamingPattern = new String();

	/* Make sure one file is a pdf and one is an xml; fileNamingPattern should be like "gwu_etd"*/
	boolean e1, e2;
	if     ((args[0].toLowerCase()).endsWith(".xml") && (args[1].toLowerCase()).endsWith(".pdf") && args[2] != null )
	    {
		xmlFileName = args[0];
		pdfFileName = args[1];
		fileNamingPattern = args[2];  
	    }
	else if((args[0].toLowerCase()).endsWith(".pdf") && (args[1].toLowerCase()).endsWith(".xml") && args[2] != null )
	    {
		xmlFileName = args[1];
		pdfFileName = args[0];
		fileNamingPattern = args[2];
	}
	else{
	    System.out.println("Error (arguments): must be exactly one .xml, one .pdf, and one file naming pattern (e.g. azu_etd) as input arguments");
	    System.exit(1);
	}

	/* Run the programs */
	Etd2Marc e2m = new Etd2Marc();

	if(e2m.parse(xmlFileName) && e2m.copyFile(xmlFileName, pdfFileName, fileNamingPattern))
	    System.out.println("File " + xmlFileName + "+" + pdfFileName + " Parsing and Copying Success!");
	else
	    System.out.println("File " + xmlFileName + "+" + pdfFileName + " Failed!");
    
    } /* main */


    /*
     * copyFile
     *
     * Creates a new directory (based on pdf_file name), changes name of pdf file and copies
     * both files into new directory.
     */
    private boolean copyFile(String xml_file, String pdf_file, String fileNamingPrefix)
    {
	File pdf = new File(pdf_file);
	File xml = new File(xml_file);
	File new_pdf;
	File new_xml;

	String pid = "";

	/* Determine if files exists */
	boolean pdf_b = pdf.exists();
	boolean xml_b = pdf.exists();
	if(!pdf_b || !xml_b)
	    {
		if(!pdf_b)
		    System.out.println("Error (copyFile): Unable to find file: " + pdf_file);
		if(!xml_b)
		    System.out.println("Error (copyFile): Unable to find file: " + xml_file);
		return false;
	    }

	/* Determine the new files and directory names by examining ProQuest's name 
	   For example, Johanson_arizona_0009D_10063.pdf has persistent id "10063" unique to arizona
	*/
	int loc_end = pdf_file.indexOf(".pdf");
	int loc_begin = pdf_file.lastIndexOf("_") + 1; 
	
	if(loc_begin < 0 || loc_end < 0)
	  {
		System.out.println("Error (copyFile): File " + pdf_file + " must be of form LastName_arizona_\\d+.xml");
		return false;
	    }
	else{
	    pid = pdf_file.substring (loc_begin, loc_end);  // get pid = something like "10063"  

	    if(pid == null || pid.length() < 1)
		{
		    System.out.println("Error (copyFile): Unable to parse out persistent identifier (pid) from name " + pdf_file);
		    return false;
		}
	}

	try{
	    /* Create a new Directory */
	    String cur_dir = System.getProperty("user.dir");
	    File new_dir = new File(fileNamingPrefix + "_"  + pid);
	    if(!new_dir.isDirectory() && !new_dir.mkdir())
		{
		    System.out.println("Error (copyFile): Unable to create directory for ProQuest's id " + pid);
		    return false;
		}

	    /* Create new files in new directory */
	    /* file name "sip0_m" is reserved for rights form. */
	    File xml_new = new File(new_dir, fileNamingPrefix + "_" + pid + "_sip2_m.xml");
	    File pdf_new = new File(new_dir, fileNamingPrefix + "_" + pid + "_sip1_m.pdf");

		if (xml_new.isFile()) {
			xml_new.delete();
		}

		if (pdf_new.isFile()) {
			pdf_new.delete();
		}

	    boolean new_x = xml_new.createNewFile();
	    boolean new_p = pdf_new.createNewFile();

	    if(!new_x || !new_p)
		{
		    System.out.println("Error (copyFile): Unable to create new files " + pid);
		    return false;
		}

	    /* Copy old files into new directory */
	    FileChannel xml_src = new FileInputStream(xml).getChannel();
	    FileChannel pdf_src = new FileInputStream(pdf).getChannel();

	    FileChannel xml_dst = new FileOutputStream(xml_new).getChannel();
	    FileChannel pdf_dst = new FileOutputStream(pdf_new).getChannel();

	    xml_dst.transferFrom(xml_src, 0, xml_src.size());
	    pdf_dst.transferFrom(pdf_src, 0, pdf_src.size());

	    /* Make sure marc_output has been parsed and create marc file into new directory */
	    writeMarc(new_dir, fileNamingPrefix +"_" + pid + ".mrk");

	    xml_dst.close();
	    pdf_dst.close();

	    /* Calculate the SHA keys for pdf and xml files and write into new files */
	    byte[] xml_key = calc_hash(xml);
	    byte[] pdf_key = calc_hash(pdf);
	    if(xml_key == null || pdf_key == null)
		System.out.println("Error (copyFile): Unable to calculate sha key for " + xml.getName() + " and " + pdf.getName());
	    else{
		/* get the new line character used for writing to file */
		String nl = new String();
		try{
		    /* figure out the appropiate line return character */
		    /*nl = System.getProperty("line.separator");*/
		    nl = "\n\r";
		}
		catch(Exception e)
		    {
			nl = "\n";
		    }

		File sha = new File(new_dir, fileNamingPrefix + "_" + pid + ".sha");
		try{
		    BufferedWriter sha_out = new BufferedWriter(new FileWriter(sha));
		    sha_out.write(xml_new.getName() + "\t" + algorithm + "\t" +provider + "\t " + byteArrayToHexStr(xml_key) + "" + nl);
		    sha_out.write(pdf_new.getName() + "\t" + algorithm + "\t" +provider + "\t " + byteArrayToHexStr(pdf_key) + "" + nl);
		    sha_out.close();
		}
		catch(IOException ioe){
		    System.out.println("Error (copyFile): Unable to write sha key to " + sha.getName());
		    ioe.printStackTrace();
		}
	    }
	}
	catch(Exception ioe)
	    {
		System.out.println("Error (copyFile): Unable to transfer files");
		ioe.printStackTrace();
		return false ;
	    }

	return true;
    } /* copyFile */


    /*
     * Takes the xml file and creates an mrk file.
     */
    private boolean parse(String fileName)
    {
	/* Make sure xml file exists */
	File xml = new File(fileName);
	if(!xml.exists())
	    {
		System.out.println("Error (parse): " + fileName + " does not exists");
		return false;
	    }

	/* Create the filename */
	try{
	    /* create the parser */
	    DefaultHandler handler = new ETDHandler(fileName);

	    /* Remember, Remember, the fifth friday of every december */
	    SAXParserFactory sax_fac = SAXParserFactory.newInstance();
	    SAXParser sax_parser = sax_fac.newSAXParser();
	    sax_parser.parse(xml, handler);

	}
	catch(Throwable t)
	    {
		System.out.println("Error (parse): " + t.getMessage());
		t.printStackTrace();
	    }

	return true;
    } /* parse */

    /*
     * When parsing functions from DefaultHandler functions will be called accordlingly, use to parse out
     * differnt marc values from certain xml tags, and store in the marc_out array list.
     */
    private class ETDHandler extends DefaultHandler{
      	private final int LAST = 0, FIRST = 1, MIDDLE = 2;
	private ArrayList<String> nameList;  //Keeps track of any author names
	private Stack<String> current_tag;  //Keeps track of current tag

	/* Keeps track of attributes and cdata that needs to be saved and parsed latter */
	private String[] name;
	private String language;
	private String accept_date; //format of "mm/dd/yyyy", 
	private String comp_date; //format of "yyyy", 

	private String running_date; // format of "yymmdd" for MARC field 008
	private String paragraphs;
	private String author_attr = new String();
	private String keywords = new String();
	private String title = new String();

	//private String xml_fileName;
	private String pid;

	private String issueDept="";
	private String catCode="";


	public ETDHandler(String fileName)
	{
	    DateFormat dF = new SimpleDateFormat("yyMMdd");
	    java.util.Date date = new java.util.Date();
	
	    accept_date = "";  //YH: initial value
	    running_date = dF.format(date);  

	    System.out.println(running_date);
	    paragraphs = new String();
	    name = new String[3];

	    nameList = new ArrayList<String>();
	    current_tag = new Stack<String>();

	    pid = getPid(fileName);
	    
	    /* */
	    //printMarc();
	}


	/*
	 * startDocument
	 * First functioned called, write fields which don't need to be parse from xml
	 * MARC 040, 260, 502, and 856 need to be updated with your institution.
	 * 20130430: rda: rev 006, 008, 33x (js) 
	 */
	public void startDocument()
	{
	    marc_out.add("=LDR  00000nam\\\\22000007a\\4500") ;
	    marc_out.add("=001  etd_" + pid);
	    marc_out.add("=003  MiAaPQ");
	    marc_out.add("=006  m\\\\\\\\fo\\\\d\\\\\\\\\\\\\\\\");
	    marc_out.add("=007  cr\\mnu\\\\\\aacaa");
	    marc_out.add("=040  \\\\$aMiAaPQ$beng$cDGW$dDGW");
	    marc_out.add("=049  \\\\$aDGWW");
	    marc_out.add("=504  \\\\$aIncludes bibliographical references.");
	    marc_out.add("=538  \\\\$aMode of access: Internet");
            marc_out.add("=996  \\\\$aNew title added ; 20" + running_date);
            marc_out.add("=998  \\\\$cgwjshieh ; UMI-ETDxml conv ; 20" + running_date);
	    marc_out.add("=852  8\\$bgwg ed$hGW: Electronic Dissertation");
	    marc_out.add("=856  40$uhttp://etd.gelman.gwu.edu/" + pid  + ".html$zClick here to access.");

        } /* startDocument */


	/*
	 * startElement
	 * called at the start of any xml tag, use to extract need attributes from certain tags
	 */
	public void startElement(String uri, String sName, String qName, Attributes attrs)
	{

	    current_tag.push(qName);

	    if (qName.equals("DISS_submission"))
		{
		    /* determine if there is an embargo */
		    if (attrs != null && attrs.getValue("embargo_code") != null )
			{
			    try{
				
				String emb_code = attrs.getValue("embargo_code");

				//System.out.println("emb_code: " + emb_code);
				if(emb_code.contains("0")){
				    //writer.write("embargo_code = 0 \n");
				}

				else{
				    String writeFileName = "GWU_etd_" + pid  + "_warning.txt";
				    String writeFile =  writeFileName;

				    writeFile = writeFile.replace('/', '_');
				    System.out.println ("writeFile = " + writeFile);

				    BufferedWriter writer = new BufferedWriter(new FileWriter(writeFile));

				    if(emb_code.contains("1")){
					writer.write("embargo_code = 1 \n");
					writer.write("Warning !!!!!!! " + pid + " embargo for 6 months !!!!!");
				    }
				    else if(emb_code.contains("2")){
					writer.write ("embargo_code = 2 \n");
					writer.write("Warning !!!!!!! " + pid + " embargo for 1 year !!!!!");
				    }
				    else if(emb_code.contains("3")){
					writer.write ("embargo_code = 3 \n");
					writer.write("Warning !!!!!!! " + pid + " embargo for 2 years !!!!!");
				    }
				    else if(emb_code.contains("4")){
					writer.write ("embargo_code = 4 \n");
					writer.write("Warning !!!!!!! " + pid + " embargo for ??? months !!!!!");
				    }
				    else if(!emb_code.contains("0")){
					writer.write(pid + " has an invalid embargo code.");
				    }
				    writer.close();
				}
			    }
			    catch(IOException e){
				System.err.println(pid + " IOException in startElement embargo statement.");
			    }
			}
		}
	    if(qName.equals("DISS_author"))
		{
		    /* Determine if author/s belong in 100 or 700 field */
		    if(attrs != null && attrs.getValue("type") != null){
			author_attr = attrs.getValue("type");
		    }
		    else{
			author_attr = "primary";
		    }
		}
	    else if(qName.equals("DISS_description"))
		{
		    /* get the page number and document type discriptors */
		    if(attrs != null)
			{
			    String pages, type;
			    if((pages = attrs.getValue("page_count")) != null)
				{
				    String value = "=516  \\\\$aText (PDF: " + pages + " p.)";
				    marc_out.add(value);
				}
			    /*if((type = attrs.getValue("type")) != null)
				{
				    String value = "=500  \\\\$a" + type + ".";
				    marc_out.add(value);
				}*/
			}
		}
	} /* startElement */


	/*
	 * endElement
	 * called at the end of a tag. use to determine marc fields which require multiple tags to parse
	 * out data.
	 */
	public void endElement(String uri, String sName, String qName)
	{

	    if(qName.equals("DISS_name") && name[LAST] != null){

		/* Append first, middle, and last name into one string */
		String full_name = name[LAST];

		if(name[FIRST] != null && name[FIRST].length() > 0)
		    {
			full_name += ", " + name[FIRST];

			if(name[MIDDLE] != null && name[MIDDLE].length() > 0)
			    full_name += " " + name[MIDDLE];
		    }

		if(!full_name.endsWith("."))
		    full_name += ".";

		nameList.add(full_name);
		name = new String[3];
	    }
	    else if(qName.equals("DISS_author")){
		/* Only authors name get added to the marc data */
		String field = "700";
		if(author_attr.length() == 0 || author_attr.equals("primary"))
		    field = "100";

		for(int a = 0; a < nameList.size(); a++)
		    {
			String author = nameList.get(a);
			marc_out.add("=" + field + "  1\\$a" + author);
		    }

		author_attr = new String();
		nameList.clear();
	    }
	    else if(qName.equals("DISS_abstract")){
		/* paste together all paragraphs into one abstract */
		if(paragraphs.length() > 0)
			//Clean html from abstract.
			marc_out.add("=520  3\\$a" + Jsoup.clean(paragraphs, Whitelist.none()));
	    }
	    else if(qName.equals("DISS_keyword")){

                /* 20100105: JS rm condition */
		/*String[] keywords_t = keywords.split(";");
		for(int a = 0; a < keywords_t.length; a++)
		    { 
			marc_out.add("=699  04$a" + keywords_t[a].trim() + "."); */
			marc_out.add("=699  04$a" + keywords + ".");
		/*    } */

		keywords = new String();
	    }
	    else if(qName.equals("DISS_title") && title.length() > 0){
		marc_out.add("=245  10$a" + title + "$h[electronic resource].");
			title = new String();
	    }
	    else if (qName.equals("DISS_inst_contact")) {
		marc_out.add("=710  2\\$aGeorge Washington University.$b" + issueDept + "."); //blah
		issueDept = "";
		}

	    if(!qName.equals(current_tag.peek()))
		System.out.println("Warning (ETDHandler:endElement): possible mismatched tag  " + qName);
	    else
		current_tag.pop();
	} /* endElement */


	/*
	 * characters
	 * called when any cdata is seen, use to parse out the data of certain tags
	 */
	public void characters(char[] ch, int start, int length)
	{
	    String cdata = (new String(ch, start, length)).trim();


	    if(cdata.length() < 1)
		return;

	    String cur_element = current_tag.peek();

	    /* Process the cdata for the current element */
	    if(cur_element == null)
		{
		    System.out.println("Error (ETDHandler:characters): no tag for with cdata " + cdata);
		    System.exit(1);
		}

	    if(cur_element.equals("DISS_surname")){
		name[LAST] = cdata;
	    }
	    else if(cur_element.equals("DISS_fname")){
		name[FIRST] = cdata;
	    }
	    else if(cur_element.equals("DISS_middle")){
		name[MIDDLE] = cdata;
	    }
	    else if(cur_element.equals("DISS_language")){
		language = getLangCode(cdata);
	    }
	    else if(cur_element.equals("DISS_title")){
		title += "" + cdata;
		//marc_out.add("=245  10$a" + cdata.trim() + "$h[electronic resource]" + ".");
	    }
	    else if(cur_element.equals("DISS_comp_date")){
		 comp_date = cdata.substring(0,4);
		}

	    //else if(cur_element.equals("DISS_accept_date") && cdata.length() >= 4){
		// ProQuest/UMI changed to a new system, which uses "01/01/08" for any ETD submitted in 2008. The accept_date is
		// no longer useful to generate the real accept date. 
		//accept_date = cdata.substring(0, 4);
		// if DISS_accept_date is format of "20050731", length of 8
		//if(cdata.length() >= 8) {
		    //running_date = cdata.substring(2, 8);
		//   accept_date = cdata.substring(0,4);
		//}

		//-------revised by xing--------begin-------
		// if DISS_accept_date is format 0f "07/31/2008", lenght of 10
		//if(cdata.length() >= 10){
		    //running_date = cdata.substring(8,10) + cdata.substring(0,2)+ cdata.substring(3,5);
		 //   accept_date = cdata.substring (6,10);
		//}
		//-------end--------------------------------

	//-------revised by JS 20090107, use full date--------begin-------
	    else if(cur_element.equals("DISS_accept_date")){
		 accept_date = cdata.substring (0,10);
	         marc_out.add("=500  \\\\$aTitle and description based on DISS metadata (ProQuest UMI) as of " + accept_date + ".");
	    }
		// added JS 200912; if DISS_cat_code is present, grab to add to 502 $o
	    else if(cur_element.equals("DISS_cat_code")){
		catCode += cdata + "";
	    }
		//-------end--------------------------------
	    else if(cur_element.equals("DISS_para")){
		paragraphs += cdata;
	    }
	    else if(cur_element.equals("DISS_inst_contact")){
		issueDept += cdata + "";

		/*marc_out.add("=699  \\\\$a" + cdata + "."); //blah*/
		/*=699  \\\\$a" + cdata + "."=> 200912JS: changed to 710 /blah*/
	    }
	    else if(cur_element.equals("DISS_keyword")){
		/* Preferably parsing keywords into 699 marc data should be done here, but bug in java
		   SAX causes characters() call on <DISS_keywords> to happen twice inbetween cdata */
		String n_cdata = cdata.replace(',', ';');
		n_cdata = n_cdata.replace(':', ';');
		n_cdata = n_cdata.replace('.', ';');

		keywords += n_cdata;
	    }
	    else if(cur_element.equals("DISS_ISBN")){
		marc_out.add("=020  \\\\$a" + cdata);
	    }
	    /* else if(cur_element.equals("DISS_cat_code")) {
		marc_out.add("=502  \\\\$o" + catCode + ".");
		} */  
	    else if(cur_element.equals("DISS_degree")) {
		marc_out.add("=502  \\\\$aThesis$b(" + cdata + ")--$cGeorge Washington University,$d" + comp_date + ".");
		} 
                 //System.out.println(catCode); /* output content of cat_code */

	} /* characters */

	/*
	 * endDocument
	 * Finish up with final marc fields. you need to change 008 and 260 for your institutions.
	 */
	public void endDocument()
	{
	    /* marc_out.add("=008  " + running_date + "s\\\\\\\\\\\\\\\\dcu\\\\\\\\\\sbm\\\\\\\\\\\\\\\\\\" + language + "\\d"); */ /** 008/24-27: biblio+thesis codes **/
	    marc_out.add("=008  " + running_date + "s" + comp_date + "\\\\\\\\dcu\\\\\\\\\\obm\\\\\\000\\0\\" + language + "\\d"); /** 008/24-27: biblio+thesis codes **/
	    marc_out.add("=264  30$a[Washington, D. C.] :$bGeorge Washington University,$c" + comp_date + ".");  /** 20130430: rev 260*/
	} /* endDocument */



	private String getLangCode(String lang)
	{
	    lang = lang.toUpperCase();
	    if(lang.startsWith("CH")){
		return "chi";
	    }
	    else if(lang.startsWith("CE")){
		return "eng";
	    }
	    else if(lang.startsWith("DU")){
		return "dut";
	    }
	    else if(lang.startsWith("EN")){
		return "eng";
	    }
	    else if(lang.startsWith("FI")){
		return "fin";
	    }
	    else if(lang.startsWith("FR")){
		return "fre";
	    }
	    else if(lang.startsWith("FE")){
		return "eng";
	    }
	    else if(lang.startsWith("GE")){
		return "ger";
	    }
	    else if(lang.startsWith("GN")){
		return "eng";
	    }
	    else if(lang.startsWith("GR")){
		return "gre";
	    }
	    else if(lang.startsWith("HE")){
		return "heb";
	    }
	    else if(lang.startsWith("IT")){
		return "ita";
	    }
	    else if(lang.startsWith("IE")){
		return "eng";
	    }
	    else if(lang.startsWith("JA")){
		return "jpn";
	    }
	    else if(lang.startsWith("KO")){
		return "kor";
	    }
	    else if(lang.startsWith("LA")){
		return "lat";
	    }
	    else if(lang.startsWith("PL")){
		return "pol";
	    }
	    else if(lang.startsWith("PR")){
		return "por";
	    }
	    else if(lang.startsWith("RU")){
		return "rus";
	    }
	    else if(lang.startsWith("SP")){
		return "spa";
	    }
	    else if(lang.startsWith("SE")){
		return "eng";
	    }
	    else if(lang.startsWith("SW")){
		return "swe";
	    }

	    return "eng";
	} /* getLangCode */
    }


    /*
     */
    public void printMarc()
    {
	System.out.println("Computed MARC");
	for(int a = 0; a < marc_out.size(); a++)
	    System.out.println(marc_out.get(a));
    } /* printMarc */


    /*
     * Sort the marc_out arrayList in order of each lines field number
     */
    public void sortMarc()
    {
	/* Make sure each element in marc_out is a complete string with more than 4 characters */
	for(int a = 0; a < marc_out.size(); a++)
	    {
		if(marc_out.get(a).length() < 5){
		    System.out.println("Warning (sortMarc): marc line " + marc_out.get(a) + " too short, must have least 4 characters.");
		    return;
		}
	    }

	for(int a = 0; a < (marc_out.size() - 1); a++)
	    {
		int field_a = 0;
		String value_a = (marc_out.get(a)).substring(1, 4);
		try{
		    field_a = (Integer.valueOf(value_a)).intValue();
		}
		catch(NumberFormatException nfe) { }

		for(int b = a + 1; b < marc_out.size(); b++)
		    {
			int field_b = 0;
			String value_b = (marc_out.get(b)).substring(1, 4);
			try{
			    field_b = (Integer.valueOf(value_b)).intValue();
			}
			catch(NumberFormatException nfe){ }

			if(field_b < field_a)
			    {
				String temp_field = marc_out.get(b);
				marc_out.set(b, marc_out.get(a));
				marc_out.set(a, temp_field);

				field_a = field_b;
			    }
		    }
       	    }
    } /* sortMarc */



    /*
     * Writes the value of marc_out to file 'file_name' located in 'dir'.  'dir' should already exist.
     */
    private boolean writeMarc(File dir, String file_name)
    {
	if(dir == null || !dir.exists() || file_name == null || file_name.length() < 1)
	    {
		System.out.println("Error (writeMarc): Couldn't find valid file name and/or directory.");
		return false;
	    }

	/* Create File, buffer writer, and get appropiate system properties for writing to file */
	String nl = new String();
	try{
	    /* figure out the appropiate line return character */
	    /*nl = System.getProperty("line.separator");*/
	    nl = "\n\r";
            nl = "\n";
	}
	catch(Exception e)
	    {
		nl = "\n";
	    }

	try{
	    File mrk = new File(dir, file_name);
	    BufferedWriter out = new BufferedWriter(new FileWriter(mrk));

	    /* Write sorted marc_output to file */
	    sortMarc();
	    for(int a = 0; a < marc_out.size(); a++)
		{
		    out.write(marc_out.get(a) + nl);
		}

            out.write("\n"); /* add blank line */
	    out.close();
	}
	catch(IOException ioe)
	    {
		System.out.println("Error (writeMarc): Unable to write marc data to file.");
		ioe.printStackTrace();
		return false;
	    }

	return true;
    } /* writeMarc */


    /* Calculates the SHA-256 key for a specific file */
    public byte[] calc_hash(File filename)
    {
	int digest_length = 512;
	byte message[] = new byte[digest_length];
	byte key[] = new byte[0];
	MessageDigest sha;
	int message_size;
	long file_size = 0;

	try{
	    sha = MessageDigest.getInstance(algorithm, provider);

	    try{
		/* Read file into byte array, and use array to update hash key */
		FileInputStream fin = new FileInputStream(filename);
		while((message_size = fin.read(message)) != -1)
		    {
			if(message_size < digest_length)
			    sha.update(message, 0, message_size);
			else
			    sha.update(message);
		    }
		fin.close();

		key = sha.digest();
	    }
	    catch(IOException ioe){
		System.out.println("Error (calc_hash): Unable to open and read file " + filename.getName());
		ioe.printStackTrace();
		return null;
	    }
	}
	catch(NoSuchAlgorithmException nae){
	    System.out.println("Error (calc_hash): hash algorithm " + algorithm + " not found");
	    nae.printStackTrace();
	    return null;
	}
	catch(NoSuchProviderException npe){
	    System.out.println("Error (calc_hash): Security provider " + provider + " not found");
	    npe.printStackTrace();
	    return null;
	}

	return key;
    } /* calc_hash */


    /*
     * byteArrayToHexStr
     *
     * Tranform byte array into Hexidecimal format
     */
    public static String byteArrayToHexStr(byte[] data)
    {
	String output = new String();
	String tempStr = new String();
	int tempInt = 0;

	for(int a = 0; a < data.length; a++)
	    {
		tempInt = data[a]&0xFF;
		tempStr = Integer.toHexString(tempInt);

		if(tempStr.length() == 1)
		    tempStr = "0" + tempStr;

		output += tempStr;
	    }
	return output;
    } /*byteArrayToHexStr */


    /* get persistent id from ProQuest/UMI file name */

    private String getPid(String filename) 
    {
	String xml_file = filename;
	String pid = "00000";
	
	/* make a formal XML file name , taking "DATA" out of xml filename 
	   Johnson_arizona_0009D_10063_DATA.xml ==> 10063
	*/
	int loc = xml_file.lastIndexOf("_DATA.xml"); 
	xml_file = xml_file.substring (0, loc); //  	
	System.out.println ("xml_file +" + xml_file);
 
	int loc_begin = xml_file.lastIndexOf("_") + 1 ;	

	if(loc_begin < 0)
	  {
		System.out.println("Error (copyFile): File " + xml_file + " must be of form LastName_arizona_\\d");
		return pid;
	    }
	else{
	    pid = xml_file.substring (loc_begin);  // get pid = something like "10063"  
	    //System.out.println("***** pid = " + pid);

	    if(pid == null || pid.length() < 1)
		{
		    System.out.println("Error (copyFile): Unable to parse out persistent identifier (pid) from name " + xml_file);
		    return null;
		}
	    return pid;
	}
    } // getPid()

} // class Etd2Marc
