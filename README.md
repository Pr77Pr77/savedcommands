# Saved Commands

Saved Commands is a client Minecraft mod for Fabric that lets you save commands and
chat messages for easier access later using keyboard shortcuts and the
saved commands menu.

## Installation

This mod is only supported by the Fabric mod loader. For installation guides regarding
please use the official [Fabric player guides](https://docs.fabricmc.net/players/).

You can download the mod on either [Modrinth](https://modrinth.com/mod/savedcommands) or on [GitHub](https://github.com/Pr77Pr77/savedcommands)
under [releases](https://github.com/Pr77Pr77/savedcommands/releases).
This mod has the following dependencies:

- Required: [Fabric API](https://modrinth.com/mod/fabric-api)
- Optional: [Mod Manu](https://modrinth.com/mod/modmenu)

## Using the mod

To use the mod, open the Saved Commands screen by pressing the keybind 
(defaults to Y on QWERTY and Z on QWERTZ keyboards).

On the Saved Commands screen, you can:

* Add a new command by clicking the "+"-button
* Edit saved commands by clicking on the pen icon on the right. In the appearing popup, you can edit the command,
insert variables, add or edit a name and add or edit the keybind combination (shortcut).
* Delete saved commands by clicking on the trash can icon on the right.
* Send and run commands by clicking on them
* Search for commands by entering the query into the top bar.
* Share saved commands with other players on the server using this mod.
* Change global and world settings by clicking on the sliders icon in the top right corner.

Variables can be used to insert player data, like the position and item id of the held item.
You can also select a data type like string, integer and float, for you to input before sending.
If you don't know these types, use string because it is the least "strict" one.

Custom categories can be enabled in the settings. Turning the related setting on, makes it
possible to create and select categories with custom names from within the edit popup.

Commands can be shared with players on the same server using the mod, which also shares all
command properties like the name, keybind combination and variables, but not custom categories.
When you receive one or more commands, you will get a notification within the game in
the top right corner, saying to press your keybind for opening the saved commands screen
to view or save the commands. There you can pick commands to save separately or bulk save
them. Please note, that commands are being sent unencrypted! This means, that server admins,
your ISP or other involved parties could see the commands you sent or received. The ability
to receive commands can be disabled in the settings.

> We accept no liability for any damages caused by using this mod. Use at your own risk!

## Screenshots

![Saved Commands screen](resources/saved-commands-screen-screenshot.png)
*The Saved Commands screen with some example commands.*

![Search suggestion](resources/search-suggestion-screenshot.png)
*In the top bar you can get command recommendations like in the chat. In this screenshot, custom categories are
enabled.*

![Edit command screen](resources/edit-command-screen-screenshot.png)
*In the edit command screen you can edit the name, command and keybind combination.*

![Edit variable screen](resources/edit-variable-screen-screenshot.png)
*In the edit variable screen you can edit the name, abbreviation, variable type and defult value.*

![Input variable screen](resources/input-variable-screen-screenshot.png)
*Before sending a command or text containing a custom variable, you are prompted to input the values.*

![Share select recepients screen](resources/share-select-recipients.png)
*After clicking share, this screen to select the recipients opens.*

![Share view and save screen](resources/share-view-save.png)
*This screen lets you view, save and edit received commands. The circle shows, if you already have the command saved.*

![Per world settings](resources/settings-world.png)
*There are settings which can be changed per world, as seen in the screenshot, or changed for the whole installation.*

## Working principles

The following things regarding the mod might be counterintuitive:

* The mod separates every world and server from each other. This means, that you might have to set the same command up multiple times.
* If you set a keybind to one that already exists in the Minecraft keybinds, it will block the Minecraft keybind. This only applies to
the first key of a combination or if the next key of the combination is pressed (e.g. Alt + W: Alt is never triggered, W is triggered, if Alt is not pressed)
* If you set two command keybinds to the same key or key combination, the one added first will be executed.

## Reporting issues, getting help and suggesting features

If you have trouble using the mod, if you stumble upon a bug, if you want to suggest a feature to be implemented
or if you would like to have a backport of the mod,
please don't hesitate to post an [issue on GitHub](https://github.com/Pr77Pr77/savedcommands/issues) or open a ticket on the [Discord server](https://discord.gg/wfeM63Jar5).
I will try to respond as soon as possible and maybe ask follow-up questions.
