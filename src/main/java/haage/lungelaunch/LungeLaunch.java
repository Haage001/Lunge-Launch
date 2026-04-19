package haage.lungelaunch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import haage.lungelaunch.config.LungeLaunchConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class LungeLaunch implements ClientModInitializer {
	public static final String MOD_ID = "lunge-launch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private boolean wasAttackPressed = false;
	private static LungeLaunchConfig config = null;
	private static boolean clothConfigLoaded = false;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Lunge Launch mod initialized (client-side)");
		if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
			try {
				AutoConfig.register(LungeLaunchConfig.class, GsonConfigSerializer::new);
				config = AutoConfig.getConfigHolder(LungeLaunchConfig.class).getConfig();
				clothConfigLoaded = true;
				LOGGER.info("Cloth Config integration enabled");
			} catch (Exception e) {
				LOGGER.warn("Failed to enable Cloth Config integration: {}", e.getMessage());
			}
		}

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			LocalPlayer player = client.player;
			if (player == null) {
				wasAttackPressed = false;
				return;
			}

			boolean isAttackPressed = client.options.keyAttack.isDown();
			if (isAttackPressed && !wasAttackPressed) {
				if (player.onGround()) {
					wasAttackPressed = isAttackPressed;
					return;
				}

				boolean lungeWhileFlyingEnabled = clothConfigLoaded && config != null ? config.crouchToCancelFlight : false;
				if (!lungeWhileFlyingEnabled && player.isFallFlying()) {
					wasAttackPressed = isAttackPressed;
					return;
				}

				boolean crouchPreventsFlightEnabled = clothConfigLoaded && config != null ? config.crouchToPreventFlight : true;
				if (crouchPreventsFlightEnabled && player.isCrouching()) {
					wasAttackPressed = isAttackPressed;
					return;
				}

				ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
				if (!chestStack.is(Items.ELYTRA)) {
					wasAttackPressed = isAttackPressed;
					return;
				}

				ItemStack mainHandStack = player.getMainHandItem();
				boolean isSpear = mainHandStack.is(Items.WOODEN_SPEAR)
					|| mainHandStack.is(Items.STONE_SPEAR)
					|| mainHandStack.is(Items.IRON_SPEAR)
					|| mainHandStack.is(Items.GOLDEN_SPEAR)
					|| mainHandStack.is(Items.DIAMOND_SPEAR)
					|| mainHandStack.is(Items.NETHERITE_SPEAR)
					|| mainHandStack.is(Items.COPPER_SPEAR);
				if (!isSpear) {
					wasAttackPressed = isAttackPressed;
					return;
				}

				int lungeLevel = EnchantmentHelper.getItemEnchantmentLevel(
					player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LUNGE),
					mainHandStack
				);
				if (lungeLevel < 1) {
					wasAttackPressed = isAttackPressed;
					return;
				}

				int delayMs = clothConfigLoaded && config != null ? config.activationDelayMs : 0;
				if (delayMs > 0) {
					LocalPlayer finalPlayer = player;
					new Thread(() -> {
						try {
							Thread.sleep(delayMs);
							if (finalPlayer.connection != null) {
								finalPlayer.connection.send(new ServerboundPlayerCommandPacket(finalPlayer, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
							}
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}).start();
					LOGGER.debug("Scheduled delayed Elytra activation ({} ms, Lunge level: {})", delayMs, lungeLevel);
				} else if (player.connection != null) {
					player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
					LOGGER.debug("Elytra activated after lunge spear attack (Lunge level: {})", lungeLevel);
				}
			}

			wasAttackPressed = isAttackPressed;
		});
	}
}



