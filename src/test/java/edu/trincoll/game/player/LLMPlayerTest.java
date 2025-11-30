package edu.trincoll.game.player;

import edu.trincoll.game.command.AttackCommand;
import edu.trincoll.game.command.GameCommand;
import edu.trincoll.game.command.HealCommand;
import edu.trincoll.game.factory.CharacterFactory;
import edu.trincoll.game.model.Character;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LLMPlayerTest {

    @Mock
    private ChatClient chatClient;
    
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    
    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    private LLMPlayer player;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        player = new LLMPlayer(chatClient, "TestModel");
        gameState = GameState.initial();
    }

    @Test
    @DisplayName("Should parse attack decision correctly")
    void shouldParseAttackDecision() {
        // Setup mocks
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        
        LLMPlayer.Decision decision = new LLMPlayer.Decision("attack", "Enemy", "Attack reasoning");
        when(responseSpec.entity(LLMPlayer.Decision.class)).thenReturn(decision);

        // Setup game state
        Character self = CharacterFactory.createWarrior("Self");
        Character enemy = CharacterFactory.createArcher("Enemy");
        
        GameCommand command = player.decideAction(
            self,
            List.of(self),
            List.of(enemy),
            gameState
        );

        assertThat(command).isInstanceOf(AttackCommand.class);
        assertThat(command.getDescription()).contains("Enemy");
    }

    @Test
    @DisplayName("Should parse heal decision correctly")
    void shouldParseHealDecision() {
        // Setup mocks
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        
        LLMPlayer.Decision decision = new LLMPlayer.Decision("heal", "Self", "Heal reasoning");
        when(responseSpec.entity(LLMPlayer.Decision.class)).thenReturn(decision);

        // Setup game state
        Character self = CharacterFactory.createWarrior("Self");
        Character enemy = CharacterFactory.createArcher("Enemy");
        
        GameCommand command = player.decideAction(
            self,
            List.of(self),
            List.of(enemy),
            gameState
        );

        assertThat(command).isInstanceOf(HealCommand.class);
        assertThat(((HealCommand) command).getDescription()).contains("Self");
    }
    
    @Test
    @DisplayName("Should fallback to default action on error")
    void shouldFallbackOnError() {
        // Setup mocks to throw exception
        when(chatClient.prompt()).thenThrow(new RuntimeException("API Error"));

        // Setup game state
        Character self = CharacterFactory.createWarrior("Self");
        Character enemy = CharacterFactory.createArcher("Enemy");
        
        GameCommand command = player.decideAction(
            self,
            List.of(self),
            List.of(enemy),
            gameState
        );

        // Should fallback to attack first enemy
        assertThat(command).isInstanceOf(AttackCommand.class);
        assertThat(command.getDescription()).contains("Enemy");
    }
}
