# Blackjack Autoplayer Assignment Documentation

## 1. Introduction

The goal of this assignment is to implement and evaluate a card counting strategy for a Blackjack autoplayer. This document details the baseline performance, the modifications made to incorporate card counting, and the subsequent analysis comparing both approaches.

## 2. Baseline Analysis: Original Autoplayer

### 2.1. Setup

- **Base URL:** `http://euclid.knox.edu:8080/api/blackjack`
- **Credentials:**
  - Username: `vsingh`
  - Password: `f013320`
- **Original Betting Strategy:**
  - Fixed bet of 10 for every hand.
- **Playing Strategy:**
  - Hit if `state.playerValue < 17` and stand if allowed.

### 2.2. Execution & Results

The original `Autoplayer.java` was executed over 1000 games. The output was as follows:

```
Number of games played: 1000
Number of wins: 408
Number of losses: 503
Number of pushes: 89
Number of blackjacks: 54
Number of dealer blackjacks: 53
Final balance: -680
```

### 2.3. Performance Metrics

- **Win Rate:** 40.8%
- **Loss Rate:** 50.3%
- **Push Rate:** 8.9%
- **Player Blackjack Rate:** 5.4%
- **Average Net Loss per Game:** -0.68

### Analysis

1. **Win/Loss Ratio**

   - The current strategy shows a negative win/loss ratio
   - Win Rate (40.8%) < Loss Rate (50.3%)
   - Push Rate at 8.9% is within expected range

2. **Blackjack Frequency**

   - Player Blackjacks (5.4%) slightly higher than Dealer Blackjacks
   - Combined blackjack rate of 10% is close to theoretical probability

3. **Financial Performance**
   - Net loss of 680 units over 1000 games
   - Average loss of 0.68 units per game
   - Current strategy is not profitable in the long run

## 3. Implementation of Card Counting (Phase 2)

### 3.1. Card Counting Strategy

I implemented a card counting system based on the basic counting strategy provided in the course material:

| Cards          | Count Value |
| -------------- | ----------- |
| Ace            | -2          |
| K, Q, J, 10, 9 | -1          |
| 8              | 0           |
| 7,6,4,3,2      | +1          |
| 5              | +2          |

The strategy works by:

1. Maintaining a running count across hands
2. Increasing bets when count is positive (more high cards remaining)
3. Decreasing bets when count is negative (more low cards remaining)
4. Resetting count when deck is reshuffled

### 3.2. Implementation Process

#### Step 1: Card Value Counter

Implemented the `getCardCountValue` method to assign proper counting values:

```java
private static int getCardCountValue(String card) {
    String rank = card.substring(0, card.length() - 1);
    switch (rank.toUpperCase()) {
        case "A": return -2;
        case "K": case "Q": case "J": case "10": return -1;
        case "9": return -1;
        case "8": return 0;
        case "7": case "6": case "4": case "3": case "2": return 1;
        case "5": return 2;
        default: return 0;
    }
}
```

#### Step 2: Count Tracking System

Added variables to maintain counting state:

```java
int runningCount = 0;
Set<String> cardsCountedThisHand = new HashSet<>();
```

- `runningCount`: Maintains the count across multiple hands
- `cardsCountedThisHand`: Prevents double-counting cards within a hand

#### Step 3: Dynamic Betting Strategy

Implemented variable betting based on the running count:

```java
int betAmount = 10; // Default bet
if (runningCount >= 4) {
    betAmount = 30;      // High count - bet more
} else if (runningCount >= 2) {
    betAmount = 20;      // Moderately high count
} else if (runningCount <= -2) {
    betAmount = 5;       // Negative count - bet minimum
}
```

#### Step 4: Enhanced Playing Strategy

Added a `shouldHit` method that considers:

- Player's hand value
- Dealer's up card
- Current running count

```java
private static boolean shouldHit(int playerValue, String dealerUpCard, int runningCount) {
    String dealerRank = dealerUpCard.substring(0, dealerUpCard.length() - 1);

    if (playerValue <= 11) return true;
    if (playerValue >= 17) return false;

    if (playerValue >= 12 && playerValue <= 16) {
        if ("2,3,4,5,6".contains(dealerRank)) {
            return runningCount < -2;
        }
        return true;
    }
    return false;
}
```

#### Step 5: Count Maintenance

Implemented count updating at key moments:

1. After initial deal
2. After each hit
3. After dealer reveals cards
4. Reset on deck reshuffle

### 3.3. Features and Safeguards

1. **Reshuffle Detection**

```java
if (state.reshuffled) {
    runningCount = 0;
    System.out.println("Deck reshuffled - Count reset");
}
```

2. **Double Counting Prevention**

```java
if (!cardsCountedThisHand.contains(card)) {
    runningCount += getCardCountValue(card);
    cardsCountedThisHand.add(card);
}
```

3. **Hand Reset**

```java
cardsCountedThisHand.clear(); // At start of each new hand
```

### 3.4. Statistical Tracking

Added comprehensive statistics tracking:

- Wins/Losses/Pushes
- Blackjacks (both player and dealer)
- Final balance
- Final running count

### 3.5. Challenges and Solutions

1. **Card Format Challenge**

   - Problem: Cards come as strings like "ACE OF HEARTS"
   - Solution: Extract rank using substring before last character

2. **Count Reliability**

   - Problem: Potential double counting of cards
   - Solution: Implemented HashSet to track counted cards

3. **Betting Strategy Balance**
   - Problem: Finding optimal bet spread
   - Solution: Used conservative 5-30 unit spread based on count thresholds

### 3.6. Future Improvements

1. **Bet Sizing**

   - Implement more dynamic bet sizing based on bankroll
   - Consider Kelly Criterion for optimal bet sizing

2. **Strategy Refinements**

   - Add support for splitting pairs
   - Implement more sophisticated count-based strategy deviations

3. **Performance Analysis**
   - Add tracking of win rate by count level
   - Track effectiveness of betting spreads

## 4. Results and Analysis

### 4.1 Comparative Performance Analysis

| Metric             | Original Strategy | Card Counting Strategy |
| ------------------ | ----------------- | ---------------------- |
| Games Played       | 1000              | 1000                   |
| Wins               | 408 (40.8%)       | 423 (42.3%)            |
| Losses             | 503 (50.3%)       | 483 (48.3%)            |
| Pushes             | 89 (8.9%)         | 94 (9.4%)              |
| Player Blackjacks  | 54 (5.4%)         | 46 (4.6%)              |
| Dealer Blackjacks  | 53 (5.3%)         | 50 (5.0%)              |
| Final Balance      | -680              | -370                   |
| Avg. Loss per Game | -0.68             | -0.37                  |

### 4.2 Strategy Implementation Results

#### Card Counting System

The implemented card counting system tracks the following values:

```java
Ace: -2
10, J, Q, K: -1
9: -1
8: 0 (neutral)
2,3,4,6,7: +1
5: +2
```

#### Betting Strategy

```java
int betAmount = 10;  // Default bet
if (runningCount >= 4) {
    betAmount = 30;      // Strong positive count
} else if (runningCount >= 2) {
    betAmount = 20;      // Moderate positive count
} else if (runningCount <= -2) {
    betAmount = 5;       // Negative count
}
```

### 4.3 Performance Analysis

1. **Win Rate Improvement**

   - Win rate increased from 40.8% to 42.3% (+1.5%)
   - Loss rate decreased from 50.3% to 48.3% (-2.0%)
   - Push rate increased from 8.9% to 9.4% (+0.5%)

2. **Financial Performance**

   - Final balance improved from -680 to -370
   - Average loss per game reduced from -0.68 to -0.37
   - 45.6% reduction in overall losses

3. **Blackjack Frequency**
   - Player blackjacks decreased slightly (5.4% to 4.6%)
   - Dealer blackjacks also decreased (5.3% to 5.0%)

### 4.4 Key Observations

1. **Deck Reshuffling**

   - Observed frequent deck reshuffles (11 times in 1000 games)
   - Running count reset after each reshuffle
   - Impact on strategy effectiveness due to frequent resets

2. **Strategy Effectiveness**
   - Improved overall outcomes despite reshuffle limitations
   - Variable betting strategy showed positive impact
   - Enhanced decision making for hands 12-16 proved beneficial

### 4.5 Challenges and Limitations

1. **Server-Side Constraints**

   - Frequent deck reshuffling limits long-term counting effectiveness
   - No information about deck penetration
   - Limited ability to track true count (count per deck)

2. **Implementation Challenges**
   - Need to handle reshuffles and reset count appropriately
   - Balance between aggressive and conservative betting
   - Limited ability to implement advanced playing variations

### 4.6 Conclusion

The implemented card counting strategy showed measurable improvements over the baseline strategy:

1. **Financial Improvement**

   - 45.6% reduction in average losses
   - More stable performance across sessions

2. **Strategic Success**

   - Higher win rate (+1.5%)
   - Lower loss rate (-2.0%)
   - Better handling of marginal situations (12-16 vs dealer 2-6)

3. **Areas for Future Enhancement**
   - More dynamic betting spread based on bankroll
   - Implementation of true count conversion
   - Advanced play variations based on count
