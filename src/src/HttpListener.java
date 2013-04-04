
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.regex.Pattern;
import javax.swing.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.directory.*;

public class HttpListener implements Runnable {

    private Socket conn;
    private DataOutputStream output;
    private BufferedReader in;
    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final String URL_SEND_PATTERN = "send\\?from=(.+?)&to=(.+?)&subject=(.+?)&smtp=(.*?)&timer=(.*?)&message=(.*?)";//problem when adding \n in the message
    private Pattern url_send_pattern;
    private Pattern email_pattern;
    private ArrayList<String> status;
    private String st = "Pending";
    private Encoding qp = new Encoding();
    private String smtp_from;
    Socket smtp_socket;

    public String Status(String from, String to, String subject, String smtp, String msg, int timer, String st) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return "<TABLE></TR><TR><TD>From:</TD><TD>" + from + "</TD></TR>"
                + "<TR><TD>To:</TD><TD>" + to + "</TD></TR>"
                + "<TR><TD>Subject:</TD><TD>" + subject + "</TD></TR>"
                + "<TR><TD>SMTP Server:</TD><TD>" + smtp + "</TD></TR>"
                + "<TR><TD>Start:</TD><TD>" + dateFormat.format(date) + "</TD></TR>"
                + "<TR><TD>Timer:</TD><TD>" + timer + " sec</TD></TR>"
                + "<TR><TD>Message:</TD><TD>" + msg + "</TD></TR>"
                + "<TR><TD>Status:</TD><TD>" + st + "</TD></TR></TABLE>";
    }

    //remove mail from the status page
    public void removeStatus(String s, String from, String to, String subject, String smtp, String msg, int timer, String st) {
        int index = status.indexOf(s);
        status.set(index, Status(from, to, subject, smtp, msg, timer, st));
    }

    //add mail to the status page
    public String addstatus(String from, String to, String subject, String smtp, String msg, int timer) {
        String s = Status(from, to, subject, smtp, msg, timer, st);
        status.add(s);
        return s;
    }

    //HttpListener to open the connection between the client-browser and the mailserver and handle the requests - responses
    public HttpListener(Socket conn, ArrayList<String> status) {
        this.conn = conn;
        this.status = status;
        email_pattern = Pattern.compile(EMAIL_PATTERN);
        url_send_pattern = Pattern.compile(URL_SEND_PATTERN, Pattern.DOTALL);

        try {
            output = new DataOutputStream(conn.getOutputStream());
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //DNS lookup
    public String getSMTP(String email) {
        int start = email.indexOf("@") + 1;
        String hostname = null;
        if ((email.substring(start)).startsWith("mail.")) {
            int s = email.indexOf("mail.") + 5;
            hostname = email.substring(s);
        } else {
            hostname = email.substring(start);//extract the hostname from the email
        }
        try {
            Hashtable env = new Hashtable();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(hostname, new String[]{"MX"});
            Attribute attr = attrs.get("MX");
            if (attr.size() > 0) {//if MX record exists
                String result = attr.get().toString();
                start = result.indexOf(" ") + 1;
                result = result.substring(start, result.length() - 1);
                System.out.println(result);
                return result;
            }
            return null;//else return null

        } catch (Exception e) {
            System.out.println(hostname + " : " + e.getMessage());
            return null;
        }

    }

    //send string to the browser
    public void send(String s) {
        try {
            output.writeBytes(s);
            output.close();
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //Form the confirmation page in the browser
    public void SendResult(String from, String to, String subject, String smtp, String msg, int timer) throws UnsupportedEncodingException {
        this.send("HTTP/1.1 200 OK \r\n\r\n"
                + "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta charset=\"iso-8859-15\" />"
                + "<title>send</title>"
                + "</head>"
                + "<body>"
                + "<H1>Your message is successfully sent</H1>"
                + "<TABLE>"
                + "<TR><TD>From:</TD><TD>" + from + "</TD></TR>"
                + "<TR><TD>To:</TD><TD>" + to + "</TD></TR>"
                + "<TR><TD>Subject:</TD><TD>" + subject + "</TD></TR>"
                + "<TR><TD>SMTP Server:</TD><TD>" + smtp + "</TD></TR>"
                + "<TR><TD>Timer:</TD><TD>" + timer + " sec</TD></TR>"
                + "<TR><TD>Message:</TD><TD>" + msg + "</TD></TR>"
                + "</TABLE> "
                + "<A HREF=\"index.html\">Home page</A>"
                + "</body></html>\r\n");
    }

    //form the status page in the browser
    public void SendStatus() {
        String s = "HTTP/1.1 200 OK \r\n\r\n"
                + "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta charset=\"iso-8859-15\" />"
                + "<title>Status</title>"
                + "</head>"
                + "<h1>Status</h1><body>";
        for (int i = 0; i < status.size(); i++) {
            s = s + status.get(i) + "<BR/><HR/>";
        }
        s = s + "<A HREF=\"index.html\">Home page</A></body></html>\r\n";
        this.send(s);
    }
    //send the form page to the browser to construct the email

    public void Homepage() {
        this.send("HTTP/1.1 200 OK \r\n\r\n"
                + "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta charset=\"iso-8859-15\" />"
                + "<title>Home Page : Webmail</title>"
                + "</head>"
                + "<body>"
                + "<h1>Home Page : Webmail</h1>"
                + "<A HREF=\"form.html\">Send Email</A><BR/>"
                + "<A HREF=\"status.html\">Status</A>"
                + "</body></html>\r\n");
    }

    //send the form page to the browser to construct the email
    public void SendForm() {
        this.send("HTTP/1.1 200 OK \r\n\r\n"
                + "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta charset=\"iso-8859-15\" />"
                + "<title>Webmail</title>"
                + "</head>"
                + "<body>"
                + "<form method=\"GET\" action=\"send\">"
                + "<TABLE>"
                + "<TR><TD>From:</TD><TD><input type=\"text\" name=\"from\" id=\"from\" /></TD></TR>"
                + "<TR><TD>To:</TD><TD><input type=\"text\" name=\"to\" id=\"to\" /></TD></TR>"
                + "<TR><TD>Subject:</TD><TD><input type=\"text\" name=\"subject\" id=\"subject\" /></TD></TR>"
                + "<TR><TD>SMTP Server:</TD><TD><input type=\"text\" name=\"smtp\" id=\"smtp\" /></TD></TR>"
                + "<TR><TD>Timer</TD><TD><input type=\"text\" name=\"timer\" id=\"timer\" />sec</TD></TR>"
                + "<TR><TD>Message:</TD><TD><textarea name=\"message\" id=\"message\" rows=\"10\" cols=\"70\"></textarea></TD></TR>"
                + "</TABLE> "
                + "<input type=\"submit\" value=\"Send\" />"
                + "<input type=\"reset\" value=\"Reset\" /></form>"
                + "<A HREF=\"index.html\">Home page</A></body></html>\r\n");
    }

    //send email method
    public Boolean SendEmail(String from, String to, String subject, String smtp, String msg) throws Exception {
        Boolean ok = true;
        String result = null;
        //catch exception if an error occurs while sending the message
        try {
            smtp_socket = new Socket(smtp, 25);
            BufferedReader insmtp = new BufferedReader(new InputStreamReader(smtp_socket.getInputStream()));
            DataOutputStream outputsmtp = new DataOutputStream(smtp_socket.getOutputStream());
            System.out.println(insmtp.readLine());
            smtp_from = getSMTP(from);
            outputsmtp.writeBytes("HELO " + smtp_from + "\r\n");
            System.out.println(insmtp.readLine());
            outputsmtp.writeBytes("MAIL FROM: <" + from + ">\r\n");
            System.out.println(insmtp.readLine());
            outputsmtp.writeBytes("RCPT TO: <" + to + ">\r\n");
            System.out.println(insmtp.readLine());
            outputsmtp.writeBytes("DATA\r\n");
            System.out.println(insmtp.readLine());
            outputsmtp.writeBytes("To: <" + to + ">\r\n");
            outputsmtp.writeBytes("MIME-Version: 1.1\r\n");
            outputsmtp.writeBytes("Content-type: text/plain; charset=ISO-8859-15\r\n");
            outputsmtp.writeBytes("Content-transfer-encoding:  quoted-printable\r\n");
            outputsmtp.writeBytes("Subject: =?iso-8859-1?Q?" + subject + "?=\r\n");
            outputsmtp.writeBytes(msg + "\r\n");
            outputsmtp.writeBytes("\r\n.\r\n");
            result = insmtp.readLine();
            System.out.println("result" + result);
            outputsmtp.writeBytes("QUIT\r\n");
            System.out.println(insmtp.readLine());
            outputsmtp.close();
            smtp_socket.close();
        } catch (Exception e) {
            ErrorMessage("Wrong SMTP server. UnknownHostException! <h4><A HREF=\"index.html\">Home page</A></h4>");
        }
        if (!result.toUpperCase().contains("OK")) {
            ok = false;
        }
        return ok;
    }

    //error page
    public void ErrorMessage(String msg) {
        this.send("HTTP/1.1 400 Bad Request \r\n\r\n"
                + "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta charset=\"iso-8859-15\" />"
                + "<title>Error</title>"
                + "</head>"
                + "<body>"
                + "<H1>Error: " + msg + "</H1>"
                + "</body></html>\r\n");
    }

    @Override
    public void run() {//thread deals with  the Http requests
        try {
            String request = in.readLine();
            //check the HTTP methods that is used
            if ((request.toUpperCase()).startsWith("GET")) {
                System.out.println(request);
                int start = 0;
                int end = 0;
                start = (request.toUpperCase()).indexOf("/") + 1;
                end = (request.toUpperCase()).indexOf(" HTTP", start);

                String url = request.substring(start, end);
                url = URLDecoder.decode(url, "iso-8859-15");
                System.out.println(url);

                //check if the url is correct and send the appropriate form
                if (url.equalsIgnoreCase("") || url.equalsIgnoreCase("index.html")) {
                    this.Homepage();
                } else if (url.equalsIgnoreCase("form.html")) {
                    this.SendForm();
                } else if (url.equalsIgnoreCase("status.html")) {
                    this.SendStatus();
                } else if (url.startsWith("send?")) {

                    if (!url_send_pattern.matcher(url).matches()) {
                        ErrorMessage("Paremeters missing or empty <h4><A HREF=\"index.html\">Home page</A></h4>");
                    } else {
                        //extract the sender, recipient, subject, smtp, timer and msg info from the url
                        final String from = url.substring(url.indexOf("from=") + 5, url.indexOf("&to="));
                        final String to = url.substring(url.indexOf("&to=") + 4, url.indexOf("&subject="));
                        String tempsmtp = url.substring(url.indexOf("&smtp=") + 6, url.indexOf("&timer="));
                        String timerstring = url.substring(url.indexOf("&timer=") + 7, url.indexOf("&message="));
                        //create a timer to specify in how many seconds the msg will be sent
                        int time = 0;
                        //check if the timer is more than 0 
                        if (timerstring.length() > 0) {
                            //check if the timer is an integer
                            try {
                                time = Integer.parseInt(timerstring);
                            } catch (NumberFormatException e) {
                                ErrorMessage("The timer field should be an integer <h4><A HREF=\"index.html\">Home page</A></h4>");
                            }
                        }
                        final int timer = time;
                        //if the smtp is empty look at the dns
                        if (tempsmtp.length() == 0) {
                            tempsmtp = getSMTP(to);
                            System.out.println("To:" + to + "SMTP: " + tempsmtp);
                        }
                        final String smtp = tempsmtp;
                        //if timer is less than 0 then display an error msg
                        if (timer < 0) {
                            ErrorMessage("The timer field should be >0 <h4><A HREF=\"index.html\">Home page</A></h4>");
                        } else if (smtp == null) {//if smtp is null then display an error msg
                            ErrorMessage("DNS name not found <h4><A HREF=\"index.html\">Home page</A></h4>");
                        } else if (!email_pattern.matcher(to).matches() || !email_pattern.matcher(from).matches()) { //if email format doesn't comply with email pattern then display an error msg
                            ErrorMessage("Wrong email format <h4><A HREF=\"index.html\">Home page</A></h4>");
                        } else {//send email
                            final String sbj = url.substring(url.indexOf("&subject=") + 9, url.indexOf("&smtp="));
                            final String subject = qp.encode(sbj, false);
                            String message = url.substring(url.indexOf("&message=") + 9, url.length());
                            final String msg = qp.encode(message, true);
                            final String msg_clear = message;

                            Boolean ok = true;
                            //timer not specified
                            if (timer == 0) {
                                ok = this.SendEmail(from, to, subject, smtp, msg);
                                if (ok) {
                                    this.SendResult(from, to, sbj, smtp, msg_clear, timer);
                                } else {
                                    final String s = this.addstatus(from, to, sbj, smtp, msg_clear, timer);
                                    removeStatus(s, from, to, sbj, smtp, msg_clear, timer, "Unsuccessful");
                                    ErrorMessage("An error occurred while sending email <h4><A HREF=\"index.html\">Home page</A></h4>");
                                }
                            } else {//delay the delivery of the msg based on the timer value
                                int delay = timer * 1000; //timer in milliseconds
                                final String s = this.addstatus(from, to, sbj, smtp, msg_clear, timer);
                                ActionListener taskPerformer = new ActionListener() {//actionListener to delay the delivery and form the status page

                                    public void actionPerformed(ActionEvent evt) {
                                        try {
                                            SendEmail(from, to, subject, smtp, msg);
                                            removeStatus(s, from, to, sbj, smtp, msg_clear, timer, "Successful");
                                            String smtpsender = getSMTP(from);
                                            if (smtpsender != null) {
                                                SendEmail(from, from, "Confirmation sending: " + subject, smtpsender, msg);
                                            }
                                        } catch (Exception ex) {
                                            Logger.getLogger(HttpListener.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                };
                                Timer t = new Timer(delay, taskPerformer);
                                t.setRepeats(false);//send the message only once
                                t.start();
                                this.SendStatus();
                            }


                        }
                    }
                } else {//display error if the url is not the appropriate one
                    ErrorMessage("This page doesn't exist <h4><A HREF=\"index.html\">Home page</A></h4>");
                }
            } else {//display an error if method is not the correct one 
                ErrorMessage("This method is not implemented <h4><A HREF=\"index.html\">Home page</A></h4>");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}