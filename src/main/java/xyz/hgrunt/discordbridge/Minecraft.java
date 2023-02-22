package xyz.hgrunt.discordbridge;

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
import org.bukkit.event.Event;
import org.bukkit.event.Event.Type;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.TextChannel;

public class Minecraft extends JavaPlugin implements Listener, CommandExecutor {
	private Discord discord = null;
	FileConfiguration config;
	private TextChannel ch;

	Pattern emojiPattern = Pattern.compile(":(\\w+):");

	@Override
	public void onEnable() {
		config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();

		String token = config.getString("token");

		if (token.equals("insert-bot-token-here")) {
			Bukkit.getLogger().severe("Please set up your config.");
			getPluginLoader().disablePlugin(this);
			return;
		}

		try {
			discord = new Discord(this, token);
		} catch (LoginException e) {
			Bukkit.getLogger().severe("Failed to init Discord! Is your token correct?");
			getPluginLoader().disablePlugin(this);
			return;
		}

		getServer().getPluginManager().registerEvent(Type.PLAYER_CHAT, new PlayerEvents(), Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Type.PLAYER_JOIN, new PlayerEvents(), Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Type.PLAYER_QUIT, new PlayerEvents(), Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Type.ENTITY_DEATH, new EntityEvents(), Event.Priority.Normal,
				this);
		getCommand("discordbridge").setExecutor(this);
	}

	@Override
	public void onDisable() {
		if (discord != null && discord.jda != null)
			discord.jda.shutdownNow();
	}

	public class PlayerEvents extends PlayerListener {
		@Override
		public void onPlayerChat(PlayerChatEvent e) {
			if (!ensureChannel())
				return;

			String msg = e.getMessage();
			Matcher match = emojiPattern.matcher(msg);
			if (match.find()) {
				List<Emote> emotes = discord.jda.getEmotesByName(match.group(1), true);
				if (!emotes.isEmpty()) {
					msg = msg.replace(":" + match.group(1) + ":", emotes.get(0).getAsMention());
				}
			}

			ch.sendMessage(
					"**<" + Discord.escapeMarkdown(ChatColor.stripColor(e.getPlayer().getDisplayName())) + ">** " + msg)
					.queue();
		}

		@Override
		public void onPlayerJoin(PlayerJoinEvent e) {
			if (!ensureChannel())
				return;
			ch.sendMessage(Discord.escapeMarkdown(ChatColor.stripColor(e.getJoinMessage()))).queue();
			discord.jda.getPresence().setActivity(Activity.playing(
					getServer().getOnlinePlayers().length + "/" + getServer().getMaxPlayers() + " players online"));
		}

		@Override
		public void onPlayerQuit(PlayerQuitEvent e) {
			if (!ensureChannel())
				return;
			ch.sendMessage(Discord.escapeMarkdown(ChatColor.stripColor(e.getQuitMessage()))).queue();
			discord.jda.getPresence().setActivity(Activity.playing(
					getServer().getOnlinePlayers().length - 1 + "/" + getServer().getMaxPlayers() + " players online"));
		}
	}

	public class EntityEvents extends EntityListener {
		@Override
		public void onEntityDeath(EntityDeathEvent e) {
			if (!ensureChannel() || !(e instanceof PlayerDeathEvent))
				return;

			PlayerDeathEvent pde = (PlayerDeathEvent) e;
			ch.sendMessage(Discord.escapeMarkdown(ChatColor.stripColor(pde.getDeathMessage()))).queue();
		}
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		reloadConfig();
		config = getConfig();

		if (discord != null && discord.jda != null)
			discord.jda.shutdownNow();

		try {
			discord = new Discord(this, config.getString("token"));
		} catch (LoginException e) {
			Bukkit.getLogger().severe("Failed to init Discord! Is your token correct?");
			getPluginLoader().disablePlugin(this);
		}

		Bukkit.getLogger().info("Reloaded.");
		sender.sendMessage("Reloaded.");

		return true;
	}

	private boolean ensureChannel() {
		if (ch == null) {
			String id = config.getString("channel");
			Bukkit.getLogger().info(id);
			ch = discord.jda.getTextChannelById(id);
		}

		if (ch == null) {
			Bukkit.getLogger().severe("Couldn't find the channel!");
			return false;
		}

		if (!ch.canTalk()) {
			Bukkit.getLogger().severe("I don't have permission to talk in #" + ch.getName() + "!");
			return false;
		}

		return true;
	}
}
