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

	// is there any better way to do this?
	private Color[] colors = { new Color(0x0), new Color(0xAA), new Color(0xAA00), new Color(0xAAAA),
			new Color(0xAA0000), new Color(0xAA00AA), new Color(0xFFAA00), new Color(0xAAAAAA), new Color(0x555555),
			new Color(0x5555FF), new Color(0x55FF55), new Color(0x55FFFF), new Color(0xFF5555), new Color(0xFF55FF),
			new Color(0xFFFF55), new Color(0xFFFFFF) };

	public Discord(Minecraft minecraft, String token) throws LoginException {
		this.minecraft = minecraft;
		chId = minecraft.config.getString("channel");
		jda = JDABuilder.createDefault(token).addEventListeners(this)
				.setActivity(Activity.playing(minecraft.getServer().getOnlinePlayers().length + "/"
						+ minecraft.getServer().getMaxPlayers() + " players online"))
				.build();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		if (!e.getChannel().getId().equals(chId) || e.getAuthor().isBot() || !((TextChannel) e.getChannel()).canTalk())
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

		String color = null;

		if (minecraft.config.getBoolean("use-role-color-as-username-color") && e.getMember().getColor() != null) {
			color = getClosestColor(e.getMember().getColor());
		} else {
			color = ChatColor.translateAlternateColorCodes('&', minecraft.config.getString("username-color"));
		}

		minecraft.getServer()
				.broadcastMessage("<" + color + (ChatColor.stripColor(e.getAuthor().getName())) + "#"
						+ e.getAuthor().getDiscriminator() + ChatColor.RESET + "> " + e.getMessage().getContentDisplay()
						+ attch);
	}

	private String getClosestColor(Color c) {
		double min = Double.MAX_VALUE;
		int minIndex = -1;

		for (int i = 0; i < colors.length; i++) {
			Color color = colors[i];
			double d = Math.pow((c.getRed() - color.getRed()), 2) + Math.pow((c.getGreen() - color.getGreen()), 2)
					+ Math.pow((c.getBlue() - color.getBlue()), 2);

			if (d < min) {
				if (i == 7 || i == 8 && min - d < 3000 || i == 15 && (c.getRed() != 0xFF || c.getGreen() != 0xFF || c.getBlue() != 0xFF))
					continue;
				min = d;
				minIndex = i;
			}
		}

		return ChatColor.getByChar(String.format("%x", minIndex)).toString();
	}

	public static String escapeMarkdown(String text) {
		return text == null ? "" : text.replace("_", "\\_").replace("*", "\\*").replace("~", "\\~");
	}
}
