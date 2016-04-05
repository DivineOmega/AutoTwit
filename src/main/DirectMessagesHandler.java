package main;

import java.util.Date;

import twitter4j.DirectMessage;
import twitter4j.ResponseList;
import twitter4j.TwitterException;

public class DirectMessagesHandler extends Thread {

public void run() {
		
		Date date = new Date();
	
		while (true)
		{
			try {
				
				Main.delay(5000);
				
				ResponseList<DirectMessage> directMessages = Main.twitter.getDirectMessages();
				
				for (DirectMessage directMessage : directMessages) {
					if (directMessage.getCreatedAt().after(date)) {
						
						Main.twitter.sendDirectMessage(directMessage.getSenderId(), directMessage.getText());
						
					}
				}
				
				date = new Date();
				
			} catch (TwitterException e) {
				e.printStackTrace();
			} 
			
		}
		
	}

}
