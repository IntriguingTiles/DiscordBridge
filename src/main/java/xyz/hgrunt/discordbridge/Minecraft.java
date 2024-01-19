package xyz.hgrunt.discordbridge;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.TextChannel;

public class Minecraft extends JavaPlugin implements Listener {
	private Discord discord = null;
	Configuration config;
	private TextChannel ch;

	Pattern emojiPattern = Pattern.compile(":(\\w+):");

	@Override
	public void onEnable() {
		config = getConfiguration();
		String token = config.getString("token", "insert-bot-token-here");
		config.getString("channel", "insert-channel-id-here");
		config.getString("username-color", "&f");
		config.getBoolean("use-role-color-as-username-color", false);
		config.save();

		if (token.equals("insert-bot-token-here")) {
			getServer().getLogger().severe("Please set up your config.");
			getPluginLoader().disablePlugin(this);
			return;
		}

		try {
			discord = new Discord(this, token);
		} catch (LoginException e) {
			getServer().getLogger().severe("Failed to init Discord! Is your token correct?");
			getPluginLoader().disablePlugin(this);
			return;
		}

		getServer().getPluginManager().registerEvent(Type.PLAYER_CHAT, new PlayerEvents(), Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Type.PLAYER_JOIN, new PlayerEvents(), Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Type.PLAYER_QUIT, new PlayerEvents(), Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Type.ENTITY_DEATH, new EntityEvents(), Priority.Normal, this);
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
			HashSet<String> foundMatches = new HashSet<String>();
			while (match.find()) {
				List<Emote> emotes = discord.jda.getEmotesByName(match.group(1), true);
				if (!emotes.isEmpty() && !foundMatches.contains(match.group(1))) {
					foundMatches.add(match.group(1));
					msg = msg.replaceAll(":(" + match.group(1) + "):", emotes.get(0).getAsMention());
				}
			}

			ch.sendMessage("**<" + Discord.escapeMarkdown(stripColor(e.getPlayer().getDisplayName())) + ">** " + msg)
					.queue();
		}

		@Override
		public void onPlayerJoin(PlayerEvent e) {
			if (!ensureChannel())
				return;

			ch.sendMessage(Discord.escapeMarkdown(stripColor(e.getPlayer().getDisplayName()) + " joined the game"))
					.queue();
			discord.jda.getPresence().setActivity(Activity.playing(
					getServer().getOnlinePlayers().length + "/" + getServer().getMaxPlayers() + " players online"));
		}

		@Override
		public void onPlayerQuit(PlayerEvent e) {
			if (!ensureChannel())
				return;

			ch.sendMessage(Discord.escapeMarkdown(stripColor(e.getPlayer().getDisplayName()) + " left the game"))
					.queue();
			discord.jda.getPresence().setActivity(Activity.playing(
					getServer().getOnlinePlayers().length + "/" + getServer().getMaxPlayers() + " players online"));
		}
	}

	public class EntityEvents extends EntityListener {

		@Override
		public void onEntityDeath(EntityDeathEvent e) {
			if (e.getEntity() instanceof Player) {
				if (!ensureChannel())
					return;

				Player p = (Player) e.getEntity();

				getServer().broadcastMessage(p.getDisplayName() + " died");
				ch.sendMessage(Discord.escapeMarkdown(stripColor(p.getDisplayName()) + " died")).queue();
			}
		}
	}

	private boolean ensureChannel() {
		if (ch == null) {
			String id = config.getString("channel");
			ch = discord.jda.getTextChannelById(id);
		}

		if (ch == null) {
			getServer().getLogger().severe("Couldn't find the channel!");
			return false;
		}

		if (!ch.canTalk()) {
			getServer().getLogger().severe("I don't have permission to talk in #" + ch.getName() + "!");
			return false;
		}

		return true;
	}

	public static final char COLOR_CHAR = '\u00A7';
	private static final Pattern STRIP_COLOR_PATTERN = Pattern
			.compile("(?i)" + String.valueOf(COLOR_CHAR) + "[0-9A-FK-OR]");

	public static String stripColor(final String input) {
		if (input == null) {
			return null;
		}

		return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
	}
}
