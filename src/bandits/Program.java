package bandits;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class Program {

	public static void main(String[] args) {  
		try 
		{	
			String algo = args[0];
			String rewardType = args[1];
			double range = Double.parseDouble(args[2]);
			double min = Double.parseDouble(args[3]);
			double step = Double.parseDouble(args[4]);
			double max = Double.parseDouble(args[5]);
			String engagementFile = args[6];
			String networkFile = args[7];			
			
			int n=1;
			if(!algo.equals("Static") && !algo.equals("Rolling"))
			{
				System.out.println("IMPLEMENTED Static AND Rolling Bandits, NOT " + algo + ". Returning code.");
				return;
			}
			System.out.println("ALGORITHM : " + algo + "\nReward Type : " + rewardType);

			List<String> userIdList = new ArrayList<String>();
			List<String> contentIdList = new ArrayList<String>();
			
			//Load Engagement Data
			Map<String, Map<String, Integer>> engagementData = loadEngagementData(range, engagementFile, contentIdList);
			
			//Load Network Data
			Map<String, List<String>> networkData = loadNetworkData(networkFile);
			
			//User ID List
			userIdList = new ArrayList<String>(networkData.keySet());
			
			//Parameters
			double alpha = 0.0;
			double beta = 0.0;
			double eta = 0.0;
			double uniformDist = userIdList.size();
			
			BanditType bandit = new BanditType(algo, rewardType, alpha, beta, eta, uniformDist);
			
			//Best Parameters
			double maxCumulativeReward = Integer.MIN_VALUE;
			double bestAlpha = 0.0;
			double bestBeta = 0.0;
			double bestEta = 0.0;
			double bestN = 0;

			//This iterates over different alpha, beta and eta values to find best parameters
			for(double i=0 ;i<10 ;i++)
			{
				//Only update alpha once every two times
				if(i%2==0)
				{
					bandit.setAlpha(bandit.getAlpha()+0.1);
				}
				
				bandit.setBeta(bandit.getBeta()+0.1);
				bandit.setEta(bandit.getEta()+0.1);
				
				performGridSearch(userIdList, contentIdList, engagementData, networkData, bandit, rewardType, range, min, max, step);
				if(maxCumulativeReward<bandit.getCumulativeReward())
				{
					maxCumulativeReward = bandit.getCumulativeReward();
					bestAlpha = bandit.getAlpha();
					bestBeta = bandit.getBeta();
					bestEta = bandit.getEta();
					bestN = bandit.getN();
				}
			}

			System.out.println("ITERATION WITH BEST PARAMETERS");
			
			//Bandit is reinitialized with best parameters. 
			//Also, last parameter is set true to initiate codes and print logs which need to be done only for best parameters
			bandit = new BanditType(algo, rewardType, bestAlpha, bestBeta, bestEta, uniformDist, true);
			
			performGridSearch(userIdList, contentIdList, engagementData, networkData, bandit, rewardType, range, min, max, step);           
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }

	}
	
	public static Map<String, Map<String, Integer>> loadEngagementData(double range, String engagementFile, List<String> contentIdList)
	{
		//Creates a map to store engagement data and initializes a CSV reader to read from the specified file.
		//Structure : Map<ContentId, Map<UserId, EngagementScore>>
        Map<String, Map<String, Integer>> engagementData = new HashMap<>();

        CSVReader reader = null;
        try 
        {
        	//Iterates through the CSV, skipping the header. For each row (up to the specified range), it extracts the content ID and populates a map with user IDs and their engagement scores.
        	//Adds the populated engagement map to the main data map and closes the reader.
            reader = new CSVReaderBuilder(new FileReader(engagementFile)).withSkipLines(1).build();
            String[] nextLine;
            int count=0;
            while ((nextLine = reader.readNext()) != null && count<=range) {
                String contentId = nextLine[0];  // First column is content_id
                contentIdList.add(contentId);
                Map<String, Integer> userEngagement = new HashMap<>();
                // Loop through user engagement scores starting from column 2
                for (int i = 1; i < nextLine.length; i++) {
                    
                	// Assuming the first user is user_101, user_102, etc.
                    String userId = "user_" + (i + 100); 
                    
                    userEngagement.put(userId, Integer.parseInt(nextLine[i]));
                }
                engagementData.put(contentId, userEngagement);
                count++;
            }
            reader.close();
        } catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

        return engagementData;
    }
	
	public static Map<String, List<String>> loadNetworkData(String networkFile)
	{
		//Creates a map to hold user network data and initializes a CSV reader to read the specified network file.
        Map<String, List<String>> networkData = new HashMap<>();

        CSVReader reader = null;
        try 
        {
        	//Iterates through the CSV, skipping the header. For each row, it extracts the user ID from the first column and splits the second column (containing follower IDs) into a list.
            reader = new CSVReaderBuilder(new FileReader(networkFile)).withSkipLines(1).build();  // Skip the header
            String[] nextLine;
            //Adds the user ID and their corresponding list of followers to the network data map.
            while ((nextLine = reader.readNext()) != null) {
                String userId = nextLine[0];  // The first column is the user ID
                List<String> followers = Arrays.asList(nextLine[1].split(","));
                networkData.put(userId, followers);
            }
        }
        catch(Exception ex)
        {
        	System.out.println(ex.getMessage());
        }
        return networkData;
    }
	
	
	
	public static void performGridSearch(List<String> userIdList, List<String> contentIdList, Map<String, Map<String, Integer>> userEngagementData, Map<String, List<String>> networkData, BanditType bandit, String rewardType, double range, double min, double max, double step) 
	{
		//Sets up necessary variables, including maps for user weights and pulls, and initializes the search over the specified parameter range. It tracks the most influential user and maximum probability encountered.
		String mostInfluentialUser = "";
        double maximumProbability = 0;
        double cumulativeReward = 0.0;
        Map<String, Double> userWeight = new HashMap();
        Map<String, Integer> userPull = new HashMap();
        int count=0;
        for (double x = min; x <= max; x += step) 
        {
        	System.out.println("\n----Step Count: " + x + "----\n");
        	//Go through each content to select a random user
            for(String contentId : contentIdList)
            {
                double totalProbability = 0.0;
            	Map<String, Double> userProbabilityMap = new HashMap();
            	Map<String, Double> tempWeight = new HashMap();
        		//Go through user for each content, this will be used as the N for rolling bandits
        		count++;
        		
            	for (String userId : userIdList) 
            	{
            		
            		//Initialize user weight which will store each user's estimated weight when user for a content is picked.
            		//Initiliaze userPull map for keeping count of how many times a user was picked for a content.
	            	if(!userWeight.containsKey(userId))
	            	{
	            		userWeight.put(userId, (double) 0);
	            		userPull.put(userId, 0);
	            	}
	            	
	            	//Initialize user probability to be picked to normalize it later on once we get the total probability value.
	            	if(!userProbabilityMap.containsKey(userId))
	            	{
	            		userProbabilityMap.put(userId, (double) 0);
	            	}
	            	
	            	//Set N value for rolling bandit
	            	bandit.setN(count);
	            	
	            	//Goes to the selected bandit's function and provides user's probability
	                double userProbability = bandit.banditSelection(userId,contentId, userEngagementData, networkData, userWeight);
	                tempWeight.put(userId, bandit.getWeight());
	                
	                userProbabilityMap.put(userId, userProbability);
	                totalProbability += userProbability;
	                
	                //Based on if the grid search based on best parameters, then use this logic to get the best client
	                if(bandit.isPrintLog())
	                {
	                	if(userProbability>maximumProbability)
		                {
		                	maximumProbability = userProbability;
		                	mostInfluentialUser = userId;
		                	userPull.put(userId, userPull.get(userId)+1);
		                }
		                else if(!mostInfluentialUser.isBlank() && maximumProbability>=0)
		                {
		                	userPull.put(mostInfluentialUser, userPull.get(mostInfluentialUser)+1);
		                }
	                }
            	}
            	
            	//Normalize Probabilities
            	calculateNormalizedProbability(userProbabilityMap, totalProbability);
            	
            	//If grid search is to find the best parameter, pick users randomly
            	if(!bandit.isPrintLog())
            	{            	
	            	//Select user at random
	                String userSelectedAtRandom = selectUserAtRandom(userProbabilityMap);
	                
	                //Update estimated weight of the selected user
	                userWeight.put(userSelectedAtRandom, tempWeight.get(userSelectedAtRandom));
	                
	                //Update the count of user chosen
	                userPull.put(userSelectedAtRandom, userPull.get(userSelectedAtRandom)+1);
	                
	                //Calculate current and cumulative reward based on the user selected
	                bandit.calculateCumulativeReward(userSelectedAtRandom, contentId, userEngagementData, networkData, userPull);	                
	                System.out.println("AT STEP " + x + ", FOR CONTENT : " + contentId + " SELECTED USER : " + userSelectedAtRandom + " WITH PROBABILITY OF " + userProbabilityMap.get(userSelectedAtRandom) + "\n"); 
            	}
            	else
            	{
            		//Update estimated weight of the best user
            		userWeight.put(mostInfluentialUser, tempWeight.get(mostInfluentialUser));
            		
            		//Calculate current and cumulative reward based on the best user
	                bandit.calculateCumulativeReward(mostInfluentialUser, contentId, userEngagementData, networkData, userPull);
	                System.out.println("AT STEP " + x + ", FOR CONTENT : " + contentId + " SELECTED USER : " + mostInfluentialUser + " WITH PROBABILITY OF " + userProbabilityMap.get(mostInfluentialUser) + "\n"); 
            	}
            }
        }
        
        //If grid search is on best parameters, then evaluate below results
        if(bandit.isPrintLog())
        {
        	String parameter = "";
        	if(bandit.getAlgo().toUpperCase().equals("STATIC"))
        	{
        		parameter = "\tBest Eta : " + bandit.getEta();
        	}
        	else
        	{
        		parameter = "\tBest N : " + bandit.getN();
        	}
        	System.out.println("\n== BEST PARAMETERS FOR " + bandit.getAlgo() + " REWARD TYPE " + rewardType + " == \nrange : " + range + "\tBest alpha : " + bandit.getAlpha() + "\tBest beta : " + bandit.getBeta() + parameter);
        	bandit.calculateTopThreeUsers(userPull);
        	System.out.println("BEST CLIENT : " + mostInfluentialUser + "\tCUMULATIVE REWARD : " + bandit.getCumulativeReward());            
        }
    }
	
	//Generates a random value between 0 and 1. It then iterates through the cumulative probabilities to find the first user whose cumulative probability exceeds this random value, indicating that this user should be selected.
	private static String selectUserAtRandom(Map<String, Double> userProbabilityMap)
	{
		List<String> userIdList = new ArrayList<>();
        List<Double> cumulativeProbabilities = new ArrayList<>();
        double cumulative = 0.0;
        for (Map.Entry<String, Double> entry : userProbabilityMap.entrySet()) {
            userIdList.add(entry.getKey());
            cumulative += entry.getValue();
            cumulativeProbabilities.add(cumulative);
        }
        
        double randomValue = Math.random();
        String selectedUser = null;
        for (int i = 0; i < cumulativeProbabilities.size(); i++) {
            if (randomValue <= cumulativeProbabilities.get(i)) {
            	selectedUser = userIdList.get(i);
                break;
            }
        }
        
        return selectedUser;
        
	}
	
	//Iterates over each user in the userProbabilityMap, dividing each user's probability by the totalProbability. This step ensures that all probabilities sum up to 1, making them valid probabilities.
	private static void calculateNormalizedProbability(Map<String, Double> userProbabilityMap, double totalProbability)
	{
		for(String userId : userProbabilityMap.keySet())
		{
			userProbabilityMap.put(userId, userProbabilityMap.get(userId)/totalProbability);
		}
	}

}
