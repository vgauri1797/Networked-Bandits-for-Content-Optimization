# Networked-Bandits-for-Content-Optimization
Built a networked bandit library to identify the highest-rewarding user (“best client”) for content exposure. The system loads engagement scores and a follower network, supports two bandit algorithms, and evaluates two reward models: 
(1) Individual engagement (direct per-user scores) 
(2) Influence effects (½ of each follower’s engagement, excluding the user).

Implements command-line execution with grid search over parameters (min, step, max) on a row range, then runs full-file evaluation. Prints per-step choices, rewards, cumulative totals, and final pull distribution/best user. Includes two manually defined networks and a report comparing algorithm performance, tuned parameters, and how graph structure impacts results.

Comman to run the project:
java -jar Bandit.jar <alg> <reward> <range> <min> <step> <max> <engagement file> <network file>
Arguments:
• alg is one of ”Static” or”Rolling” - DID NOT IMPLEMENT RECENCY AS ONLY TWO TYPES OF BANDITS WERE TOLD TO IMPLEMENT.
• reward is one if ”Individual” or ”Influence”
• range is the number of rows used for parameter fitting.
• min is the minimum parameter value for your grid search (e.g. 0)
• step is the step size for your grid search (e.g. 0.2)
• max is the maximum parameter value for your grid search (e.g. 0.9)
• engagement file: A file containing data for the users and their engagement scores for each content
(row).
• network file: A file representing the network topology (connections between users).

Test files included:
1. ORGINIAL FILES : content_engagements.csv and user_network_connections.csv
2. CUSTOM DEFINED(Content custom-made) : content_engagements-Test.csv anduser_network_connections-Test.csv
3. CUSTOM DEFINED(Content custom-made) : content_engagements-Test-1.csv anduser_network_connections-Test-1.csv

Parameters in the code included:
1. alpha - exploration rate
2. beta - decay parameter for the weight
3. eta - reward weight
4. uniformDist - uniform distribution parameter(equal to 1/no. of users)
5. n - Dynamic value used in rolling bandit for estimated weight evaluation. Number of times any user has been pulled for a content(Counter is present to calculate this inside content loop).
6. bestAlpha - best alpha paramter found
7. bestBeta - best beta paramter found
8. bestEta - best eta paramter found
9. bestN - best N paramter found

Class Program
loadEngagementData - 
Initialization: Creates a map to store engagement data and initializes a CSV reader to read from the specified file.
Reading Data: Iterates through the CSV, skipping the header. For each row (up to the specified range), it extracts the content ID and populates a map with user IDs and their engagement scores.
Storing Data: Adds the populated engagement map to the main data map and closes the reader.

loadNetworkData - 
Initialization: Creates a map to hold user network data and initializes a CSV reader to read the specified network file.
Reading Data: Iterates through the CSV, skipping the header. For each row, it extracts the user ID from the first column and splits the second column (containing follower IDs) into a list.
Storing Data: Adds the user ID and their corresponding list of followers to the network data map.

performGridSearch - 
Initialization: Sets up necessary variables, including maps for user weights and pulls, and initializes the search over the specified parameter range. It tracks the most influential user and maximum probability encountered.
Nested Iteration: Loops through content IDs, calculating the probability of each user for a given content using the selected bandit algorithm. It updates user weights and records the selected user based on the maximum probability (if logging is enabled) or randomly (if logging is not enabled).
Results Output: After completing the iterations, it prints the best parameters for the bandit algorithm, displays the top three users based on their pulls, and provides information about the most influential user along with the cumulative reward.

selectUserAtRandom - 
Setup: Initializes two lists, one for user IDs and another for cumulative probabilities. It calculates cumulative probabilities based on the provided user probability map, effectively creating a distribution for selection.
Random Selection: Generates a random value between 0 and 1. It then iterates through the cumulative probabilities to find the first user whose cumulative probability exceeds this random value, indicating that this user should be selected.
Return Selected User: Once a user is selected based on the random draw, the function returns the ID of that user.

calculateNormalizedProbability - 
Normalization Calculation: Iterates over each user in the userProbabilityMap, dividing each user's probability by the totalProbability. This step ensures that all probabilities sum up to 1, making them valid probabilities.
Updating Map: While normalizing, it updates the userProbabilityMap in place, replacing each user's original probability with its normalized value.

Class BanditType
Variables:
cumulativeReward - Total cumulative reward on selecting random/best user for each content.
rewardType - Individual or Influence
algo - Static or Rolling
alpha - exploration rate
beta - decay parameter for the weight
eta - reward weight
uniformDist - uniform distribution parameter(equal to 1/no. of users)
weight - used for evaluation of each user's weight during each bandit.
N - Number of times any user has been pulled for a content(Counter is present to calculate this inside content loop). 
printLog - Dynamic value used in rolling bandit for estimated weight evaluation. It will be equal the observed count of the user in the grid search.

calculateContentReward - 
Calculate user-content reward of user's direct or influence engagement based on reward type value set.

banditSelection - Function call for type of bandit set.
staticBanditSelection - 
1. Calculates the engagement reward for a specific user and content combination through the calculateContentReward method. 
This reward reflects the user's direct engagement or the influence from their followers based on the rewardType.
2. The weight for the user is updated using a static bandit approach. 
It multiplies the previous weight by a constant factor (beta) and adds the influence reward scaled by a weight (eta). 
This approach maintains the importance of historical weights while incorporating new engagement data.
	w(a,i) ← βw(a,i−1) + ηri
3. Finally, the function calculates the probability of selecting the content. 
It combines the updated weight with a uniform distribution factor, ensuring a balance between leveraging past performance and exploring new options, then returns this probability.
	p(a,i) = ˜ w(a,i)(1 − γ) + γξa

rollingBanditSelection -
1. Calculates the reward for a specific user and content combination using the calculateContentReward method. 
This reward reflects the user's engagement with the content or the influence of their followers based on the rewardType.
2. The weight for the specified user is updated based on the calculated influence reward. 
The new weight is determined by taking the previous weight and adjusting it using a rolling average formula. 
Here, n represents a constant that influences the rate of adjustment, ensuring the weight converges towards the current influence reward over time.
	w(a,i) ← w(a,i−1) + 1/n(ri − w(a,i−1))
3. Finally, the function calculates the probability of the content being selected based on the updated weight. 
It combines the weight with a uniform distribution factor, balancing between the existing weight and exploration of new content, and returns this probability.

calculateCumulativeReward-
1. The function starts by retrieving the engagement score for the specified user and content from the userEngagementData. This score represents the user's direct engagement with the content.
2. It then gathers the list of followers for the user and iterates through this list. For each follower, it adds half of their engagement score (from the same content) to the current cumulative reward. This reflects the influence effect, where a user's engagement can impact their followers.
3. The function prints the algorithm type, reward type, and parameters used for clarity. It updates the total cumulative reward by adding the current cumulative reward and outputs both the current and total cumulative rewards to provide insight into the performance of the algorithm over time.

calculateTopThreeUsers-
1. The function begins by calculating the total sum of the engagement counts from the userPull map. This total will be used to compute the percentage of each user's engagement relative to the overall total.
2. It then sorts the entries of the userPull map by their engagement values in descending order. Using a stream, it collects the top three entries into a list, ensuring that you get the users with the highest engagement counts.
3. Finally, the function prints the top three users along with their engagement counts and the percentage of total engagement they represent. This provides a clear view of the most influential users based on the number of times their content was engaged with during the iterations.




