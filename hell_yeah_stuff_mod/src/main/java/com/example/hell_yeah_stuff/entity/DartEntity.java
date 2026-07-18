package com.example.hell_yeah_stuff.entity;

import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Обычный дротик — базовый боеприпас мини-арбалета. */
public class DartEntity extends AbstractArrow {

    private static final double DAMAGE = 4.0D;

    public DartEntity(EntityType<? extends DartEntity> type, Level level) {
        super(type, level);
    }

    public DartEntity(Level level, LivingEntity shooter, ItemStack pickupStack, @Nullable ItemStack firedFromWeapon) {
        super(ModEntities.DART.get(), shooter, level, pickupStack, firedFromWeapon);
        this.setBaseDamage(DAMAGE);
    }

    public DartEntity(Level level, double x, double y, double z, ItemStack pickupStack) {
        super(ModEntities.DART.get(), x, y, z, level, pickupStack, null);
        this.setBaseDamage(DAMAGE);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.DART.get());
    }
}
