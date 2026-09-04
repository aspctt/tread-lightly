// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;

import com.aspctt.treadlightly.config.TreadLightlyConfig;
import com.aspctt.treadlightly.sound.generator.StepSoundContext;
import com.aspctt.treadlightly.sound.generator.StepSoundGenerator;
import com.aspctt.treadlightly.sound.player.DelayedSoundPlayer;
import com.aspctt.treadlightly.sound.player.ImmediateSoundPlayer;
import com.aspctt.treadlightly.sound.player.PlaybackSource;
import com.aspctt.treadlightly.sound.player.SoundPlayer;
import com.aspctt.treadlightly.util.PlayerUtil;
import com.aspctt.treadlightly.world.FootstepSolver;
import com.aspctt.treadlightly.world.Lookups;
import com.aspctt.treadlightly.world.Solver;

/** Drives the whole thing: holds what the packs loaded, and gives every nearby entity a frame. */
public class SoundEngine implements PlaybackSource {
    /** How far out to look for entities worth giving footsteps to. */
    private static final double RANGE = 16;
    private static final double RANGE_SQUARED = 256;

    private final TreadLightlyConfig config;
    private final SoundPlayer player;
    private final Solver solver;
    private final StepSoundContext stepContext;

    private Lookups lookups;

    public SoundEngine(TreadLightlyConfig config) {
        this.config = config;
        this.player = new DelayedSoundPlayer(new ImmediateSoundPlayer(this));
        this.solver = new FootstepSolver(this::getLookups, config);
        this.stepContext = new StepSoundContext(this::getLookups, config, solver);
        this.lookups = Lookups.empty(player, config, config::getStance);
    }

    public Lookups getLookups() {
        return lookups;
    }

    public StepSoundContext getStepContext() {
        return stepContext;
    }

    public TreadLightlyConfig getConfig() {
        return config;
    }

    /** Rebuilds everything from the enabled packs, or drops it all if the mod is switched off. */
    public void reload(ResourceManager manager) {
        lookups = config.isEnabled()
                ? Lookups.load(manager, player, config, config::getStance)
                : Lookups.empty(player, config, config::getStance);
    }

    public void reload() {
        reload(Minecraft.getInstance().getResourceManager());
    }

    public boolean isActive() {
        Minecraft client = Minecraft.getInstance();
        return lookups.hasData()
                && config.isEnabled()
                && (client.hasSingleplayerServer() || config.isEnabledInMultiplayer());
    }

    public boolean isRunning() {
        return !Minecraft.getInstance().isPaused() && isActive();
    }

    /**
     * Whether the game's own player sounds should be silenced because this mod is making them
     * instead. Turning off the replace setting gives you both, which is what it promises.
     */
    public boolean replacesVanillaSounds() {
        return config.isExclusive() && isActive();
    }

    public boolean isEnabledFor(Entity entity) {
        return isRunning() && config.getEntitySelector().test(entity);
    }

    @Override
    public float getVolumeFor(LivingEntity source) {
        float volume = config.getGlobalVolume() / 100F;

        if (source instanceof Player) {
            volume *= PlayerUtil.isClientPlayer(source) ? config.clientPlayer() : config.otherPlayer();
        } else if (source instanceof Monster) {
            volume *= config.hostile();
        } else {
            volume *= config.passive();
        }

        @Nullable StepSoundGenerator generator = getGeneratorFor(source);
        float running = generator == null ? 0 : generator.getMotionTracker().getSpeedScalingRatio(source);

        return volume * (1F + ((config.getRunningVolumeIncrease() / 100F) * running));
    }

    @Override
    @Nullable
    public StepSoundGenerator getGeneratorFor(LivingEntity entity) {
        return entity instanceof StepSoundSource source
                ? source.getStepGenerator(this).orElse(null)
                : null;
    }

    /** Gives every entity worth hearing a frame, then releases any sound whose delay elapsed. */
    public void onFrame(Entity cameraEntity) {
        if (!isRunning()) {
            return;
        }

        for (Entity entity : getTargets(cameraEntity)) {
            try {
                if (entity instanceof StepSoundSource source) {
                    source.getStepGenerator(this).ifPresent(StepSoundGenerator::generateFootsteps);
                }
            } catch (Throwable t) {
                throw new ReportedException(describe(t, entity));
            }
        }

        lookups.acoustics().think();
    }

    private CrashReport describe(Throwable t, Entity entity) {
        CrashReport report = CrashReport.forThrowable(t, "Generating footsteps for an entity");
        CrashReportCategory section = report.addCategory("Entity being ticked");
        entity.fillCrashReportCategory(section);
        section.setDetail("Gait", () -> String.valueOf(lookups.locomotions().lookup(entity)));
        section.setDetail("Stood on as an entity", () -> String.valueOf(lookups.golems().contains(entity.getType())));
        return report;
    }

    /**
     * Nearby entities that should make footsteps.
     * <p>
     * Excludes anything that does not walk, anything being ridden or slept in, and anything the
     * golem map says is walked on rather than walking. Past the configured ceiling it keeps
     * players and the nearest of each distinct type per block, so a packed mob farm costs a
     * bounded amount rather than one solve per mob.
     */
    private List<? extends Entity> getTargets(Entity cameraEntity) {
        List<? extends Entity> entities = cameraEntity.level().getEntities((Entity) null,
                cameraEntity.getBoundingBox().inflate(RANGE),
                e -> e instanceof LivingEntity living
                        && !(e instanceof WaterAnimal)
                        && !(e instanceof FlyingMob)
                        && !(e instanceof Shulker || e instanceof ArmorStand
                                || e instanceof Boat || e instanceof AbstractMinecart)
                        && !lookups.golems().contains(e.getType())
                        && !e.isPassenger()
                        && !living.isSleeping()
                        && !(e instanceof Player && e.isSpectator())
                        && e.distanceToSqr(cameraEntity) <= RANGE_SQUARED
                        && config.getEntitySelector().test(e));

        int limit = config.getMaxSteppingEntities();
        if (entities.size() < limit) {
            return entities;
        }

        Set<Integer> seen = new IntOpenHashSet();
        return entities.stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(cameraEntity)))
                .filter(e -> e == cameraEntity
                        || e instanceof Player
                        || (seen.size() < limit && seen.add(Objects.hash(e.getType(), e.blockPosition()))))
                .toList();
    }

    /**
     * Whether the game's own footstep for this entity should be suppressed because this mod is
     * about to make its own.
     * <p>
     * Hosting a world with other players in it leaves the game's footsteps alone: the sounds
     * reaching those players come from this machine, and silencing them here would silence
     * them for everybody.
     */
    public boolean isStepBlocked(LivingEntity entity, StepSoundSource source) {
        Minecraft client = Minecraft.getInstance();
        if (!client.hasSingleplayerServer() && client.isLocalServer()) {
            return true;
        }
        if (!config.isExclusive() && !(entity instanceof Player)) {
            return false;
        }
        return isEnabledFor(entity) && source.getStepGenerator(this).isPresent();
    }
}
