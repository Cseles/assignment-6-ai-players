package edu.trincoll.game.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.trincoll.game.command.AttackCommand;
import edu.trincoll.game.command.GameCommand;
import edu.trincoll.game.command.HealCommand;
import edu.trincoll.game.model.Character;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * LLM-based AI player using Spring AI.
 * <p>
 * This class demonstrates how to integrate Large Language Models
 * into game AI using the Strategy pattern. The LLM acts as the
 * decision-making algorithm, making this player fundamentally
 * different from rule-based AI.
 * <p>
 * Design Patterns:
 * <p>
 * - STRATEGY: Implements Player interface with LLM-based decisions
 * - ADAPTER: Adapts LLM output format to game commands
 * - FACADE: Simplifies complex LLM interaction
 * <p>
 * Students will implement the prompt engineering and response parsing.
 */
public class LLMPlayer implements Player {
    private final ChatClient chatClient;
    private final String modelName;

    public LLMPlayer(ChatClient chatClient, String modelName) {
        this.chatClient = chatClient;
        this.modelName = modelName;
    }

    @Override
    public GameCommand decideAction(Character self,
                                   List<Character> allies,
                                   List<Character> enemies,
                                   GameState gameState) {
        // TODO 1: Build the prompt
        String prompt = buildPrompt(self, allies, enemies, gameState);

        try {
            // TODO 2: Call the LLM and parse response
            Decision decision = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(Decision.class);

            // TODO 3: Validate and convert Decision to GameCommand
            if (decision == null || decision.action() == null || decision.target() == null) {
                System.err.println("Invalid decision from LLM: " + decision);
                return new AttackCommand(self, enemies.getFirst()); // Fallback
            }

            System.out.println("[" + modelName + "] Reasoning: " + decision.reasoning());

            Character target;
            if ("attack".equalsIgnoreCase(decision.action())) {
                target = findCharacterByName(decision.target(), enemies);
                return new AttackCommand(self, target);
            } else if ("heal".equalsIgnoreCase(decision.action())) {
                target = findCharacterByName(decision.target(), allies);
                return new HealCommand(target, 30);
            } else {
                System.err.println("Unknown action from LLM: " + decision.action());
                return new AttackCommand(self, enemies.getFirst()); // Fallback
            }

        } catch (Exception e) {
            System.err.println("Error calling LLM: " + e.getMessage());
            return new AttackCommand(self, enemies.getFirst()); // Fallback
        }
    }

    /**
     * TODO 1: Build an effective prompt for the LLM.
     * 
     * A good prompt should include:
     * 1. Role definition: "You are a [character type] in a tactical RPG..."
     * 2. Current situation: HP, mana, position in battle
     * 3. Allies: Who's on your team and their status
     * 4. Enemies: Who you're fighting and their status
     * 5. Available actions: attack (with damage estimate) or heal
     * 6. Strategic guidance: "Consider focus fire, protect wounded allies..."
     * 7. Output format: JSON structure expected
     *
     * @param self your character
     * @param allies your team
     * @param enemies opponent team
     * @param gameState current game state
     * @return prompt string for the LLM
     */
    private String buildPrompt(Character self,
                               List<Character> allies,
                               List<Character> enemies,
                               GameState gameState) {
        // Get health percentage for self
        double selfHealthPercent = (double) self.getStats().health() / self.getStats().maxHealth() * 100;
        
        // Calculate estimated damage against first enemy as baseline
        int estimatedDamage = enemies.isEmpty() ? 0 : estimateDamage(self, enemies.getFirst());
        
        // Build type-specific tactical advice
        String roleAdvice = switch (self.getType()) {
            case WARRIOR -> "As a Warrior, you have high HP and attack power. Focus on protecting allies and eliminating threats.";
            case MAGE -> "As a Mage, you have powerful attacks but low HP. Stay alive and deal maximum damage.";
            case ARCHER -> "As an Archer, focus on picking off weakened enemies from a distance.";
            case ROGUE -> "As a Rogue, use your agility to strike at the most vulnerable targets.";
            default -> "Make strategic decisions based on the current battle state.";
        };
        
        return """
            You are %s, a %s in a tactical RPG combat.
            
            YOUR STATUS:
            - HP: %d/%d (%.0f%%)
            - Mana: %d/%d
            - Attack Power: %d, Defense: %d
            - Strategies: %s (attack), %s (defense)
            
            YOUR TEAM (allies):
            %s
            
            ENEMIES:
            %s
            
            AVAILABLE ACTIONS:
            1. attack <enemy_name> - Estimated damage: ~%d
            2. heal <ally_name> - Restores 30 HP
            
            TACTICAL GUIDANCE:
            - Focus fire: Attack wounded enemies to eliminate threats
            - Protect allies: Heal teammates below 30%% HP
            - Consider your role: %s
            - Prioritize finishing off low-HP enemies to reduce incoming damage
            - Save critically wounded allies (below 30%% HP) before they fall
            
            Respond ONLY with JSON (no markdown, no extra text):
            {
              "action": "attack" | "heal",
              "target": "character_name",
              "reasoning": "brief tactical explanation"
            }
            """.formatted(
                self.getName(), 
                self.getType(),
                self.getStats().health(), 
                self.getStats().maxHealth(),
                selfHealthPercent,
                self.getStats().mana(), 
                self.getStats().maxMana(),
                self.getStats().attackPower(), 
                self.getStats().defense(),
                self.getAttackStrategy().getClass().getSimpleName(),
                self.getDefenseStrategy().getClass().getSimpleName(),
                formatCharacterList(allies),
                formatCharacterList(enemies),
                estimatedDamage,
                roleAdvice
            );
    }

    /**
     * Formats a list of characters for display in the prompt.
     *
     * Helper method provided to students.
     */
    private String formatCharacterList(List<Character> characters) {
        StringBuilder sb = new StringBuilder();
        for (Character c : characters) {
            double healthPercent = (double) c.getStats().health() / c.getStats().maxHealth() * 100;
            sb.append(String.format("  - %s (%s): %d/%d HP (%.0f%%), %d ATK, %d DEF%n",
                c.getName(),
                c.getType(),
                c.getStats().health(),
                c.getStats().maxHealth(),
                healthPercent,
                c.getStats().attackPower(),
                c.getStats().defense()));
        }
        return sb.toString();
    }

    /**
     * Estimates damage this character would deal to a target.
     *
     * Helper method provided to students.
     */
    private int estimateDamage(Character attacker, Character target) {
        // Rough estimate using attack strategy
        int baseDamage = attacker.attack(target);
        return target.getDefenseStrategy()
            .calculateDamageReduction(target, baseDamage);
    }

    /**
     * Finds a character by name in a list.
     *
     * Helper method provided to students.
     */
    private Character findCharacterByName(String name, List<Character> characters) {
        return characters.stream()
            .filter(c -> c.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(characters.getFirst()); // Fallback to first if not found
    }

    /**
     * Record for parsing LLM JSON response.
     *
     * Uses Jackson annotations for JSON deserialization.
     * This is provided to students as a reference for JSON structure.
     */
    public record Decision(
        @JsonProperty(required = true) String action,
        @JsonProperty(required = true) String target,
        @JsonProperty String reasoning
    ) {}
}