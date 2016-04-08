package main;

import java.util.ArrayList;
import java.util.List;

import org.jibble.jmegahal.JMegaHal;

import twitter4j.DirectMessage;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserMentionEntity;
import twitter4j.UserStreamListener;

public class StreamHandler extends Thread {

public void run() {
		
		UserStreamListener listener = new UserStreamListener(){

			@Override
			public void onDeletionNotice(StatusDeletionNotice arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onScrubGeo(long arg0, long arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStallWarning(StallWarning arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStatus(Status status) {
				
				try {
					
					// Don't tweet in response to my own tweets.
					if (status.getUser().getScreenName().equals(Main.twitter.getScreenName())) {
						return;
					}
					
					// Am I mentioned in this tweet?
					boolean mentionedInTweet = false;
					for (UserMentionEntity userMentionEntity : status.getUserMentionEntities()) {
				    	if (userMentionEntity.getScreenName().equals(Main.twitter.getScreenName())) {
				    		mentionedInTweet = true;
				    		break;
				    	}
					}
					
					if (!mentionedInTweet) {
						return;
					}
					
					String words[] = status.getText().split(" ");
					
					String searchQuery = "";
					
					int i = 6;
					
					while (searchQuery.isEmpty()) {
					    for (String word : words) {
					    	
					    	if (word.contains("@")) {
					    		continue;
					    	}
					    	
					        if (word.length() >= i) {
					            searchQuery += word;
					            searchQuery += " ";
					        }
					    }
					    i--;
					}
					
					SearchRateLimitUtils.waitIfNeeded();
					    
				    QueryResult queryResult = Main.twitter.search(new Query(searchQuery).lang("en"));
				    
					SearchRateLimitUtils.setRateLimit(queryResult.getRateLimitStatus());
				    
				    List<Status> tweets = queryResult.getTweets();
				    
				    JMegaHal megahal = new JMegaHal();
				    
				    for (Status tweet : tweets) {
				    	megahal.add(tweet.getText());
					}
				    				    
				    String userMentionString = "@" + status.getUser().getScreenName() + " ";
				    
				    for (UserMentionEntity userMentionEntity : status.getUserMentionEntities()) {
				    	if (userMentionEntity.getScreenName().equals(Main.twitter.getScreenName())) {
				    		continue;
				    	}
						userMentionString += "@" + userMentionEntity.getScreenName() + " ";
					}
				    
				    int maxLength = 140 - userMentionString.length();
				    				    
				    String toSend = "";
				    
				    ArrayList<String> exclusions = Main.loadExclusions();
				    
				    int x = 0;
				    
				    boolean sentenceContainsExclusion = false;
				    
					while (toSend.isEmpty() || toSend.length() > maxLength || toSend.startsWith("RT ") 
				    		|| toSend.contains("@") || sentenceContainsExclusion) {
				    	
				    	toSend = megahal.getSentence();
				    	
				    	for (String exclusion : exclusions) 
						{
							if (toSend.toLowerCase().contains(exclusion.toLowerCase()))
							{
								sentenceContainsExclusion = true;
								break;
							}
						}
				    	
				    	x++;
				    	
				    	if (x>1000) {
				    		return;
				    	}
				    }
				    
				    toSend = userMentionString + toSend;
				    
				    StatusUpdate statusUpdate = new StatusUpdate(toSend);
				    statusUpdate.setInReplyToStatusId(status.getId());
				    
				    Main.twitter.updateStatus(statusUpdate);
					
				} catch (TwitterException e) {
					e.printStackTrace();
				}
				
			}

			@Override
			public void onTrackLimitationNotice(int arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onException(Exception arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onBlock(User arg0, User arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onDeletionNotice(long arg0, long arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onDirectMessage(DirectMessage dm) {
				
				try {
					
					String words[] = dm.getText().split(" ");
					
					String searchQuery = "";
					
					int i = 6;
					
					while (searchQuery.isEmpty()) {
					    for (String word : words) {
					        if (word.length() >= i) {
					            searchQuery += word;
					            searchQuery += " ";
					        }
					    }
					    i--;
					}
					
					SearchRateLimitUtils.waitIfNeeded();
					    
				    QueryResult queryResult = Main.twitter.search(new Query(searchQuery).lang("en"));
				    
				    SearchRateLimitUtils.setRateLimit(queryResult.getRateLimitStatus());
				    
				    List<Status> tweets = queryResult.getTweets();
				    
				    JMegaHal megahal = new JMegaHal();
				    
				    for (Status tweet : tweets) {
				    	megahal.add(tweet.getText());
					}
				    
				    int maxLength = 1000;
				    
				    String toSend = "";
				    
				    ArrayList<String> exclusions = Main.loadExclusions();
				    
				    int x = 0;
				    
				    boolean sentenceContainsExclusion = false;
				    
					while (toSend.isEmpty() || toSend.length() > maxLength || toSend.startsWith("RT ") 
				    		|| toSend.contains("@") || sentenceContainsExclusion) {
				    	
						toSend = megahal.getSentence();
						
						x++;
				    	
						for (String exclusion : exclusions) 
						{
							if (toSend.toLowerCase().contains(exclusion.toLowerCase()))
							{
								sentenceContainsExclusion = true;
								break;
							}
						}
				    	
				    	if (x>1000) {
				    		return;
				    	}
				    }
				    
				    Main.twitter.sendDirectMessage(dm.getSenderScreenName(), toSend);
					
				} catch (TwitterException e) {
					e.printStackTrace();
				}
				
			}

			@Override
			public void onFavorite(User arg0, User arg1, Status arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onFollow(User arg0, User arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onFriendList(long[] arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUnblock(User arg0, User arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUnfavorite(User arg0, User arg1, Status arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUnfollow(User arg0, User arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUserListCreation(User arg0, UserList arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUserListDeletion(User arg0, UserList arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUserListMemberAddition(User arg0, User arg1,
					UserList arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUserListMemberDeletion(User arg0, User arg1,
					UserList arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUserListSubscription(User arg0, User arg1,
					UserList arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUserListUnsubscription(User arg0, User arg1,
					UserList arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUserListUpdate(User arg0, UserList arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUserProfileUpdate(User arg0) {
				// TODO Auto-generated method stub
				
			}
			
	    };
	    
	    TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
	    twitterStream.addListener(listener);
	    twitterStream.user();
		
	}

}
