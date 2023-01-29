package toast.specialMobs.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

import toast.specialMobs.Properties;

public class EntityAILavaMonsterAttack extends EntityAIBase {

    /// Handy properties for this class.
    public static final int WINDUP = Properties.getInt(Properties.LAVAMONSTER_GENERAL, "lavamonster_attack_windup");
    public static final int SHOT_COUNT = Properties.getInt(Properties.LAVAMONSTER_GENERAL, "lavamonster_attack_shots");
    public static final int SPACING = Properties.getInt(Properties.LAVAMONSTER_GENERAL, "lavamonster_attack_spacing");
    public static final int COOLDOWN = Properties.getInt(Properties.LAVAMONSTER_GENERAL, "lavamonster_attack_cooldown");

    /// The entity (host) using this AI.
    public EntityLavaMonster lavaMonster;
    /// The host's target.
    public EntityLivingBase target;
    /// The host's moveSpeed.
    public double moveSpeed;
    /// Ticks until the host stops after seeing its target.
    public byte pathDelay = 0;
    /// The attack phase the host is currently in.
    public byte attackPhase = 0;

    public EntityAILavaMonsterAttack(EntityLavaMonster entity, double speed) {
        lavaMonster = entity;
        moveSpeed = speed;
        setMutexBits(3);
    }

    /// Whether this AI should run.
    @Override
    public boolean shouldExecute() {
        EntityLivingBase e = lavaMonster.getAttackTarget();
        if (e == null) return false;
        target = e;
        return true;
    }

    /// Whether this AI should continue running.
    @Override
    public boolean continueExecuting() {
        return shouldExecute() || !lavaMonster.getNavigator().noPath();
    }

    /// Called when this AI is stopped.
    @Override
    public void resetTask() {
        lavaMonster.setAttackTarget(null);
        target = null;
    }

    /// Called each tick while this AI is running.
    @Override
    public void updateTask() {
        double distanceSq = lavaMonster.getDistanceSq(target.posX, target.boundingBox.minY, target.posZ);
        boolean canSee = lavaMonster.getEntitySenses().canSee(target);
        if (canSee) pathDelay = (byte) Math.min(20, pathDelay + 1);
        else pathDelay = 0;
        if (distanceSq <= 225.0 && pathDelay >= 20) lavaMonster.getNavigator().clearPathEntity();
        else lavaMonster.getNavigator().tryMoveToEntityLiving(target, moveSpeed);
        lavaMonster.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        if (lavaMonster.attackDelay > 0) return;
        if (distanceSq > 225.0 || !canSee) {
            lavaMonster.attackDelay = 10;
            return;
        }
        if (++attackPhase == 1) {
            lavaMonster.attackDelay = WINDUP;
            lavaMonster.setBurningState(true);
            return;
        }
        lavaMonster.attackEntityWithFireball(target);
        if (attackPhase <= SHOT_COUNT) lavaMonster.attackDelay = SPACING;
        else {
            lavaMonster.attackDelay = COOLDOWN;
            attackPhase = 0;
            lavaMonster.setBurningState(false);
        }
    }
}
