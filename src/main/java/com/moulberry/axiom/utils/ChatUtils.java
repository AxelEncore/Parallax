package com.moulberry.axiom.utils;

import com.moulberry.axiom.Axiom;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class ChatUtils {
   public static void sendCommand(String command) {
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      if (localPlayer != null) {
         localPlayer.connection.sendUnsignedCommand(command);
      }
   }

   public static void info(Component tip) {
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      if (localPlayer != null) {
         Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Parallax] ").append(tip).withStyle(ChatFormatting.GREEN));
      } else {
         Axiom.LOGGER.info(tip.getString());
      }
   }

   public static void info(String tip) {
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      if (localPlayer != null) {
         Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Parallax] " + tip).withStyle(ChatFormatting.GREEN));
      } else {
         Axiom.LOGGER.info(tip);
      }
   }

   public static void warning(Component component) {
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      if (localPlayer != null) {
         Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Parallax] ").append(component).withStyle(ChatFormatting.YELLOW));
      } else {
         Axiom.LOGGER.warn(component.getString());
      }
   }

   public static void warning(String warning) {
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      if (localPlayer != null) {
         Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Parallax] " + warning).withStyle(ChatFormatting.YELLOW));
      } else {
         Axiom.LOGGER.warn(warning);
      }
   }

   public static void error(Component error) {
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      if (localPlayer != null) {
         Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Parallax] ").append(error).withStyle(ChatFormatting.RED));
      } else {
         Axiom.LOGGER.error(error.getString());
      }
   }

   public static void error(String error) {
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      if (localPlayer != null) {
         Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Parallax] " + error).withStyle(ChatFormatting.RED));
      } else {
         Axiom.LOGGER.error(error);
      }
   }
}
