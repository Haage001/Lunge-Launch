package haage.lungelaunch;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LungeLaunch implements ModInitializer {
	public static final String MOD_ID = "lunge-launch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	// Config: Delay in milliseconds before activating elytra after attack
	// Default is 0 (instant activation). Can be configured via ModMenu/Cloth Config
	public static int ACTIVATION_DELAY_MS = 0;

	@Override
	public void onInitialize() {
		LOGGER.info("Lunge Launch mod initialized!");
		
		// Register a ticker that checks for attacks every tick
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				// Check if player is attacking (handSwinging means they just attacked)
				if (player.handSwinging) {
					// Check if player is not on the ground
					if (player.isOnGround()) {
						continue;
					}
					
					// Check if player has elytra equipped
					ItemStack chestStack = player.getEquippedStack(EquipmentSlot.CHEST);
					if (!chestStack.isOf(Items.ELYTRA)) {
						continue;
					}
					
					// Check if player is holding a spear
					ItemStack mainHandStack = player.getMainHandStack();
					boolean isSpear = mainHandStack.isOf(Items.WOODEN_SPEAR) ||
					                  mainHandStack.isOf(Items.STONE_SPEAR) ||
					                  mainHandStack.isOf(Items.IRON_SPEAR) ||
					                  mainHandStack.isOf(Items.GOLDEN_SPEAR) ||
					                  mainHandStack.isOf(Items.DIAMOND_SPEAR) ||
					                  mainHandStack.isOf(Items.NETHERITE_SPEAR) ||
					                  mainHandStack.isOf(Items.COPPER_SPEAR);
					
					if (!isSpear) {
						continue;
					}
					
					// Check for Lunge enchantment (any level)
					ItemEnchantmentsComponent enchantments = mainHandStack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
					int lungeLevel = enchantments.getLevel(player.getRegistryManager()
						.getOrThrow(RegistryKeys.ENCHANTMENT)
						.getOrThrow(Enchantments.LUNGE));
					
					if (lungeLevel < 1) {
						continue;
					}
					
					// All conditions met - activate elytra with optional delay
					if (ACTIVATION_DELAY_MS > 0) {
						// Schedule delayed activation
						final ServerPlayerEntity finalPlayer = player;
						new Thread(() -> {
							try {
								Thread.sleep(ACTIVATION_DELAY_MS);
								server.execute(() -> {
									((haage.lungelaunch.mixin.EntityAccessor) finalPlayer).invokeSetFlag(7, true);
								});
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}).start();
					} else {
						// Instant activation (default)
						((haage.lungelaunch.mixin.EntityAccessor) player).invokeSetFlag(7, true);
					}
				}
			}
		});
	}
}