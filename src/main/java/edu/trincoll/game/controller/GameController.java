package edu.trincoll.game.controller;

import edu.trincoll.game.command.CommandInvoker;
import edu.trincoll.game.command.GameCommand;
import edu.trincoll.game.model.Character;
import edu.trincoll.game.player.GameState;
import edu.trincoll.game.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main game controller that orchestrates turn-based combat.
 * <p>
 * Design Patterns Demonstrated:
 * <p>
 * - FACADE: Simplifies complex game loop interactions
 * - MEDIATOR: Coordinates between players, characters, and commands
 * - ITERATOR: Manages turn order
 * <p>
 * This class shows how multiple patterns work together:
 * <p>
 * - Players (Strategy) make decisions
 * - Commands (Command) encapsulate actions
 * - Controller (Mediator/Facade) orchestrates everything
 */
public class GameController {
    private final List<Character> team1;
    private final List<Character> team2;
    private final Map<Character, Player> playerMap;
    private final CommandInvoker invoker;
    private GameState gameState;

    public GameController(List<Character> team1,
                         List<Character> team2,
                         Map<Character, Player> playerMap) {
        this.team1 = new ArrayList<>(team1);
        this.team2 = new ArrayList<>(team2);
        this.playerMap = new HashMap<>(playerMap);
        this.invoker = new CommandInvoker();
        this.gameState = GameState.initial();
    }

    /**
     * Runs the main game loop until one team is defeated.
     * <p>
     * TODO 4: Implement game loop (15 points)
     */
    public void playGame() {
        System.out.println("=".repeat(60));
        System.out.println("AI-POWERED RPG GAME");
        System.out.println("=".repeat(60));
        
        displayTeamSetup();
        
        // Main game loop - continues until one team is defeated
        while (!isGameOver()) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("TURN " + gameState.turnNumber() + " - ROUND " + gameState.roundNumber());
            System.out.println("=".repeat(60));
            
            // Team 1's turn - process each living character
            for (Character character : team1) {
                if (character.getStats().health() > 0) {
                    processTurn(character, team1, team2);
                    
                    // Check if game ended after this action
                    if (isGameOver()) {
                        break;
                    }
                }
            }
            
            // Check again before Team 2's turn
            if (isGameOver()) {
                break;
            }
            
            // Team 2's turn - process each living character
            for (Character character : team2) {
                if (character.getStats().health() > 0) {
                    processTurn(character, team2, team1);
                    
                    // Check if game ended after this action
                    if (isGameOver()) {
                        break;
                    }
                }
            }
            
            // Move to next round
            gameState = gameState.nextRound();
            
            // Display round summary
            displayRoundSummary();
        }
    }

    /**
     * Processes a single character's turn.
     * <p>
     * TODO 5: Implement turn processing (10 points)
     * 
     * @param character the character taking their turn
     * @param allies the character's team
     * @param enemies the opposing team
     */
    private void processTurn(Character character,
                            List<Character> allies,
                            List<Character> enemies) {
        // Skip if character is defeated
        if (character.getStats().health() <= 0) {
            return;
        }
        
        System.out.println("\n" + character.getName() + "'s turn...");
        
        // Get the player controlling this character
        Player player = playerMap.get(character);
        
        if (player == null) {
            System.err.println("Error: No player found for " + character.getName());
            return;
        }
        
        // Get the player's decision
        GameCommand command = player.decideAction(character, allies, enemies, gameState);
        
        if (command == null) {
            System.err.println("Error: Player returned null command");
            return;
        }
        
        // Execute the command
        invoker.executeCommand(command);
        
        // Display action result (commands typically print their own results)
        displayActionResult(command, character);
        
        // Update game state - increment turn and track command
        gameState = gameState.nextTurn()
            .withUndo(true, invoker.getCommandHistory().size());
    }

    /**
     * Checks if the game is over.
     * <p>
     * Game ends when all characters on one team are defeated (HP <= 0).
     *
     * @return true if game is over, false otherwise
     */
    private boolean isGameOver() {
        boolean team1Alive = team1.stream()
            .anyMatch(c -> c.getStats().health() > 0);
        boolean team2Alive = team2.stream()
            .anyMatch(c -> c.getStats().health() > 0);

        return !team1Alive || !team2Alive;
    }

    /**
     * Displays the game result.
     */
    public void displayResult() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GAME OVER");
        System.out.println("=".repeat(60));

        boolean team1Wins = team1.stream().anyMatch(c -> c.getStats().health() > 0);

        if (team1Wins) {
            System.out.println("🏆 Team 1 wins!");
        } else {
            System.out.println("🏆 Team 2 wins!");
        }

        System.out.println("\nFinal Status:");
        System.out.println("\nTeam 1:");
        for (Character c : team1) {
            displayCharacterStatus(c);
        }

        System.out.println("\nTeam 2:");
        for (Character c : team2) {
            displayCharacterStatus(c);
        }

        System.out.println("\nTotal turns played: " + gameState.turnNumber());
        System.out.println("Total commands executed: " + gameState.commandHistorySize());
    }

    private void displayCharacterStatus(Character c) {
        String status = c.getStats().health() > 0 ? "Alive" : "Defeated";
        System.out.printf("  %s (%s): %d HP - %s%n",
            c.getName(),
            c.getType(),
            Math.max(0, c.getStats().health()),
            status);
    }
    
    /**
     * Displays initial team setup.
     */
    private void displayTeamSetup() {
        System.out.println("\n=== Team Setup ===");
        
        System.out.println("Team 1:");
        for (Character c : team1) {
            Player player = playerMap.get(c);
            String playerType = player.getClass().getSimpleName();
            System.out.printf("  - %s (%s) - %s%n", 
                c.getName(), c.getType(), playerType);
        }
        
        System.out.println("\nTeam 2:");
        for (Character c : team2) {
            Player player = playerMap.get(c);
            String playerType = player.getClass().getSimpleName();
            System.out.printf("  - %s (%s) - %s%n", 
                c.getName(), c.getType(), playerType);
        }
    }
    
    /**
     * Displays summary at end of each round.
     */
    private void displayRoundSummary() {
        System.out.println("\n--- End of Round " + gameState.roundNumber() + " ---");
        
        System.out.println("\nTeam 1 Status:");
        for (Character c : team1) {
            String status = c.getStats().health() > 0 
                ? String.format("%d/%d HP", c.getStats().health(), c.getStats().maxHealth())
                : "DEFEATED";
            System.out.printf("  %s: %s%n", c.getName(), status);
        }
        
        System.out.println("\nTeam 2 Status:");
        for (Character c : team2) {
            String status = c.getStats().health() > 0 
                ? String.format("%d/%d HP", c.getStats().health(), c.getStats().maxHealth())
                : "DEFEATED";
            System.out.printf("  %s: %s%n", c.getName(), status);
        }
    }
    
    /**
     * Displays the result of an action.
     */
    private void displayActionResult(GameCommand command, Character actor) {
        // Commands typically print their own results
        // This method is here for additional logging if needed
    }
}