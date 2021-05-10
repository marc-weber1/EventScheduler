import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Random;
import java.util.Base64;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.security.MessageDigest;
//NEEDS JSONSIMPLE:
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

class Master{
	private final static String BOT_USERNAME = "ab_furmeets_bot";
	private final static int TELEGRAM_REQUEST_DELAY = 2000; //in milliseconds, must be more than 33
	private final static String WEBSITE_URL = "https://perfectfacade.xyz/ABFurmeets/";
	private final static String SQL_URL = "jdbc:mysql://localhost:3306/abfurmeets";

	private static TelegramBot tg;
	private static String sqlUser;
	private static String sqlPass;
	private static BufferedReader consoleIn;
	private static ScheduledExecutorService mainThread;
	
	public static void main(String[] args){
		tg = new TelegramBot(args[0]);
		consoleIn = new BufferedReader(new InputStreamReader(System.in));
		sqlUser = args[1];
		sqlPass = args[2];

		mainThread = Executors.newSingleThreadScheduledExecutor();
		mainThread.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run(){
				mainLoopIteration();
			}
		}, 0, TELEGRAM_REQUEST_DELAY, TimeUnit.MILLISECONDS);

		System.out.println("Server online.");
	}
	
	private static void mainLoopIteration(){
		
		//Check for console commands
		try{
			if(consoleIn.ready()){
				String cmd = consoleIn.readLine();
				if(cmd.equals("quit")){
					System.exit(0);
				}
			}
		}
		catch(IOException e){} //can't read console input
		
		//Check for telegram messages
		JSONArray newMessages = tg.update();
		if(newMessages != null){
			for(int i=0; i<newMessages.size(); i++){
				try{
					JSONObject currentUpdate = (JSONObject) newMessages.get(i);
					
					if(currentUpdate.containsKey("message")){
						JSONObject currentMessage = (JSONObject) currentUpdate.get("message");
						processMessage(currentMessage);
					}
				}
				catch(IndexOutOfBoundsException e){} //message array was modified before it finished processing
				catch(NullPointerException e){} //message property doesn't exist, e.g. they sent a message with only a photo and no text
				catch(ClassCastException e){} //JSON was not in the format expected
			}
		}
	}
	
	private static void processMessage(JSONObject message){
		long chatID = (long) ((JSONObject)message.get("chat")).get("id");
		long userID = (long) ((JSONObject)message.get("from")).get("id");
		long messageID = (long) message.get("message_id");
		String username = (String) ((JSONObject)message.get("from")).get("username");
		String text = (String) message.get("text");
		
		if(text==null){ //they probably sent an image/gif/sticker
			if(chatID==userID) tg.sendMessage(userID,"You didn't say anything.");
		}
		else{
			text = text.replace("@"+BOT_USERNAME,"");
			
			System.out.println("FROM "+userID+": "+text);
			String[] cmds = text.toLowerCase().split("\\s+");
			String args;
			try{
				args = text.toLowerCase().split("\\s+",2)[1];
			}
			catch(IndexOutOfBoundsException e){
				args = "";
			}
			if(cmds.length<1) return;
			
			//INTERPRET COMMANDS HERE; CMDS[0] IS SAFE

			if("/events".equals(cmds[0])){
				try{
					Connection sq = DriverManager.getConnection(SQL_URL,sqlUser,sqlPass);
					Statement stmt = sq.createStatement();
					ResultSet res = stmt.executeQuery("SELECT * FROM event WHERE DATEDIFF( date, CURDATE() ) > -1 AND DATEDIFF( date, CURDATE() ) < 183 ORDER BY date ASC");
					String retStr = "";
					int num = 0;
					while(res.next()){
						num++;
						retStr += "["+num+"] "+res.getString("name")+"      "+res.getString("date")+"    ("+res.getString("city")+")\n";
					}
					sq.close();
					tg.sendReply(chatID,retStr,messageID);
				}
				catch(Exception e){
					tg.sendReply(chatID,"Failed to access SQL: "+e.getMessage(),messageID);
				}
			}
			
			else if("/eventlist".equals(cmds[0])){
				tg.sendReply(chatID,"The event list can be found at:\n"+WEBSITE_URL,messageID);
			}
			
			else if("/newevent".equals(cmds[0])){
				try{
					Random rand = new Random();
					byte[] token_bytes = new byte[6];
					rand.nextBytes(token_bytes);
					String session_token = new String(Base64.getEncoder().encode(token_bytes));
					MessageDigest hash_digest = MessageDigest.getInstance("SHA-256");
					byte[] token_hash = hash_digest.digest(token_bytes);

					Connection sq = DriverManager.getConnection(SQL_URL,sqlUser,sqlPass);
					
					sq.close();
				}
				catch(Exception e){
					
				}
				tg.sendReply(chatID,"Make a new event using the following form:\n"+WEBSITE_URL+"neweventform.html"+
								"\n\nTo edit or delete events, contact an admin",messageID);
			}

			else if("/deleteevent".equals(cmds[0])){
				
			}

			else if("/whereis".equals(cmds[0])){
				if(!"".equals(args)){
				try{
					Connection sq = DriverManager.getConnection(SQL_URL,sqlUser,sqlPass);
					PreparedStatement query = sq.prepareStatement("select latitude, longitude from event where datediff( date, curdate() ) >= 0 and name = ? order by date asc;");
					query.setString(1,args);
					ResultSet res = query.executeQuery();
					if(res.first()){
						float latitude = res.getFloat("latitude");
						float longitude = res.getFloat("longitude");
						if(latitude==0.0f && longitude == 0.0f)
							tg.sendReply(chatID,"No coordinates found for that event.",messageID);
						else
							tg.sendLocationReply(chatID,latitude,longitude,messageID);
					}
					else{
						tg.sendReply(chatID,"Could not find an event with that name.",messageID);
					}
				}
				catch(Exception e){
					tg.sendReply(chatID,"Failed to access SQL: "+e.getMessage(),messageID);
				}
				}
				else
					tg.sendReply(chatID,"Usage: /whereis <event name>",messageID);
			}
			
		}
	}
}
