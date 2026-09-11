package toast.specialMobs.spiceOfLife;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import squeek.spiceoflife.items.ItemFoodContainer;

public class SpiceOfLifeCompat {

    public static void addFoodSources(EntityPlayer player, List<IInventory> sources) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemFoodContainer
                    && stack != player.inventory.getCurrentItem()) {
                sources.add(((ItemFoodContainer) stack.getItem()).getInventory(stack));
            }
        }
    }
}
