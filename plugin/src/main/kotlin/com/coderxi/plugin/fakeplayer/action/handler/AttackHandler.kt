package com.coderxi.plugin.fakeplayer.action.handler

import com.coderxi.plugin.fakeplayer.action.base.CommonActionHandler
import com.coderxi.plugin.fakeplayer.action.base.CommonActionMode
import com.coderxi.plugin.fakeplayer.action.type.AttackAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import org.bukkit.FluidCollisionMode
import org.bukkit.GameMode
import kotlin.math.ceil

object AttackHandler : CommonActionHandler<AttackAction>(
    type = AttackAction::class.java,
    name = "attack",
    modes = CommonActionMode.ALL
) {

    override fun runOnce(fakePlayer: FakePlayer, action: AttackAction) {
        val player = fakePlayer.player
        val reach = if (player.gameMode == GameMode.CREATIVE) 5.0 else 4.5

        val eye = player.eyeLocation
        val eyePos = eye.toVector()
        val direction = eye.direction

        // Blocks first, then entities. Entities behind the block hit are ignored,
        // matching the upstream Tracer implementation.
        val blockHit = player.world.rayTraceBlocks(eye, direction, reach, FluidCollisionMode.NEVER, false)
        val blockDistanceSq = blockHit?.hitPosition?.distanceSquared(eyePos) ?: (reach * reach)

        // getTargetEntity only accepts an int; reject hits outside the real reach below.
        val entityHit = player.rayTraceEntities(ceil(reach).toInt(), true) ?: return
        val target = entityHit.hitEntity ?: return
        val entityDistanceSq = entityHit.hitPosition.distanceSquared(eyePos)
        if (entityDistanceSq > reach * reach || entityDistanceSq >= blockDistanceSq) {
            return
        }

        player.attack(target)
        player.swingMainHand()
        runCatching { player.resetCooldown() }
        fakePlayer.nms.resetLastActionTime()
    }

    override fun runIntervalTick(fakePlayer: FakePlayer, action: AttackAction) {
        runOnce(fakePlayer, action)
    }

    override fun runContinuousTick(fakePlayer: FakePlayer, action: AttackAction) {
        runOnce(fakePlayer, action)
    }

}
