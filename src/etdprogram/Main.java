package etdprogram;
import java.io.*;
import com.jcraft.jsch.*;
import java.net.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;







/**
 *
 * @author gilani
 */
public class Main {
    static String m_file,title,author,year,month,dateNow,from;
    static File cWorkDir,GW_ETD,log,web_log,record_file;
    static PrintWriter pr;
    static JSch jsch;
    static com.jcraft.jsch.Session session;
    static ArrayList codes=new ArrayList();
    static Vector tags = new Vector();
    static int count;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

       BufferedWriter pr=null;
       from=args[0];
        log= new File("/home/jshieh/RDG/ETD-UMI/GW-ETD/cWorkDir/log.txt");
       record_file=new File("/home/jshieh/RDG/ETD-UMI/GW-ETD/cWorkDir/record");
       PrintStream original = System.out;
       try
       {
       pr = new BufferedWriter(new FileWriter(record_file,true));
       PrintStream printStream = new PrintStream(new FileOutputStream(log));
       System.setOut(printStream);
       System.out.println("log file is created");
       System.out.flush();
       }
       catch(Exception e)
       {
           System.out.println(e);
           System.out.flush();
           e.printStackTrace();
           System.out.flush();
       }
       
       
        
        
        
        Calendar currentDate = Calendar.getInstance();
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy/MM/dd");
        dateNow = formatter.format(currentDate.getTime());
        int index=dateNow.indexOf("/");
        year=dateNow.substring(0,index);
        int index2=dateNow.lastIndexOf("/");
        month=dateNow.substring(index+1,index2);
        dateNow=dateNow.replaceAll("/", "");
        if(month.equals("01"))
            month="12";
        else if(month.equals("02"))
            month="01";
        else if(month.equals("03"))
            month="02";
        else if(month.equals("04"))
            month="03";
        else if(month.equals("05"))
            month="04";
        else if(month.equals("06"))
            month="05";
        else if(month.equals("07"))
            month="06";
        else if(month.equals("08"))
            month="07";
        else if(month.equals("09"))
            month="08";
        else if(month.equals("10"))
            month="09";
        else if(month.equals("11"))
            month="10";
        else if(month.equals("12"))
            month="11";
        int intyear = Integer.parseInt(year);
        if(month.equals("12"))
        {
            intyear=intyear-1;
        year = Integer.toString(intyear);
        }
String monthyear=month+year;
        try
        {
       BufferedReader br=new BufferedReader(new FileReader(record_file));
       String line=br.readLine();
       boolean found =false;
       while(line!=null)
       {

           if(line.equals(monthyear))
           {
               found=true;
               break;
           }
           line=br.readLine();
       }
       if(found==true)
       {
           System.setOut(original);
           System.out.println("<Font Color=\"FF0000\"><h1> the files for "+monthyear+" have already been processed</h1></Font>");
           System.exit(0);
        }
        
        
            pr.append(monthyear);
            pr.newLine();
            pr.flush();
           
        
        }
        catch(Exception e)
        {
            System.out.println(e);
            e.printStackTrace();
        }
       
        getFiles(month+"-"+year);

        File fake=new File(cWorkDir.getAbsolutePath()+"/fake");
        File temp = new File (cWorkDir.getAbsolutePath()+"/fake.pdf");
        fake.renameTo(temp);
        
       File f=null;
        String[] child=null;
        
        String parent = cWorkDir.getParent();
        GW_ETD=new File(parent);
        System.out.println("Current Directory: "+GW_ETD.getAbsolutePath());
        System.out.flush();
        String[] children = cWorkDir.list();
        for (int i =0;i<children.length;i++)
        {
            if(children[i].endsWith("mrk"))
            {
                m_file=cWorkDir.getAbsolutePath()+"/"+children[i];
                System.out.println("mrk file: "+m_file);
            }
        }
        checkTags();
        
        try
            {
        for (int i =0;i<children.length;i++)
        {
            
            
            f= new File(cWorkDir.getAbsolutePath()+"/"+children[i]);
            if((children[i].equals("destinationPDF")==false) && (children[i].equals("destinationXML")==false) && (children[i].equals("lib")==false) && (children[i].equals("PDF")==false) && (children[i].equals("XML")==false) && (children[i].equals("LOADED")==false) && (children[i].equals("src")==false) && (f.isDirectory()==true))
            {
                System.out.println("subfolder: "+children[i]);//the thesis thesis folders
                System.out.flush();
                
            if(children[i].contains("0075"))//check for gwu thesis code
            {
                index=children[i].indexOf("_");
                String lname=children[i].substring(0, index);
                String code=find(lname);
                System.out.println("GWU thesis: "+f.getAbsolutePath()+ "has code" +code+"with lname"+lname);
                System.out.flush();
                
                process(children[i],cWorkDir);

               
                codes.add(code);

            }
            /*else if(children[i].contains("_55"))
            {

                int index=children[i].indexOf("_");
                String lname=children[i].substring(0, index);
                String code=find(lname);
                codes.add(code);
                
            }*/
            }

          }
        Vector v = getCodes();
        Iterator it=v.iterator();
        while(it.hasNext())
        {
            String code=(String)it.next();
            boolean found=false;
            for (int i =0;i<codes.size();i++)
            {
                String dest=(String)codes.get(i);
                if(code.equals(dest))
                {
                    found=true;
                    break;
                }
            }
            if(found==false)
            {
                File html_f = new File (cWorkDir.getAbsolutePath()+"/"+code+".html");
                createHtml(html_f,null,cWorkDir,code);

            }
        }
            
        
        System.out.println("Doing Post transfer work");
        System.out.flush();




        
        
           // Process p = Runtime.getRuntime().exec("perl 2gwMARC.pl "+m_file+" gwu"+dateNow+".mrc");
            //p.waitFor();
            String mrkfile;
            int ind=m_file.lastIndexOf("/");
            mrkfile=m_file.substring(ind+1);
            System.out.println("mrkfile: "+mrkfile);

            Process p = Runtime.getRuntime().exec("perl "+ cWorkDir.getAbsolutePath()+"/2gwMARC.pl "+m_file.toString()+ " " +cWorkDir.getAbsolutePath()+"/gwu"+dateNow+".mrc");
            InputStream in = p.getInputStream();
            BufferedReader bin=new BufferedReader(new InputStreamReader(in));
            String line=bin.readLine();
            System.out.println("perl "+cWorkDir.getAbsolutePath()+ "/2gwMARC.pl "+m_file.toString()+ " "+cWorkDir.getAbsolutePath()+"/gwu"+dateNow+".mrc");
            while(line!=null)
            {
                System.out.println(line);
                line=bin.readLine();
            }
            p.waitFor();
            p = Runtime.getRuntime().exec("perl "+cWorkDir.getAbsolutePath()+"/gw245etd.pl " +cWorkDir.getAbsolutePath()+"/gwu"+dateNow+".mrc " +cWorkDir.getAbsolutePath()+"/gwu"+dateNow+"rev.mrc");
            in = p.getInputStream();
            bin=new BufferedReader(new InputStreamReader(in));
            line=bin.readLine();
            System.out.println("perl gw245etd.pl gwu"+dateNow+".mrc  gwu"+dateNow+"rev.mrc");
            while(line!=null)
            {
                System.out.println(line);
                line=bin.readLine();
            }
            p.waitFor();
            PrintStream printStream = new PrintStream(new FileOutputStream(new File(cWorkDir.getAbsolutePath()+"/gwu"+dateNow+"revU8.mrc")));
            System.setOut(printStream);
            p = Runtime.getRuntime().exec("/usr/local/bin/yaz-marcdump -f MARC-8 -t UTF-8 -o marc -l 9=97 "+ cWorkDir.getAbsolutePath()+"/gwu"+dateNow+"rev.mrc > "+cWorkDir.getAbsolutePath()+"/gwu"+dateNow+"revU8.mrc");
            
            in = p.getInputStream();
            bin=new BufferedReader(new InputStreamReader(in));
            line=bin.readLine();
              // System.out.println("/usr/local/bin/yaz-marcdump -f MARC-8 -t UTF-8 -o marc -l 9=97 gwu"+dateNow+"rev.mrc > gwu"+dateNow+"revU8.mrc");
            while(line!=null)
            {
               System.out.println(line);
                line=bin.readLine();
            }
            p.waitFor();
            printStream = new PrintStream(new FileOutputStream(log,true));
            System.setOut(printStream);
            p = Runtime.getRuntime().exec("mv "+GW_ETD.getAbsolutePath()+"/destinationPDF/*.pdf "+GW_ETD.getAbsolutePath()+"/PDF");
            in = p.getInputStream();
            bin=new BufferedReader(new InputStreamReader(in));
            line=bin.readLine();
            System.out.println("mv "+GW_ETD.getAbsolutePath()+"/destinationPDF/*.pdf "+GW_ETD.getAbsolutePath()+"/PDF");
            while(line!=null)
            {
                System.out.println(line);
                line=bin.readLine();
            }
            p.waitFor();
            p = Runtime.getRuntime().exec("cp "+GW_ETD.getAbsolutePath()+"/destinationXML/*.xml "+GW_ETD.getAbsolutePath()+"/XML");
            in = p.getInputStream();
            bin=new BufferedReader(new InputStreamReader(in));
            line=bin.readLine();
            System.out.println("cp "+GW_ETD.getAbsolutePath()+"/destinationXML/*.xml "+GW_ETD.getAbsolutePath()+"/XML");
            while(line!=null)
            {
                System.out.println(line);
                line=bin.readLine();
            }
            p.waitFor();
            p = Runtime.getRuntime().exec("cp  "+cWorkDir.getAbsolutePath()+"/gwu"+dateNow+"revU8.mrc /srv/www/htdocs/rdg/");
            in = p.getInputStream();
            bin=new BufferedReader(new InputStreamReader(in));
            line=bin.readLine();
            System.out.println("cp  gwu"+dateNow+"revU8.mrc /srv/www/htdocs/rdg/");
            while(line!=null)
            {
                System.out.println(line);
                line=bin.readLine();
            }
            p.waitFor();
            web_log = new File(cWorkDir.getAbsolutePath()+"/web_log"+dateNow+".txt");

        writeWebLog();
        p= Runtime.getRuntime().exec("cp "+web_log.getAbsolutePath() +" /tmp/");
            p.waitFor();
        System.setProperty("user.dir", cWorkDir.getAbsolutePath());
        child=cWorkDir.list();
        for(int i =0;i<child.length;i++)
        {
            File file = new File(cWorkDir.getAbsolutePath()+"/"+child[i]);
            if(child[i].contains("GW_etd_")&& file.isDirectory()==true)
                deleteDir(file);
            if(child[i].endsWith(".mrk"))
                file.delete();
            if(child[i].endsWith(".mrc"))
                file.delete();
            if(child[i].equals("log.txt"))
            {
                File logFile=new File(cWorkDir.getAbsolutePath()+"/log"+dateNow+".txt");
                file.renameTo(logFile);
            }
            if(child[i].endsWith(".txt") && child[i].endsWith("warning.txt")==false)
                file.delete();
            if(child[i].contains("_0075")&&file.isDirectory()==true)
                deleteDir(file);
            if(child[i].endsWith("warning.txt"))
            {
               p = Runtime.getRuntime().exec("cp "+cWorkDir.getAbsolutePath()+"/"+child[i]+ " "+GW_ETD.getAbsolutePath()+"/embargo_warning");
            while(line!=null)
            {
                System.out.println(line);
                line=bin.readLine();
            }
            p.waitFor();
            File warning=new File(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i]);
            warning.delete();
            }

        }
            

            /*p= Runtime.getRuntime().exec("rm -r "+cWorkDir.getAbsolutePath()+"/GW_etd_*");
            p.waitFor();
            p= Runtime.getRuntime().exec("rm -r "+cWorkDir.getAbsolutePath()+"/*_gwu_0075*");
            p.waitFor();
            p= Runtime.getRuntime().exec("rm "+cWorkDir.getAbsolutePath()+"/*.mrk");
            p.waitFor();
            p= Runtime.getRuntime().exec("rm "+cWorkDir.getAbsolutePath()+"/*.mrc");
            p.waitFor();
            p= Runtime.getRuntime().exec("rm "+cWorkDir.getAbsolutePath()+"/*.txt");
            p.waitFor();*/
        new_embargo(new File(GW_ETD.getAbsolutePath()+"/embargo_warning"));
        old_embargo(new File(GW_ETD.getAbsolutePath()+"/embargo_warning"));
           postMail(args[0]);
            System.setOut(original);
            temp=new File(cWorkDir.getAbsolutePath()+"/fake.pdf");
            fake=new File(cWorkDir.getAbsolutePath()+"/fake");
            temp.renameTo(fake);
        System.out.println("<Font Color=\"FFD700\">done</Font>");
        System.exit(0);

        


        }
            
        catch(Exception e)
        {
            System.out.println(e);
            e.printStackTrace();
            System.out.flush();
        }


        }
        
     
    
    public static void postMail(String from )
{
   try
   {
       Properties props = new Properties();
        props.put("mail.smtp.host", "127.0.0.1");
        props.put("mail.smtp.port", "25");
        javax.mail.Session mailSession = javax.mail.Session.getDefaultInstance(props);
		Message simpleMessage = new MimeMessage(mailSession);

		InternetAddress fromAddress = null;
                InternetAddress cc=null,cc1=null;
		InternetAddress toAddress = null;
		try {
			fromAddress = new InternetAddress(from);
                        cc= new InternetAddress("rdg@gwu.edu");
                        cc1=new InternetAddress(from);
			toAddress = new InternetAddress("rdg@gwu.edu");
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


      /* File f = new File(cWorkDir.getAbsolutePath()+"/tmp");
        PrintWriter pr = new PrintWriter(new FileWriter(f));
        pr.println("date: "+dateNow);
        pr.println("to: sgilani@gwmail.gwu.edu");
        pr.println("subject: rec load request: gw ETD "+ year+" "+month );
        pr.println("from: jshieh@gelman.gwu.edu");
        pr.println();
        pr.println();
        pr.println();
        pr.println();
        pr.println();
        pr.println("Fields 852 and 856 to be used in MFDH with 852 $b and $h coded for electronic dissertation.");
        pr.println("Please create Item for Item type: Electronic.");
        pr.println("Please let me know if there is any question. Thank you.");
        pr.println("*Jackie");
        pr.println();
        pr.close();*/
        try {
			simpleMessage.setFrom(fromAddress);
                        simpleMessage.setRecipient(javax.mail.Message.RecipientType.CC, cc);
                         simpleMessage.setRecipient(javax.mail.Message.RecipientType.CC, cc1);
			simpleMessage.setRecipient(javax.mail.Message.RecipientType.TO, toAddress);
			simpleMessage.setSubject("rec load request: gw ETD "+ year+" "+month );
			simpleMessage.setText("WRLC Loader;\n the set of "+count+" electronic thesis and dissertation is found: \n"+"http://gwdroid.wrlc.org/rdg/gwu"+dateNow+"revU8.mrc\n\n"+"Records are coded 7 (minimal) in LDR/17 and i(ISBD) in LDR/18\n"+"MFHD:\n\n"+"Fields 852 and 856 to be used in MFHD with 852 $b and $h coded for electronic dissertation.\n\n"+"Please create Item for Item type: Electronic.\n"+"Please let me know if there is any question. Thank you.\n\n"+"*"+from+"\n");

			Transport.send(simpleMessage);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

       /*String message = "";
        BufferedReader br=new BufferedReader(new FileReader(f));
        String line=br.readLine();
        while(line!=null)
        {
            message=message+line;
            line=br.readLine();

        }
        InputStream orig =System.in;
        FileInputStream in =new FileInputStream(f);
        System.setIn(in);

        Process p = Runtime.getRuntime().exec("/usr/lib/sendmail -t < "+f.getAbsolutePath());
        p.waitFor();
        System.setIn(orig);
        System.out.println("Finished sending mail");
        //System.setIn(orig);
        //f.delete();*/
   }
   catch(Exception e)
   {
       System.out.println("PostMail Exception:"+e);
       e.printStackTrace();
   }


}



   static void process(String Dissertation_folder,File Dissertation_parent)
    {
       int index=Dissertation_folder.indexOf("_");
       String lname=Dissertation_folder.substring(0, index);
       String code=find(lname);
       System.out.println("DP: "+Dissertation_parent.getAbsolutePath());
       System.out.flush();
       Boolean embargo=isEmbargo(code,Dissertation_parent);



        try
        {
        if(embargo==true)
       {
             System.out.println(Dissertation_folder +" is embargoed");
             copyfile(cWorkDir.getAbsolutePath()+"/fake.pdf",cWorkDir.getAbsolutePath()+"/"+Dissertation_folder+"/"+code+".pdf");
             /*Process p = Runtime.getRuntime().exec("cp "+cWorkDir.getAbsolutePath()+"/fake.pdf"+ " "+cWorkDir.getAbsolutePath()+"/"+Dissertation_folder);
             System.out.println("cp "+cWorkDir.getAbsolutePath()+"/fake.pdf"+ " "+cWorkDir.getAbsolutePath()+"/"+Dissertation_folder);
             BufferedReader bin=new BufferedReader(new InputStreamReader(p.getInputStream()));
             String line=bin.readLine();
            while(line!=null)
            {
                System.out.println(line);
                line=bin.readLine();
            }*/

             File fake = new File(cWorkDir.getAbsolutePath()+"/destinationPDF/"+"embargo_"+code+".pdf");
             //File temp = new File (Dissertation_folder+"/embargo_"+code+".pdf");
             File original= new File(cWorkDir.getAbsolutePath()+"/destinationPDF/"+code+".pdf");
             original.renameTo(fake);
             //fake.renameTo(original);
             File html_f = new File (cWorkDir.getAbsolutePath()+"/"+code+".html");
             codes.add(code);
             createHtml(html_f,Dissertation_folder,Dissertation_parent,code);
             copyfile(cWorkDir.getAbsolutePath()+"/GWU_etd_"+code+"_warning.txt",GW_ETD.getAbsolutePath()+"/embargo_warning"+"/GWU_etd_"+code+"_warning.txt");

       }
        else
       {
            System.out.println(Dissertation_folder +" is not embargoed");
       File html_f = new File (cWorkDir.getAbsolutePath()+"/"+code+".html");
       codes.add(code);
       createHtml(html_f,Dissertation_folder,Dissertation_parent,code);
       }
       }
       catch(Exception e)
       {
           System.out.println(e);
           System.out.flush();
           e.printStackTrace();
       }


    }
   static void new_embargo(File embargo_folder)
   {
       String child[]=embargo_folder.list();
       ChannelSftp channel=getChannel("S0uth12@","192.245.136.151");
       int months=0;
       for(int i =0;i<child.length;i++)
       {
           File warning=new File(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i]);
           try
           {
               String code=getEmbargoCode(child[i]);
               BufferedReader bin=new BufferedReader(new FileReader(warning));
               String line=bin.readLine();
               int index = line.lastIndexOf(" ");
               String length=line.substring(index+1);
               long yourLong=warning.lastModified();
               Date yourDate = new Date(yourLong);
               DateFormat formatter =  new SimpleDateFormat("dd-MM-yyyy");
               String formattedDate = formatter.format(yourDate);
               index = formattedDate.lastIndexOf("-");
               String lyear=formattedDate.substring(index+1);
               index=formattedDate.indexOf("-");
               String mtime=formattedDate.substring(index+1);
               String lmonth=mtime.substring(0,index+1);
               String m=changeMonth(lmonth);
               int yearint=Integer.parseInt(year);
               int monthint=Integer.parseInt(month);
               int lyearint=Integer.parseInt(lyear);
               int lm=Integer.parseInt(m);
               int yeardifference=yearint-lyearint;
               int monthdifference=monthint-lm;
               int absmonthdiff=(yeardifference*12)+monthdifference;
               if(length.equals("3")&&absmonthdiff>=24)
               {
                   channel.rm("/var/web/etd_"+code+"/"+code+".pdf");
                   channel.rename("/var/web/etd_"+code+"/embargo_"+code+".pdf", "/var/web/etd_"+code+"/"+code+".pdf");
                   copyfile(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i],GW_ETD.getAbsolutePath()+"/embargo_warning/unembargo/"+child[i]);
                   File f = new File(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i]);
                   f.delete();
               }
               else if(length.equals("2")&&absmonthdiff>=12)
               {

                   channel.rm("/var/web/etd_"+code+"/"+code+".pdf");
                   channel.rename("/var/web/etd_"+code+"/embargo_"+code+".pdf", "/var/web/etd_"+code+"/"+code+".pdf");
                   copyfile(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i],GW_ETD.getAbsolutePath()+"/embargo_warning/unembargo/"+child[i]);
                   File f = new File(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i]);
                   f.delete();
               }
               else if(length.equals("1")&&absmonthdiff>=6)
               {
                   channel.rm("/var/web/etd_"+code+"/"+code+".pdf");
                   channel.rename("/var/web/etd_"+code+"/embargo_"+code+".pdf", "/var/web/etd_"+code+"/"+code+".pdf");
                   copyfile(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i],GW_ETD.getAbsolutePath()+"/embargo_warning/unembargo/"+child[i]);
                   File f = new File(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i]);
                   f.delete();
               }

               channel.disconnect();

           }
           catch(Exception e)
           {
               System.out.println(e);
               e.printStackTrace();
           }

       }

   }
   static void old_embargo(File embargo_folder)
   {
       String child[]=embargo_folder.list();
       ChannelSftp channel=getChannel("S0uth12@","192.245.136.151");
       try
       {
           channel.cd("/var/web");
       }
       catch(Exception e)
       {
           System.out.println(e);
           e.printStackTrace();
       }
       int months=0;
       for(int i =0;i<child.length;i++)
       {
           File warning=new File(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i]);
           try
           {
               String code=getEmbargoCode(child[i]);
               System.out.println("Embargo file is "+child[i]+" embargo code is "+code);
               BufferedReader bin=new BufferedReader(new FileReader(warning));
               String line=bin.readLine();
               bin.close();
               int index = line.lastIndexOf(" ");
               System.out.println(line+ "and last index of space is "+index);
               String length =line.substring(index-1);
               length=length.replaceAll(" ", "");
               long yourLong=warning.lastModified();
               Date yourDate = new Date(yourLong);
               DateFormat formatter =  new SimpleDateFormat("dd-MM-yyyy");
               String formattedDate = formatter.format(yourDate);
               System.out.println("Modified date is "+formattedDate);
               index = formattedDate.lastIndexOf("-");
               String lyear=formattedDate.substring(index+1);
               index=formattedDate.indexOf("-");
               String mtime=formattedDate.substring(index+1);
               String lmonth=mtime.substring(0,index);
               //String m=changeMonth(lmonth);
               int yearint=Integer.parseInt(year);
               int monthint=Integer.parseInt(month);
               int lyearint=Integer.parseInt(lyear);
               int lm=Integer.parseInt(lmonth);

               int yeardifference=yearint-lyearint;
               int monthdifference=Math.abs(monthint-lm);
               int absmonthdiff=(yeardifference*12)+monthdifference;
               System.out.println("year is "+lyearint+" month is "+lm+" lmonth is "+monthint+ " yeardifference is "+yeardifference+" monthdifference is "+monthdifference);
               System.out.println("embargo duration is "+length+" and absolute months sinse embargoed is "+absmonthdiff);

               if(length.equals("3")&&absmonthdiff>=24)
               {
                   System.out.println(" deleting file "+ "/var/web/"+code+".pdf");
                   //File remoteFile = new File(new URI("file:///var/web/"+code+".pdf"));
                   System.out.println(executeCommand("S0uth12@","192.245.136.151","rm /var/web/"+code+".pdf"));
                   System.out.println(executeCommand("S0uth12@","192.245.136.151","mv /var/web/"+code+"_embargo.pdf /var/web/"+code+".pdf"));
                   //remoteFile.delete();
                   //File ren=new File(new URI("file:///var/web/embargo_"+code+".pdf"));
                   //ren.renameTo(remoteFile);
                   copyfile(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i],GW_ETD.getAbsolutePath()+"/embargo_warning/unembargo/"+child[i]);
                   File f = new File(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i]);
                   System.out.println(" deleting file "+child[i]);
                   f.delete();
               }
               else if(length.equals("2")&&absmonthdiff>=12)
               {

                   System.out.println(" deleting file "+ "/var/web/"+code+".pdf");
                   //File remoteFile = new File(new URI("file:///var/web/"+code+".pdf"));
                   System.out.println(executeCommand("S0uth12@","192.245.136.151","rm /var/web/"+code+".pdf"));
                   System.out.println(executeCommand("S0uth12@","192.245.136.151","mv /var/web/"+code+"_embargo.pdf /var/web/"+code+".pdf"));
                   //remoteFile.delete();
                   //File ren=new File(new URI("file:///var/web/embargo_"+code+".pdf"));
                   //ren.renameTo(remoteFile);
                   copyfile(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i],GW_ETD.getAbsolutePath()+"/embargo_warning/unembargo/"+child[i]);
                   File f = new File(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i]);
                    System.out.println(" deleting file "+child[i]);
                   f.delete();
               }
               else if(length.equals("1")&&absmonthdiff>=6)
               {
                   System.out.println(" deleting file "+ "/var/web/"+code+".pdf");
                   //File remoteFile = new File(new URI("file:///var/web/"+code+".pdf"));
                   System.out.println(executeCommand("S0uth12@","192.245.136.151","rm /var/web/"+code+".pdf"));
                   System.out.println(executeCommand("S0uth12@","192.245.136.151","mv /var/web/"+code+"_embargo.pdf /var/web/"+code+".pdf"));
                   //remoteFile.delete();
                   //File ren=new File(new URI("file:///var/web/embargo_"+code+".pdf"));
                   //ren.renameTo(remoteFile);
                   copyfile(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i],GW_ETD.getAbsolutePath()+"/embargo_warning/unembargo/"+child[i]);
                   File f = new File(GW_ETD.getAbsolutePath()+"/embargo_warning/"+child[i]);
                    System.out.println(" deleting file "+child[i]);
                   f.delete();
               }

               channel.disconnect();

           }
           catch(Exception e)
           {
               System.out.println(e);
               e.printStackTrace();
           }

       }
   }
   static String getEmbargoCode(String name)
   {
       int index1=name.indexOf("_");
               String temp1=name.substring(index1+1);
               int index2=temp1.indexOf("_");
               int index3=temp1.lastIndexOf("_");
               String id=temp1.substring(index2+1, index3);

               return id;
   }
   static boolean isEmbargo(String code, File Parent)
   {
       String child[]=Parent.list();
       boolean found=false;
       for(int i =0;i<child.length;i++)
       {
           if(child[i].contains("warning.txt"))
           {
               
               String id=getEmbargoCode(child[i]);

               
               if(code.equals(id))
               {
                   
                   found=true;
                   //copyfile(Parent.getAbsolutePath()+"/"+child[i],GW_ETD.getAbsolutePath()+"/embargo_warning");
                   break;
               }
           }

       }
       return found;
   }
   static String find(String lname)
   {
       File f = new File(m_file);
       String code="";
        String pattern = "[^A-Za-z]";
       try
       {
       BufferedReader br=new BufferedReader(new FileReader(f));
       String line=br.readLine();

        while(line!=null)
        {
            
            if(line.startsWith("=001"))
            {
                int ind=line.indexOf("_");
                code=line.substring(ind+1);
            }
            
            if(line.startsWith("=100"))
            {
                int ind1= line.indexOf("a");
                int ind2=line.indexOf(",");
                int ind3=line.lastIndexOf(".");
                String name=line.substring(ind1+1,ind2);
                name=name.replace(pattern,"");
                String tname="";
                for(int i =0;i<name.length();i++)
                {
                    char c = name.charAt(i);
                    if(Character.isLetter(c))
                        tname=tname+c;
                }
                //System.out.println(tname+" is the last name in find method");
                if(tname.equals(lname))
                {
                    author=line.substring(ind1+1,ind3);
                    line=br.readLine();
                    
                    if(line.startsWith("=245"))
            {
                ind1=line.indexOf("a");
                ind2=line.lastIndexOf("h");
                title=line.substring(ind1+1, ind2-1);
                title=title.toLowerCase();
                title=changeCase(title);
                //System.out.println(title);
                System.out.flush();

            }
                    break;
                }

            }
            line=br.readLine();

        }
       }
       catch(Exception e)
       {
           System.out.println(e);
           e.printStackTrace();
       }
       return code;

   }
static void createHtml(File html_f,String Dissertation_folder,File Dissertation_parent,String code)
{
    //File dir=new File(html_f);
    
    ChannelSftp channel=getChannel("S0uth12@","192.245.136.151");

    FileNameMap fileNameMap = URLConnection.getFileNameMap();
    System.out.println("Processing "+code +"to create HTML with Dissertation folder set to "+Dissertation_folder);

    Boolean embargo=isEmbargo(code,Dissertation_parent);
    System.out.println(code+" embargoed status is "+embargo);
    String mime="",val="";
    long size=0;
    try
    {
        channel.cd("/var/web");
        if(Dissertation_folder!=null&&embargo==true)
        {
            
            {
                
                channel.mkdir("etd_"+code);
    channel.cd("/var/web/etd_"+code);
    File parentDir=new File(Dissertation_parent.getAbsolutePath()+"/"+Dissertation_folder);
    System.out.println("Create Html pwd : "+channel.pwd());
    String[] child=parentDir.list();
    for(int i=0;i<child.length;i++)
    {
        File file = new File(parentDir.getAbsolutePath()+"/"+child[i]);
        //System.out.println(file.getAbsolutePath());
       
        channel.put(new FileInputStream(file),file.getName());
    }

          val=Dissertation_parent.getAbsolutePath()+"/destinationPDF/embargo_"+code+".pdf";
          File file = new File(Dissertation_parent.getAbsolutePath()+"/"+Dissertation_folder+"/"+code+".pdf");
          URL url=file.toURL();
          String fileurl=url.toExternalForm();


          mime=fileNameMap.getContentTypeFor(fileurl);
          size=file.length();
          //System.out.println(file.getAbsolutePath());
          File f =new File(val);
          channel.put(new FileInputStream(f),f.getName());
            }
        }
        else if(Dissertation_folder!=null && embargo==false)
        {
            channel.mkdir("etd_"+code);
            channel.cd("/var/web/etd_"+code);
            System.out.println("Create Html pwd : "+channel.pwd());
            File parentDir=new File(Dissertation_parent.getAbsolutePath()+"/"+Dissertation_folder);

            String[] child=parentDir.list();
            for(int i=0;i<child.length;i++)
            {
                File file = new File(parentDir.getAbsolutePath()+"/"+child[i]);
                //System.out.println(file.getAbsolutePath());
                channel.put(new FileInputStream(file),file.getName());
            }
            val=Dissertation_parent.getAbsolutePath()+"/destinationPDF/"+code+".pdf";
            File file=new File(val);
            URL url=file.toURL();
            String fileurl=url.toExternalForm();


            mime=fileNameMap.getContentTypeFor(fileurl);
            size=file.length();
            channel.put(new FileInputStream(file),file.getName());
        }
        else if(Dissertation_folder==null)
        {
            if(embargo==true)
            {
                System.out.println(code +" is embargoed");
            channel.mkdir("etd_"+code);
            channel.cd("/var/web/etd_"+code);
            System.out.println("Create Html pwd : "+channel.pwd());
            val=Dissertation_parent.getAbsolutePath()+"/destinationPDF/"+code+".pdf";

          File file = new File(val);
          File temp = new File(Dissertation_parent.getAbsolutePath()+"/destinationPDF/embargo_"+code+".pdf");
          File fake=new File(cWorkDir.getAbsolutePath()+"/fake.pdf");
          file.renameTo(temp);
          URL url=fake.toURL();
          String fileurl=url.toExternalForm();
          //File fake=new File(cWorkDir.getAbsolutePath()+"/fake.pdf");
          System.out.println(file.getAbsolutePath());
          mime=fileNameMap.getContentTypeFor(fileurl);
          size=fake.length();
          channel.put(new FileInputStream(fake),code+".pdf");
          channel.put(new FileInputStream(temp),temp.getName());
            }
            else
            {
                System.out.println(code +" is not embargoed");
                 channel.mkdir("etd_"+code);
                 channel.cd("/var/web/etd_"+code);
                 System.out.println("Create Html pwd : "+channel.pwd());
                 val=Dissertation_parent.getAbsolutePath()+"/destinationPDF/"+code+".pdf";
                 File file = new File(val);
                 URL url=file.toURL();
                 String fileurl=url.toExternalForm();


                 mime=fileNameMap.getContentTypeFor(fileurl);
                 size=file.length();
                 channel.put(new FileInputStream(file),file.getName());
            }

        }



    }
    catch(Exception e)
    {
        System.out.println(e);
        e.printStackTrace();
    }

   
    try
    {
        if(Dissertation_folder!=null&&embargo==false)
        {
            getTitle(code);
    PrintWriter out=new PrintWriter(new FileWriter(html_f));
       out.println("<Html>");
       out.println("<head>");
       out.println("<title>");
       out.print(title);
       out.print("</title>");
       out.println("</head>");
       out.println("<body>");
       out.println("<table border=0 width=640 ALIGN=center STYLE=margin-left:25px;>");
       out.println("<tr><td><img src=etdbanner.jpg alt=Electronic Thesis and Dissertation /></td></tr>");
       out.println("<tr><td> <table width=85% align=center>");
       out.println("<tr width=85%><td><h1>Electronic Thesis and Dissertation</h1></td></tr>");
       out.println("<tr width=85%><td><h2>"+title+"</h2></td></tr>");
       out.println("<tr width=85%><td><h2>Author: "+author+"<h2></td></tr>");
       out.println("<tr width=85%><td><table>");

       File parentDir=new File(Dissertation_parent.getAbsolutePath()+"/"+Dissertation_folder);
       String[] child=parentDir.list();
       out.println("<tr VALIGN=BOTTOM>");
       out.println("<td>");
       out.println("<h2><a href=http://etd.gelman.gwu.edu/etd_"+code+"/"+code+".pdf>");
       out.println("Main Document");
       out.println("</a></h2>");
       out.println("</td>");
       out.println("<td >");
       out.println("<h2>( pdf "+" <img src=pdf_file.gif > , "+size/1024+" KB )</h2>");
       out.println("</td>");
       out.println("</tr>");
       for(int i=0;i<child.length;i++)
       {
           if(child[i].equals(code+".pdf")==false)
           {
               System.out.println(child[i]);
           out.println("<tr VALIGN=BOTTOM>");
           out.println("<td>");
       out.println("<h2><a href=http://etd.gelman.gwu.edu/etd_"+code+"/"+child[i]+">");
       out.println("Supporting Document");
       out.println("</a></h2>");
       out.println("</td>");
       File f =new File(parentDir.getAbsolutePath()+"/"+child[i]);
       URL url=f.toURL();
       String fileUrl=url.toExternalForm();

       out.println("<td>");
       out.println("<h2>( "+insertImage(child[i])+" , "+f.length()/1024+" KB )</h2>");
       out.println("</td>");
       out.println("</tr>");

           }
       }
       
       out.println("</table></td></tr></table></tr></tr></table></body></html>");
       out.close();
       channel.cd("/var/web");
       channel.put(new FileInputStream(html_f),html_f.getName());
       disconnect(channel);
       html_f.delete();
       }
        else if(Dissertation_folder!=null&&embargo==true)
           
        {
            getTitle(code);
    PrintWriter out=new PrintWriter(new FileWriter(html_f));
       out.println("<Html>");
       out.println("<head>");
       out.println("<title>");
       out.print(title);
       out.print("</title>");
       out.println("</head>");
       out.println("<body>");
       out.println("<table border=0 width=640 ALIGN=center STYLE=margin-left:25px;>");
       out.println("<tr><td><img src=etdbanner.jpg alt=Electronic Thesis and Dissertation /></td></tr>");
       out.println("<tr><td> <table width=85% align=center>");
       out.println("<tr width=85%><td><h1>Electronic Thesis and Dissertation</h1></td></tr>");
       out.println("<tr width=85%><td><h2>"+title+"</h2></td></tr>");
       out.println("<tr width=85%><td><h2>Author: "+author+"<h2></td></tr>");
       out.println("<tr width=85%><td><table>");

       File parentDir=new File(Dissertation_parent.getAbsolutePath()+"/"+Dissertation_folder);
       String[] child=parentDir.list();
       out.println("<tr VALIGN=BOTTOM>");
       out.println("<td>");
       out.println("<h2><a href=http://etd.gelman.gwu.edu/etd_"+code+"/"+code+".pdf>");
       out.println("Main Document");
       out.println("</a></h2>");
       out.println("</td>");
       out.println("<td >");
       out.println("<h2>( pdf "+" <img src=pdf_file.gif > , "+size/1024+" KB )</h2>");
       out.println("</td>");
       out.println("</tr>");
       /*for(int i=0;i<child.length;i++)
       {
           if(child[i].equals(code+".pdf")==false)
           {
               System.out.println(child[i]);
           out.println("<tr VALIGN=BOTTOM>");
           out.println("<td>");
       out.println("<h2><a href=http://etd.gelman.gwu.edu/etd_"+code+"/"+child[i]+">");
       out.println("Supporting Document");
       out.println("</a></h2>");
       out.println("</td>");
       File f =new File(parentDir.getAbsolutePath()+"/"+child[i]);
       URL url=f.toURL();
       String fileUrl=url.toExternalForm();

       out.println("<td>");
       out.println("<h2>( "+insertImage(child[i])+" , "+f.length()/1024+" KB )</h2>");
       out.println("</td>");
       out.println("</tr>");

           }
       }*/

       out.println("</table></td></tr></table></tr></tr></table></body></html>");
       out.close();
       channel.cd("/var/web");
       channel.put(new FileInputStream(html_f),html_f.getName());
       disconnect(channel);
       html_f.delete();
       }
        else
        {
            getTitle(code);
       PrintWriter out=new PrintWriter(new FileWriter(html_f));
       out.println("<Html>");
       out.println("<head>");
       out.println("<title>");
       out.print(title);
       out.print("</title>");
       out.println("</head>");
       out.println("<body>");
       out.println("<table border=0 width=640 ALIGN=center STYLE=margin-left:25px;>");
       out.println("<tr><td><img src=etdbanner.jpg alt=Electronic Thesis and Dissertation /></td></tr>");
       out.println("<tr><td> <table width=85% align=center>");
       out.println("<tr width=85%><td><h1>Electronic Thesis and Dissertation</h1></td></tr>");
       out.println("<tr width=85%><td><h2>"+title+"</h2></td></tr>");
       out.println("<tr width=85%><td><h2>Author: "+author+"<h2></td></tr>");
       out.println("<tr width=85%><td><table>");

       //File parentDir=new File(Dissertation_parent.getAbsolutePath()+"/"+Dissertation_folder);
       //String[] child=parentDir.list();
       out.println("<tr VALIGN=BOTTOM>");
       out.println("<td>");
       out.println("<h2><a href=http://etd.gelman.gwu.edu/etd_"+code+"/"+code+".pdf>");
       out.println("Main Document");
       out.println("</a></h2>");
       out.println("</td>");
       out.println("<td >");
       out.println("<h2>( pdf "+" <img src=pdf_file.gif > , "+size/1024+" KB )</h2>");
       out.println("</td>");
       out.println("</tr>");
        out.println("</table></td></tr></table></tr></tr></table></body></html>");
       out.close();
       channel.cd("/var/web");
       channel.put(new FileInputStream(html_f),html_f.getName());
       disconnect(channel);
       html_f.delete();
      
            
        }
    }


       catch(Exception e)
       {
           System.out.println(e);
           System.out.flush();
           e.printStackTrace();
       }


}
static ChannelSftp getChannel(String password,String ip)
{
    ChannelSftp sftpChannel=null;
     jsch = new JSch();
 session = null;
try {
    System.out.println(ip);
session = jsch.getSession("jshieh", ip, 9999);
//System.out.println("session obtained, about to set password");
session.setConfig("StrictHostKeyChecking", "no");
session.setConfig("PreferredAuthentications", "password");

session.setPassword(password);
//System.out.println("password set");
session.connect();
//System.out.println("connected ....");

Channel channel = session.openChannel("sftp");
channel.connect();
 sftpChannel = (ChannelSftp) channel;
//sftpChannel.cd("/var/web");

} catch (JSchException e) {
    System.out.println(e);
e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
}
 //catch (SftpException e) {
//e.printStackTrace();
//}
return sftpChannel;
}
static String executeCommand(String password,String ip,String command)
{
    ChannelSftp sftpChannel=null;
   
    String result="";
     jsch = new JSch();
    com.jcraft.jsch.Session ssn = null;
    System.out.println(command);
    try
    {
        System.out.println(ip);
        ssn = jsch.getSession("jshieh", ip, 9999);
        //System.out.println("session obtained, about to set password");
        ssn.setConfig("StrictHostKeyChecking", "no");
        ssn.setConfig("PreferredAuthentications", "password");

        ssn.setPassword(password);
        //System.out.println("password set");
        ssn.connect();
        //System.out.println("connected ....");

        Channel channel = ssn.openChannel("exec");
        //channel.connect();
        ((ChannelExec)channel).setCommand(command);

        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);

        InputStream in=channel.getInputStream();
        channel.connect();

        byte[] tmp=new byte[1024];
        while(true)
        {

            while(in.available()>0)
            {

                int i=in.read(tmp, 0, 1024);
                if(i<0)break;
                result=result+new String(tmp, 0, i);

            }
            if(channel.isClosed())
            {
            System.out.println("exit-status: "+channel.getExitStatus());
            break;
            }
            try
            {
                Thread.sleep(1000);
            }
            catch(Exception ee)
            {
                System.out.println(ee);
                ee.printStackTrace();
            }
      }

      channel.disconnect();
      ssn.disconnect();





 //sftpChannel = (ChannelSftp) channel;
//sftpChannel.cd("/var/web");
    }
    catch (Exception e)
    {
        System.out.println(e);
        e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
    }
 //catch (SftpException e) {
//e.printStackTrace();
//}
    return result;
}
static void disconnect(ChannelSftp channel)
{
    channel.exit();
    session.disconnect();
}
static String changeCase(String title)
{
    try
    {
            System.out.println("TITLE: "+title);
            title=title.replaceAll("  ", " ");
                final StringBuilder result = new StringBuilder(title.length());
                //System.out.println(result.length());
                String[] words = title.split(" ");
                //System.out.println("Length: "+words.length);
        for(int i=0,l=words.length;i<l;++i)
        {
            if(i>0)
                result.append(" ");
            //System.out.println("TITLE: "+words[i]);
            result.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));

        }
                title=result.toString();
              //  System.out.println("TITLE: "+title);
    }
    catch(Exception e)
    {
        System.out.println(e);
        e.printStackTrace();
        System.out.flush();
    }
                return title;
}
static String insertImage(String name)
{
    String ext="";
    int ind=name.lastIndexOf(".");
    String val="";
    ext=name.substring(ind+1);
    if(ext.equals("xls"))
        val=ext+" <img src=excel_file.gif />";
    else if(ext.equals("pdf"))
        val=ext+" <img src=pdf_file.gif />";
    else if(ext.equals("doc"))
        val=ext+" <img src=word_file.gif />";
    else if(ext.equals("ppt"))
        val=ext+" <img src=powerpoint_file.gif />";
    else if(ext.equals("avi")||ext.equals("mov")||ext.equals("moov")||ext.equals("mp21")||ext.equals("movie")||ext.equals("mpeg")||ext.equals("mpeg4")||ext.equals("swf")||ext.equals("asf")||ext.equals("flv")||ext.equals("rm")||ext.equals("wmv"))
        val=ext+" <img src=movie.gif />";
    else if(ext.equals("wav")||ext.equals("mp3")||ext.equals("au")||ext.equals("aiff")||ext.equals("raw"))
         val=ext+" <img src=Sound_Icon.jpg />";
    else
        val=ext+" <img src= unknown.jpg />";
    return val;
}
static void getFiles(String monthyear)
{
     try
        {

            //String currentdir = System.getProperty("user.dir");
            cWorkDir= new File("/home/jshieh/RDG/ETD-UMI/GW-ETD/cWorkDir");
            File f= new File(cWorkDir.getAbsolutePath()+"/data");
            BufferedReader in = new BufferedReader(new FileReader(f));
            String password=in.readLine();
            System.out.println(password);
            in.close();
            System.out.println("about to connect");
            ChannelSftp channel=getChannel(password,"128.164.212.151");
            System.out.println("connected");
            channel.cd("/media/storage/etd_uploads");
            System.out.println("connected to the server");
            Vector v=channel.ls(".");
            Iterator i=v.iterator();
            String filename="";
            Vector store=new Vector();
                    
            while(i.hasNext())
            {

                String line=i.next().toString();
               // System.out.println(line);
                int ind=line.lastIndexOf(" ");
                filename=line.substring(ind+1);
                SftpATTRS attr=channel.lstat(filename);
                String mtime=attr.getMtimeString();
                int index = mtime.lastIndexOf(" ");
                String year=mtime.substring(index+1);
                index=mtime.indexOf(" ");
                mtime=mtime.substring(index);
                mtime.indexOf(" ");
                String month=mtime.substring(1,index+1);
                String m=changeMonth(month);
                String date=m+"-"+year;
                //System.out.println(monthyear+" "+date);
                if(monthyear.equals(date)&&!filename.equals(".")&&!filename.equals("..") &&filename.endsWith(".zip"))
                {
                    channel.get(filename,cWorkDir.getAbsolutePath()+"/"+filename);
                    Process p=Runtime.getRuntime().exec("chmod 777 "+cWorkDir.getAbsolutePath()+"/"+filename);
                    p.waitFor();
                    store.add(filename);
                }
            }
            System.out.println("Starting to run runGW.pl");
            Process p = Runtime.getRuntime().exec(cWorkDir.getAbsolutePath()+"/runGW.pl");
            p.waitFor();
            p.destroy();
            System.out.println("runGW.pl finished execution");
             i=store.iterator();
             File backup=null;
             String archive = "/home/jshieh/RDG/ETD-UMI/GW-ETD/"+year+month;
             System.out.println("GetFiles: Archive Path: "+archive);
             boolean success = (new File(archive)).mkdir();
             if(success)
             {
                 backup=new File(archive);
                 while(i.hasNext())
                 {
                     System.out.println("GetGiles backup loop "+filename);
                     filename=i.next().toString();
                     copyfile(cWorkDir.getAbsolutePath()+"/"+filename,backup.getAbsolutePath()+"/"+filename);
                 }
             }
             else
             {
                 System.out.println(archive+" folder could not be created");
             }
             i=store.iterator();
             while(i.hasNext())
            {
                 filename=i.next().toString();
               if(filename.equals("log.txt"))
                   continue;
                 if(filename.equals("fake.pdf"))
                     continue;
                //int ind=line.lastIndexOf(" ");
                //filename=line.substring(ind+1);
                System.out.println(filename);
                File file = new File(cWorkDir.getAbsolutePath()+"/"+filename);
                System.out.println(file.getAbsolutePath()+ " is deleted");
                
                file.delete();
             }
        }
        catch(Exception e)
        {
            System.out.println(e);
            e.printStackTrace();
        }

}
private static void copyfile(String srFile, String dtFile){
    try{
      File f1 = new File(srFile);
      File f2 = new File(dtFile);
      InputStream in = new FileInputStream(f1);

      //For Append the file.
//      OutputStream out = new FileOutputStream(f2,true);

      //For Overwrite the file.
      OutputStream out = new FileOutputStream(f2);

      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0){
        out.write(buf, 0, len);
      }
      in.close();
      out.close();
      System.out.println("File copied.");
    }
    catch(FileNotFoundException ex){
      System.out.println(ex.getMessage() + " in the specified directory.");
      System.exit(0);
    }
    catch(IOException e){
      System.out.println(e.getMessage());
    }
  }

static String changeMonth(String month)
{
    if(month.equals("Jan"))
        return "01";
    else if(month.equals("Feb"))
        return "02";
    else if(month.equals("Mar"))
        return "03";
    else if(month.equals("Apr"))
        return "04";
    else if(month.equals("May"))
        return "05";
    else if(month.equals("Jun"))
        return "06";
    else if(month.equals("Jul"))
        return "07";
    else if(month.equals("Aug"))
        return "08";
    else if(month.equals("Sep"))
        return "09";
    else if(month.equals("Oct"))
        return "10";
    else if(month.equals("Nov"))
        return "11";
    return "12";

}
static Vector  getCodes()
{
    Vector v = new Vector();
     try
       {
         File f = new File(m_file);
         BufferedReader br=new BufferedReader(new FileReader(f));
        String line=br.readLine();
        while(line!=null)
        {
        if(line.startsWith("=001"))
            {
                int ind=line.indexOf("_");
                String code=line.substring(ind+1);
                //System.out.println("code: "+code);
                v.add(code);
            }
        line=br.readLine();
        }

     }

     catch(Exception e)
     {
         System.out.println(e);
     }
       return v;

}
static void getTitle(String target)
{
    try
       {

        File f = new File(m_file);
        BufferedReader br=new BufferedReader(new FileReader(f));
        String line=br.readLine();
        boolean found=false;
        while(line!=null)
        {

            if(line.startsWith("=001"))
            {
                int ind=line.indexOf("_");
                String code=line.substring(ind+1);
                if(code.equals(target))
                {

                    while(!found)
                    {
                        if(line.startsWith("=100"))
                        {

                            int ind1= line.indexOf("a");
                            int ind2=line.indexOf(",");
                            int ind3=line.lastIndexOf(".");
                            String name=line.substring(ind1+1,ind2);
                            author=line.substring(ind1+1,ind3);
                            //System.out.println(author);
                            System.out.flush();
                            line=br.readLine();
                            if(line.startsWith("=245"))
                            {
                                ind1=line.indexOf("a");
                                ind2=line.lastIndexOf("h");
                                title=line.substring(ind1+1, ind2-1);
                                title=title.toLowerCase();
                                title=changeCase(title);
                                found=true;
                                //System.out.println(title);
                                System.out.flush();

                            }
                        }
                        line=br.readLine();
                    }
                    break;
                }

            }
        line=br.readLine();
        }

     }

     catch(Exception e)
     {
         System.out.println(e);
     }

}
public static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
        String[] children = dir.list();
        for (int i=0; i<children.length; i++) {
            boolean success = deleteDir(new File(dir, children[i]));
            if (!success) {
                return false;
            }
        }
    }

    // The directory is now empty so delete it
    return dir.delete();
}
public static void checkTags()
{
    File f = new File(m_file);
    try
    {
    BufferedReader br=new BufferedReader(new FileReader(f));
    String line=br.readLine();
    String code="",author="",title="";
    while(line!=null)
    {
        if(line.startsWith("=001"))
            {
                int ind=line.indexOf("_");
                code=line.substring(ind+1);
                line=br.readLine();
                while(!line.startsWith("=520"))
                {
                    if(line.startsWith("=100"))
                    {
                        int index=line.indexOf("a");
                        author=line.substring(index+1);
                    }
                    if(line.startsWith("=245"))
                    {
                        int index=line.indexOf("a");
                        int index2 =line.indexOf("$h");
                        title=line.substring(index+1,index2);
                    }
                    line=br.readLine();

                }
                //System.out.println("code:"+code+" " +line);
                if(line.contains("<"))
                {
                    Code c = new Code (code,true,title,author);
                    tags.add(c);
                }
                else
                {
                    Code c = new Code(code,false,title,author);
                    tags.add(c);
                }
            }
        line=br.readLine();
    }
    }
    catch(Exception e)
    {
        System.out.println(e);
        e.printStackTrace();
    }

}
public static void writeWebLog()
{
    System.out.println("Writing Web Log");
    try
    {
            PrintWriter pr = new PrintWriter(new FileWriter(web_log));
            Iterator i = tags.iterator();
            count =0;
        while(i.hasNext())
        {
            count++;
            Code c = (Code)i.next();
            //System.out.println(c.getAuthor()+ " "+c.getCode()+ " "+c.getTitle());
            if(c.getTag())
            {
                pr.println("Record Number:"+count+ "# Author: "+ c.getAuthor()+ "# Title: "+c.getTitle()+ "# This record <b>has HTML tags</b>" );
                System.out.println("Record Number:"+count+ "# Author: "+ c.getAuthor()+ "# Title: "+c.getTitle()+ "# This record <b>has HTML tags</b>" );
            }
            else
            {
                pr.println("Record Number:"+count+ "# Author: "+ c.getAuthor()+ "# Title: "+c.getTitle()+"# This record has <b>No HTML tags</b>" );
                System.out.println("Record Number:"+count+ "# Author: "+ c.getAuthor()+ "# Title: "+c.getTitle()+"# This record has <b>No HTML tags</b>" );
            }
        }
            pr.close();
    }
    catch(Exception e)
    {
        System.out.println();
        e.printStackTrace();
    }
}

}
class Code
{
    private String code;
    private boolean tag;
    private String title;
    private String author;
    public Code(String code,boolean tag,String Title,String author)
    {
        this.code=code;
        this.tag=tag;
        this.title=Title;
        this.author=author;
    }
    public String getCode()
    {
        return code;
    }
    public boolean getTag()
    {
        return tag;
    }
    public String getTitle()
    {
        return title;
    }
    public String getAuthor()
    {
        return author;
    }
}