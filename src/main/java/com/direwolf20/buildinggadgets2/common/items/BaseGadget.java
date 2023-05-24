package com.direwolf20.buildinggadgets2.common.items;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetModes;
import com.direwolf20.buildinggadgets2.api.gadgets.GadgetTarget;
import com.direwolf20.buildinggadgets2.setup.ModSetup;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.VectorHelper;
import com.direwolf20.buildinggadgets2.util.context.ItemActionContext;
import com.direwolf20.buildinggadgets2.util.modes.BaseMode;
import com.google.common.collect.ImmutableSortedSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;

public abstract class BaseGadget extends Item {
    public BaseGadget(Properties builder) {
        super(builder.tab(ModSetup.ITEM_GROUP)
                .stacksTo(1)
                .setNoRepair());
    }

    /**
     * Implementation level of for the onAction & onShiftAction methods below.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gadget = player.getItemInHand(hand);

        if (level.isClientSide()) //No client
            return InteractionResultHolder.success(gadget);

        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, ClipContext.Fluid.NONE);
        ItemActionContext context = new ItemActionContext(lookingAt.getBlockPos(), lookingAt, player, level, hand, gadget);

        if (player.isShiftKeyDown()) {
            return this.onShiftAction(context);
        }

        return this.onAction(context);
    }

    InteractionResultHolder<ItemStack> onAction(ItemActionContext context) {
        return InteractionResultHolder.pass(context.stack());
    }

    InteractionResultHolder<ItemStack> onShiftAction(ItemActionContext context) {
        return InteractionResultHolder.pass(context.stack());
    }

    /**
     * Rotates through the registered building modes, useful for key bindings.
     *
     * @param stack the gadget
     * @return the selected mode's id
     */
    public ResourceLocation rotateModes(ItemStack stack) {
        ImmutableSortedSet<BaseMode> modesForGadget = GadgetModes.INSTANCE.getModesForGadget(this.gadgetTarget());
        var arrayOfModes = new ArrayList<>(modesForGadget); // This is required to work with index's
        var currentMode = GadgetNBT.getMode(stack);

        var modeIndex = arrayOfModes.indexOf(currentMode);

        // Fix the mode or move it back to zero if the next index is outside of the list
        if (modeIndex == -1 || (++modeIndex > arrayOfModes.size())) {
            modeIndex = 0; // Use zero if for some reason we can't find the mode
        }

        var mode = arrayOfModes.get(modeIndex);
        GadgetNBT.setMode(stack, mode);

        return mode.getId();
    }

    public abstract GadgetTarget gadgetTarget();

    public static ItemStack getGadget(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof BaseGadget)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof BaseGadget)) {
                return ItemStack.EMPTY;
            }
        }
        return heldItem;
    }
}