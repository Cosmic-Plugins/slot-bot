package me.randomhashtags.slotbot.util;

import com.sun.istack.internal.NotNull;
import org.bukkit.inventory.ItemStack;

public interface RPItemStack extends Versionable {
    default String asNMSCopy(@NotNull ItemStack itemstack) {
        if(EIGHT) {
            return org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack.asNMSCopy(itemstack).save(new net.minecraft.server.v1_8_R3.NBTTagCompound()).toString();
        } else if(NINE) {
            return org.bukkit.craftbukkit.v1_9_R2.inventory.CraftItemStack.asNMSCopy(itemstack).save(new net.minecraft.server.v1_9_R2.NBTTagCompound()).toString();
        } else if(TEN) {
            return org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack.asNMSCopy(itemstack).save(new net.minecraft.server.v1_10_R1.NBTTagCompound()).toString();
        } else if(ELEVEN) {
            return org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack.asNMSCopy(itemstack).save(new net.minecraft.server.v1_11_R1.NBTTagCompound()).toString();
        } else if(TWELVE) {
            return org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack.asNMSCopy(itemstack).save(new net.minecraft.server.v1_12_R1.NBTTagCompound()).toString();
        } else if(THIRTEEN) {
            return org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack.asNMSCopy(itemstack).save(new net.minecraft.server.v1_13_R2.NBTTagCompound()).toString();
        } else if(FOURTEEN) {
            return org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack.asNMSCopy(itemstack).save(new net.minecraft.server.v1_14_R1.NBTTagCompound()).toString();
        } else if(FIFTEEN) {
            return org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack.asNMSCopy(itemstack).save(new net.minecraft.server.v1_15_R1.NBTTagCompound()).toString();
        } else {
            return null;
        }
    }
}
