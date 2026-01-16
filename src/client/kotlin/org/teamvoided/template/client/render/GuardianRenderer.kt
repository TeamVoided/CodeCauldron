package org.teamvoided.template.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.model.GuardianModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.teamvoided.template.Template.mc
import kotlin.math.acos
import kotlin.math.atan2

@Suppress("unused")
class GuardianRenderer(context: EntityRendererProvider.Context, f: Float, modelLayerLocation: ModelLayerLocation) :
    MobRenderer<Guardian, GuardianModel>(context, GuardianModel(context.bakeLayer(modelLayerLocation)), f) {
    constructor(context: EntityRendererProvider.Context) : this(context, 0.5f, ModelLayers.GUARDIAN)

    override fun shouldRender(guardian: Guardian, frustum: Frustum, d: Double, e: Double, f: Double): Boolean {
        if (super.shouldRender(guardian, frustum, d, e, f)) return true
        else {
            if (guardian.hasActiveAttackTarget()) {
                val target = guardian.activeAttackTarget
                if (target != null) {
                    val targetPos = getLerpedPos(target, target.bbHeight * 0.5, 1f)
                    val guardianPos = getLerpedPos(guardian, guardian.eyeHeight.toDouble(), 1f)
                    return frustum.isVisible(
                        AABB(guardianPos.x, guardianPos.y, guardianPos.z, targetPos.x, targetPos.y, targetPos.z)
                    )
                }
            }

            return false
        }
    }

    fun getLerpedPos(entity: Entity, yOffset: Double, partialTick: Float): Vec3 {
        val e = Mth.lerp(partialTick.toDouble(), entity.xOld, entity.x)
        val g = Mth.lerp(partialTick.toDouble(), entity.yOld, entity.y) + yOffset
        val h = Mth.lerp(partialTick.toDouble(), entity.zOld, entity.z)
        return Vec3(e, g, h)
    }

    override fun render(
        guardian: Guardian,
        f: Float,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        light: Int,
    ) {
        super.render(guardian, f, partialTick, poseStack, bufferSource, light)
        val target = guardian.activeAttackTarget
        if (target != null) {
            val animationTime = guardian.clientSideAttackTime + partialTick
            val yOffset = guardian.bbHeight * 0.5f
            poseStack.pushPose()
            poseStack.translate(0f, yOffset, 0f)
            val targetPos = getLerpedPos(target, target.bbHeight * 0.5, partialTick)
            val guardianPos = getLerpedPos(guardian, yOffset.toDouble(), partialTick)
            var dirVector = targetPos.subtract(guardianPos)
            val y = dirVector.length().toFloat() /* offset the beam to be behind target */ + 1f
            dirVector = dirVector.normalize()
            // Rot
            val xpRot = acos(dirVector.y.toFloat())
            val ypRot = atan2(dirVector.z, dirVector.x).toFloat()
            poseStack.mulPose(Axis.YP.rotationDegrees(((PI_F / 2f) - ypRot) * (180f / PI_F)))
            poseStack.mulPose(Axis.XP.rotationDegrees(xpRot * (180f / PI_F)))

            // Color calc
            val animScale = guardian.getAttackAnimationScale(partialTick)
            val colorMod = animScale * animScale
            val red = 64 + (colorMod * 191f).toInt()
            val green = 32 + (colorMod * 191f).toInt()
            val blue = 128 - (colorMod * 64f).toInt()

            // Pos Calc
            val partialTickedPos = animationTime * 0.05f * -1.5f
            val beamSize = 0.2f
            val x1 = Mth.cos(partialTickedPos + Math.PI.toFloat()) * beamSize
            val z1 = Mth.sin(partialTickedPos + Math.PI.toFloat()) * beamSize
            val x2 = Mth.cos(partialTickedPos + 0f) * beamSize
            val z2 = Mth.sin(partialTickedPos + 0f) * beamSize
            val x3 = Mth.cos(partialTickedPos + (Math.PI / 2).toFloat()) * beamSize
            val z3 = Mth.sin(partialTickedPos + (Math.PI / 2).toFloat()) * beamSize
            val x4 = Mth.cos(partialTickedPos + (Math.PI * 3.0 / 2.0).toFloat()) * beamSize
            val z4 = Mth.sin(partialTickedPos + (Math.PI * 3.0 / 2.0).toFloat()) * beamSize
            // UVs
            val u0 = 0.4999f // the UV is inverted for no reason
            val u1 = 0f

            val vOffset = animationTime * 0.5f % 1f
            val v1 = -1f + vOffset // can just be 0, no idea why its -1f

            val textureScale = 2.5f
            val v0 = y * textureScale + v1 // only has to add v1 if animated
            // Buffs
            val buffer = bufferSource.getBuffer(BEAM_RENDER_TYPE)
            val pose = poseStack.last()

            buffer.vertex(pose, x1, y, z1, red, green, blue, u0, v0)
            buffer.vertex(pose, x1, 0f, z1, red, green, blue, u0, v1)
            buffer.vertex(pose, x2, 0f, z2, red, green, blue, u1, v1)
            buffer.vertex(pose, x2, y, z2, red, green, blue, u1, v0)

            buffer.vertex(pose, x3, y, z3, red, green, blue, u0, v0)
            buffer.vertex(pose, x3, 0f, z3, red, green, blue, u0, v1)
            buffer.vertex(pose, x4, 0f, z4, red, green, blue, u1, v1)
            buffer.vertex(pose, x4, y, z4, red, green, blue, u1, v0)

            // Beam cap
            val capSize = 0.282f
            val capX1 = Mth.cos(partialTickedPos + (Math.PI * 3.0 / 4.0).toFloat()) * capSize
            val capZ1 = Mth.sin(partialTickedPos + (Math.PI * 3.0 / 4.0).toFloat()) * capSize
            val capX2 = Mth.cos(partialTickedPos + (Math.PI / 4).toFloat()) * capSize
            val capZ2 = Mth.sin(partialTickedPos + (Math.PI / 4).toFloat()) * capSize
            val capX4 = Mth.cos(partialTickedPos + (PI_F * 5f / 4f)) * capSize
            val capZ4 = Mth.sin(partialTickedPos + (PI_F * 5f / 4f)) * capSize
            val capX3 = Mth.cos(partialTickedPos + (PI_F * 7f / 4f)) * capSize
            val capZ3 = Mth.sin(partialTickedPos + (PI_F * 7f / 4f)) * capSize

            var modV = 0f
            if (guardian.tickCount % 2 == 0) {
                modV = 0.5f
            }

            buffer.vertex(pose, capX1, y, capZ1, red, green, blue, 0.5f, modV + 0.5f)
            buffer.vertex(pose, capX2, y, capZ2, red, green, blue, 1f, modV + 0.5f)
            buffer.vertex(pose, capX3, y, capZ3, red, green, blue, 1f, modV)
            buffer.vertex(pose, capX4, y, capZ4, red, green, blue, 0.5f, modV)

            poseStack.popPose()
        }
    }

    override fun getTextureLocation(guardian: Guardian?): ResourceLocation = GUARDIAN_LOCATION

    companion object {
        val GUARDIAN_LOCATION = mc("textures/entity/guardian.png")
        val GUARDIAN_BEAM_LOCATION = mc("textures/entity/guardian_beam.png")
        val BEAM_RENDER_TYPE: RenderType = RenderType.entityCutoutNoCull(GUARDIAN_BEAM_LOCATION)

        const val PI_F: Float = Math.PI.toFloat()
        fun VertexConsumer.vertex(
            pose: PoseStack.Pose,
            x: Float, y: Float, z: Float,
            r: Int, g: Int, b: Int,
            u: Float, v: Float,
        ) {
            addVertex(pose, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0f, 1f, 0f)
        }
    }
}