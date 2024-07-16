package boats.jojo.grindbot;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.io.ByteArrayOutputStream;

import boats.jojo.grindbot.structs.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

@Mod(modid = "keystrokesmod", name = "gb", version = "1.13", acceptedMinecraftVersions = "1.8.9")
public class GrindBot {
	@EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
		ClientCommandHandler.instance.registerCommand(new KeyCommand());
	}

	@EventHandler
	public void serverLoad(FMLServerStartingEvent event) {
		event.registerServerCommand(new KeyCommand());
	}

	static Base64.Encoder base64encoder = Base64.getEncoder();
	static Base64.Decoder base64decoder = Base64.getDecoder();

	Minecraft mcInstance = Minecraft.getMinecraft();

	String apiUrl = "http://127.0.0.1:8080/grinder";
	// String apiUrl = "http://127.0.0.1:5000/api/grinder"; // testing url

	boolean loggingEnabled = true;

	float curFps = 0;

	double mouseTargetX = 0;
	double mouseTargetY = 0;
	double mouseTargetZ = 0;

	boolean attackedThisTick = false;

	String curTargetName = "null";
	String[] nextTargetNames = null;

	static String apiKey = "null";

	int minimumFps = 0;

	int ticksPerApiCall = 200;

	float initialFov = 120;
	float fovWhenGrinding = 120;

	double curSpawnLevel = 999;

	long lastCalledApi = 0;
	long lastReceivedApiResponse = 0;

	int apiLastPing = 0;
	int apiLastTotalProcessingTime = 0;

	long lastTickTime = 0;

	List<String> chatMsgs = new ArrayList<>();
	String importantChatMsg = "";

	double keyChanceForwardDown = 0; // make good
	double keyChanceForwardUp = 0;

	double keyChanceSideDown = 0;
	double keyChanceSideUp = 0;

	double keyChanceBackwardDown = 0;
	double keyChanceBackwardUp = 0;

	double keyChanceJumpDown = 0;
	double keyChanceJumpUp = 0;

	double keyChanceCrouchDown = 0;
	double keyChanceCrouchUp = 0;

	double keyChanceSprintDown = 0;
	double keyChanceSprintUp = 0;

	double keyChanceUseDown = 0;
	double keyChanceUseUp = 0;

	double keyAttackChance = 0;

	double mouseSpeed = 0;

	boolean autoClickerEnabled = false;
	long lastToggledAutoClicker = 0;

	boolean grinderEnabled = false;
	long lastToggledGrinder = 0;

	long preApiProcessingTime = 0;

	String apiMessage = "null";

	double mouseVelX, mouseVelY;
	long lastMouseUpdate;

	Gson gson = new GsonBuilder().serializeNulls().create();

	@SubscribeEvent
	public void onKeyPress(InputEvent.KeyInputEvent event) {
		long curTime = System.currentTimeMillis();

		long toggledGrinderTimeDiff = curTime - lastToggledGrinder;

		if (toggledGrinderTimeDiff > 500 && org.lwjgl.input.Keyboard.isKeyDown(Keyboard.KEY_J)) {
			grinderEnabled = !grinderEnabled;

			if (grinderEnabled) { // newly enabled
				initialFov = mcInstance.gameSettings.fovSetting;
				chatMsgs.clear(); // reset list of chat messages to avoid picking up ones received earlier
			} else { // newly disabled
				allKeysUp();
				mcInstance.gameSettings.fovSetting = initialFov;
			}

			lastToggledGrinder = curTime;
		}

		long toggledAutoClickerTimeDiff = curTime - lastToggledAutoClicker;

		if (toggledAutoClickerTimeDiff > 500 && org.lwjgl.input.Keyboard.isKeyDown(Keyboard.KEY_K)) {
			autoClickerEnabled = !autoClickerEnabled;

			lastToggledAutoClicker = curTime;
		}
	}

	@SubscribeEvent
	public void overlayFunc(RenderGameOverlayEvent.Post event) {
		long curTime = System.currentTimeMillis();
		try {
			if (event.type == ElementType.HEALTH) {
				return;
			}
			if (event.type == ElementType.ARMOR) {
				return;
			}

			interpolateMousePosition();

			int screenWidth = event.resolution.getScaledWidth();
			int screenHeight = event.resolution.getScaledHeight();

			String[][] infoToDraw = {
					{ "Username", mcInstance.thePlayer.getName() },
					{ "FPS", Integer.toString((int) curFps) },
					{ "API time", apiLastTotalProcessingTime + "ms" },
					{ "AutoClicker", autoClickerEnabled ? "ENABLED" : "disabled" },
					{ "X", Double.toString(Math.round(mcInstance.thePlayer.posX * 10.0) / 10.0) },
					{ "Y", Double.toString(Math.round(mcInstance.thePlayer.posY * 10.0) / 10.0) },
					{ "Z", Double.toString(Math.round(mcInstance.thePlayer.posZ * 10.0) / 10.0) },
					{ "API msg", apiMessage },
			};

			for (int i = 0; i < infoToDraw.length; i++) {
				String[] curInfo = infoToDraw[i];

				drawText(curInfo[0] + ": " + curInfo[1], 4, 4 + i * 10);
			}

			int drawKeyboardPositionX = screenWidth - 77;
			int drawKeyboardPositionY = screenHeight - 60;

			if (mcInstance.gameSettings.keyBindForward.isKeyDown()) { // W
				drawText("W", drawKeyboardPositionX + 41, drawKeyboardPositionY + 4);
			}

			if (mcInstance.gameSettings.keyBindBack.isKeyDown()) { // S
				drawText("S", drawKeyboardPositionX + 41, drawKeyboardPositionY + 22);
			}

			if (mcInstance.gameSettings.keyBindLeft.isKeyDown()) { // A
				drawText("A", drawKeyboardPositionX + 23, drawKeyboardPositionY + 22);
			}

			if (mcInstance.gameSettings.keyBindRight.isKeyDown()) { // D
				drawText("D", drawKeyboardPositionX + 59, drawKeyboardPositionY + 22);
			}

			if (mcInstance.gameSettings.keyBindSneak.isKeyDown()) { // Shift
				drawText("Sh", drawKeyboardPositionX + 2, drawKeyboardPositionY + 22);
			}

			if (mcInstance.gameSettings.keyBindSprint.isKeyDown()) { // Ctrl
				drawText("Ct", drawKeyboardPositionX + 3, drawKeyboardPositionY + 40);
			}

			if (mcInstance.gameSettings.keyBindJump.isKeyDown()) { // Space
				drawText("Space", drawKeyboardPositionX + 28, drawKeyboardPositionY + 40);
			}

			if (mcInstance.gameSettings.keyBindAttack.isKeyDown() || attackedThisTick) { // Mouse1
				drawText("LM", drawKeyboardPositionX + 2, drawKeyboardPositionY + 4);
			}

			if (mcInstance.gameSettings.keyBindUseItem.isKeyDown()) { // Mouse2
				drawText("RM", drawKeyboardPositionX + 20, drawKeyboardPositionY + 4);
			}

			// bot controlling

			// get fps

			curFps = Minecraft.getDebugFPS();

			// bot tick handler

			long tickTimeDiff = curTime - lastTickTime;

			if (grinderEnabled && curTime - (lastReceivedApiResponse - apiLastTotalProcessingTime) >= 1000 // 1000ms per
																											// api call
					&& curTime - lastCalledApi >= 500 // absolute minimum time to avoid spamming before any responses
														// received
			) {
				lastCalledApi = curTime;
				callBotApi();
			}

			if (tickTimeDiff < 1000 / 20) { // 20 ticks per second
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// doing bot tick

		lastTickTime = curTime;

		attackedThisTick = false;

		if (grinderEnabled) {
			mcInstance.gameSettings.fovSetting = fovWhenGrinding;
			doBotTick();
		}
	}

	@SubscribeEvent
	public void onChat(ClientChatReceivedEvent event) {
		String curChatRaw = StringUtils.stripControlCodes(event.message.getUnformattedText());

		curChatRaw = new String(curChatRaw.getBytes(), StandardCharsets.UTF_8); // probably unnecessary

		// idk what the first thing is for `!curChatRaw.startsWith(":")`
		if (!curChatRaw.startsWith(":") && (curChatRaw.startsWith("MAJOR EVENT!")
				|| curChatRaw.startsWith("BOUNTY CLAIMED!") || curChatRaw.startsWith("NIGHT QUEST!")
				|| curChatRaw.startsWith("QUICK MATHS!") || curChatRaw.startsWith("DONE!")
				|| curChatRaw.startsWith("MINOR EVENT!") || curChatRaw.startsWith("MYSTIC ITEM!")
				|| curChatRaw.startsWith("PIT LEVEL UP!") || curChatRaw.startsWith("A player has"))) {
			importantChatMsg = curChatRaw;
		}

		chatMsgs.add(curChatRaw);
	}

	public void doBotTick() {
		try {
			// go afk if fps too low (usually when world is loading)

			if (curFps < minimumFps) {
				goAfk();
				apiMessage = "fps too low";
				return;
			}

			// main things

			long timeSinceReceivedApiResponse = System.currentTimeMillis() - lastReceivedApiResponse;

			if (timeSinceReceivedApiResponse > 3000) {
				allKeysUp();

				if (Math.floor((float) timeSinceReceivedApiResponse / 50) % 20 == 0) {
					goAfk();
					String issueStr = "too long since successful api response: " + timeSinceReceivedApiResponse
							+ "ms. last api ping: " + apiLastPing + "ms. last api time: " + apiLastTotalProcessingTime
							+ " ms.";
					apiMessage = issueStr;
					System.out.println(issueStr);
				}

				return;
			}

			if (!curTargetName.equals("null")) {
				double[] curTargetPos = getPlayerPos(curTargetName);

				if (curTargetPos[1] > mcInstance.thePlayer.posY + 4 && nextTargetNames.length > 0) {
					makeLog("switching to next target " + nextTargetNames[0] + " because Y of " + curTargetPos[1]
							+ " too high");

					curTargetName = nextTargetNames[0];
					nextTargetNames = Arrays.copyOfRange(nextTargetNames, 1, nextTargetNames.length);

					curTargetPos = getPlayerPos(curTargetName);
				}

				mouseTargetX = curTargetPos[0];
				mouseTargetY = curTargetPos[1] + 1;
				mouseTargetZ = curTargetPos[2];
			}

			if (mcInstance.currentScreen == null) {
				if (mouseTargetX != 0 || mouseTargetY != 0 || mouseTargetZ != 0) { // dumb null check
					mouseMove();
				}
				doMovementKeys();
			} else {
				allKeysUp();
			}

			if (mcInstance.thePlayer.posY > curSpawnLevel - 4 && !curTargetName.equals("null") && !farFromMid()) {

				// in spawn but has target (bad)

				curTargetName = "null";

				mouseTargetX = 0;
				mouseTargetY = curSpawnLevel - 4;
				mouseTargetZ = 0;

				allKeysUp();

				if (!autoClickerEnabled) { // only needs to switch away from sword if an external KA is enabled
					mcInstance.thePlayer.inventory.currentItem = 5;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void reloadKey() {
		String[] possibleKeyFileNames = { "key.txt", "key.txt.txt", "key", "token.txt", "token.txt.txt", "token" }; // from
																													// best
																													// to
																													// worst...

		boolean foundKeyFile = false;

		for (String curPossibleKeyFileName : possibleKeyFileNames) {
			File potentialKeyFile = new File(curPossibleKeyFileName);

			if (potentialKeyFile.isFile()) {
				// key file found, read it
				try (FileInputStream inputStream = new FileInputStream(curPossibleKeyFileName)) {
					apiKey = IOUtils.toString(inputStream);

					System.out.println("set key");

					foundKeyFile = true;

					break; // only breaks if reading key file didn't error
				} catch (Exception e) {
					System.out.println("reading key error");
					apiMessage = "error reading key";
					e.printStackTrace();
				}
			}
		}

		if (!foundKeyFile) {
			apiMessage = "no key file found";
		}
	}

	public void callBotApi() {
		// set key from file if unset
		if (apiKey.equals("null")) {
			reloadKey();
		}

		// return if key is still null - no key was read so no point calling API
		if (apiKey.equals("null")) {
			return;
		}

		makeLog("getting api url: " + apiUrl);

		preApiProcessingTime = System.currentTimeMillis();

		APIMainPayload payload = new APIMainPayload();

		EntityPlayerSP player = mcInstance.thePlayer;

		// construct client info string

		// auth key

		payload.setApiKey(apiKey);

		// client username

		payload.setPlayerName(player.getName());

		// client uuid

		payload.setUuid(player.getUniqueID().toString());

		// client position + viewing angles

		payload.setPosition(new APIVec3(player.posX, player.posY, player.posZ));
		payload.setYaw(player.rotationYaw);
		payload.setPitch(player.rotationPitch);

		// client inventory
		ArrayList<APIInventoryItem> inventoryItems = new ArrayList<>();

		for (int i = 0; i < player.inventoryContainer.getInventory().size(); i++) {
			ItemStack curItem = player.inventoryContainer.getInventory().get(i);

			if (curItem != null) {
				inventoryItems.add(
						new APIInventoryItem(curItem.getItem().getRegistryName().split(":")[1], curItem.stackSize));
			}
		}

		payload.setInventory(inventoryItems);

		// players

		payload.setPlayers(mcInstance.theWorld.playerEntities
				.stream()
				.filter(p -> !p.isInvisible())
				.map(p -> {
					APIVec3 pos = new APIVec3(p.posX, p.posY, p.posZ);
					APIPlayer apiPlayer = new APIPlayer();
					apiPlayer.setPos(pos);
					apiPlayer.setUsername(p.getName());
					apiPlayer.setHealth(p.getHealth());
					apiPlayer.setArmor(p.getTotalArmorValue());
					return apiPlayer;
				})
				.collect(Collectors.toList()));

		// middle block

		String middleBlockname = "null";
		try {
			middleBlockname = mcInstance.theWorld.getBlockState(new BlockPos(0, (int) player.posY - 1, 0)).getBlock()
					.getRegistryName().split(":")[1];
		} catch (Exception e) {
			e.printStackTrace();
		}

		payload.setMiddleBlock(middleBlockname);

		// last chat message

		payload.setLastMessages(chatMsgs);

		chatMsgs.clear();

		// container items

		StringBuilder containerStr = new StringBuilder("null");

		List<ItemStack> containerItems = mcInstance.thePlayer.openContainer.getInventory();

		if (containerItems.size() > 46) { // check if a container is open (definitely a better way to do that)
			List<APIContainerItem> apiContainerItems = new ArrayList<>();
			for (int i = 0; i < containerItems.size() - 36; i++) { // minus 36 to cut off inventory
				ItemStack curItem = containerItems.get(i);

				String curItemName = "air";
				String curItemDisplayName = "air";
				int curItemStackSize = 0;

				if (curItem != null) {
					APIContainerItem item = new APIContainerItem();
					item.setDisplayName(curItemDisplayName);
					item.setItem(curItemName);
					item.setStackSize(curItemStackSize);
					apiContainerItems.add(item);
				}
			}
			payload.setContainer(apiContainerItems);
		}

		// dropped items
		List<EntityItem> droppedItems = mcInstance.theWorld.getEntitiesWithinAABB(EntityItem.class,
				new AxisAlignedBB(
						new BlockPos(mcInstance.thePlayer.posX - 32, mcInstance.thePlayer.posY - 4,
								mcInstance.thePlayer.posZ - 32),
						new BlockPos(mcInstance.thePlayer.posX + 32, mcInstance.thePlayer.posY + 32,
								mcInstance.thePlayer.posZ + 32)));

		List<APIDroppedItem> dItems = new ArrayList<>();
		for (int i = 0; i < Math.min(128, droppedItems.size()); i++) {
			EntityItem curItem = droppedItems.get(i);
			String curItemName = curItem.getEntityItem().getItem().getRegistryName().split(":")[1];

			APIDroppedItem dItem = new APIDroppedItem();
			dItem.setPos(new APIVec3(curItem.getPosition().getX(), curItem.getPosition().getY(),
					curItem.getPosition().getZ()));
			dItem.setName(curItemName);
			dItems.add(dItem);
		}

		payload.setDroppedItems(dItems);

		// important chat msg

		if (!importantChatMsg.equals("")) {
			payload.setImportantChatMsg(importantChatMsg);
			importantChatMsg = "";
		}

		// current open gui

		String curOpenGui = "null";
		if (mcInstance.currentScreen != null) {
			payload.setCurrentOpenGui(mcInstance.currentScreen.getClass().toString());

		}

		// villager positions

		List<Entity> allEntities = mcInstance.theWorld.getLoadedEntityList();

		List<Entity> villagerEntities = allEntities.stream()
				.filter(entity -> entity.getClass().equals(EntityVillager.class)).collect(Collectors.toList());

		List<APIVec3> villagerPositions = new ArrayList<>();
		for (int i = 0; i < Math.min(8, villagerEntities.size()); i++) {
			Entity curVillager = villagerEntities.get(i);

			double curVillagerPositionX = curVillager.getPosition().getX();
			double curVillagerPositionY = curVillager.getPosition().getY();
			double curVillagerPositionZ = curVillager.getPosition().getZ();

			villagerPositions.add(new APIVec3(curVillagerPositionX, curVillagerPositionY, curVillagerPositionZ));
		}

		payload.setVillagerPositions(villagerPositions);

		// client health

		payload.setHealth(player.getHealth());

		// client xp level

		payload.setXpLevel(player.experienceLevel);

		// mod version

		String modVersion = null;
		ModContainer modContainer = Loader.instance().getIndexedModList().get("keystrokesmod");
		if (modContainer != null) {
			payload.setVersion(modContainer.toString());
		}
		// // compress
		//
		// infoStr = compressString(infoStr);
		//
		// // add "this is compressed" tag to support API version compatibility
		//
		// infoStr += "xyzcompressed";

		// done, set client info header

		String infoStr = gson.toJson(payload);

		makeLog(infoStr);

		makeLog("api info header length is " + infoStr.length() + " chars");

		// do request

		ticksPerApiCall = 20;

		long preApiGotTime = System.currentTimeMillis();

		ForkJoinPool.commonPool().execute(() -> {
			HttpPost post = new HttpPost(apiUrl);

			int timeoutMs = 5000;
			RequestConfig requestConfig = RequestConfig.custom()
					.setConnectionRequestTimeout(timeoutMs)
					.setConnectTimeout(timeoutMs)
					.setSocketTimeout(timeoutMs)
					.build();

			post.setConfig(requestConfig);

			String apiResponse;
			int statusCode;
			try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
				post.setEntity(new StringEntity(infoStr));
				HttpResponse response = httpclient.execute(post);
				statusCode = response.getStatusLine().getStatusCode();
				apiResponse = IOUtils.toString(response.getEntity().getContent());
			} catch (IOException e) {
				e.printStackTrace();
				apiMessage = "api call fatal error";
				return;
			}

			apiLastPing = (int) (System.currentTimeMillis() - preApiGotTime);

			makeLog("api ping was " + apiLastPing + "ms");

			if (apiLastPing > 1000) {
				makeLog("api ping too high");
				apiMessage = "api ping too high - " + apiLastPing + "ms";
				return;
			}

			try {
				ingestApiResponse(apiResponse, statusCode);
			} catch (Exception exception) {
				exception.printStackTrace();
				apiMessage = "errored on ingesting api response";
			}
		});
	}

	public void ingestApiResponse(String apiText, int statusCode) {
		// check if it's an error
		if (statusCode >= 400) {
			// parse the error and display it
			apiMessage = "Error: " + gson.fromJson(apiText, APIError.class).getError();
			return;
		}

		// Parse the response payload
		APIResponsePayload payload = gson.fromJson(apiText, APIResponsePayload.class);

		apiMessage = payload.getMessage();
	}

	public void doMovementKeys() { // so long
		if (Math.random() <= keyChanceForwardUp) {
			setKeyUp(1);
		}
		if (Math.random() <= keyChanceForwardDown) {
			setKeyDown(1);
		}

		if (Math.random() <= keyChanceBackwardUp) {
			setKeyUp(2);
		}
		if (Math.random() <= keyChanceBackwardDown) {
			setKeyDown(2);
		}

		if (Math.random() <= keyChanceSideUp) {
			setKeyUp(3);
		}
		if (Math.random() <= keyChanceSideDown) {
			setKeyDown(3);
		}

		if (Math.random() <= keyChanceSideUp) {
			setKeyUp(4);
		}
		if (Math.random() <= keyChanceSideDown) {
			setKeyDown(4);
		}

		if (Math.random() <= keyChanceJumpUp) {
			setKeyUp(5);
		}
		if (Math.random() <= keyChanceJumpDown) {
			setKeyDown(5);
		}

		if (Math.random() <= keyChanceCrouchUp) {
			setKeyUp(6);
		}
		if (Math.random() <= keyChanceCrouchDown) {
			setKeyDown(6);
		}

		if (Math.random() <= keyChanceSprintUp) {
			setKeyUp(7);
		}
		if (Math.random() <= keyChanceSprintDown) {
			setKeyDown(7);
		}

		if (Math.random() <= keyChanceUseUp) {
			setKeyUp(9);
		}
		if (Math.random() <= keyChanceUseDown) {
			setKeyDown(9);
		}

		if (Math.random() <= keyAttackChance) {
			if (autoClickerEnabled) {
				doAttack();
			}
		}
	}

	public void doAttack() {
		KeyBinding.onTick(mcInstance.gameSettings.keyBindAttack.getKeyCode());
		attackedThisTick = true;
	}

	public void goAfk() {
		mouseVelX = 0;
		mouseVelY = 0;
		allKeysUp();
		pressChatKeyIfNoGuiOpen();
	}

	public void pressInventoryKeyIfNoGuiOpen() {
		if (mcInstance.currentScreen == null) {
			KeyBinding.onTick(mcInstance.gameSettings.keyBindInventory.getKeyCode());
		}
	}

	public void pressChatKeyIfNoGuiOpen() {
		if (mcInstance.currentScreen == null) {
			KeyBinding.onTick(mcInstance.gameSettings.keyBindChat.getKeyCode());
		}
	}

	public void allKeysUp() {
		setKeyUp(1);
		setKeyUp(2);
		setKeyUp(3);
		setKeyUp(4);
		setKeyUp(5);
		setKeyUp(6);
		setKeyUp(7);
		setKeyUp(8);
		setKeyUp(9);
	}

	public double[] getPlayerPos(String playerName) { // weird
		List<EntityPlayer> playerList = mcInstance.theWorld.playerEntities;
		List<EntityPlayer> playerToGet = playerList.stream().filter(pl -> pl.getName().equals(playerName))
				.collect(Collectors.toList());
		if (playerToGet.size() > 0) {
			Entity foundPlayer = playerToGet.get(0);

			return new double[] { foundPlayer.getPosition().getX(), foundPlayer.getPosition().getY(),
					foundPlayer.getPosition().getZ() };
		} else {
			System.out.println("could not find player");
			return new double[] { 0, 999, 0 };
		}
	}

	public void setKeyDown(int whichKey) {
		Minecraft mc = Minecraft.getMinecraft();
		if (whichKey >= 1 && whichKey <= 9) {
			KeyBinding[] keysList = { mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack,
					mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindJump,
					mc.gameSettings.keyBindSneak, mc.gameSettings.keyBindSprint, mc.gameSettings.keyBindAttack,
					mc.gameSettings.keyBindUseItem };
			KeyBinding.setKeyBindState(keysList[whichKey - 1].getKeyCode(), true);
		}
	}

	public void setKeyUp(int whichKey) {
		Minecraft mc = Minecraft.getMinecraft();
		if (whichKey >= 1 && whichKey <= 9) {
			KeyBinding[] keysList = { mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack,
					mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindJump,
					mc.gameSettings.keyBindSneak, mc.gameSettings.keyBindSprint, mc.gameSettings.keyBindAttack,
					mc.gameSettings.keyBindUseItem };
			KeyBinding.setKeyBindState(keysList[whichKey - 1].getKeyCode(), false);
		}
	}

	public void drawText(String text, float x, float y) {
		mcInstance.fontRendererObj.drawStringWithShadow(text, x, y, 0xffffff);
	}

	public void interpolateMousePosition() {
		if (!grinderEnabled || mcInstance.currentScreen != null) {
			lastMouseUpdate = 0;
			mouseVelX = 0;
			mouseVelY = 0;
			return;
		}

		long currentTime = System.currentTimeMillis();
		if (lastMouseUpdate != 0) {
			float timePassed = (currentTime - lastMouseUpdate) / 1000f;

			// Limit time passed (so that the change can't be that large)
			timePassed = Math.min(timePassed, 1);

			mcInstance.thePlayer.rotationYaw += getAdjustedMouseChange(mouseVelY * timePassed);
			mcInstance.thePlayer.rotationPitch += getAdjustedMouseChange(mouseVelX * timePassed);

			// Limit pitch
			float pitch = mcInstance.thePlayer.rotationPitch;
			mcInstance.thePlayer.rotationPitch = Math.min(Math.max(-90, pitch), 90);
		}
		lastMouseUpdate = currentTime;
	}

	public void mouseMove() {
		interpolateMousePosition();

		float currentYaw = mcInstance.thePlayer.rotationYaw, currentPitch = mcInstance.thePlayer.rotationPitch;
		double x = mcInstance.thePlayer.posX, y = mcInstance.thePlayer.posY, z = mcInstance.thePlayer.posZ;

		double headHeight = 1.62;

		// old af math probably stupid
		double targetRotY = fixRotY(360 - Math.toDegrees(Math.atan2(mouseTargetX - x, mouseTargetZ - z)));
		double targetRotX = -Math.toDegrees(Math.atan(
				(mouseTargetY - y - headHeight) / Math.hypot(mouseTargetX - x, mouseTargetZ - z)));

		// add random waviness to target
		targetRotY += timeSinWave(310) * 2 + timeSinWave(500) * 2 + timeSinWave(260) * 2;
		targetRotX += timeSinWave(290) * 2 + timeSinWave(490) * 2 + timeSinWave(270) * 2;

		targetRotY = fixRotY(targetRotY);
		targetRotX = fixRotX(targetRotX);

		// calculate mouse speed
		double timeNoise = timeSinWave(40) * 2 +
				timeSinWave(50) * 2 +
				timeSinWave(100) * 2 +
				timeSinWave(150) * 4 +
				timeSinWave(200) * 6;

		double mouseCurSpeed = mouseSpeed + timeNoise;

		currentYaw = (float) fixRotY(currentYaw);

		double diffRotX = targetRotX - currentPitch;
		double diffRotY = range180(targetRotY - currentYaw);

		double rotAng = Math.atan2(diffRotY, diffRotX) + Math.PI;

		double changeRotX = -Math.cos(rotAng) * mouseCurSpeed / 4;
		double changeRotY = -Math.sin(rotAng) * mouseCurSpeed;

		mouseVelX = Math.abs(diffRotX) < Math.abs(changeRotX)
				? targetRotX - currentPitch
				: changeRotX;
		mouseVelY = Math.abs(diffRotY) < Math.abs(changeRotY)
				? targetRotY - currentYaw
				: changeRotY;

		// Reach target in 1/20th of a second (1 tick)
		double TPS = 20.0;
		mouseVelX *= TPS;
		mouseVelY *= TPS;
	}

	public boolean farFromMid() {
		return mcInstance.thePlayer.posX > 32 || mcInstance.thePlayer.posZ > 32;
	}

	public double timeSinWave(double div) { // little odd
		double num = System.currentTimeMillis() / div * 100.0D;
		num %= 360.0D;
		num = Math.toRadians(num);
		num = Math.sin(num);
		return num;
	}

	public double range180(double rot) {
		rot = rot % 360;
		if (rot > 180) {
			rot -= 360;
		}
		return rot;
	}

	public double fixRotY(double rotY) {
		rotY = rotY % 360;
		while (rotY < 0) {
			rotY = rotY + 360;
		}
		return rotY;
	}

	public double fixRotX(double rotX) {
		if (rotX > 90) {
			rotX = 90;
		}
		if (rotX < -90) {
			rotX = -90;
		}
		return rotX;
	}

	public void makeLog(String logStr) {
		if (loggingEnabled) {
			System.out.println(logStr);
		}
	}

	public static String compressString(String inputString) {
		byte[] inputBytes = inputString.getBytes(StandardCharsets.UTF_8);

		Deflater deflater = new Deflater();
		deflater.setInput(inputBytes);
		deflater.finish();

		byte[] compressedBytes = new byte[inputBytes.length];
		int compressedLength = deflater.deflate(compressedBytes);
		deflater.end();

		byte[] compressedData = new byte[compressedLength];
		System.arraycopy(compressedBytes, 0, compressedData, 0, compressedLength);

		String compressedString = base64encoder.encodeToString(compressedData);

		double compressionRatio = (double) compressedString.length() / inputBytes.length;

		if (compressionRatio > 1) {
			System.out.println("compression ratio NOT GOOD: " + compressionRatio);
		}

		return compressedString;
	}

	public static String decompressString(String compressedString) {
		byte[] compressedBytes = base64decoder.decode(compressedString);

		Inflater inflater = new Inflater();
		inflater.setInput(compressedBytes);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedBytes.length);
		byte[] buffer = new byte[1024];

		try {
			while (!inflater.finished()) {
				int count = inflater.inflate(buffer);
				outputStream.write(buffer, 0, count);
			}
			try {
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (DataFormatException e) {
			e.printStackTrace();
		}

		inflater.end();

		byte[] decompressedData = outputStream.toByteArray();

		return new String(decompressedData, StandardCharsets.UTF_8);
	}

	// https://goldenstack.net/blog/sensitivity
	private static float getAdjustedMouseChange(double degrees) {
		double scale = 0.5 * 0.6000000238418579 + 0.20000000298023224;
		double factor = (scale * scale * scale) * 8.0;

		double pixels = Math.round(degrees / factor);

		double change = pixels * factor;

		return (float) change * 0.15F;
	}
}
