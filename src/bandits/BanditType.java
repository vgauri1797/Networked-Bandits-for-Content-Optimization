package bandits;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BanditType {
	private double cumulativeReward = 0;
    private String rewardType;
    private String algo;
    private double alpha = 0;
    private double beta = 0;
    private double eta = 0;
    private double uniformDist = 0;
    private double weight = 0.0;
    private double n = 0;
    private boolean printLog = false;
    public BanditType(String algo, String rewardType, double alpha, double beta, double eta, double uniformDist) 
    {
    	this.algo = algo;
		this.rewardType = rewardType;
		this.alpha = alpha;
		this.beta = beta;
		this.eta = eta;
		this.uniformDist = uniformDist;
	}
    
    public BanditType(String algo, String rewardType, double alpha, double beta, double eta, double uniformDist, boolean printLog) 
    {
    	this.algo = algo;
		this.rewardType = rewardType;
		this.alpha = alpha;
		this.beta = beta;
		this.eta = eta;
		this.uniformDist = uniformDist;
		this.printLog = printLog;
	}
    
	public String getRewardType() {
		return rewardType;
	}

	public void setRewardType(String rewardType) {
		this.rewardType = rewardType;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public double getBeta() {
		return beta;
	}

	public void setBeta(double beta) {
		this.beta = beta;
	}

	public double getEta() {
		return eta;
	}

	public void setEta(double eta) {
		this.eta = eta;
	}
	
	public double getCumulativeReward() {
		return cumulativeReward;
	}

	public void setCumulativeReward(double cumulativeReward) {
		this.cumulativeReward = cumulativeReward;
	}
	
	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getN() {
		return n;
	}

	public void setN(double n) {
		this.n = n;
	}

	public boolean isPrintLog() {
		return printLog;
	}

	public void setPrintLog(boolean printLog) {
		this.printLog = printLog;
	}

	public String getAlgo() {
		return algo;
	}

	public void setAlgo(String algo) {
		this.algo = algo;
	}

	public double getUniformDist() {
		return uniformDist;
	}

	public void setUniformDist(double uniformDist) {
		this.uniformDist = uniformDist;
	}

	public double calculateContentReward(String userId, String contentId, Map<String, Map<String, Integer>> userEngagementData, Map<String, List<String>> networkData) {
		double influenceReward = 0.0;
		
		if (rewardType.toUpperCase().equals("INDIVIDUAL")) {
			influenceReward = userEngagementData.get(contentId).get(userId);
        } 
		else if (rewardType.toUpperCase().equals("INFLUENCE")) {
			
			List<String> followers = networkData.get(userId);
			if(followers==null)
			{
				return influenceReward;
			}
			//List<String> contentIdList = userEngagementData.get(userId).entrySet().stream().filter(entry -> entry.getValue()!=0).map(Map.Entry::getKey).collect(Collectors.toList());
			for(int i=0; i<followers.size(); i++)
			{
				int reward = userEngagementData.get(contentId).get(followers.get(i));//.entrySet().stream().filter(entry -> contentIdList.contains(entry.getKey())).map(Map.Entry::getValue).reduce(Integer::sum).orElse(0);
				influenceReward += 0.5*reward;
			}
        }
		
		return influenceReward;
    }
	
	public double banditSelection(String userId, String contentId, Map<String, Map<String, Integer>> userEngagementData, Map<String, List<String>> networkData, Map<String, Double> userWeight)
	{
		return algo.toUpperCase().equals("STATIC") ? staticBanditSelection(userId, contentId, userEngagementData, networkData, userWeight) : rollingBanditSelection(userId, contentId, userEngagementData, networkData, userWeight);
	}
	
	private double staticBanditSelection(String userId, String contentId, Map<String, Map<String, Integer>> userEngagementData, Map<String, List<String>> networkData, Map<String, Double> userWeight)
	{
		Map<String, Double> userWeightTemp = userWeight;
		//Reward at step i
		double influenceReward = calculateContentReward(userId, contentId, userEngagementData, networkData);

		//Weight of content a at time i
		//In Static Bandit, I have taken a constant value of beta multiplied by the previous content weight and add it to the multiplication of n value(constant value) and influence reward based on user engagements.
		weight = beta * userWeight.get(userId) + eta * influenceReward;
		
		//Probability of content a at time i
		//For probability, I have taken alpha as constant and uniformDist as 1/range
		double contentProbability = weight * (1-alpha) + alpha * uniformDist;
		
		return contentProbability;		
	}
	
	private double rollingBanditSelection(String userId, String contentId, Map<String, Map<String, Integer>> userEngagementData, Map<String, List<String>> networkData, Map<String, Double> userWeight)
	{
		//Reward at step i
		double influenceReward = calculateContentReward(userId, contentId, userEngagementData, networkData);
		
		//Weight of content a at time i
		//In Rolling bandits, I have taken constant value of 1/n value(i.e. also the same value of unfiromDist variable) where n is the range of contents we have taken
		weight = userWeight.get(userId) + (double)(1/n) * (influenceReward - userWeight.get(userId));
		//Probability of content a at time i
		double contentProbability = weight * (1-alpha) + alpha * uniformDist;
		return contentProbability;	
	}
	
	public void calculateCumulativeReward(String userId, String contentId, Map<String, Map<String, Integer>> userEngagementData, Map<String, List<String>> networkData, Map<String, Integer> userPull)
	{
        double currentCumulativeReward = userEngagementData.get(contentId).get(userId);
        
        List<String> followers = networkData.get(userId);
		for(int i=0; i<followers.size(); i++)
		{
			int reward = userEngagementData.get(contentId).get(followers.get(i));
			currentCumulativeReward += 0.5*reward;
		}
		
		System.out.println("\nALGORITHM : " + algo + "\tREWARD TYPE : " + rewardType);
		System.out.println("== PARAMETERS USED == alpha : " + alpha + "\tbeta : " + beta + "\tn : " + eta);
		
		cumulativeReward += currentCumulativeReward;		
		System.out.println("Current Reward : " + currentCumulativeReward + "\tTotal Cumulative Record : " + getCumulativeReward());
	}
	
	public void calculateTopThreeUsers(Map<String, Integer> userPull)
	{
		// Step 1: Calculate the total sum of all values
		int totalSum = userPull.values().stream().mapToInt(Integer::intValue).sum();
		
		// Step 2: Sort the map by values in descending order and get the top 3 entries
		List<Map.Entry<String, Integer>> topEntries = userPull.entrySet()
	            .stream()
	            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
	            .limit(3)
	            .collect(Collectors.toList());
		
		// Step 3: Print the top 3 values and their percentages
        System.out.println("== Top 3 users maximum no. of times pulled(In Percentage) over the min and max iteration: ==");
        for (Map.Entry<String, Integer> entry : topEntries) {
            double percentage = (entry.getValue() / (double) totalSum) * 100;
            System.out.printf("%s: %d (%.2f%%)%n", entry.getKey(), entry.getValue(), percentage);
        }
	}
}
