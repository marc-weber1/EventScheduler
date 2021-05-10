import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.net.SocketTimeoutException;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.DataOutputStream;
import java.io.IOException;
//NEEDS JSONSIMPLE:
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

public class ConnectionUtils{
	
	private final static int CHARBUF_SIZE = 1024;
	
	private static String cleanInput(byte[] charBuf, int bufLength){
		if(bufLength>charBuf.length) return "";
		
		StringBuilder builder = new StringBuilder();
		for(int i=0;i<bufLength;i++){
			if(charBuf[i]>=32 && charBuf[i]<=126){
				builder.append((char) charBuf[i]);
			}
		}
		return builder.toString();
	}
	
	public static String getStringFromPOSTURL(URL siteURL,String args){
		try{
			HttpsURLConnection con = (HttpsURLConnection) siteURL.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent","Test project by check080");
			
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(args);
			wr.flush();
			wr.close();
			
			return getStringFromConnection(con);
		}
		catch(IOException e){
			System.out.println("[DEBUG] Site not found. Is your URL correct?");
			return null;
		}
	}
	
	public static String getStringFromGETURL(URL siteURL){
		try{
			HttpsURLConnection con = (HttpsURLConnection) siteURL.openConnection();
			con.setRequestProperty("User-Agent","Test project by check080");
			
			return getStringFromConnection(con);
		}
		catch(IOException e){
			System.out.println("[DEBUG] Site not found. Is your URL correct?");
			return null;
		}
	}
	
	private static String getStringFromConnection(HttpsURLConnection con){
		try{
			//con.getResponseCode(); to check if it's even open i guess
			
			String contentType = con.getContentType();
			String encoding;
			try{
				encoding = contentType.substring(contentType.indexOf('=')+1);
				encoding = encoding == null ? "UTF-8" : encoding;
			}
			catch(IndexOutOfBoundsException e){
				encoding = "UTF-8";
			}
			
			InputStream in = con.getInputStream();
			byte[] charBuf = new byte[CHARBUF_SIZE];
			StringBuilder builder = new StringBuilder();
			for(int numRead=in.read(charBuf,0,CHARBUF_SIZE); numRead>0 && numRead<=CHARBUF_SIZE; numRead=in.read(charBuf,0,CHARBUF_SIZE)){
				for(int i=0;i<numRead;i++){
					builder.append((char) charBuf[i]);
				}
			}
			String inputText = builder.toString();
			
			if(inputText.equals("")){
				System.out.println("[DEBUG] Array size error or site was empty.");
				return null;
			}
			else return inputText;
		}
		catch(SocketTimeoutException e){ //if the internet is so slow it times out
			System.out.println("[DEBUG] Connection timed out.");
			return null;
		}
		catch(IOException e){
			System.out.println("[DEBUG] Error reading from site. "+e.getMessage());
			return null;
		}
	}

    public static Object getJSONFromURL(String siteURL,String args,boolean isPost){
		String siteString="";
		try{
			if(isPost){
				URL urlObj = new URL(siteURL);
				siteString = getStringFromPOSTURL(urlObj,args);
			}
			else{
				URL urlObj = new URL(siteURL+"?"+args);
				siteString = getStringFromGETURL(urlObj);
			}
			if(siteString==null) return null;
			else if(siteString.equals("")) return new JSONArray();
			
			JSONParser parser = new JSONParser();
			Object JSONData = parser.parse(siteString);
			return JSONData;
		}
		catch(MalformedURLException e){ //if the URL isn't valid
			System.out.println("[DEBUG] Invalid URL: "+siteURL);
			return null;
		}
		catch(ParseException e){ //if the website JSON isn't formatted correctly
			System.out.println("[DEBUG] Failed to parse site JSON. Site contained: "+siteString);
			return null;
		}
    }
	
	public static Object getJSONFromURL(String siteURL,String args){
		return getJSONFromURL(siteURL,args,false); //use get by default
	}
}