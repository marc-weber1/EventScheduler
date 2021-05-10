import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.lang.Math;
//NEEDS JSONSIMPLE:
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;


public class TelegramBot{
	private final static int TELEGRAM_MAX_MESSAGE_SIZE = 4096;
	private final static int MAX_MESSAGE_UPDATES = 99; //this is per update() call, must be from 1 to 99
    private final static String TELEGRAM_URL = "https://api.telegram.org/bot";

    private String accessURL;
    private long lastUpdate;

    public TelegramBot(String APIKey){
        this.accessURL = TELEGRAM_URL+APIKey+"/";
        this.lastUpdate=0;
    }

    /**
    * Gets all the messages sent to the bot since last time update() was called
    * @return The JSON data obtained from the site, or null on failure
    */
    public JSONArray update(){
		try{
			JSONObject jsonObj = (JSONObject) ConnectionUtils.getJSONFromURL(accessURL+"getUpdates","offset="+this.lastUpdate+"&limit="+(MAX_MESSAGE_UPDATES+1));
			if(jsonObj == null){ return null; }
			else if((boolean) jsonObj.get("ok") == false){
				System.out.println("[DEBUG] Website returned not ok.");
				return null;
			}
		
			//Clear updates that have been received
			JSONArray objData = (JSONArray) jsonObj.get("result");
			int objDataSize = objData.size();
			if(objDataSize==0){ //if there's no new updates
				return objData;
			}
			else if(objData.size()>MAX_MESSAGE_UPDATES){ //someone's spamming
				this.lastUpdate = (long) ((JSONObject) objData.get(objData.size()-1)).get("update_id") + 1; //The ID of the latest message plus 1
				System.out.println("[DEBUG] Buffer reset. Possible spam.");
				return this.update(); //recurse until buffer is clear
			}
			else{
				this.lastUpdate = (long) ((JSONObject) objData.get(objData.size()-1)).get("update_id") + 1; //The ID of the latest message plus 1
				return objData;
			}
		}
		catch(ClassCastException e){ //URL didn't return an array or objData doesn't contain JSONObjects
			System.out.println("[DEBUG] Site did not return the expected JSON array.");
			return null;
		}
		catch(NullPointerException e){ //no "update_id"
			System.out.println("[DEBUG] Site JSON was not formatted as expected.");
			return null;
		}
    }
	
	private boolean sendMessage(long id, String message, String args){
		try{
			boolean success = true;
			int messageLength = message.length();
			int numMessages = messageLength/TELEGRAM_MAX_MESSAGE_SIZE+1;
			for(int i=0;i<numMessages;i++){
				String messagePiece = message.substring(i*TELEGRAM_MAX_MESSAGE_SIZE,Math.min((i+1)*TELEGRAM_MAX_MESSAGE_SIZE,messageLength));
				JSONObject objData = (JSONObject) ConnectionUtils.getJSONFromURL(accessURL + "sendMessage","chat_id=" + id + "&text=" + URLEncoder.encode(messagePiece,"UTF-8") + args);
				if((boolean) objData.get("ok")){
					System.out.println("TO "+id+": "+message);
				}
				else success = false;
			}
			if(success){
				return true;
			}
			else return false;
		}
		catch(NullPointerException e){ return false; } //no "ok"
		catch(UnsupportedEncodingException e){
			System.out.println("[DEBUG] Unable to URL encode message "+message);
			return false;
		}
	}

	public boolean sendLocationReply(long id, float latitude, float longitude, long messageID){
		String strLat = String.format("%.6f",latitude);
		String strLong = String.format("%.6f",longitude);

		try{
			JSONObject objData = (JSONObject) ConnectionUtils.getJSONFromURL(accessURL+"sendLocation","chat_id="+id+"&latitude="+strLat+"&longitude="+strLong+"&reply_to_message_id="+messageID);
			if((boolean) objData.get("ok")){
				System.out.println("TO "+id+": LOCATION lat="+strLat+" long="+strLong);
				return true;
			}
			return false;
		}
		catch(NullPointerException e){return false;}
	}

    /**
    * Sends a message to a chat.
    * @param id The chat ID
    * @param message The text to send
    * @return True if success, false if failure
    */
    public boolean sendMessage(long id, String message){
		return sendMessage(id,message,"");
    }

    /**
    *  Sends a message to the chat ID with a keyboard. If sent to a group, all members will be given a keyboard.
    * @param id The chat ID
    * @param message The text to send
    * @param keyboard a 2D string array for the keyboard buttons; e.g. {{"topleft","topright"},{"bottomleft","bottomright"}}
	* @param resize true if every button should be the same height, false if the keyboard should be the default height
	* @param oneTime true if the keyboard hides itself after a button press. Use sendMessageRemoveKeyboard to fully remove it.
    * @return True if success, false if failure
    */
    public boolean sendMessage(long id, String message, String[][] keyboard, boolean resize, boolean oneTime){
        int rowNum = keyboard.length;
        String[] rows = new String[rowNum];
        for(int i=0;i<rowNum;i++){
            int buttonNum = keyboard[i].length;
            String[] buttons = new String[buttonNum];
            for(int j=0;j<buttonNum;j++)
                buttons[j] = new String("\""+keyboard[i][j]+"\"");
            rows[i] = new String("["+String.join(",",buttons)+"]");
        }
        String args = "&reply_markup={\"keyboard\":["+String.join(",",rows)+"]";
		if(resize) args += ",\"resize_keyboard\":true";
		if(oneTime) args += ",\"one_time_keyboard\":true";
		args += "}";
        return sendMessage(id,message,args);
    }
	
	/**
    *  Sends a message to the chat ID with a keyboard. If sent to a group, all members will be given a keyboard.
    * @param id The chat ID
    * @param message The text to send
    * @param keyboard a 2D string array for the keyboard buttons; e.g. {{"topleft","topright"},{"bottomleft","bottomright"}}
    * @return True if success, false if failure
    */
	public boolean sendMessage(long id, String message, String[][] keyboard){
		return sendMessage(id,message,keyboard,true,false);
	}
	
	public boolean sendMessageRemoveKeyboard(long id, String message){
		String args = "&reply_markup={\"remove_keyboard\":true}";
		return sendMessage(id,message,args);
	}
	
	public boolean sendReply(long chatID, String message, long messageID){
		String args = "&reply_to_message_id="+messageID;
		return sendMessage(chatID,message,args);
	}
	
	public boolean callbackQueryResponse(String queryID, String parameters){
		JSONObject objData = (JSONObject) ConnectionUtils.getJSONFromURL(accessURL + "answerCallbackQuery", "callback_query_id=" + queryID + "&" + parameters);
		if((boolean) objData.get("ok")) return true;
		else return false;
	}
}
