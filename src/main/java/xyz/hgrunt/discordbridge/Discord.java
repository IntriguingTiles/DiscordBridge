package xyz.hgrunt.discordbridge;

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
	Minecraft minecraft;
	String chId;

	public Discord() {
	}

	public Discord(Minecraft minecraft) {
		this.minecraft = minecraft;
		chId = minecraft.config.getString("channel");
	}

	public void init(String token, Minecraft minecraft) throws LoginException, InterruptedException {
		jda = JDABuilder.createDefault(token).addEventListeners(new Discord(minecraft))
				.setActivity(Activity.playing(minecraft.getServer().getOnlinePlayers().length + "/"
						+ minecraft.getServer().getMaxPlayers() + " players online"))
				.build();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		if (e.getAuthor().isBot())
			return;
		if (!((TextChannel) e.getChannel()).canTalk())
			return;

		if (!e.getChannel().getId().equals(chId))
			return;

		if (e.getMessage().getContentRaw().equalsIgnoreCase("!list")) {
			String msg = "There are " + minecraft.getServer().getOnlinePlayers().length + "/"
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

		minecraft.getServer()
				.broadcastMessage("<" + ChatColor.valueOf(minecraft.config.getString("username-color").toUpperCase())
						+ (ChatColor.stripColor(e.getAuthor().getName())) + "#" + e.getAuthor().getDiscriminator()
						+ ChatColor.WHITE + ChatColor.stripColor("> " + e.getMessage().getContentDisplay() + attch));
	}

	public static String escapeMarkdown(String text) {
		return text == null ? "" : text.replace("_", "\\_").replace("*", "\\*").replace("~", "\\~");
	}
}
