package edu.trincoll.game;

import edu.trincoll.game.controller.GameController;
import edu.trincoll.game.factory.CharacterFactory;
import edu.trincoll.game.model.Character;
import edu.trincoll.game.model.CharacterType;
import edu.trincoll.game.player.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Main Spring Boot application for AI-powered RPG game.
 * <p>
 * This application demonstrates:
 * <p>
 * - Spring Boot autoconfiguration
 * - Spring AI integration
 * - Command-line game interface
 * - Design patterns working together
 * <p>
 * Run with:
 * <p>
 *   ./gradlew run
 * <p>
 * Or with API keys:
 * <p>
 *   OPENAI_API_KEY=xxx ANTHROPIC_API_KEY=yyy ./gradlew run
 */
@SpringBootApplication
public class GameApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameApplication.class, args);
    }

    /**
     * CommandLineRunner bean that executes after Spring Boot starts.
     * <p>
     * This is where the game setup and execution happens.
     * Students will implement team configuration here.
     *
     * @param openAiClient ChatClient for OpenAI/GPT-5
     * @param anthropicClient ChatClient for Anthropic/Claude Sonnet 4.5
     * @return CommandLineRunner that starts the game
     */
    @Bean
    public CommandLineRunner run(
            @Qualifier("openAiChatClient") ChatClient openAiClient,
            @Qualifier("anthropicChatClient") ChatClient anthropicClient) {

        return args -> {
            System.out.println("""
                ============================================================
                AI-POWERED RPG GAME
                ============================================================
                
                This game demonstrates design patterns with AI players:
                - Strategy Pattern: Different AI decision-making algorithms
                - Command Pattern: Undoable game actions
                - Factory Pattern: Character creation
                - Builder Pattern: Complex object construction
                
                Players can be:
                - Human (you control via console)
                - LLM-based (GPT-4, Claude, or Gemini)
                - Rule-based AI (simple if-then logic)
                ============================================================
                """);

                GameController controller = createTeamConfiguration(openAiClient, anthropicClient);
                
                // Start the game
                controller.playGame();
                controller.displayResult();
            };
    }

    /**
     * Helper method to create team configuration.
     *
     * TODO 6 (part of): Students implement this to set up teams.
     *
     * Example implementation structure:
     * ```
     * // Team 1: Human + RuleBasedAI
     * Character humanWarrior = CharacterFactory.createWarrior("Conan");
     * Character aiMage = CharacterFactory.createMage("Gandalf");
     * List<Character> team1 = List.of(humanWarrior, aiMage);
     *
     * // Team 2: Three LLM players
     * Character gptArcher = CharacterFactory.createArcher("Legolas");
     * Character claudeRogue = CharacterFactory.createRogue("Assassin");
     * Character geminiWarrior = CharacterFactory.createWarrior("Tank");
     * List<Character> team2 = List.of(gptArcher, claudeRogue, geminiWarrior);
     *
     * // Map characters to players
     * Map<Character, Player> playerMap = new HashMap<>();
     * playerMap.put(humanWarrior, new HumanPlayer());
     * playerMap.put(aiMage, new RuleBasedPlayer());
     * playerMap.put(gptArcher, new LLMPlayer(openAiClient, "GPT-5"));
     * playerMap.put(claudeRogue, new LLMPlayer(anthropicClient, "Claude-Sonnet-4.5"));
     * playerMap.put(geminiWarrior, new LLMPlayer(geminiClient, "Gemini-2.5-Pro"));
     *
     * return new GameController(team1, team2, playerMap);
     * ```
     */
    private GameController createTeamConfiguration(
        ChatClient openAiClient,
        ChatClient anthropicClient) {
    
    // Team 1: Weaker characters (Archer + Rogue) - easier to defeat
    Character archer1 = CharacterFactory.createArcher("Legolas");
    Character rogue1 = CharacterFactory.createRogue("Shadow");
    List<Character> team1 = List.of(archer1, rogue1);
    
    // Team 2: Stronger characters (Warrior + Mage) - should win
    Character warrior2 = CharacterFactory.createWarrior("Conan");
    Character mage2 = CharacterFactory.createMage("Gandalf");
    List<Character> team2 = List.of(warrior2, mage2);
    
    // Map each character to their player type
    Map<Character, Player> playerMap = new HashMap<>();
    playerMap.put(archer1, new HumanPlayer());  // Human controls archer
    playerMap.put(rogue1, new RuleBasedPlayer());  // RuleBasedAI controls rogue
    playerMap.put(warrior2, new LLMPlayer(openAiClient, "GPT-5"));  // GPT-5 controls warrior
    playerMap.put(mage2, new LLMPlayer(anthropicClient, "Claude-Sonnet-4.5"));  // Claude controls mage
    
    // Create and return GameController
    return new GameController(team1, team2, playerMap);
}
}