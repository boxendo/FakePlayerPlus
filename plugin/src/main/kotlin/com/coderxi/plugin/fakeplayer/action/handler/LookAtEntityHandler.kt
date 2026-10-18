package com.coderxi.plugin.fakeplayer.action.handler

import com.coderxi.plugin.fakeplayer.action.base.CommonActionHandler
import com.coderxi.plugin.fakeplayer.action.base.CommonActionMode
import com.coderxi.plugin.fakeplayer.action.type.LookAtEntityAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.utils.messages.sendLocalizedMessage
import io.papermc.paper.entity.LookAnchor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity

object LookAtEntityHandler: CommonActionHandler<LookAtEntityAction>(
    type = LookAtEntityAction::class.java,
    name = "look_at_entity",
    modes = setOf(CommonActionMode.ONCE, CommonActionMode.CONTINUOUS)
) {
    override fun runOnce(fakePlayer: FakePlayer, action: LookAtEntityAction) {
        val player = fakePlayer.player
        val center = player.location
        // Keep this in sync with the attack action: a target that is attackable
        // at 4.5/5.0 blocks must also be "lookable".
        val radius = if (player.gameMode == GameMode.CREATIVE) 5.0 else 4.5
        val entities = center.getNearbyEntitiesByType(Damageable::class.java, radius)
        if (entities.size > 150) {
            fakePlayer.actions.stop(action)
            fakePlayer.owners.forEach { Bukkit.getPlayer(it.uuid)?.sendLocalizedMessage("fakeplayer.action.look-at-entity.stop.too-many-entities", it.name, entities.size) }
            return
        }
        var nearestEntity: Entity? = null
        var nearestEntityDistance = 0.0
        for (entity in entities) {
            if (entity === player) {
                continue
            }
            val distance = entity.location.distanceSquared(center)
            // getNearbyEntitiesByType uses a cube; reject the corners outside reach.
            if (distance > radius * radius) {
                continue
            }
            if (nearestEntity == null || distance < nearestEntityDistance) {
                nearestEntity = entity
                nearestEntityDistance = distance
            }
        }
        if (nearestEntity != null) {
            player.lookAt(nearestEntity, LookAnchor.EYES, LookAnchor.EYES)
        }
    }

    override fun runIntervalTick(fakePlayer: FakePlayer, action: LookAtEntityAction) {
        throw UnsupportedOperationException()
    }

    override fun runContinuousTick(fakePlayer: FakePlayer, action: LookAtEntityAction) {
        if (action.freezeTick > 0) {
            action.freezeTick--
            return
        }
        runOnce(fakePlayer, action)
        action.freezeTick = 3
    }

}
