package com.victoralonso.cubixprofiles.command;

import com.victoralonso.cubixprofiles.CubixProfiles;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.List;

public interface SubCommand {

    void execute(CubixProfiles plugin, CommandSourceStack source, String[] args);

    List<String> suggest(CubixProfiles plugin, CommandSourceStack source, String[] args);
}
