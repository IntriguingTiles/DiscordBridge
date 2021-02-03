package xyz.hgrunt.discordbridge;

import java.awt.Color;

import javax.security.auth.login.LoginException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Discord extends ListenerAdapter {
	JDA jda;
	private Minecraft minecraft;
	private String chId;

	public Discord(Minecraft minecraft, String token) throws LoginException {
		this.minecraft = minecraft;
		chId = minecraft.config.getString("channel");
		jda = JDABuilder.createDefault(token).addEventListeners(this)
				.setActivity(Activity.playing(minecraft.getServer().getOnlinePlayers().size() + "/"
						+ minecraft.getServer().getMaxPlayers() + " players online"))
				.build();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		if (!e.getChannel().getId().equals(chId) || e.getAuthor().isBot() || !((TextChannel) e.getChannel()).canTalk())
			return;

		if (e.getMessage().getContentRaw().equalsIgnoreCase("!list")) {
			String msg = "There are " + minecraft.getServer().getOnlinePlayers().size() + "/"
					+ minecraft.getServer().getMaxPlayers() + " players online:\n";

			for (Player p : minecraft.getServer().getOnlinePlayers()) {
				msg += escapeMarkdown(p.getName()) + "\n";
			}

			e.getChannel().sendMessage(msg).queue();
			return;
		}

		String attch = "";

		if (!e.getMessage().getAttachments().isEmpty()) {
			if (!e.getMessage().getContentDisplay().isEmpty())
				attch += " ";
			attch += "[" + e.getMessage().getAttachments().size() + " attachment(s)]";
		}

		String color = ChatColor.translateAlternateColorCodes('&', minecraft.config.getString("username-color"));

		minecraft.getServer()
				.broadcastMessage("<" + color + (ChatColor.stripColor(e.getAuthor().getName())) + "#"
						+ e.getAuthor().getDiscriminator() + ChatColor.RESET + "> " + e.getMessage().getContentDisplay()
						+ attch);
	}

	public static String escapeMarkdown(String text) {
		return text == null ? "" : text.replace("_", "\\_").replace("*", "\\*").replace("~", "\\~");
	}
}
