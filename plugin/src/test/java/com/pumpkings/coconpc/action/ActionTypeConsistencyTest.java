package com.pumpkings.coconpc.action;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.NpcCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Guards the seam between the command that writes actions and the manager that runs them.
 *
 * <p>These are two independent lists, and they silently drifted apart once already:
 * {@code /coconpc action add connect ...} wrote {@code [connect]} while
 * {@code ConnectProcessor} registered itself as {@code [server]}, so the action was
 * accepted, saved, and then dispatched to nothing.
 */
@DisplayName("Action type registry consistency")
class ActionTypeConsistencyTest {

    private ActionsManager actionsManager;

    @BeforeEach
    void setUp() {
        // The default processors only stash the plugin reference in their constructors,
        // so a bare mock is enough to build the full registry.
        actionsManager = new ActionsManager(mock(CocoNPC.class));
    }

    /** The tags the command would actually write, derived the same way the command does. */
    private Set<String> tagsTheCommandCanWrite() {
        return NpcCommand.ACTION_TYPES.stream()
                .map(type -> "[" + type + "]")
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Test
    @DisplayName("every action type the command accepts can actually be dispatched")
    void everyAcceptedTypeIsDispatchable() {
        Set<String> dispatchable = new LinkedHashSet<>(actionsManager.getRegisteredTags());
        dispatchable.addAll(ActionsManager.getConditionTags());
        dispatchable.addAll(ActionsManager.getInterceptedTags());

        assertThat(tagsTheCommandCanWrite())
                .as("a type the command writes but nothing handles produces a saved, silently dead action")
                .allSatisfy(tag -> assertThat(dispatchable).contains(tag));
    }

    @Test
    @DisplayName("every registered processor is reachable from the command")
    void everyProcessorIsReachable() {
        assertThat(actionsManager.getRegisteredTags())
                .as("a processor the command refuses to write is unusable")
                .allSatisfy(tag -> assertThat(tagsTheCommandCanWrite()).contains(tag));
    }

    @Test
    @DisplayName("the network transfer processor is registered as [server]")
    void serverTagIsRegistered() {
        // Pinned explicitly: the docs and README both name this tag, and it is the one
        // that previously mismatched.
        assertThat(actionsManager.getRegisteredTags()).contains("[server]");
        assertThat(NpcCommand.ACTION_TYPES).contains("server");
    }

    @Test
    @DisplayName("all three conditions are offered by the command")
    void conditionsAreOffered() {
        assertThat(tagsTheCommandCanWrite()).containsAll(ActionsManager.getConditionTags());
    }

    @Test
    @DisplayName("tags are lowercase and bracketed, which is what dispatch matches on")
    void tagsAreNormalised() {
        assertThat(actionsManager.getRegisteredTags()).allSatisfy(tag -> {
            assertThat(tag).isEqualTo(tag.toLowerCase());
            assertThat(tag).startsWith("[").endsWith("]");
        });
    }

    @Test
    @DisplayName("the command offers no duplicate types")
    void noDuplicateTypes() {
        assertThat(NpcCommand.ACTION_TYPES).doesNotHaveDuplicates();
    }
}
