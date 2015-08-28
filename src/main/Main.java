package main;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jibble.jmegahal.JMegaHal;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Trend;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class Main 
{
	static JMegaHal megahal;
	static Twitter twitter = TwitterFactory.getSingleton();
	static ArrayList<String> allTweets = new ArrayList<String>();
	
	private static boolean learnFromTrends = true;

	public static void main(String[] args)
	{
		Trends trendsResponses = null;
		int x;
		String sentence, sentenceLower;
		long randomTime;
		Date date = new Date();
		
		while (true)
		{
			System.out.println("Learning from searching Twitter...");
			
			megahal = new JMegaHal();
			
			if (learnFromTrends)
			{
				try 
				{
					trendsResponses = twitter.getPlaceTrends(23424975); // 1 = Worldwide/global, 23424975 = UK, for others, look up 'WOEID'
				} 
				catch (TwitterException e) 
				{
					e.printStackTrace();
					System.out.println("Unable to get current trends.");
					System.out.println("Waiting for 5 minutes before trying again...");
					delay(1000*60*5);
					continue;
				}
				
				Trend[] trends = trendsResponses.getTrends();
				
				for (Trend trend : trends) 
				{
					try 
					{
						learnFromSearchQuery(trend.getQuery());
					} 
					catch (TwitterException e) 
					{
						e.printStackTrace();
						System.out.println("Unable to get search results.");
						System.out.println("Skipping this query...");
						System.out.println("Waiting for 5 minutes before trying again...");
						delay(1000*60*5);
						continue;
					}
				}
			}
			
			ArrayList<String> interests = new ArrayList<String>();
			interests.add("Bitcoin");
			interests.add("#netsec");
			interests.add("@SwiftOnSecurity");
			interests.add("#gamedev");
			interests.add("@DivineOmega");
			interests.add("@ojdon");
			interests.add("@KirstyGasston");
			interests.add("@timlees11");
			interests.add("@peterchiuy");
			interests.add("#webdev");
			interests.add("PHP");
			interests.add("Mario Maker");
			interests.add("Star Trek");
			interests.add("#staffswebmeetup");
			interests.add("#Humans");
			interests.add("#homeautomation");
			
			for (String interest : interests) 
			{
				try 
				{
					learnFromSearchQuery(interest);
				} 
				catch (TwitterException e) 
				{
					e.printStackTrace();
					System.out.println("Unable to get search results.");
					System.out.println("Skipping this query...");
					System.out.println("Waiting for 5 minutes before trying again...");
					delay(1000*60*5);
					continue;
				}
			}
			
			System.out.println();
			
			x = 0;
			sentence = null;
			while (x<1000)
			{
				sentence = megahal.getSentence();
				
				sentenceLower = sentence.toLowerCase();
				
				if (allTweets.contains(sentence) || sentence.length()>140
					|| sentenceLower.contains("rip") || sentenceLower.contains("bomb") || sentenceLower.contains("explo") || sentenceLower.contains("terror")
					|| sentenceLower.contains("die") || sentenceLower.contains("death") || sentenceLower.contains("dead") || sentenceLower.contains("…")
					|| sentenceLower.contains("hostage") || sentenceLower.contains("attack") || sentenceLower.contains("kill") || sentenceLower.contains("innocent")
					|| sentenceLower.contains("\r") || sentenceLower.contains("\n")) continue;
				x++;
				break;
			}
			
			if (sentence==null)
			{
				System.out.println("No tweet could be generated.");
			}
			else
			{
				System.out.println("Generated tweet: "+sentence);
				System.out.println("Waiting for 10 seconds before tweeting...");
				delay(1000*10);
				System.out.println("Tweeting...");
				try 
				{
					twitter.updateStatus(sentence);
				} 
				catch (TwitterException e) 
				{
					e.printStackTrace();
					System.out.println("Unable to tweet.");
					System.out.println("Waiting for 5 minutes before trying again...");
					delay(1000*60*5);
					continue;
				}
				
				date = new Date();
				System.out.println("Tweet complete at "+date.toString());
				System.out.println();
			}
			
			randomTime = (long) (Math.random()*1000*60*120);
			System.out.println("Waiting "+randomTime/1000+" seconds ("+randomTime/1000/60+" minutes) before considering next tweet...");
			delay(randomTime);
		}
	}

	private static void learnFromSearchQuery(String queryString) throws TwitterException 
	{
		System.out.print("Searching for '"+queryString+"'... ");
		QueryResult queryResult = twitter.search(new Query(queryString).lang("en"));
		
		System.out.print("Getting Tweets... ");
		List<Status> tweets = queryResult.getTweets();
		
		System.out.print("("+tweets.size()+") ");
		
		System.out.print("Learning... ");
		for (Status tweet : tweets) 
		{
			System.out.print("+");
			allTweets.add(tweet.getText());
			megahal.add(tweet.getText());
		}
		System.out.println();
	}
	
	private static void delay (long milliseconds)
	{
		try 
		{
			Thread.sleep(milliseconds);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
			System.exit(1);
		}
		
	}

}