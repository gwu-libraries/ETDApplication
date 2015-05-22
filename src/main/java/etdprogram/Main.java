package etdprogram;

import java.io.*;

import com.jcraft.jsch.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;
import java.util.Properties;


public class Main {
    static String m_file, title, author, year, month, dateNow;
    static File cWorkDir, GW_ETD, web_log, record_file;
    static PrintWriter pr;
    static JSch jsch;
    static com.jcraft.jsch.Session session;
    static ArrayList codes = new ArrayList();
    static Vector tags = new Vector();
    static int count;
    static String cWorkDir_filepath, GW_ETD_filepath;
    static Properties props;

    public static void main(String[] args) {
        try {
            //Properties loaded from etd.props.
            props = new Properties();
            try {
                props.load(new FileInputStream(("etd.props")));
            } catch (IOException ex) {
                System.out.println("Cannot load etd.props");
                throw ex;
            }
            props.list(System.out);
            GW_ETD_filepath = props.getProperty("base", (new File(".")).getCanonicalPath());

            System.out.println("ETD base: " + GW_ETD_filepath);
            cWorkDir_filepath = GW_ETD_filepath + "/cWorkDir";
            GW_ETD = new File(GW_ETD_filepath);
            cWorkDir = new File(cWorkDir_filepath);
            record_file=new File(cWorkDir, "record");
            BufferedWriter pr = new BufferedWriter(new FileWriter(record_file, true));
            Calendar currentDate = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
            dateNow = formatter.format(currentDate.getTime());
            //monthyear can be provided as a commandline argument.
            //Otherwise, it is the previous month.
            String monthyear = null;
            if (args.length > 1) {
                throw new Exception("Only 0 or 1 arguments can be provided.");
            } else if (args.length == 1) {
                monthyear = args[0];
                if (monthyear.length() != 6) {
                    throw new Exception("Format of monthyear argument must be YYYYMM");
                }
                month = monthyear.substring(0, 2);
                year = monthyear.substring(2);
            } else {
                int index = dateNow.indexOf("/");
                year = dateNow.substring(0, index);
                int index2 = dateNow.lastIndexOf("/");
                month = dateNow.substring(index + 1, index2);
                if (month.equals("01"))
                    month = "12";
                else if (month.equals("02"))
                    month = "01";
                else if (month.equals("03"))
                    month = "02";
                else if (month.equals("04"))
                    month = "03";
                else if (month.equals("05"))
                    month = "04";
                else if (month.equals("06"))
                    month = "05";
                else if (month.equals("07"))
                    month = "06";
                else if (month.equals("08"))
                    month = "07";
                else if (month.equals("09"))
                    month = "08";
                else if (month.equals("10"))
                    month = "09";
                else if (month.equals("11"))
                    month = "10";
                else if (month.equals("12"))
                    month = "11";
                int intyear = Integer.parseInt(year);
                if (month.equals("12")) {
                    intyear = intyear - 1;
                    year = Integer.toString(intyear);
                }
                monthyear = month + year;
            }
            dateNow = dateNow.replaceAll("/", "");
            System.out.println("Monthyear: " + monthyear);
            BufferedReader br = new BufferedReader(new FileReader(record_file));
            String line = br.readLine();
            boolean found = false;
            while (line != null) {

                if (line.equals(monthyear)) {
                    found = true;
                    break;
                }
                line = br.readLine();
            }
            if (found == true) {
                System.out.println("The files for " + monthyear + " have already been processed.");
                System.exit(0);
            }


            pr.append(monthyear);
            pr.newLine();
            pr.flush();

            getFiles(month + "-" + year);

            File fake = new File(cWorkDir.getAbsolutePath() + "/fake");
            File temp = new File(cWorkDir.getAbsolutePath() + "/fake.pdf");
            fake.renameTo(temp);

            File f = null;
            String[] child = null;
            System.out.println("Current Directory: " + GW_ETD.getAbsolutePath());
            String[] children = cWorkDir.list();
            for (int i = 0; i < children.length; i++) {
                if (children[i].endsWith("mrk")) {
                    m_file = cWorkDir.getAbsolutePath() + "/" + children[i];
                    System.out.println("mrk file: " + m_file);
                }
            }
            checkTags();
            for (int i = 0; i < children.length; i++) {


                f = new File(cWorkDir.getAbsolutePath() + "/" + children[i]);
                if ((children[i].equals("destinationPDF") == false) && (children[i].equals("destinationXML") == false) && (children[i].equals("lib") == false) && (children[i].equals("PDF") == false) && (children[i].equals("XML") == false) && (children[i].equals("LOADED") == false) && (children[i].equals("src") == false) && (f.isDirectory() == true)) {
                    System.out.println("subfolder: " + children[i]);//the thesis thesis folders

                    if (children[i].contains("0075"))//check for gwu thesis code
                    {
                        int index = children[i].indexOf("_");
                        String lname = children[i].substring(0, index);
                        String code = find(lname);
                        System.out.println("GWU thesis: " + f.getAbsolutePath() + " has code " + code + " with lname " + lname);

                        process(children[i], cWorkDir);


                        codes.add(code);

                    }
                }

            }

            Vector v = getCodes();
            Iterator it = v.iterator();
            while (it.hasNext()) {
                String code = (String) it.next();
                found = false;
                for (int i = 0; i < codes.size(); i++) {
                    String dest = (String) codes.get(i);
                    if (code.equals(dest)) {
                        found = true;
                        break;
                    }
                }
                if (found == false) {
                    File html_f = new File(cWorkDir.getAbsolutePath() + "/" + code + ".html");
                    createHtml(html_f, null, cWorkDir, code);

                }
            }


            System.out.println("Doing Post transfer work");

            String mrkfile;
            int ind = m_file.lastIndexOf("/");
            mrkfile = m_file.substring(ind + 1);
            System.out.println("mrkfile: " + mrkfile);

            execute("perl " + GW_ETD.getAbsolutePath() + "/2gwMARC.pl " + m_file.toString() + " " + cWorkDir.getAbsolutePath() + "/gwu" + dateNow + ".mrc");
            execute("perl " + GW_ETD.getAbsolutePath() + "/gw245etd.pl " + cWorkDir.getAbsolutePath() + "/gwu" + dateNow + ".mrc " + cWorkDir.getAbsolutePath() + "/gwu" + dateNow + "rev.mrc");
            PrintStream printStream = new PrintStream(new FileOutputStream(new File(cWorkDir.getAbsolutePath() + "/gwu" + dateNow + "revU8.mrc")));
            String cmd = props.getProperty("marcdump.exe") + " -f MARC-8 -t UTF-8 -o marc -l 9=97 " + cWorkDir.getAbsolutePath() + "/gwu" + dateNow + "rev.mrc > " + cWorkDir.getAbsolutePath() + "/gwu" + dateNow + "revU8.mrc";
            Process p = Runtime.getRuntime().exec(cmd);
            InputStream in = p.getInputStream();
            BufferedReader bin = new BufferedReader(new InputStreamReader(in));
            line = bin.readLine();
            while (line != null) {
                printStream.println(line);
                System.out.println(line);
                line = bin.readLine();
            }
            p.waitFor();
            //This was originally:
            //p = Runtime.getRuntime().exec("mv "+GW_ETD.getAbsolutePath()+"/destinationPDF/*.pdf "+GW_ETD.getAbsolutePath()+"/PDF");
            //but that seems wrong, so changed.
            execute("mv " + cWorkDir.getAbsolutePath() + "/destinationPDF/*.pdf " + cWorkDir.getAbsolutePath() + "/PDF");
            //This was originally:
            //p = Runtime.getRuntime().exec("cp "+GW_ETD.getAbsolutePath()+"/destinationXML/*.xml "+GW_ETD.getAbsolutePath()+"/XML");
            //but that seems wrong, so changed.
            execute("mv " + cWorkDir.getAbsolutePath() + "/destinationXML/*.xml " + cWorkDir.getAbsolutePath() + "/XML");
            execute("cp  " + cWorkDir.getAbsolutePath() + "/gwu" + dateNow + "revU8.mrc " + props.getProperty("www.root") + "/rdg/");
            web_log = new File(cWorkDir.getAbsolutePath() + "/web_log" + dateNow + ".txt");

            writeWebLog();
            execute("cp " + web_log.getAbsolutePath() + " /tmp/");
            System.setProperty("user.dir", cWorkDir.getAbsolutePath());
            child = cWorkDir.list();
            for (int i = 0; i < child.length; i++) {
                File file = new File(cWorkDir.getAbsolutePath() + "/" + child[i]);
                if (child[i].contains("GW_etd_") && file.isDirectory() == true)
                    deleteDir(file);
                if (child[i].endsWith(".mrk"))
                    file.delete();
                if (child[i].endsWith(".mrc"))
                    file.delete();
                if (child[i].equals("log.txt")) {
                    File logFile = new File(cWorkDir.getAbsolutePath() + "/log" + dateNow + ".txt");
                    file.renameTo(logFile);
                }
                if (child[i].endsWith(".txt") && child[i].endsWith("warning.txt") == false)
                    file.delete();
                if (child[i].contains("_0075") && file.isDirectory() == true)
                    deleteDir(file);
                if (child[i].endsWith("warning.txt")) {
                    execute("cp " + cWorkDir.getAbsolutePath() + "/" + child[i] + " " + GW_ETD.getAbsolutePath() + "/embargo_warning");
                    //This code was originally:
                    //File warning = new File(GW_ETD.getAbsolutePath() + "/embargo_warning/" + child[i]);
                    //However, it makes no sense to delete the file that was just copied so changed to:
                    File warning = new File(cWorkDir.getAbsolutePath() + "/" + child[i]);
                    warning.delete();
                }

            }


            new_embargo(new File(GW_ETD.getAbsolutePath() + "/embargo_warning"));
            old_embargo(new File(GW_ETD.getAbsolutePath() + "/embargo_warning"));
            postMail();
            temp = new File(cWorkDir.getAbsolutePath() + "/fake.pdf");
            fake = new File(cWorkDir.getAbsolutePath() + "/fake");
            temp.renameTo(fake);
            System.out.println("Done");
            System.exit(0);

        } catch (Exception e) {
            System.out.flush();
            System.out.println(e);
            e.printStackTrace();
            System.out.flush();
            System.exit(1);
        }

    }

    public static void execute(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", cmd});
        InputStream in = p.getInputStream();
        BufferedReader bin = new BufferedReader(new InputStreamReader(in));
        String line = bin.readLine();
        System.out.println(cmd);
        while (line != null) {
            System.out.println(line);
            line = bin.readLine();
        }
        p.waitFor();
        if (p.exitValue() != 0) {
            throw new Exception(cmd + " returned " + p.exitValue());
        }
    }

    public static void postMail() throws Exception {
        javax.mail.Session mailSession = javax.mail.Session.getDefaultInstance(props);
        Message simpleMessage = new MimeMessage(mailSession);

        String from = props.getProperty("mail.from");
        InternetAddress fromAddress = null;
        InternetAddress cc1 = null;
        InternetAddress toAddress = null;
        fromAddress = new InternetAddress(from);
        cc1 = new InternetAddress(from);
        toAddress = new InternetAddress(props.getProperty("mail.to"));

        simpleMessage.setFrom(fromAddress);
        simpleMessage.setRecipient(javax.mail.Message.RecipientType.CC, cc1);
        simpleMessage.setRecipient(javax.mail.Message.RecipientType.TO, toAddress);
        simpleMessage.setSubject("rec load request: gw ETD " + year + " " + month);
        simpleMessage.setText("WRLC Loader;\n the set of " + count + " electronic thesis and dissertation is found: \n" + props.get("base.url") + "/rdg/gwu" + dateNow + "revU8.mrc\n\n" + "Records are coded 7 (minimal) in LDR/17 and i(ISBD) in LDR/18\n" + "MFHD:\n\n" + "Fields 852 and 856 to be used in MFHD with 852 $b and $h coded for electronic dissertation.\n\n" + "Please create Item for Item type: Electronic.\n" + "Please let me know if there is any question. Thank you.\n\n" + "*" + from + "\n");

        Transport.send(simpleMessage);
    }


    static void process(String Dissertation_folder, File Dissertation_parent) throws Exception {
        int index = Dissertation_folder.indexOf("_");
        String lname = Dissertation_folder.substring(0, index);
        String code = find(lname);
        System.out.println("DP: " + Dissertation_parent.getAbsolutePath());
        Boolean embargo = isEmbargo(code, Dissertation_parent);


        if (embargo == true) {
            System.out.println(Dissertation_folder + " is embargoed");
            copyfile(cWorkDir.getAbsolutePath() + "/fake.pdf", cWorkDir.getAbsolutePath() + "/" + Dissertation_folder + "/" + code + ".pdf");
            File fake = new File(cWorkDir.getAbsolutePath() + "/destinationPDF/" + "embargo_" + code + ".pdf");
            File original = new File(cWorkDir.getAbsolutePath() + "/destinationPDF/" + code + ".pdf");
            original.renameTo(fake);
            File html_f = new File(cWorkDir.getAbsolutePath() + "/" + code + ".html");
            codes.add(code);
            createHtml(html_f, Dissertation_folder, Dissertation_parent, code);
            copyfile(cWorkDir.getAbsolutePath() + "/GWU_etd_" + code + "_warning.txt", GW_ETD.getAbsolutePath() + "/embargo_warning" + "/GWU_etd_" + code + "_warning.txt");

        } else {
            System.out.println(Dissertation_folder + " is not embargoed");
            File html_f = new File(cWorkDir.getAbsolutePath() + "/" + code + ".html");
            codes.add(code);
            createHtml(html_f, Dissertation_folder, Dissertation_parent, code);
        }

    }

    static void new_embargo(File embargo_folder) throws Exception {
        System.out.println("New embargo: " + embargo_folder.getAbsolutePath());
        String child[] = embargo_folder.list();
        ChannelSftp channel = getChannel(
                props.getProperty("dest.sftp.username"),
                props.getProperty("dest.sftp.password"),
                props.getProperty("dest.sftp.host"),
                Integer.parseInt(props.getProperty("dest.sftp.port")));
        int months = 0;
        for (int i = 0; i < child.length; i++) {
            File warning = new File(GW_ETD.getAbsolutePath() + "/embargo_warning/" + child[i]);
            if (warning.isDirectory()) {
                continue;
            }
            System.out.println("Child file: " + warning.getAbsolutePath());
            String code = getEmbargoCode(child[i]);
            BufferedReader bin = new BufferedReader(new FileReader(warning));
            String line = bin.readLine();
            int index = line.lastIndexOf(" ");
            String length = line.substring(index + 1);
            long yourLong = warning.lastModified();
            Date yourDate = new Date(yourLong);
            DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            String formattedDate = formatter.format(yourDate);
            index = formattedDate.lastIndexOf("-");
            String lyear = formattedDate.substring(index + 1);
            index = formattedDate.indexOf("-");
            String mtime = formattedDate.substring(index + 1);
            String lmonth = mtime.substring(0, index + 1);
            String m = changeMonth(lmonth);
            int yearint = Integer.parseInt(year);
            int monthint = Integer.parseInt(month);
            int lyearint = Integer.parseInt(lyear);
            int lm = Integer.parseInt(m);
            int yeardifference = yearint - lyearint;
            int monthdifference = monthint - lm;
            int absmonthdiff = (yeardifference * 12) + monthdifference;
            if ((length.equals("3") && absmonthdiff >= 24) || (length.equals("2") && absmonthdiff >= 12) || (length.equals("1") && absmonthdiff >= 6)) {
                System.out.println("Unembargoing " + code);
                channel.rm("/var/web/etd_" + code + "/" + code + ".pdf");
                channel.rename("/var/web/etd_" + code + "/embargo_" + code + ".pdf", "/var/web/etd_" + code + "/" + code + ".pdf");
                copyfile(GW_ETD.getAbsolutePath() + "/embargo_warning/" + child[i], GW_ETD.getAbsolutePath() + "/embargo_warning/unembargo/" + child[i]);
                File f = new File(GW_ETD.getAbsolutePath() + "/embargo_warning/" + child[i]);
                f.delete();
            }
            channel.disconnect();

        }

    }

    static void old_embargo(File embargo_folder) throws Exception {
        String child[] = embargo_folder.list();
        String destUsername = props.getProperty("dest.sftp.username");
        String destPassword = props.getProperty("dest.sftp.password");
        String destHost = props.getProperty("dest.sftp.host");
        Integer destPort = Integer.parseInt(props.getProperty("dest.sftp.port"));
        ChannelSftp channel = getChannel(destUsername, destPassword, destHost, destPort);

        channel.cd("/var/web");
        int months = 0;
        for (int i = 0; i < child.length; i++) {
            File warningFile = new File(embargo_folder, child[i]);
            if (warningFile.isDirectory()) {
                continue;
            }
            String code = getEmbargoCode(child[i]);
            System.out.println("Embargo file is " + child[i] + " embargo code is " + code);
            BufferedReader bin = new BufferedReader(new FileReader(warningFile));
            String line = bin.readLine();
            bin.close();
            int index = line.lastIndexOf(" ");
            System.out.println(line + "and last index of space is " + index);
            String length = line.substring(index - 1);
            length = length.replaceAll(" ", "");
            long yourLong = warningFile.lastModified();
            Date yourDate = new Date(yourLong);
            DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            String formattedDate = formatter.format(yourDate);
            System.out.println("Modified date is " + formattedDate);
            index = formattedDate.lastIndexOf("-");
            String lyear = formattedDate.substring(index + 1);
            index = formattedDate.indexOf("-");
            String mtime = formattedDate.substring(index + 1);
            String lmonth = mtime.substring(0, index);
            int yearint = Integer.parseInt(year);
            int monthint = Integer.parseInt(month);
            int lyearint = Integer.parseInt(lyear);
            int lm = Integer.parseInt(lmonth);

            int yeardifference = yearint - lyearint;
            int monthdifference = Math.abs(monthint - lm);
            int absmonthdiff = (yeardifference * 12) + monthdifference;
            System.out.println("year is " + lyearint + " month is " + lm + " lmonth is " + monthint + " yeardifference is " + yeardifference + " monthdifference is " + monthdifference);
            System.out.println("embargo duration is " + length + " and absolute months since embargoed is " + absmonthdiff);

            if ((length.equals("3") && absmonthdiff >= 24) || (length.equals("2") && absmonthdiff >= 12) || (length.equals("2") && absmonthdiff >= 12)) {
                System.out.println("Unembargoing " + code);
                System.out.println(executeCommand(destUsername, destPassword, destHost, destPort, "rm /var/web/" + code + ".pdf"));
                System.out.println(executeCommand(destUsername, destPassword, destHost, destPort, "mv /var/web/" + code + "_embargo.pdf /var/web/" + code + ".pdf"));
                copyfile(warningFile.getAbsolutePath(), new File(embargo_folder, "unembargo/" + child[i]).getAbsolutePath());
                System.out.println(" deleting file " + child[i]);
                warningFile.delete();
            }

            channel.disconnect();

        }
    }

    static String getEmbargoCode(String name) {
        int index1 = name.indexOf("_");
        String temp1 = name.substring(index1 + 1);
        int index2 = temp1.indexOf("_");
        int index3 = temp1.lastIndexOf("_");

        String id = temp1.substring(index2 + 1, index3);

        return id;
    }

    static boolean isEmbargo(String code, File Parent) {
        String child[] = Parent.list();
        boolean found = false;
        for (int i = 0; i < child.length; i++) {
            if (child[i].contains("warning.txt")) {

                String id = getEmbargoCode(child[i]);


                if (code.equals(id)) {

                    found = true;
                    break;
                }
            }

        }
        return found;
    }

    static String find(String lname) throws Exception {
        File f = new File(m_file);
        String code = "";
        String pattern = "[^A-Za-z]";
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();

        while (line != null) {

            if (line.startsWith("=001")) {
                int ind = line.indexOf("_");
                code = line.substring(ind + 1);
            }

            if (line.startsWith("=100")) {
                int ind1 = line.indexOf("a");
                int ind2 = line.indexOf(",");
                int ind3 = line.lastIndexOf(".");
                String name = line.substring(ind1 + 1, ind2);
                name = name.replace(pattern, "");
                String tname = "";
                for (int i = 0; i < name.length(); i++) {
                    char c = name.charAt(i);
                    if (Character.isLetter(c))
                        tname = tname + c;
                }
                if (tname.equals(lname)) {
                    author = line.substring(ind1 + 1, ind3);
                    line = br.readLine();

                    if (line.startsWith("=245")) {
                        ind1 = line.indexOf("a");
                        ind2 = line.lastIndexOf("h");
                        title = line.substring(ind1 + 1, ind2 - 1);
                        title = title.toLowerCase();
                        title = changeCase(title);

                    }
                    break;
                }

            }
            line = br.readLine();

        }
        return code;

    }

    static void createHtml(File html_f, String Dissertation_folder, File Dissertation_parent, String code) throws Exception {
        ChannelSftp channel = getChannel(
                props.getProperty("dest.sftp.username"),
                props.getProperty("dest.sftp.password"),
                props.getProperty("dest.sftp.host"),
                Integer.parseInt(props.getProperty("dest.sftp.port")));

        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        System.out.println("Processing " + code + " to create HTML with Dissertation folder set to " + Dissertation_folder);

        Boolean embargo = isEmbargo(code, Dissertation_parent);
        System.out.println(code + " embargoed status is " + embargo);
        String mime = "", val = "";
        long size = 0;
        channel.cd("/var/web");
        if (Dissertation_folder != null && embargo == true) {

            {
                String dir = "etd_" + code;
                SftpATTRS attrs = null;
                try {
                    attrs = channel.stat(dir);
                } catch (SftpException ex) {
                    //Exception if does not exist
                }
                if (attrs != null && attrs.isDir()) {
                    System.out.println(dir + " already exists");
                } else {
                    channel.mkdir(dir);
                }
                channel.cd("/var/web/etd_" + code);
                File parentDir = new File(Dissertation_parent.getAbsolutePath() + "/" + Dissertation_folder);
                System.out.println("Create Html pwd : " + channel.pwd());
                String[] child = parentDir.list();
                for (int i = 0; i < child.length; i++) {
                    File file = new File(parentDir.getAbsolutePath() + "/" + child[i]);
                    channel.put(new FileInputStream(file), file.getName());
                }

                val = Dissertation_parent.getAbsolutePath() + "/destinationPDF/embargo_" + code + ".pdf";
                File file = new File(Dissertation_parent.getAbsolutePath() + "/" + Dissertation_folder + "/" + code + ".pdf");
                URL url = file.toURL();
                String fileurl = url.toExternalForm();


                mime = fileNameMap.getContentTypeFor(fileurl);
                size = file.length();
                File f = new File(val);
                channel.put(new FileInputStream(f), f.getName());
            }
        } else if (Dissertation_folder != null && embargo == false) {
            String dir = "etd_" + code;
            SftpATTRS attrs = null;
            try {
                attrs = channel.stat(dir);
            } catch (SftpException ex) {
                //Exception if does not exist
            }
            if (attrs != null && attrs.isDir()) {
                System.out.println(dir + " already exists");
            } else {
                channel.mkdir(dir);
            }
            channel.cd("/var/web/etd_" + code);
            System.out.println("Create Html pwd : " + channel.pwd());
            File parentDir = new File(Dissertation_parent.getAbsolutePath() + "/" + Dissertation_folder);

            String[] child = parentDir.list();
            for (int i = 0; i < child.length; i++) {
                File file = new File(parentDir.getAbsolutePath() + "/" + child[i]);
                channel.put(new FileInputStream(file), file.getName());
            }
            val = Dissertation_parent.getAbsolutePath() + "/destinationPDF/" + code + ".pdf";
            File file = new File(val);
            URL url = file.toURL();
            String fileurl = url.toExternalForm();


            mime = fileNameMap.getContentTypeFor(fileurl);
            size = file.length();
            channel.put(new FileInputStream(file), file.getName());
        } else if (Dissertation_folder == null) {
            if (embargo == true) {
                System.out.println(code + " is embargoed");
                String dir = "etd_" + code;
                SftpATTRS attrs = null;
                try {
                    attrs = channel.stat(dir);
                } catch (SftpException ex) {
                    //Exception if does not exist
                }
                if (attrs != null && attrs.isDir()) {
                    System.out.println(dir + " already exists");
                } else {
                    channel.mkdir(dir);
                }
                channel.cd("/var/web/etd_" + code);
                System.out.println("Create Html pwd : " + channel.pwd());
                val = Dissertation_parent.getAbsolutePath() + "/destinationPDF/" + code + ".pdf";

                File file = new File(val);
                File temp = new File(Dissertation_parent.getAbsolutePath() + "/destinationPDF/embargo_" + code + ".pdf");
                File fake = new File(cWorkDir.getAbsolutePath() + "/fake.pdf");
                file.renameTo(temp);
                URL url = fake.toURL();
                String fileurl = url.toExternalForm();
                System.out.println(file.getAbsolutePath());
                mime = fileNameMap.getContentTypeFor(fileurl);
                size = fake.length();
                channel.put(new FileInputStream(fake), code + ".pdf");
                channel.put(new FileInputStream(temp), temp.getName());
            } else {
                System.out.println(code + " is not embargoed");
                String dir = "etd_" + code;
                SftpATTRS attrs = null;
                try {
                    attrs = channel.stat(dir);
                } catch (SftpException ex) {
                    //Exception if does not exist
                }
                if (attrs != null && attrs.isDir()) {
                    System.out.println(dir + " already exists");
                } else {
                    channel.mkdir(dir);
                }
                channel.cd("/var/web/etd_" + code);
                System.out.println("Create Html pwd : " + channel.pwd());
                val = Dissertation_parent.getAbsolutePath() + "/destinationPDF/" + code + ".pdf";
                File file = new File(val);
                URL url = file.toURL();
                String fileurl = url.toExternalForm();

                mime = fileNameMap.getContentTypeFor(fileurl);
                size = file.length();
                channel.put(new FileInputStream(file), file.getName());
            }

        }


        if (Dissertation_folder != null && embargo == false) {
            getTitle(code);
            PrintWriter out = new PrintWriter(new FileWriter(html_f));
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
            out.println("<tr width=85%><td><h2>" + title + "</h2></td></tr>");
            out.println("<tr width=85%><td><h2>Author: " + author + "<h2></td></tr>");
            out.println("<tr width=85%><td><table>");

            File parentDir = new File(Dissertation_parent.getAbsolutePath() + "/" + Dissertation_folder);
            String[] child = parentDir.list();
            out.println("<tr VALIGN=BOTTOM>");
            out.println("<td>");
            out.println("<h2><a href=http://etd.gelman.gwu.edu/etd_" + code + "/" + code + ".pdf>");
            out.println("Main Document");
            out.println("</a></h2>");
            out.println("</td>");
            out.println("<td >");
            out.println("<h2>( pdf " + " <img src=pdf_file.gif > , " + size / 1024 + " KB )</h2>");
            out.println("</td>");
            out.println("</tr>");
            for (int i = 0; i < child.length; i++) {
                if (child[i].equals(code + ".pdf") == false) {
                    System.out.println(child[i]);
                    out.println("<tr VALIGN=BOTTOM>");
                    out.println("<td>");
                    out.println("<h2><a href=http://etd.gelman.gwu.edu/etd_" + code + "/" + child[i] + ">");
                    out.println("Supporting Document");
                    out.println("</a></h2>");
                    out.println("</td>");
                    File f = new File(parentDir.getAbsolutePath() + "/" + child[i]);
                    URL url = f.toURL();
                    String fileUrl = url.toExternalForm();

                    out.println("<td>");
                    out.println("<h2>( " + insertImage(child[i]) + " , " + f.length() / 1024 + " KB )</h2>");
                    out.println("</td>");
                    out.println("</tr>");

                }
            }

            out.println("</table></td></tr></table></tr></tr></table></body></html>");
            out.close();
            channel.cd("/var/web");
            channel.put(new FileInputStream(html_f), html_f.getName());
            disconnect(channel);
            html_f.delete();
        } else if (Dissertation_folder != null && embargo == true)

        {
            getTitle(code);
            PrintWriter out = new PrintWriter(new FileWriter(html_f));
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
            out.println("<tr width=85%><td><h2>" + title + "</h2></td></tr>");
            out.println("<tr width=85%><td><h2>Author: " + author + "<h2></td></tr>");
            out.println("<tr width=85%><td><table>");

            File parentDir = new File(Dissertation_parent.getAbsolutePath() + "/" + Dissertation_folder);
            String[] child = parentDir.list();
            out.println("<tr VALIGN=BOTTOM>");
            out.println("<td>");
            out.println("<h2><a href=http://etd.gelman.gwu.edu/etd_" + code + "/" + code + ".pdf>");
            out.println("Main Document");
            out.println("</a></h2>");
            out.println("</td>");
            out.println("<td >");
            out.println("<h2>( pdf " + " <img src=pdf_file.gif > , " + size / 1024 + " KB )</h2>");
            out.println("</td>");
            out.println("</tr>");

            out.println("</table></td></tr></table></tr></tr></table></body></html>");
            out.close();
            channel.cd("/var/web");
            channel.put(new FileInputStream(html_f), html_f.getName());
            disconnect(channel);
            html_f.delete();
        } else {
            getTitle(code);
            PrintWriter out = new PrintWriter(new FileWriter(html_f));
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
            out.println("<tr width=85%><td><h2>" + title + "</h2></td></tr>");
            out.println("<tr width=85%><td><h2>Author: " + author + "<h2></td></tr>");
            out.println("<tr width=85%><td><table>");

            out.println("<tr VALIGN=BOTTOM>");
            out.println("<td>");
            out.println("<h2><a href=http://etd.gelman.gwu.edu/etd_" + code + "/" + code + ".pdf>");
            out.println("Main Document");
            out.println("</a></h2>");
            out.println("</td>");
            out.println("<td >");
            out.println("<h2>( pdf " + " <img src=pdf_file.gif > , " + size / 1024 + " KB )</h2>");
            out.println("</td>");
            out.println("</tr>");
            out.println("</table></td></tr></table></tr></tr></table></body></html>");
            out.close();
            channel.cd("/var/web");
            channel.put(new FileInputStream(html_f), html_f.getName());
            disconnect(channel);
            html_f.delete();


        }


    }

    static ChannelSftp getChannel(String username, String password, String ip, int port) throws Exception {
        ChannelSftp sftpChannel = null;
        jsch = new JSch();
        session = null;
        System.out.println(ip);
        session = jsch.getSession(username, ip, port);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "password");

        session.setPassword(password);
        session.connect();

        Channel channel = session.openChannel("sftp");
        channel.connect();
        sftpChannel = (ChannelSftp) channel;

        return sftpChannel;
    }

    static String executeCommand(String username, String password, String ip, int port, String command) throws Exception {
        ChannelSftp sftpChannel = null;

        String result = "";
        jsch = new JSch();
        com.jcraft.jsch.Session ssn = null;
        System.out.println(command);
        System.out.println(ip);
        ssn = jsch.getSession(username, ip, port);
        ssn.setConfig("StrictHostKeyChecking", "no");
        ssn.setConfig("PreferredAuthentications", "password");

        ssn.setPassword(password);
        ssn.connect();

        Channel channel = ssn.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);

        InputStream in = channel.getInputStream();
        channel.connect();

        byte[] tmp = new byte[1024];
        while (true) {

            while (in.available() > 0) {

                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                result = result + new String(tmp, 0, i);

            }
            if (channel.isClosed()) {
                System.out.println("exit-status: " + channel.getExitStatus());
                break;
            }
            Thread.sleep(1000);
        }

        channel.disconnect();
        ssn.disconnect();

        return result;
    }

    static void disconnect(ChannelSftp channel) {
        channel.exit();
        session.disconnect();
    }

    static String changeCase(String title) {
        System.out.println("TITLE: " + title);
        title = title.replaceAll(" {2,}", " ");
        final StringBuilder result = new StringBuilder(title.length());
        String[] words = title.split(" ");
        for (int i = 0, l = words.length; i < l; ++i) {
            if (i > 0)
                result.append(" ");
            result.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));

        }
        title = result.toString();
        return title;
    }

    static String insertImage(String name) {
        String ext = "";
        int ind = name.lastIndexOf(".");
        String val = "";
        ext = name.substring(ind + 1);
        if (ext.equals("xls"))
            val = ext + " <img src=excel_file.gif />";
        else if (ext.equals("pdf"))
            val = ext + " <img src=pdf_file.gif />";
        else if (ext.equals("doc"))
            val = ext + " <img src=word_file.gif />";
        else if (ext.equals("ppt"))
            val = ext + " <img src=powerpoint_file.gif />";
        else if(!ext.equals("avi") && !ext.equals("mov") && !ext.equals("moov") && !ext.equals("mp21") && !ext.equals("movie") && !ext.equals("mpeg") && !ext.equals("mpeg4") && !ext.equals("swf") && !ext.equals("asf") && !ext.equals("flv") && !ext.equals("rm") && !ext.equals("wmv")) {
            if(!ext.equals("wav") && !ext.equals("mp3") && !ext.equals("au") && !ext.equals("aiff") && !ext.equals("raw")) {
                val = ext + " <img src= unknown.jpg />";
            } else {
                val = ext + " <img src=Sound_Icon.jpg />";
            }
        } else {
            val = ext + " <img src=movie.gif />";
        }
        return val;
    }

    static void getFiles(String monthyear) throws Exception {

        System.out.println("about to connect to get files for " + monthyear);
        ChannelSftp channel = getChannel(
                props.getProperty("src.sftp.username"),
                props.getProperty("src.sftp.password"),
                props.getProperty("src.sftp.host"),
                Integer.parseInt(props.getProperty("src.sftp.port")));

        System.out.println("connected");
        channel.cd("/media/storage/etd_uploads");
        System.out.println("connected to the server");
        Vector v = channel.ls(".");
        Iterator i = v.iterator();
        String filename = "";
        Vector store = new Vector();

        while (i.hasNext()) {

            String line = i.next().toString();
            int ind = line.lastIndexOf(" ");
            filename = line.substring(ind + 1);
            SftpATTRS attr = channel.lstat(filename);
            String mtime = attr.getMtimeString();
            int index = mtime.lastIndexOf(" ");
            String year = mtime.substring(index + 1);
            index = mtime.indexOf(" ");
            mtime = mtime.substring(index);
            mtime.indexOf(" ");
            String month = mtime.substring(1, index + 1);
            String m = changeMonth(month);
            String date = m + "-" + year;
            if (monthyear.equals(date) && !filename.equals(".") && !filename.equals("..") && filename.endsWith(".zip")) {
                System.out.println("Fetching " + filename);
                channel.get(filename, cWorkDir.getAbsolutePath() + "/" + filename);
                execute("chmod 777 " + cWorkDir.getAbsolutePath() + "/" + filename);
                store.add(filename);
            }
        }
        if (store.isEmpty()) {
            throw new Exception("No etd zips found for " + monthyear);
        }
        System.out.println("Starting to run runGW.pl");
        execute("perl " + GW_ETD.getAbsolutePath() + "/runGW.pl " + cWorkDir.getAbsolutePath());
        System.out.println("runGW.pl finished execution");
        i = store.iterator();
        File archive = new File(GW_ETD, year + month);
        System.out.println("GetFiles: Archive Path: " + archive);
        boolean success = archive.mkdir();
        if (success) {
            //backup = new File(archive);
            while (i.hasNext()) {
                System.out.println("GetGiles backup loop " + filename);
                filename = i.next().toString();
                copyfile(cWorkDir.getAbsolutePath() + "/" + filename, archive.getAbsolutePath() + "/" + filename);
            }
        } else {
            System.out.println(archive + " folder could not be created");
        }
        i = store.iterator();
        while (i.hasNext()) {
            filename = i.next().toString();
            if (filename.equals("log.txt"))
                continue;
            if (filename.equals("fake.pdf"))
                continue;
            System.out.println(filename);
            File file = new File(cWorkDir.getAbsolutePath() + "/" + filename);
            System.out.println(file.getAbsolutePath() + " is deleted");

            file.delete();
        }

    }

    private static void copyfile(String srFile, String dtFile) throws Exception {
        File f1 = new File(srFile);
        File f2 = new File(dtFile);
        InputStream in = new FileInputStream(f1);

        //For Overwrite the file.
        OutputStream out = new FileOutputStream(f2);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
        System.out.println("File copied.");
    }

    static String changeMonth(String month) {
        if (month.equals("Jan"))
            return "01";
        else if (month.equals("Feb"))
            return "02";
        else if (month.equals("Mar"))
            return "03";
        else if (month.equals("Apr"))
            return "04";
        else if (month.equals("May"))
            return "05";
        else if (month.equals("Jun"))
            return "06";
        else if (month.equals("Jul"))
            return "07";
        else if (month.equals("Aug"))
            return "08";
        else if (month.equals("Sep"))
            return "09";
        else if (month.equals("Oct"))
            return "10";
        else if (month.equals("Nov"))
            return "11";
        return "12";

    }

    static Vector getCodes() throws Exception {
        Vector v = new Vector();
        File f = new File(m_file);
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();
        while (line != null) {
            if (line.startsWith("=001")) {
                int ind = line.indexOf("_");
                String code = line.substring(ind + 1);
                v.add(code);
            }
            line = br.readLine();
        }

        return v;

    }

    static void getTitle(String target) throws Exception {

        File f = new File(m_file);
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();
        boolean found = false;
        while (line != null) {

            if (line.startsWith("=001")) {
                int ind = line.indexOf("_");
                String code = line.substring(ind + 1);
                if (code.equals(target)) {

                    while (!found) {
                        if (line.startsWith("=100")) {

                            int ind1 = line.indexOf("a");
                            int ind2 = line.indexOf(",");
                            int ind3 = line.lastIndexOf(".");
                            String name = line.substring(ind1 + 1, ind2);
                            author = line.substring(ind1 + 1, ind3);
                            line = br.readLine();
                            if (line.startsWith("=245")) {
                                int index = line.indexOf("a");
                                int index2 = line.indexOf("$h");
                                if (index2 == -1) {
                                    index2 = line.length();
                                }
                                title = line.substring(index + 1, index2);
                                title = title.toLowerCase();
                                title = changeCase(title);
                                found = true;

                            }
                        }
                        line = br.readLine();
                    }
                    break;
                }

            }
            line = br.readLine();
        }

    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    public static void checkTags() throws Exception {
        File f = new File(m_file);
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();
        String code = "", author = "", title = "";
        while (line != null) {
            if (line.startsWith("=001")) {
                int ind = line.indexOf("_");
                code = line.substring(ind + 1);
                line = br.readLine();
                while (!line.startsWith("=520")) {
                    if (line.startsWith("=100")) {
                        int index = line.indexOf("a");
                        author = line.substring(index + 1);
                    }
                    if (line.startsWith("=245")) {
                        int index = line.indexOf("a");
                        int index2 = line.indexOf("$h");
                        if (index2 == -1) {
                            index2 = line.length();
                        }
                        title = line.substring(index + 1, index2);
                    }
                    line = br.readLine();

                }
                if (line.contains("<")) {
                    Code c = new Code(code, true, title, author);
                    tags.add(c);
                } else {
                    Code c = new Code(code, false, title, author);
                    tags.add(c);
                }
            }
            line = br.readLine();
        }
    }

    public static void writeWebLog() throws Exception {
        System.out.println("Writing Web Log");
        PrintWriter pr = new PrintWriter(new FileWriter(web_log));
        Iterator i = tags.iterator();
        count = 0;
        while (i.hasNext()) {
            count++;
            Code c = (Code) i.next();
            if (c.getTag()) {
                pr.println("Record Number:" + count + "# Author: " + c.getAuthor() + "# Title: " + c.getTitle() + "# This record <b>has HTML tags</b>");
                System.out.println("Record Number:" + count + "# Author: " + c.getAuthor() + "# Title: " + c.getTitle() + "# This record <b>has HTML tags</b>");
            } else {
                pr.println("Record Number:" + count + "# Author: " + c.getAuthor() + "# Title: " + c.getTitle() + "# This record has <b>No HTML tags</b>");
                System.out.println("Record Number:" + count + "# Author: " + c.getAuthor() + "# Title: " + c.getTitle() + "# This record has <b>No HTML tags</b>");
            }
        }
        pr.close();
    }

}

class Code {
    private String code;
    private boolean tag;
    private String title;
    private String author;

    public Code(String code, boolean tag, String Title, String author) {
        this.code = code;
        this.tag = tag;
        this.title = Title;
        this.author = author;
    }

    public String getCode() {
        return code;
    }

    public boolean getTag() {
        return tag;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}