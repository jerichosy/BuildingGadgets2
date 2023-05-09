package com.direwolf20.buildinggadgets2.client.events;

import com.direwolf20.buildinggadgets2.client.renderer.MyRenderMethods;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.VectorHelper;
import com.direwolf20.buildinggadgets2.util.modes.BuildToMe;
import com.direwolf20.buildinggadgets2.util.modes.StatePos;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

public class VBORenderer {
    private static VertexBuffer vertexBuffer;
    private static int buildCount;
    private static ArrayList<StatePos> statePosCache;

    public static void buildRender(RenderLevelStageEvent evt, Player player, ItemStack heldItem) {
        /*if (buildCount != 0) {
            buildCount++;
            if (buildCount >= 250)
                buildCount = 0;
            return;
        }
        buildCount++;
        */
        BlockState renderBlockState = GadgetNBT.getGadgetBlockState(heldItem);
        if (renderBlockState.isAir()) return;
        BuildToMe buildToMe = new BuildToMe();
        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, heldItem);
        ArrayList<StatePos> buildList = buildToMe.collect(lookingAt.getDirection(), player, lookingAt.getBlockPos(), renderBlockState);

        if (buildList.equals(statePosCache))
            return;
        Level level = player.level;
        //player.displayClientMessage(Component.literal("Rebuilding Render due to change." + level.getGameTime()), false);
        statePosCache = buildList;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        vertexBuffer = new VertexBuffer();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        PoseStack matrix = new PoseStack(); //Create a new matrix stack for use in the buffer building process
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        for (StatePos pos : buildList) {
            matrix.pushPose();
            matrix.translate(pos.pos.getX(), pos.pos.getY(), pos.pos.getZ());
            //matrix.translate(-0.005f, -0.005f, -0.005f);
            //matrix.scale(1.01f, 1.01f, 1.01f);

            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            int color = blockColors.getColor(renderBlockState, level, pos.pos, 0);
            float f = (float) (color >> 16 & 255) / 255.0F;
            float f1 = (float) (color >> 8 & 255) / 255.0F;
            float f2 = (float) (color & 255) / 255.0F;

            BakedModel ibakedmodel = dispatcher.getBlockModel(renderBlockState);
            for (Direction direction : Direction.values()) {
                if (!buildList.contains(new StatePos(renderBlockState, pos.pos.relative(direction))))
                    MyRenderMethods.renderModelBrightnessColorQuads(matrix.last(), bufferbuilder, f, f1, f2, 0.5f, ibakedmodel.getQuads(renderBlockState, direction, RandomSource.create(), ModelData.EMPTY, null), 15728640, 655360);
            }
            MyRenderMethods.renderModelBrightnessColorQuads(matrix.last(), bufferbuilder, f, f1, f2, 0.5f, ibakedmodel.getQuads(renderBlockState, null, RandomSource.create(), ModelData.EMPTY, null), 15728640, 655360);
            matrix.popPose();
        }

        vertexBuffer.bind();
        vertexBuffer.upload(bufferbuilder.end());
        VertexBuffer.unbind();
    }

    public static void drawRender(RenderLevelStageEvent evt, Player player, ItemStack heldItem) {
        if (vertexBuffer == null) {
            return;
        }
        BlockState renderBlockState = GadgetNBT.getGadgetBlockState(heldItem);
        if (renderBlockState.isAir()) return;
        BuildToMe buildToMe = new BuildToMe();
        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, heldItem);
        List<StatePos> buildList = buildToMe.collect(lookingAt.getDirection(), player, lookingAt.getBlockPos(), renderBlockState);
        if (buildList.isEmpty()) return;
        Vec3 projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        PoseStack matrix = evt.getPoseStack();
        matrix.pushPose();
        matrix.translate(-projectedView.x(), -projectedView.y(), -projectedView.z());
        matrix.translate(lookingAt.getBlockPos().getX(), lookingAt.getBlockPos().getY(), lookingAt.getBlockPos().getZ());
        vertexBuffer.bind();
        vertexBuffer.drawWithShader(matrix.last().pose(), new Matrix4f(evt.getProjectionMatrix()), RenderSystem.getShader());
        VertexBuffer.unbind();
        matrix.popPose();
    }
}
