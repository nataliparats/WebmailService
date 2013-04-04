========================= Run the Server With Netbeans =========================

1- Start Netbeans 
2- Open the src folder in Netbeans : "File -> Open Project ..."
3- Run the webmail project : "Run -> Run Project (webmail)"

======================== Run the Server Without Netbeans =======================

1- Compile all the file in the src/scr folder: "javac *.java"
2- Run the project: "java ServerHttp"

============================== Client (browser) : ==============================

The server listen on the port 8081, so to see the webpage we shoud enter in the browser: server_address:8081
There are 3 pages in the server:
    * server-address:8081/form.html    : see the form page to the browser to construct the email
    * server-address:8081/status.html  : see the status page in the browser
    * server-address:8081/send?from=&to=&subject=&smtp=&timer=&message=   : 
          the action page of form.html, use to send an email
          


