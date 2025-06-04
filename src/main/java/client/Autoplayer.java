package client;

import java.util.HashSet;
import java.util.Set;

public class Autoplayer {

    private static int getCardCountValue(String card) {
        // Extract first character(s) of the card string for rank
        String rank = card.substring(0, card.length() - 1); // Remove last character (suit)

        switch (rank.toUpperCase()) {
            case "A":
                return -2;
            case "K":
            case "Q":
            case "J":
            case "10":
                return -1;
            case "9":
                return -1;
            case "8":
                return 0;
            case "7":
            case "6":
            case "4":
            case "3":
            case "2":
                return 1;
            case "5":
                return 2;
            default:
                return 0;
        }
    }

    private static boolean shouldHit(int playerValue, String dealerUpCard, int runningCount) {
        // Extract dealer's card value
        String dealerRank = dealerUpCard.substring(0, dealerUpCard.length() - 1);

        // Always hit on 11 or below
        if (playerValue <= 11) {
            return true;
        }

        // Always stand on 17 or above
        if (playerValue >= 17) {
            return false;
        }

        // Handle 12-16 based on dealer's up card
        if (playerValue >= 12 && playerValue <= 16) {
            // Dealer showing 2-6 (weak card)
            if ("2,3,4,5,6".contains(dealerRank)) {
                // Stand unless count is very negative
                return runningCount < -2;
            }
            // Dealer showing 7 or higher
            return true; // Hit
        }

        return false; // Default to stand
    }

    public static void main(String[] args) throws Exception {
        String baseUrl = "http://euclid.knox.edu:8080/api/blackjack";
        String username = "vsingh";
        String password = "f013320";

        ClientConnecter clientConnecter = new ClientConnecter(baseUrl, username, password);
        GameState state = clientConnecter.startGame();

        int numGames = 1000;
        int numWins = 0;
        int numLosses = 0;
        int numPushes = 0;
        int numBlackjacks = 0;
        int numDealerBlackjacks = 0;

        // Card counting variables
        int runningCount = 0;
        Set<String> cardsCountedThisHand = new HashSet<>();

        for (int i = 0; i < numGames; i++) {
            // Clear counted cards for new hand
            cardsCountedThisHand.clear();

            // Determine bet based on running count
            int betAmount = 10; // Default bet
            if (runningCount >= 4) {
                betAmount = 30;
            } else if (runningCount >= 2) {
                betAmount = 20;
            } else if (runningCount <= -2) {
                betAmount = 5;
            }

            state = clientConnecter.placeBet(state.sessionId, betAmount);

            // Count initial cards
            for (String card : state.playerCards) {
                if (!cardsCountedThisHand.contains(card)) {
                    runningCount += getCardCountValue(card);
                    cardsCountedThisHand.add(card);
                }
            }
            // Count dealer's up card
            if (!state.dealerCards.isEmpty()) {
                String dealerUpCard = state.dealerCards.get(0);
                if (!cardsCountedThisHand.contains(dealerUpCard)) {
                    runningCount += getCardCountValue(dealerUpCard);
                    cardsCountedThisHand.add(dealerUpCard);
                }
            }

            // Check for instant win/loss
            if (state.phase.equals("RESOLVED")) {
                if (state.outcome.equals("PLAYER_BLACKJACK")) {
                    numBlackjacks++;
                    numWins++;
                } else if (state.outcome.equals("DEALER_WINS")) {
                    numDealerBlackjacks++;
                    numLosses++;
                } else if (state.outcome.equals("PUSH")) {
                    numPushes++;
                }
                state = clientConnecter.newGame(state.sessionId);
                continue;
            }

            // Playing strategy based on basic rules
            while (state.canHit) {
                String dealerUpCard = state.dealerCards.get(0);

                if (shouldHit(state.playerValue, dealerUpCard, runningCount)) {
                    state = clientConnecter.hit(state.sessionId);

                    // Check for reshuffle
                    if (state.reshuffled) {
                        runningCount = 0;
                        System.out.println("Deck reshuffled - Count reset");
                    }

                    // Count the new card received
                    if (!state.playerCards.isEmpty()) {
                        String newCard = state.playerCards.get(state.playerCards.size() - 1);
                        if (!cardsCountedThisHand.contains(newCard)) {
                            runningCount += getCardCountValue(newCard);
                            cardsCountedThisHand.add(newCard);
                        }
                    }
                } else {
                    break;
                }
            }

            if (state.canStand) {
                state = clientConnecter.stand(state.sessionId);
                // Count dealer's cards after stand
                for (String card : state.dealerCards) {
                    if (!cardsCountedThisHand.contains(card)) {
                        runningCount += getCardCountValue(card);
                        cardsCountedThisHand.add(card);
                    }
                }
            }

            if (state.phase.equals("RESOLVED")) {
                if (state.outcome.equals("PLAYER_WINS")) {
                    numWins++;
                } else if (state.outcome.equals("DEALER_WINS")) {
                    numLosses++;
                } else if (state.outcome.equals("PUSH")) {
                    numPushes++;
                }
            }

            state = clientConnecter.newGame(state.sessionId);
        }

        clientConnecter.finishGame(state.sessionId);

        // Print results
        System.out.println("Number of games played: " + numGames);
        System.out.println("Number of wins: " + numWins);
        System.out.println("Number of losses: " + numLosses);
        System.out.println("Number of pushes: " + numPushes);
        System.out.println("Number of blackjacks: " + numBlackjacks);
        System.out.println("Number of dealer blackjacks: " + numDealerBlackjacks);
        System.out.println("Final balance: " + state.balance);
        System.out.println("Final running count: " + runningCount);
    }
}
