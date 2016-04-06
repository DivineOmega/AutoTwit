package main;

import twitter4j.RateLimitStatus;

public abstract class SearchRateLimitUtils {

	private static int rateLimitRemaining = 0;
	private static int rateLimitSecondsUntilReset = 0;
	
	public static void setRateLimit(RateLimitStatus rateLimitStatus) {
		
		rateLimitRemaining = rateLimitStatus.getRemaining();
		rateLimitSecondsUntilReset = rateLimitStatus.getSecondsUntilReset();
		
	}
	
	public static void waitIfNeeded() {
		
		if (rateLimitRemaining <= 1 && rateLimitSecondsUntilReset > 0) {
			
			System.out.println("Rate limit for search reached! Waiting " + rateLimitSecondsUntilReset+" seconds...");
			
			
			while(rateLimitSecondsUntilReset > 0) {
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				rateLimitSecondsUntilReset--;
				
			}
			
		}
		
	}

	
	
}
