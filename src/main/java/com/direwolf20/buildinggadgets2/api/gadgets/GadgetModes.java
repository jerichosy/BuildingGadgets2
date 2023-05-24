package com.direwolf20.buildinggadgets2.api.gadgets;

import com.direwolf20.buildinggadgets2.util.modes.BaseMode;
import com.direwolf20.buildinggadgets2.util.modes.BuildToMe;
import com.google.common.collect.ImmutableSortedSet;

import java.util.HashMap;
import java.util.LinkedHashSet;

public enum GadgetModes {
    INSTANCE;

    private final HashMap<GadgetTarget, LinkedHashSet<BaseMode>> gadgetModes = new HashMap<>();

    GadgetModes() {
        setupDefaultModes();
    }

    /**
     * Building gadgets comes with the following default modes, you can not remove these, only add to them
     * using the {@link #registerMode(GadgetTarget, BaseMode)} method
     */
    private void setupDefaultModes() {
        var modes = new HashMap<GadgetTarget, LinkedHashSet<BaseMode>>();

        // Building Gadget
        modes.put(GadgetTarget.BUILDING, new LinkedHashSet<>() {{
            add(new BuildToMe());
        }});

        // Exchanging Gadget
        modes.put(GadgetTarget.EXCHANGING, new LinkedHashSet<>() {{
        }});

        modes.put(GadgetTarget.DESTRUCTION, new LinkedHashSet<>() {{
        }});

        this.gadgetModes.putAll(modes);
    }

    /**
     * Register a given mode to a given gadget
     *
     * @param target the target gadget
     * @param mode   the mode you want to register
     */
    public boolean registerMode(GadgetTarget target, BaseMode mode) {
        return this.gadgetModes
                .computeIfAbsent(target, key -> new LinkedHashSet<>())
                .add(mode);
    }

    /**
     * Get an immutable set of building modes for any given gadget target
     */
    public ImmutableSortedSet<BaseMode> getModesForGadget(GadgetTarget target) {
        return ImmutableSortedSet.copyOf(gadgetModes.computeIfAbsent(target, key -> new LinkedHashSet<>()));
    }
}