package com.github.tartaricacid.maidsconstruct.util;

import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatBubbleManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

public class SmelteryBubbles {
    public static final String[] FUELING_BUBBLES = {
            "chat.maidsconstruct.state.fueling.0",
            "chat.maidsconstruct.state.fueling.1",
            "chat.maidsconstruct.state.fueling.2"
    };
    public static final String[] INTERACTING_BUBBLES = {
            "chat.maidsconstruct.state.interacting.0",
            "chat.maidsconstruct.state.interacting.1",
            "chat.maidsconstruct.state.interacting.2"
    };
    public static final String[] POURING_BUBBLES = {
            "chat.maidsconstruct.state.pouring.0",
            "chat.maidsconstruct.state.pouring.1",
            "chat.maidsconstruct.state.pouring.2"
    };
    public static final String[] COLLECTING_BUBBLES = {
            "chat.maidsconstruct.state.collecting.0",
            "chat.maidsconstruct.state.collecting.1",
            "chat.maidsconstruct.state.collecting.2"
    };
    public static final String[] WAITING_MELT_BUBBLES = {
            "chat.maidsconstruct.state.waiting_melt.0",
            "chat.maidsconstruct.state.waiting_melt.1",
            "chat.maidsconstruct.state.waiting_melt.2"
    };
    public static final String[] WAITING_CAST_BUBBLES = {
            "chat.maidsconstruct.state.waiting_cast.0",
            "chat.maidsconstruct.state.waiting_cast.1",
            "chat.maidsconstruct.state.waiting_cast.2"
    };
    public static final String[] NO_CAST_BUBBLES = {
            "chat.maidsconstruct.no_cast.0",
            "chat.maidsconstruct.no_cast.1",
            "chat.maidsconstruct.no_cast.2"
    };
    public static final String[] INVENTORY_FULL_BUBBLES = {
            "chat.maidsconstruct.inventory_full.0",
            "chat.maidsconstruct.inventory_full.1",
            "chat.maidsconstruct.inventory_full.2"
    };
    public static final String[] COLLECT_SUCCESS_BUBBLES = {
            "chat.maidsconstruct.collect_success.0",
            "chat.maidsconstruct.collect_success.1",
            "chat.maidsconstruct.collect_success.2"
    };

    public static final String NO_FUEL_BUBBLE = "chat.maidsconstruct.no_fuel";
    public static final String NO_ITEMS_BUBBLE = "chat.maidsconstruct.no_items";

    private static final int MAX_CHAT_BUBBLES = 2;

    public static String randomBubble(EntityMaid maid, String[] pool) {
        return pool[maid.getRandom().nextInt(pool.length)];
    }

    public static void addBubbleIfNotTooMany(EntityMaid maid, String langKey) {
        ChatBubbleManager manager = maid.getChatBubbleManager();
        if (manager.getChatBubbleDataCollection().size() < MAX_CHAT_BUBBLES) {
            manager.addTextChatBubble(langKey);
        }
    }

    public static void showStateBubble(EntityMaid maid, SmelteryWorkState state) {
        String[] pool = switch (state) {
            case FUELING -> FUELING_BUBBLES;
            case INTERACTING -> INTERACTING_BUBBLES;
            case POURING -> POURING_BUBBLES;
            case COLLECTING -> COLLECTING_BUBBLES;
            case WAITING_MELT -> WAITING_MELT_BUBBLES;
            case WAITING_CAST -> WAITING_CAST_BUBBLES;
            default -> null;
        };
        if (pool != null) {
            addBubbleIfNotTooMany(maid, randomBubble(maid, pool));
        }
    }

    public static void showNoFuelBubble(EntityMaid maid) {
        addBubbleIfNotTooMany(maid, NO_FUEL_BUBBLE);
    }

    public static void showNoItemsBubble(EntityMaid maid) {
        addBubbleIfNotTooMany(maid, NO_ITEMS_BUBBLE);
    }
}
