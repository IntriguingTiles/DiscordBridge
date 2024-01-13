package xyz.hgrunt.discordbridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.TextChannel;
import nz.co.lolnet.james137137.FactionChat.API.FactionChatAPI;

public class Minecraft extends JavaPlugin implements Listener, CommandExecutor {
	private Discord discord = null;
	FileConfiguration config = getConfig();
	private HashMap<String, String> lang = new HashMap<String, String>();
	private TextChannel ch;

	Pattern emojiPattern = Pattern.compile(":(\\w+):");

	@Override
	public void onEnable() {
		config.options().copyDefaults(true);
		saveConfig();

		try {
			loadAdvancements();
		} catch (IOException e) {
			getLogger().severe("Failed to read Minecraft's lang file");
			e.printStackTrace();
			getPluginLoader().disablePlugin(this);
			return;
		}

		String token = config.getString("token");

		if (token.equals("insert-bot-token-here")) {
			getLogger().severe("Please set up your config.");
			getPluginLoader().disablePlugin(this);
			return;
		}

		try {
			discord = new Discord(this, token);
		} catch (LoginException e) {
			getLogger().severe("Failed to init Discord! Is your token correct?");
			getPluginLoader().disablePlugin(this);
			return;
		}

		getServer().getPluginManager().registerEvents(this, this);
		getCommand("discordbridge").setExecutor(this);
	}

	@Override
	public void onDisable() {
		if (discord != null && discord.jda != null)
			discord.jda.shutdownNow();
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		if (getServer().getPluginManager().getPlugin("FactionChat") != null) {
			if (FactionChatAPI.isFactionChatMessage(e))
				return;
		}

		if (!ensureChannel())
			return;

		String msg = e.getMessage();
		Matcher match = emojiPattern.matcher(msg);
		HashSet<String> foundMatches = new HashSet<String>();
		while (match.find()) {
			List<Emote> emotes = discord.jda.getEmotesByName(match.group(1), true);
			if (!emotes.isEmpty() && !foundMatches.contains(match.group(1))) {
				foundMatches.add(match.group(1));
				msg = msg.replaceAll(":(" + match.group(1) + "):", emotes.get(0).getAsMention());
			}
		}

		ch.sendMessage("**<" + Discord.escapeMarkdown(ChatColor.stripColor(e.getPlayer().getDisplayName())) + ">** " + msg).queue();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (!ensureChannel())
			return;
		ch.sendMessage(Discord.escapeMarkdown(ChatColor.stripColor(e.getJoinMessage()))).queue();
		discord.jda.getPresence().setActivity(Activity.playing(
				getServer().getOnlinePlayers().size() + "/" + getServer().getMaxPlayers() + " players online"));
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e) {
		if (!ensureChannel())
			return;
		ch.sendMessage(Discord.escapeMarkdown(ChatColor.stripColor(e.getQuitMessage()))).queue();
		discord.jda.getPresence().setActivity(Activity.playing(
				getServer().getOnlinePlayers().size() - 1 + "/" + getServer().getMaxPlayers() + " players online"));
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (!ensureChannel())
			return;
		ch.sendMessage(Discord.escapeMarkdown(ChatColor.stripColor(e.getDeathMessage()))).queue();
	}

	@EventHandler
	public void onPlayerAdvancement(PlayerAdvancementDoneEvent e) {
		if (!ensureChannel())
			return;

		if (e.getAdvancement().getKey().getKey().startsWith("recipes/")
				|| e.getAdvancement().getKey().getKey().endsWith("/root"))
			return;

		ch.sendMessage(Discord.escapeMarkdown(ChatColor.stripColor(e.getPlayer().getName())) + " has made the advancement **"
				+ lang.get(e.getAdvancement().getKey().getKey()) + "**").queue();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		reloadConfig();
		config = getConfig();

		if (discord != null && discord.jda != null)
			discord.jda.shutdownNow();

		try {
			discord = new Discord(this, config.getString("token"));
		} catch (LoginException e) {
			getLogger().severe("Failed to init Discord! Is your token correct?");
			getPluginLoader().disablePlugin(this);
		}

		getLogger().info("Reloaded.");
		sender.sendMessage("Reloaded.");

		return true;
	}

	private boolean ensureChannel() {
		if (ch == null) {
			String id = config.getString("channel");
			ch = discord.jda.getTextChannelById(id);
		}

		if (ch == null) {
			getLogger().severe("Couldn't find the channel!");
			return false;
		}

		if (!ch.canTalk()) {
			getLogger().severe("I don't have permission to talk in #" + ch.getName() + "!");
			return false;
		}

		return true;
	}

	private void loadAdvancements() throws IOException {
		InputStream is = Bukkit.class.getClassLoader().getResourceAsStream("assets/minecraft/lang/en_us.lang");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.startsWith("advancements.") || !line.contains(".title="))
				continue;

			if (line.contains("root.title="))
				continue;

			String key = line.split("=")[0].replace("advancements.", "").replace(".title", "").replace(".", "/");
			String value = line.split("=")[1];

			if (key.equals("husbandry/breed_all_animals"))
				key = "husbandry/bred_all_animals"; // why
			lang.put(key, value);
		}

		reader.close();
		is.close();
	}
}
