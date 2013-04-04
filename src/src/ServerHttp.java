import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class ServerHttp {
	private ArrayList<String> status=new ArrayList<String>();
	final int port = 8081;
	private ServerSocket s;


        
	public void Start_Server(){
		try {
			s =  new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void Close_Server(){
		try {
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		
	}
	public void Listen(){
		try {
			while (true)
			{	
				final Socket conn=s.accept();
				Thread t = new Thread(new HttpListener(conn,status));
				t.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	public static void main(String[] args) {
		ServerHttp servhttp = new ServerHttp();
		servhttp.Start_Server();
		servhttp.Listen();
		servhttp.Close_Server();
	}

}
