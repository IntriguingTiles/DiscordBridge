package xyz.hgrunt.discordbridge;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.TextChannel;

public class Minecraft extends JavaPlugin implements Listener {
	Discord discord = new Discord();
	Configuration config;
	TextChannel ch;

	Pattern emojiPattern = Pattern.compile(":(\\w+):");

	@Override
	public void onEnable() {
		config = getConfiguration();
		String token = config.getString("token", "insert-bot-token-here");
		config.getString("channel", "insert-channel-id-here");
		config.getString("username-color", "WHITE");
		config.save();

		if (token.equals("insert-bot-token-here")) {
			Bukkit.getLogger().severe("Please set up your config.");
			getPluginLoader().disablePlugin(this);
			return;
		}

		try {
			discord.init(token, this);
		} catch (LoginException | InterruptedException e) {
			Bukkit.getLogger().severe("Failed to init Discord! Is your token correct?");
			getPluginLoader().disablePlugin(this);
			return;
		}

		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, new PlayerEvents(), Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, new PlayerEvents(), Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, new PlayerEvents(), Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, new EntityEvents(), Event.Priority.Normal, this);
	}

	@Override
	public void onDisable() {
		if (discord.jda != null)
			discord.jda.shutdownNow();
	}

	public class PlayerEvents extends PlayerListener {
		public void onPlayerChat(PlayerChatEvent e) {
			if (ch == null) {
				String id = config.getString("channel");
				ch = discord.jda.getTextChannelById(id);
			}

			if (ch == null) {
				Bukkit.getLogger().severe("Couldn't find the channel!");
				return;
			}

			String msg = e.getMessage();
			Matcher match = emojiPattern.matcher(msg);
			if (match.find()) {
				List<Emote> emotes = discord.jda.getEmotesByName(match.group(1), true);
				if (!emotes.isEmpty()) {
					msg = msg.replace(":" + match.group(1) + ":", emotes.get(0).getAsMention());
				}
			}

			ch.sendMessage("**<" + Discord.escapeMarkdown(e.getPlayer().getDisplayName()) + ">** " + msg).queue();
		}
		
		public void onPlayerJoin(PlayerJoinEvent e) {
			if (ch == null) {
				String id = config.getString("channel");
				ch = discord.jda.getTextChannelById(id);
			}
			
			if (ch == null) {
				Bukkit.getLogger().severe("Couldn't find the channel!");
				return;
			}
			
			ch.sendMessage(Discord.escapeMarkdown(ChatColor.stripColor(e.getJoinMessage()))).queue();
			discord.jda.getPresence().setActivity(Activity.playing(
					Bukkit.getServer().getOnlinePlayers().length + "/" + getServer().getMaxPlayers() + " players online"));
		}
		
		public void onPlayerQuit(PlayerQuitEvent e) {
			if (ch == null) {
				String id = config.getString("channel");
				ch = discord.jda.getTextChannelById(id);
			}
			
			if (ch == null) {
				Bukkit.getLogger().severe("Couldn't find the channel!");
				return;
			}
			
			ch.sendMessage(Discord.escapeMarkdown(ChatColor.stripColor(e.getQuitMessage()))).queue();
			discord.jda.getPresence().setActivity(Activity.playing(Bukkit.getServer().getOnlinePlayers().length - 1 + "/"
					+ getServer().getMaxPlayers() + " players online"));
		}
	}

	public class EntityEvents extends EntityListener {
		public void onEntityDeath(EntityDeathEvent e) {
			if (e.getEntity() instanceof Player) {
				Player p = (Player) e.getEntity();
				
				if (ch == null) {
					String id = config.getString("channel");
					ch = discord.jda.getTextChannelById(id);
				}
				
				if (ch == null) {
					Bukkit.getLogger().severe("Couldn't find the channel!");
					return;
				}
				
				ch.sendMessage(Discord.escapeMarkdown(ChatColor.stripColor(p.getDisplayName()) + " died")).queue();
			}
		}
	}
}
