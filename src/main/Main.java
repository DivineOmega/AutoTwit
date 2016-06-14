package main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.jibble.jmegahal.JMegaHal;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.DirectMessage;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Trend;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterListener;

public class Main 
{
	static JMegaHal megahal;
	static Twitter twitter = TwitterFactory.getSingleton();
	static ArrayList<String> allTweets = new ArrayList<String>();
	
	private static boolean learnFromTrends = true;
	private static int trendsWOEID = 1;
	private static int maxDelayBetweenTweetingMinutes = 120;

	public static void main(String[] args)
	{
		Trends trendsResponses = null;
		int x;
		int maxTweetGenerationAttempts = 10000;
		String sentence, sentenceLower;
		long randomTime;
		Date date = new Date();
		BufferedReader br = null;
		Properties prop = new Properties();
		InputStream input = null;
		
		StreamHandler streamHandler = new StreamHandler();
		streamHandler.start();
		
		
		while (true)
		{
			System.out.println("Loading configuration from autotwit.properties...");
			
			try {

				input = new FileInputStream("autotwit.properties");

				prop.load(input);

				learnFromTrends = (prop.getProperty("learnFromTrends").equalsIgnoreCase("true"));
				trendsWOEID = Integer.parseInt(prop.getProperty("trendsWOEID"));
				maxDelayBetweenTweetingMinutes = Integer.parseInt(prop.getProperty("maxDelayBetweenTweetingMinutes"));

			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			System.out.println("Clearing list of all previous tweets...");
			
			allTweets.clear();
			
			System.out.println("Creating new Markov change engine...");
			
			megahal = new JMegaHal();
				
			if (learnFromTrends)
			{
				System.out.println("Retrieving Twitter trends from WOEID: "+trendsWOEID+"...");
				
				TrendsRateLimitUtils.waitIfNeeded();
				
				try 
				{
					trendsResponses = twitter.getPlaceTrends(trendsWOEID); // 1 = Worldwide/global, 23424975 = UK, for others, look up 'WOEID'
				} 
				catch (TwitterException e) 
				{
					e.printStackTrace();
					System.out.println("Unable to get current trends.");
					System.out.println("Waiting for 5 minutes before trying again...");
					delay(1000*60*5);
					continue;
				}
				
				TrendsRateLimitUtils.setRateLimit(trendsResponses.getRateLimitStatus());
				
				Trend[] trends = trendsResponses.getTrends();
				
				Collections.shuffle(Arrays.asList(trends));
				
				System.out.println("Learning from Twitter trends...");
				
				int trendsLearntFrom = 0;
				
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
					
					trendsLearntFrom++;
					
					if (trendsLearntFrom > 10) {
						break;
					}
				}
			}
			
			System.out.println("Loading interests from interests.txt...");
			
			ArrayList<String> interests = new ArrayList<String>();
				
			try 
			{

				String currentLine;

				br = new BufferedReader(new FileReader("interests.txt"));

				while ((currentLine = br.readLine()) != null) 
				{
					currentLine = currentLine.trim();
					
					if (!currentLine.isEmpty())
					{
						interests.add(currentLine);
					}
				}

			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			} 
			finally 
			{
				try 
				{
					if (br != null)
					{
						br.close();
					}
				} 
				catch (IOException ex) 
				{
					ex.printStackTrace();
				}
			}
			
			System.out.println("Learning from specified interests...");
			
			Collections.shuffle(interests);
			
			int interestsLearntFrom = 0;
			
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
				
				interestsLearntFrom++;
				
				if (interestsLearntFrom > 10) {
					break;
				}
			}
						
			System.out.println("Loading exclusions from exclusions.txt...");
			
			ArrayList<String> exclusions = loadExclusions();
			
			System.out.println("Generating new tweet...");
			
			x = 0;
			sentence = null;
			
			while (x<maxTweetGenerationAttempts)
			{
				x++;
			
				sentence = megahal.getSentence();
				
				sentenceLower = sentence.toLowerCase();
				
				if (allTweets.contains(sentence) || sentence.length()>140 || sentenceLower.contains("…") 
						|| sentenceLower.contains("\r") || sentenceLower.contains("\n") 
						|| sentenceLower.startsWith("rt ")) continue;

				boolean sentenceContainsExclusion = false; 
				
				for (String exclusion : exclusions) 
				{
					if (sentenceLower.contains(exclusion.toLowerCase()))
					{
						sentenceContainsExclusion = true;
						break;
					}
				}
				
				if (sentenceContainsExclusion) continue;
				
				break;
			}
			
			if (sentence==null || x>=maxTweetGenerationAttempts)
			{
				System.out.println("No tweet could be generated. Perhaps your exclusions list is too restrictive.");
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
			
			randomTime = (long) (Math.random()*1000*60*maxDelayBetweenTweetingMinutes);
			System.out.println("Waiting "+randomTime/1000+" seconds ("+randomTime/1000/60+" minutes) before considering next tweet...");
			delay(randomTime);
		}
		
	}

	static ArrayList<String> loadExclusions() {
		
		BufferedReader br = null;
		
		ArrayList<String> exclusions = new ArrayList<String>();
		
		try 
		{

			String currentLine;

			br = new BufferedReader(new FileReader("exclusions.txt"));

			while ((currentLine = br.readLine()) != null) 
			{
				currentLine = currentLine.trim();
				
				if (!currentLine.isEmpty())
				{
					exclusions.add(currentLine);
				}
			}

		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			try 
			{
				if (br != null)
				{
					br.close();
				}
			} 
			catch (IOException ex) 
			{
				ex.printStackTrace();
			}
		}
		
		return exclusions;
	}

	private static void learnFromSearchQuery(String queryString) throws TwitterException 
	{
		System.out.print("Searching for '"+queryString+"'... ");
		
		SearchRateLimitUtils.waitIfNeeded();
		
		QueryResult queryResult = twitter.search(new Query(queryString).lang("en"));
		
		SearchRateLimitUtils.setRateLimit(queryResult.getRateLimitStatus());
		
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
	
	static void delay (long milliseconds)
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
